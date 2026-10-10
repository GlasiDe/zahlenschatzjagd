package io.github.glaside.zahlenschatzjagd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.data.Datenbankstart
import io.github.glaside.zahlenschatzjagd.data.Schutzhaltgrund
import io.github.glaside.zahlenschatzjagd.ui.schutzhalt.SchutzhaltScreen
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Der Bildschirm des **Schutzhalts**. Er ist der einzige, den ein Erwachsener in diesem Zustand
 * zu sehen bekommt – auf einem Gerät, an das niemand mit `adb` herankommt. Was darauf steht,
 * muss also wirklich ankommen.
 *
 * Die Texte selbst prüft `SchutzhaltMeldungTest` ohne Gerät; hier geht es darum, dass sie
 * sichtbar **werden** – auch bei Anzeigegröße „groß", wo schon zwei andere Bildschirme
 * unbedienbar waren (`docs/agents/testing.md`).
 */
@RunWith(AndroidJUnit4::class)
class SchutzhaltScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val zaehler = AusgabenZaehler()

    private val wegFehlt = Datenbankstart.Schutzhalt(
        grund = Schutzhaltgrund.WEG_FEHLT,
        gefunden = 3,
        erwartet = 6,
        ursache = "A migration from 3 to 6 was required but not found.",
    )

    @Test
    fun derSchutzhalt_zeigtBeruhigungHandlungUndDieDreiZahlen() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { SchutzhaltScreen(wegFehlt, appVersion = "1.7") }
        }

        composeTestRule.onNodeWithTag("schutzhalt_ueberschrift").assertIsDisplayed()
        composeTestRule.onNodeWithTag("schutzhalt_erklaerung").assertIsDisplayed()
        composeTestRule.onNodeWithTag("schutzhalt_handlung").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("schutzhalt_zahlen").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Gefunden: 3", substring = true).assertIsDisplayed()
    }

    /**
     * Die englische Meldung aus Room bleibt dem Fall vorbehalten, in dem sie etwas beiträgt.
     * Bei einem fehlenden Migrationsweg sagt sie nichts, was die drei Zahlen nicht schon sagen –
     * und unlesbarer Text schreckt Eltern mehr, als er hilft.
     */
    @Test
    fun dieEnglischeMeldung_stehtNurDaWennSieEtwasBeitraegt() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                SchutzhaltScreen(wegFehlt, appVersion = "1.7")
            }
        }

        composeTestRule.onNodeWithTag("schutzhalt_ursache").assertDoesNotExist()
    }

    @Test
    fun beiUnlesbarerDatei_stehtDieTechnischeMeldungDa() {
        val unlesbar = Datenbankstart.Schutzhalt(
            grund = Schutzhaltgrund.NICHT_LESBAR,
            gefunden = 6,
            erwartet = 6,
            ursache = "file is not a database (code 26)",
        )
        composeTestRule.setContent {
            ZahlenschatzjagdTheme { SchutzhaltScreen(unlesbar, appVersion = "1.7") }
        }

        composeTestRule.onNodeWithTag("schutzhalt_ursache").performScrollTo().assertIsDisplayed()
    }

    /**
     * Bei Anzeigegröße „groß" bleiben rund 320 x 640 dp übrig. Der Bildschirm scrollt, also darf
     * auch dort nichts unerreichbar sein.
     */
    @Test
    fun beiKleinerFlaeche_bleibtAllesErreichbar() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                Box(Modifier.size(320.dp, 640.dp)) {
                    SchutzhaltScreen(wegFehlt, appVersion = "1.7")
                }
            }
        }

        composeTestRule.onNodeWithTag("schutzhalt_zahlen").performScrollTo().assertIsDisplayed()
    }

    /**
     * Ella liest die Meldung einmal vor – nicht noch einmal, wenn der Bildschirm nach dem Drehen
     * aus dem gespeicherten Zustand neu entsteht.
     */
    @Test
    fun derSchutzhalt_liestNachDemNeuaufbauNichtNochEinmalVor() {
        val neuaufbau = StateRestorationTester(composeTestRule)
        neuaufbau.setContent {
            ZahlenschatzjagdTheme { SchutzhaltScreen(wegFehlt, appVersion = "1.7") }
        }
        composeTestRule.waitForIdle()
        assertEquals(1, zaehler.saetze.size)

        neuaufbau.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        assertEquals(1, zaehler.saetze.size)
    }
}
