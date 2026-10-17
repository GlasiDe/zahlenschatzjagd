package io.github.glaside.zahlenschatzjagd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.parent.SchalterZeile
import io.github.glaside.zahlenschatzjagd.ui.selection.NachbarzahlenSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.SelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.VerdoppelnSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.ZahlenspieleSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Neben einer Beschriftung reagiert die ganze Zeile, nicht nur das Kästchen oder der Schalter
 * daneben – für junge Kinder ist die Beschriftung das leichtere Ziel (#7).
 *
 * Getippt wird deshalb auf den **Text**, nicht auf die Marke: Die sitzt an der Zeile, und ein
 * Tipp auf ihre Mitte bewiese nichts über die Beschriftung. Den Freigabe-Bildschirm prüft
 * `FreigabeInstrumentedTest`, weil er an der echten Datenbank hängt.
 */
@RunWith(AndroidJUnit4::class)
class GanzeZeileAntippbarInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** Gescrollt wird nur auf den Auswahl-Bildschirmen; die nackte Schalterzeile scrollt nicht. */
    private fun tippeAuf(beschriftung: String, scrollen: Boolean = true) {
        val text = composeTestRule.onNodeWithText(beschriftung, useUnmergedTree = true)
        if (scrollen) text.performScrollTo()
        text.performClick()
    }

    @Test
    fun rechenarten_einTippAufDieBeschriftungHaktAnUndWiederAb() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { SelectionScreen(freigabe = Freigabe.ALLES, onStartGame = {}) }
        }

        tippeAuf("Minus (-)")
        composeTestRule.onNodeWithTag("checkbox_minus").assertIsOn()

        tippeAuf("Minus (-)")
        composeTestRule.onNodeWithTag("checkbox_minus").assertIsOff()
    }

    /**
     * Das Kästchen hat keinen eigenen Klick mehr, liegt aber in der Zeile – ein Tipp darauf
     * schaltet weiter. Es steht am Zeilenanfang, mit seinem 48-dp-Rand also 24 dp nach innen.
     */
    @Test
    fun rechenarten_einTippAufDasKaestchenSelbstHaktWeiterAn() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { SelectionScreen(freigabe = Freigabe.ALLES, onStartGame = {}) }
        }

        composeTestRule.onNodeWithTag("checkbox_minus").performScrollTo()
            .performTouchInput { click(centerLeft + Offset(24.dp.toPx(), 0f)) }

        composeTestRule.onNodeWithTag("checkbox_minus").assertIsOn()
    }

    /** Wie beim Kästchen: Der Schalter am Zeilenende (52 dp breit) schaltet weiter selbst. */
    @Test
    fun nachbarzahlen_einTippAufDenSchalterSelbstSchaltetWeiterUm() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { NachbarzahlenSelectionScreen(freigabe = Freigabe.ALLES, onStartGame = {}) }
        }

        composeTestRule.onNodeWithTag("toggle_difficulty").performScrollTo()
            .performTouchInput { click(centerRight - Offset(26.dp.toPx(), 0f)) }

        composeTestRule.onNodeWithTag("toggle_difficulty").assertIsOn()
    }

    @Test
    fun nachbarzahlen_einTippAufSchwererUebenSchaltetUm() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { NachbarzahlenSelectionScreen(freigabe = Freigabe.ALLES, onStartGame = {}) }
        }
        composeTestRule.onNodeWithTag("toggle_difficulty").assertIsOff()

        tippeAuf("Schwerer üben")

        composeTestRule.onNodeWithTag("toggle_difficulty").assertIsOn()
    }

    @Test
    fun verdoppeln_einTippAufDieBeschriftungSchaltetUm() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { VerdoppelnSelectionScreen(freigabe = Freigabe.ALLES, onStartGame = {}) }
        }
        composeTestRule.onNodeWithTag("toggle_difficulty").assertIsOn()

        tippeAuf("Leicht: Antwort auswählen")

        composeTestRule.onNodeWithTag("toggle_difficulty").assertIsOff()
    }

    @Test
    fun zahlenspiele_einTippAufMitHilfeSchaltetUm() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { ZahlenspieleSelectionScreen(freigabe = Freigabe.ALLES, onStartGame = {}) }
        }
        composeTestRule.onNodeWithTag("toggle_hilfe").assertIsOn()

        tippeAuf("Mit Hilfe")

        composeTestRule.onNodeWithTag("toggle_hilfe").assertIsOff()
        composeTestRule.onNodeWithTag("hilfe_stufe", useUnmergedTree = true).assertTextEquals("Ohne Hilfe")
    }

    /** Die Schalterzeile des Elternbereichs – Ton, Vorlesen und Profilwechsel teilen sie sich. */
    @Test
    fun elternbereich_einTippAufDieBeschriftungSchaltetUm() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                var an by remember { mutableStateOf(false) }
                SchalterZeile(beschriftung = "Töne", an = an, testTag = "schalter", onSetze = { an = it }) {}
            }
        }

        tippeAuf("Töne", scrollen = false)
        composeTestRule.onNodeWithTag("schalter").assertIsOn()

        tippeAuf("Töne", scrollen = false)
        composeTestRule.onNodeWithTag("schalter").assertIsOff()
    }
}
