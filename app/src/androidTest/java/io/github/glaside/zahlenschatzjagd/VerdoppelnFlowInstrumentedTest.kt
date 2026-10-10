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
 * Deckt den Flow des Rechenspiels „Verdoppeln" ab: von der Rechnen-Insel über die
 * Spielauswahl-Szene (`game_verdoppeln`) in die Verdoppeln-Konfiguration und durch eine Runde in
 * beiden Schwierigkeitsgraden – leicht (Auswahl-Buttons) und schwer (Zifferneingabe). Sichert
 * zusätzlich, dass das Abbrechen (✕) zur **Verdoppeln**-Konfiguration zurückführt (nicht zur
 * Grundrechenarten- oder Zahlenspiele-Auswahl).
 */
@RunWith(AndroidJUnit4::class)
class VerdoppelnFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun leichterModus_laesstSichMitAuswahlButtonsDurchspielen() {
        // "Leicht" ist die Vorbelegung des Schwierigkeits-Umschalters, kein Toggle nötig.
        startVerdoppeln()

        val num1 = parseVerdoppelnEquation(readEquationText())
        val correct = num1 * 2
        composeTestRule.onNodeWithTag("answer_option_$correct").performClick()

        // Antippen wählt nur aus: Die Zahl steht in der Aufgabe, ausgewertet ist noch nichts.
        assertTrue("Auswahl nicht sichtbar in: ${readEquationText()}", readEquationText().endsWith("$correct"))
        composeTestRule.onAllNodesWithTag("next_button").assertCountEquals(0)
        composeTestRule.onNodeWithTag("digit_check").performClick()

        // Feedback zeigt die Äquivalenz zu Addition und Multiplikation.
        composeTestRule.onNodeWithTag("verdoppeln_explanation").assertIsDisplayed()

        spieleRestlicheAuswahlRundeDurch(taskCount = 9)

        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    @Test
    fun schwererModus_laesstSichMitZifferneingabeDurchspielen() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_verdoppeln").performClick()
        composeTestRule.onNodeWithTag("toggle_difficulty").performClick() // schaltet auf "schwer"
        composeTestRule.onNodeWithTag("start_button").performClick()

        repeat(10) {
            val num1 = parseVerdoppelnEquation(readEquationText())
            val typed = (num1 * 2).toString()
            typed.forEach { digit ->
                composeTestRule.onNodeWithTag("digit_$digit").performClick()
            }
            // Regression: die eingetippten Ziffern müssen sichtbar sein (nicht nur intern im State).
            assertTrue("Eingetippte Ziffern nicht sichtbar in: ${readEquationText()}", readEquationText().endsWith(typed))
            composeTestRule.onNodeWithTag("digit_check").performClick()
            composeTestRule.onNodeWithTag("next_button").performClick()
        }

        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    @Test
    fun spielAbbrechen_kehrtZurVerdoppelnAuswahlZurueck() {
        startVerdoppeln()

        composeTestRule.onNodeWithTag("game_cancel_button").performClick()

        // Der Zurück-Button der Verdoppeln-Konfiguration ist deren eindeutiges Merkmal.
        composeTestRule.onNodeWithTag("verdoppeln_back_button").assertIsDisplayed()
    }

    /** Öffnet die Rechnen-Insel, wählt „Verdoppeln" und startet eine Runde im leichten Modus. */
    private fun startVerdoppeln() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_verdoppeln").performClick()
        composeTestRule.onNodeWithTag("start_button").performClick()
    }

    /** Beantwortet die restlichen Aufgaben einer 10er-Runde im Auswahl-Modus korrekt. */
    private fun spieleRestlicheAuswahlRundeDurch(taskCount: Int) {
        composeTestRule.onNodeWithTag("next_button").performClick()
        repeat(taskCount) {
            val num1 = parseVerdoppelnEquation(readEquationText())
            val correct = num1 * 2
            composeTestRule.onNodeWithTag("answer_option_$correct").performClick()
            composeTestRule.onNodeWithTag("digit_check").performClick()
            composeTestRule.onNodeWithTag("next_button").performClick()
        }
    }

    private fun readEquationText(): String {
        val node = composeTestRule.onNodeWithTag("equation_text").fetchSemanticsNode()
        return node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
    }

    /**
     * Zerlegt die Verdoppeln-Kachel ("Verdoppeln" + Zahl, per `mergeDescendants` zu einem
     * Text-Knoten zusammengefasst, siehe `GameScreen.VerdoppelnPrompt`) in die Ausgangszahl.
     */
    private fun parseVerdoppelnEquation(text: String): Int {
        val match = Regex("""Verdoppeln(\d+)""").find(text)
            ?: error("Unerwartetes Aufgabenformat: $text")
        return match.groupValues[1].toInt()
    }
}
