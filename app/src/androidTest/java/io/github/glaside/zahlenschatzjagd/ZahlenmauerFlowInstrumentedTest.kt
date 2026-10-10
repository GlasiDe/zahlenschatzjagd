package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deckt den Weg der Übungsform **Zahlenmauer** ab: Startkarte → Rechnen-Insel → Kachel → Auswahl
 * der Schwierigkeitsstufe → Lücken füllen → ✓ → nächste Mauer → Auswertung. Vorbild ist
 * `NachbarzahlenFlowInstrumentedTest`.
 *
 * Abgegeben wird mit dem ✓ des gemeinsamen Ziffernblocks. Ob die Mauer richtig gelöst ist,
 * spielt für den Ablauf keine Rolle – der Test füllt jede Lücke mit derselben Ziffer. Die Wertung
 * selbst prüft `ZahlenmauerViewModelTest`.
 *
 * Die Ultra-Stufe läuft eigens durch: Mit fünf Etagen und Anzeigegröße „groß" rutschte der
 * Ziffernblock schon einmal aus dem Bild (Kommentar in `ZahlenmauerScreen`). Jedes Tippen
 * scrollt deshalb vorher zu seinem Ziel.
 */
@RunWith(AndroidJUnit4::class)
class ZahlenmauerFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun ganzeRunde_laesstSichMitDemHakenDurchspielen() {
        starteZahlenmauer("LEICHT")

        composeTestRule.onAllNodesWithText("Prüfen").assertCountEquals(0)
        spieleRundeDurch()

        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    @Test
    fun ultraStufe_laesstSichBeiGrosserAnzeigeDurchspielen() {
        starteZahlenmauer("ULTRA")

        // Ohne Scrollen: Der ✓ steht schon beim Start der höchsten Mauer im Bild. Das
        // Scrollen in [tippe] ist nur das Sicherheitsnetz des Bildschirms für noch größere Anzeigen.
        composeTestRule.onNodeWithTag("digit_check").assertIsDisplayed()

        spieleRundeDurch()

        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    /** Der ✓ ist nie gesperrt, wertet aber erst, wenn jede Lücke gefüllt ist. */
    @Test
    fun derHakenWertetErst_wennAlleLueckenGefuelltSind() {
        starteZahlenmauer("LEICHT")
        val luecken = lueckenTags()
        assertTrue("Eine Mauer hat mindestens zwei Lücken", luecken.size >= 2)

        fuelle(luecken.first())
        tippe("digit_check")
        composeTestRule.onAllNodesWithTag("zahlenmauer_result_text").assertCountEquals(0)

        luecken.drop(1).forEach(::fuelle)
        tippe("digit_check")
        composeTestRule.onNodeWithTag("zahlenmauer_result_text").assertIsDisplayed()
    }

    @Test
    fun spielAbbrechen_kehrtZurZahlenmauerAuswahlZurueck() {
        starteZahlenmauer("LEICHT")

        composeTestRule.onNodeWithTag("zahlenmauer_cancel_button").performClick()

        // Die Auswahl kehrt an ihre alte Scrollposition zurück – beim Start wurde zu „Los geht's!"
        // hinuntergescrollt, der Zurück-Knopf oben liegt dann außerhalb des Bildes.
        composeTestRule.onNodeWithTag("zahlenmauer_back_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun andereAufgaben_kehrtZurZahlenmauerAuswahlZurueck() {
        starteZahlenmauer("LEICHT")
        spieleRundeDurch()

        composeTestRule.onNodeWithTag("back_to_selection_button").performClick()

        composeTestRule.onNodeWithTag("zahlenmauer_back_button").performScrollTo().assertIsDisplayed()
    }

    /** Öffnet die Rechnen-Insel, wählt die Zahlenmauer in [stufe] und startet eine Runde. */
    private fun starteZahlenmauer(stufe: String) {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_zahlenmauer").performClick()
        composeTestRule.onNodeWithTag("difficulty_$stufe").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("start_button").performScrollTo().performClick()
    }

    /** Füllt Mauer um Mauer, gibt mit ✓ ab und geht weiter, bis die Auswertung steht. */
    private fun spieleRundeDurch() {
        repeat(MAX_MAUERN) {
            if (composeTestRule.onAllNodesWithTag("back_to_selection_button").fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            lueckenTags().forEach(::fuelle)
            tippe("digit_check")
            tippe("zahlenmauer_next_button")
            composeTestRule.waitForIdle()
        }
        error("Die Runde endete nicht nach $MAX_MAUERN Mauern")
    }

    /** Die Testmarken aller Lücken der aktuellen Mauer – nur Lücken sind antippbar. */
    private fun lueckenTags(): List<String> =
        composeTestRule.onAllNodes(istZelle and hasClickAction())
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }

    /** Tippt die Lücke [zellenTag] an und gibt eine Ziffer ein. */
    private fun fuelle(zellenTag: String) {
        tippe(zellenTag)
        tippe("digit_1")
    }

    private fun tippe(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    private companion object {
        /** Mehr Mauern lässt die Auswahl nicht zu (Regler 3–10). */
        const val MAX_MAUERN = 10

        val istZelle = SemanticsMatcher("ist eine Zelle der Zahlenmauer") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("zahlenmauer_cell_") == true
        }
    }
}
