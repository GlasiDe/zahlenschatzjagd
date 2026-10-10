package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deckt den Zahlenspiele-Flow („verliebte Zahlen") ab und sichert insbesondere die
 * Rücksprünge ab: Sowohl das Abbrechen (✕) im Spiel als auch „Andere Aufgaben" auf dem
 * Auswertungs-Bildschirm müssen zur **Zahlenspiele-Auswahl** zurückführen – nicht zur
 * Rechnen-Auswahl (das war der Bug: fest verdrahtetes `popBackStack(SELECTION)`).
 */
@RunWith(AndroidJUnit4::class)
class ZahlenspieleFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun spielAbbrechen_kehrtZurZahlenspieleAuswahlZurueck() {
        startZahlenspiele()

        composeTestRule.onNodeWithTag("game_cancel_button").performClick()

        // Der Hilfe-Schalter ist das eindeutige Merkmal der Zahlenspiele-Auswahl.
        composeTestRule.onNodeWithTag("toggle_hilfe").assertIsDisplayed()
    }

    @Test
    fun andereAufgaben_kehrtZurZahlenspieleAuswahlZurueck() {
        startZahlenspiele()
        spieleRundeDurch(taskCount = 10)

        composeTestRule.onNodeWithTag("back_to_selection_button").performClick()

        composeTestRule.onNodeWithTag("toggle_hilfe").assertIsDisplayed()
    }

    /**
     * Öffnet die Zahlenspiele-Insel, wählt in der Spielauswahl-Szene „Verliebte Zahlen" und
     * startet eine Runde. Seit der Spielauswahl liegt zwischen Insel und Konfiguration der
     * Menü-Schritt (`game_verliebte`).
     */
    private fun startZahlenspiele() {
        composeTestRule.onNodeWithTag("island_zahlenspiele").performClick()
        composeTestRule.onNodeWithTag("game_verliebte").performClick()
        composeTestRule.onNodeWithTag("start_button").performClick()
    }

    /** Beantwortet [taskCount] „verliebte Zahlen"-Aufgaben korrekt bis zur Auswertung. */
    private fun spieleRundeDurch(taskCount: Int) {
        repeat(taskCount) {
            val (known, target) = parseVerliebteEquation(readEquationText())
            (target - known).toString().forEach { digit ->
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

    /**
     * Zerlegt „num1 + ? = 10" **oder** „? + num1 = 10" (die Lücke kann seit der Variation der
     * Lückenposition vorne oder hinten stehen) in (bekannter Summand, Zielzahl).
     */
    private fun parseVerliebteEquation(text: String): Pair<Int, Int> {
        val match = Regex("""(\d+|\?) \+ (\d+|\?) = (\d+)""").find(text)
            ?: error("Unerwartetes Aufgabenformat: $text")
        val (first, second, target) = match.destructured
        val known = (if (first != "?") first else second).toInt()
        return known to target.toInt()
    }
}
