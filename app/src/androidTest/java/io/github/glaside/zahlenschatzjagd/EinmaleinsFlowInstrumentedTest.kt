package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deckt den Weg der Übungsform **Einmaleins** ab: Startkarte → Rechnen-Insel → Kachel → Auswahl →
 * Runde → Auswertung, in beiden Stufen (Vorbild: `NachbarzahlenFlowInstrumentedTest`). Dazu: Keine
 * Reihe ist vorgewählt, ein falsches Kästchen beim Aufsagen zeigt die richtige Zahl, und das
 * Abbrechen führt zur Einmaleins-Auswahl zurück.
 */
@RunWith(AndroidJUnit4::class)
class EinmaleinsFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun keineReiheIstVorgewaehlt_undOhneReiheStartetNichts() {
        oeffneEinmaleins()

        composeTestRule.onNodeWithTag("einmaleins_reihe_2").performScrollTo().assertIsOff()
        composeTestRule.onNodeWithTag("start_button").performScrollTo().assertIsNotEnabled()

        composeTestRule.onNodeWithTag("einmaleins_reihe_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("start_button").performScrollTo().assertIsEnabled()
    }

    @Test
    fun aufsagen_ganzeReiheLaesstSichDurchspielen_einFehlerZeigtDieRichtigeZahl() {
        oeffneEinmaleins()
        composeTestRule.onNodeWithTag("einmaleins_reihe_3").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("start_button").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("aufsagen_reihe_text").assertTextEquals("3er-Reihe")
        tippe(3)
        tippe(7) // bewusst falsch statt 6
        assertEquals("6", zahlImKaestchen(1))
        (3..10).forEach { tippe(it * 3) }

        composeTestRule.onNodeWithTag("aufsagen_ergebnis_text").assertTextEquals("9 von 10 richtig")
        composeTestRule.onNodeWithTag("einmaleins_next_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    @Test
    fun durcheinander_rundeLaesstSichDurchspielen_undStelltNurAufgabenDerGewaehltenReihe() {
        oeffneEinmaleins()
        composeTestRule.onNodeWithText("Durcheinander").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("einmaleins_reihe_7").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("start_button").performScrollTo().performClick()

        repeat(DEFAULT_AUFGABENZAHL) {
            val (faktor, reihe) = aktuelleAufgabe()
            assertEquals(7, reihe)
            assertTrue(faktor in 1..10)
            tippe(faktor * reihe)
            composeTestRule.onNodeWithTag("einmaleins_next_button").performScrollTo().performClick()
        }

        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    @Test
    fun abbrechen_kehrtZurEinmaleinsAuswahlZurueck_ohneGemerkteWahl() {
        oeffneEinmaleins()
        composeTestRule.onNodeWithTag("einmaleins_reihe_5").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("start_button").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("einmaleins_cancel_button").performClick()

        composeTestRule.onNodeWithTag("einmaleins_back_button").assertIsDisplayed()
        // Gemerkt wird nichts – wie bei den Grundrechenarten.
        composeTestRule.onNodeWithTag("einmaleins_reihe_5").performScrollTo().assertIsOff()
    }

    @Test
    fun dieAbzeichenStehenAlsRasterReiheMalStufe() {
        composeTestRule.onNodeWithTag("island_abzeichen").performClick()
        composeTestRule.onNodeWithTag("badge_tab_EINMALEINS").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("badge_truhe_EINMALEINS_2_AUFSAGEN").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("badge_truhe_EINMALEINS_10_DURCHEINANDER").fetchSemanticsNodes()
            .let { assertEquals(1, it.size) }
    }

    private fun oeffneEinmaleins() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_einmaleins").performClick()
    }

    /** Gibt [wert] über den Ziffernblock ein und gibt mit ✓ ab. */
    private fun tippe(wert: Int) {
        composeTestRule.onNodeWithTag("digit_clear").performScrollTo().performClick()
        wert.toString().forEach { ziffer ->
            composeTestRule.onNodeWithTag("digit_$ziffer").performScrollTo().performClick()
        }
        composeTestRule.onNodeWithTag("digit_check").performScrollTo().performClick()
    }

    private fun zahlImKaestchen(index: Int): String = text("aufsagen_zahl_$index")

    /** Liest `8 · 7 = ?` vom Bildschirm und liefert Faktor und Reihe. */
    private fun aktuelleAufgabe(): Pair<Int, Int> {
        val (faktor, rest) = text("durcheinander_aufgabe").split(" · ")
        return faktor.trim().toInt() to rest.substringBefore(" =").trim().toInt()
    }

    private fun text(tag: String): String =
        composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }.trim()

    private companion object {
        /** Die Voreinstellung des Aufgabenzahl-Reglers. */
        const val DEFAULT_AUFGABENZAHL = 10
    }
}
