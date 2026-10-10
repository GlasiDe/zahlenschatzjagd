package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deckt den Flow des Zahlenspiels „Zahlen vergleichen" ab: von der Zahlenspiele-Insel über die
 * Spielauswahl-Szene (`game_vergleich`) in die Vergleich-Konfiguration und durch eine Runde mit
 * den Antwort-Tasten `<`/`=`/`>`. Sichert zusätzlich, dass das Abbrechen (✕) zur **Vergleich**-
 * Konfiguration zurückführt (nicht zur Verliebte- oder Rechnen-Auswahl).
 */
@RunWith(AndroidJUnit4::class)
class VergleichenFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun ganzeRunde_laesstSichDurchspielen() {
        startVergleichen()
        spieleRundeDurch(taskCount = 10)

        // Nach der letzten Aufgabe steht der Auswertungs-Bildschirm mit „Andere Aufgaben".
        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    /**
     * Ein Tippen auf ein Zeichen wählt nur aus: Es steht dann in der Lücke, ausgewertet wird
     * erst mit ✓, und bis dahin lässt es sich noch ändern.
     */
    @Test
    fun einZeichenWirdErstMitDemHakenAbgegeben() {
        startVergleichen()

        composeTestRule.onNodeWithTag("symbol_lt").performClick()
        assertTrue(readEquationText().contains("<"))
        composeTestRule.onNodeWithTag("symbol_gt").performClick()
        assertTrue(readEquationText().contains(">"))
        composeTestRule.onAllNodesWithTag("next_button").assertCountEquals(0)

        composeTestRule.onNodeWithTag("digit_check").performClick()

        composeTestRule.onNodeWithTag("next_button").assertIsDisplayed()
    }

    @Test
    fun spielAbbrechen_kehrtZurVergleichAuswahlZurueck() {
        startVergleichen()

        composeTestRule.onNodeWithTag("game_cancel_button").performClick()

        // Der Zurück-Button der Vergleich-Konfiguration ist deren eindeutiges Merkmal.
        composeTestRule.onNodeWithTag("vergleichen_back_button").assertIsDisplayed()
    }

    /** Öffnet die Zahlenspiele-Insel, wählt „Zahlen vergleichen" und startet eine Runde. */
    private fun startVergleichen() {
        composeTestRule.onNodeWithTag("island_zahlenspiele").performClick()
        composeTestRule.onNodeWithTag("game_vergleich").performClick()
        composeTestRule.onNodeWithTag("start_button").performClick()
    }

    /** Beantwortet [taskCount] Vergleichsaufgaben korrekt bis zur Auswertung. */
    private fun spieleRundeDurch(taskCount: Int) {
        repeat(taskCount) {
            val (a, b) = parseVergleichEquation(readEquationText())
            val tag = when {
                a > b -> "symbol_gt"
                a < b -> "symbol_lt"
                else -> "symbol_eq"
            }
            composeTestRule.onNodeWithTag(tag).performClick()
            composeTestRule.onNodeWithTag("digit_check").performClick()
            composeTestRule.onNodeWithTag("next_button").performClick()
        }
    }

    private fun readEquationText(): String {
        val node = composeTestRule.onNodeWithTag("equation_text").fetchSemanticsNode()
        return node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
    }

    /** Zerlegt „num1 ? num2" (bzw. „num1 < num2") in die beiden Zahlen. */
    private fun parseVergleichEquation(text: String): Pair<Int, Int> {
        val match = Regex("""(\d+)\s+\S+\s+(\d+)""").find(text)
            ?: error("Unerwartetes Aufgabenformat: $text")
        val (a, b) = match.destructured
        return a.toInt() to b.toInt()
    }
}
