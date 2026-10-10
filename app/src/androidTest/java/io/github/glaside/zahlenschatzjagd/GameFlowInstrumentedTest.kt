package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Deckt die wichtigsten UI-Abläufe ab: Auswahlbildschirm -> Spiel -> Auswertung.
// Nutzt die Standardeinstellungen (nur "Plus", Zahlenraum 0-10, 10 Aufgaben),
// damit die erwartete Lösung direkt aus dem angezeigten Aufgabentext berechnet
// werden kann, unabhängig von der zufälligen Aufgaben-Generierung.
@RunWith(AndroidJUnit4::class)
class GameFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun selectionScreen_startButtonEnabledByDefault() {
        goToSelection()
        composeTestRule.onNodeWithTag("start_button").assertIsEnabled()
    }

    @Test
    fun selectionScreen_startButtonDisabledWhenNoOperationSelected() {
        goToSelection()
        // Standardmäßig ist nur "Plus" angehakt -> abwählen
        composeTestRule.onNodeWithTag("checkbox_plus").performClick()
        composeTestRule.onNodeWithTag("start_button").assertIsNotEnabled()
    }

    @Test
    fun fullGameFlow_answeringEveryTaskCorrectly_showsPerfectStats() {
        goToSelection()
        composeTestRule.onNodeWithTag("start_button").performClick()

        val taskCount = 10

        repeat(taskCount) {
            val equationText = readEquationText()
            val (num1, num2) = parsePlusEquation(equationText)
            val answer = (num1 + num2).toString()

            answer.forEach { digit ->
                composeTestRule.onNodeWithTag("digit_$digit").performClick()
            }
            composeTestRule.onNodeWithTag("digit_check").performClick()
            composeTestRule.onNodeWithTag("next_button").performClick()
        }

        composeTestRule.onNodeWithTag("stats_result_text")
            .assertTextEquals("Du hast $taskCount von $taskCount richtig!")
    }

    @Test
    fun statsScreen_backToSelection_returnsToSelectionScreen() {
        playFullGameCorrectly()

        composeTestRule.onNodeWithTag("back_to_selection_button").performClick()

        // Zurück auf dem Auswahlbildschirm: Start-Button ist wieder da und aktiv.
        composeTestRule.onNodeWithTag("start_button").assertIsEnabled()
    }

    @Test
    fun statsScreen_playAgain_startsNewGame() {
        playFullGameCorrectly()

        composeTestRule.onNodeWithTag("play_again_button").performClick()

        // Neue Runde: es wird wieder eine Aufgabe angezeigt.
        composeTestRule.onNodeWithTag("equation_text").assertExists()
    }

    /**
     * Öffnet von der Startkarte aus die Rechnen-Insel und tippt dort in der Zwischenseite auf
     * „Grundrechenarten", um zum Auswahlbildschirm (Plus/Minus/Mal/Geteilt) zu gelangen.
     */
    private fun goToSelection() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_grundrechenarten").performClick()
    }

    /** Spielt eine komplette Runde mit korrekten Antworten bis zum Statistik-Bildschirm. */
    private fun playFullGameCorrectly(taskCount: Int = 10) {
        goToSelection()
        composeTestRule.onNodeWithTag("start_button").performClick()
        repeat(taskCount) {
            val (num1, num2) = parsePlusEquation(readEquationText())
            (num1 + num2).toString().forEach { digit ->
                composeTestRule.onNodeWithTag("digit_$digit").performClick()
            }
            composeTestRule.onNodeWithTag("digit_check").performClick()
            composeTestRule.onNodeWithTag("next_button").performClick()
        }
    }

    private fun readEquationText(): String {
        val node = composeTestRule.onNodeWithTag("equation_text").fetchSemanticsNode()
        return node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
    }

    private fun parsePlusEquation(text: String): Pair<Int, Int> {
        val match = Regex("""(\d+) \+ (\d+) = .*""").find(text)
            ?: error("Unerwartetes Aufgabenformat: $text")
        val (a, b) = match.destructured
        return a.toInt() to b.toInt()
    }
}
