package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.ui.HOME_WELCOME_SPOKEN
import io.github.glaside.zahlenschatzjagd.ui.Narrator
import io.github.glaside.zahlenschatzjagd.ui.SpeakAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Die beiden Schalter aus dem Elternbereich, vom Umlegen bis zur Wirkung.
 *
 * Was hier **nicht** geprüft wird, ist das hörbare Ergebnis: Ob wirklich ein Ton aus dem
 * Lautsprecher kommt und ob eine Stimme spricht, hängt vom Gerät ab (`docs/agents/testing.md`).
 * Geprüft wird der ganze Weg davor – Schalter → DataStore → [SoundPlayer] bzw. [Narrator] –,
 * denn genau dort kann er reißen, ohne dass irgendetwas sichtbar kaputtgeht.
 *
 * Die Schalter liegen im echten DataStore der App und reichen sonst in andere Testklassen
 * hinein; deshalb werden sie vor **und** nach jedem Test auf die Voreinstellung gestellt, und die
 * Activity startet über [ActivityScenario.launch], damit das vor dem ersten Compose-Frame greift.
 */
@RunWith(AndroidJUnit4::class)
class TonUndVorlesenInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        clearParentPin()
        setzeTonUndVorlesen(tonAn = true, vorlesenAn = true)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.ensureActiveProfileExists()
    }

    @After
    fun tearDown() {
        scenario?.close()
        clearParentPin()
        setzeTonUndVorlesen(tonAn = true, vorlesenAn = true)
        // Beide sind prozessweit: Ohne das Zurückstellen liefe eine Stummschaltung in die
        // nächste Testklasse hinein.
        SoundPlayer.setzeTonAn(true)
        Narrator.setzeVorlesenAn(true)
    }

    @Test
    fun derEintragZeigtBeideSchalterEingeschaltet() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneTonUndVorlesen()

        composeTestRule.onNodeWithTag("ton_switch").performScrollTo().assertIsDisplayed().assertIsOn()
        composeTestRule.onNodeWithTag("vorlesen_switch").performScrollTo().assertIsDisplayed()
            .assertIsOn()
    }

    /**
     * Der Kern des Vorhabens: Wer den Ton abschaltet, übt sofort still weiter – ohne die App neu
     * zu starten. Genau daran hinge es, wenn der Schalter nur beim Start gelesen würde; im
     * Wartezimmer legt man ihn um und spielt weiter.
     *
     * **Wie „während einer Runde" hier zu lesen ist:** Der Elternbereich ist nur von der
     * Startkarte aus erreichbar, eine laufende Runde also nicht zu verlassen und wieder zu
     * betreten. Geprüft wird deshalb der nächstliegende echte Fall – der Schalter wird umgelegt,
     * die Runde beginnt, und die Prüfung steht **mitten in ihr**: nach der beantworteten Aufgabe,
     * vor der nächsten.
     */
    @Test
    fun ausgeschalteterTonMachtDieRundeStumm() {
        assertNotNull(
            "Vor dem Umlegen muss der Ton der richtigen Antwort spielen",
            SoundPlayer.zuSpielendeRessource(GameSound.CORRECT),
        )

        schalteTonAus()
        starteRundeUndBeantworteErsteAufgabe()

        assertNull(
            "Die laufende Runde beklingt die richtige Antwort trotz ausgeschaltetem Ton",
            SoundPlayer.zuSpielendeRessource(GameSound.CORRECT),
        )
        // Die Runde läuft normal weiter, sie ist nur still: Die nächste Aufgabe kommt.
        composeTestRule.onNodeWithTag("next_button").performClick()
        composeTestRule.onNodeWithTag("equation_text").assertIsDisplayed()
    }

    @Test
    fun ausgeschalteteVorlesefunktionLaesstEllaSchweigen() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneTonUndVorlesen()
        composeTestRule.onNodeWithTag("vorlesen_switch").performScrollTo().performClick()
        composeTestRule.warteBis { Narrator.naechsteAktion(HOME_WELCOME_SPOKEN) == SpeakAction.DROP }

        composeTestRule.verlasseTonUndVorlesen()
        composeTestRule.verlasseDenElternbereich()

        assertEquals(
            "Die Begrüßung der Startkarte wird trotz ausgeschalteter Vorlesefunktion gesprochen",
            SpeakAction.DROP,
            Narrator.naechsteAktion(HOME_WELCOME_SPOKEN),
        )
    }

    @Test
    fun wiederEinschaltenGibtBeidesZurueck() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneTonUndVorlesen()

        composeTestRule.onNodeWithTag("ton_switch").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("vorlesen_switch").performScrollTo().performClick()
        composeTestRule.warteBis { SoundPlayer.zuSpielendeRessource(GameSound.CORRECT) == null }
        composeTestRule.onNodeWithTag("ton_switch").performScrollTo().assertIsOff()
        composeTestRule.onNodeWithTag("vorlesen_switch").performScrollTo().assertIsOff()

        composeTestRule.onNodeWithTag("ton_switch").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("vorlesen_switch").performScrollTo().performClick()

        composeTestRule.warteBis { SoundPlayer.zuSpielendeRessource(GameSound.CORRECT) != null }
        composeTestRule.warteBis {
            Narrator.naechsteAktion(HOME_WELCOME_SPOKEN) != SpeakAction.DROP
        }
    }

    /** Gerätegebunden heißt auch: Der Schalter überlebt das Schließen der App. */
    @Test
    fun derSchalterUeberlebtDenNeustartDerApp() {
        schalteTonAus()

        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneTonUndVorlesen()
        composeTestRule.onNodeWithTag("ton_switch").performScrollTo().assertIsOff()
    }

    /**
     * Legt den Ton-Schalter um und wartet, bis er beim [SoundPlayer] angekommen ist – der Weg
     * über den DataStore ist asynchron. Führt anschließend zurück auf die Startkarte.
     */
    private fun schalteTonAus() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneTonUndVorlesen()
        composeTestRule.onNodeWithTag("ton_switch").performScrollTo().performClick()
        composeTestRule.warteBis { SoundPlayer.zuSpielendeRessource(GameSound.CORRECT) == null }

        composeTestRule.verlasseTonUndVorlesen()
        composeTestRule.verlasseDenElternbereich()
    }

    /**
     * Startet eine Runde „Plus" und beantwortet die erste Aufgabe richtig. Hält **vor** dem
     * Weitertippen an: Genau dort läuft die Runde noch und der Ton wäre gerade gefallen.
     */
    private fun starteRundeUndBeantworteErsteAufgabe() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_grundrechenarten").performClick()
        composeTestRule.onNodeWithTag("start_button").performClick()
        composeTestRule.warteAuf("equation_text")

        val node = composeTestRule.onNodeWithTag("equation_text").fetchSemanticsNode()
        val text = node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val match = Regex("""(\d+) \+ (\d+) = .*""").find(text)
            ?: error("Unerwartetes Aufgabenformat: $text")
        val (a, b) = match.destructured
        (a.toInt() + b.toInt()).toString().forEach { ziffer ->
            composeTestRule.onNodeWithTag("digit_$ziffer").performClick()
        }
        composeTestRule.onNodeWithTag("digit_check").performClick()
        composeTestRule.warteAuf("next_button")
    }
}
