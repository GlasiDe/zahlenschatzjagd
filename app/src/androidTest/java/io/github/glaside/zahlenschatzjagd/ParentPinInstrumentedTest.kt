package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Der Ablauf aus `docs/adr/0016-pin-optional-neben-der-elternfrage.md`: Elternfrage → PIN
 * vergeben → beim nächsten Mal fragt die PIN → vergessen → Sicherheitsfrage → neue PIN.
 *
 * Die PIN liegt im echten DataStore der App und überlebt sonst in andere Testklassen hinein –
 * deshalb wird sie vor **und** nach jedem Test entfernt, und die Activity startet über
 * [ActivityScenario.launch], damit das Aufräumen vor dem ersten Compose-Frame greift.
 */
@RunWith(AndroidJUnit4::class)
class ParentPinInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val frage = "Wie hieß meine erste Chefin?"

    @Before
    fun setUp() {
        clearParentPin()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.ensureActiveProfileExists()
    }

    @After
    fun tearDown() {
        scenario?.close()
        clearParentPin()
    }

    @Test
    fun elternInsel_fuehrtErstNachBestandenerElternfrageZumElternbereich() {
        composeTestRule.onNodeWithTag("island_eltern").performClick()

        // Zuerst erscheint die Elternfrage, nicht direkt der Elternbereich.
        composeTestRule.warteAuf("adult_gate_question")
        composeTestRule.onAllNodesWithTag("parent_back_button").assertCountEquals(0)

        composeTestRule.loeseDieElternfrage()

        composeTestRule.onNodeWithTag("parent_back_button").assertIsDisplayed()
    }

    @Test
    fun uebersicht_zeigtAlleEintraege() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()

        listOf(
            "parent_entry_zugang",
            "parent_entry_uebungsstand",
            "parent_entry_uebungskalender",
            "parent_entry_freigabe",
            "parent_entry_ton",
            "parent_entry_sicherung",
        ).forEach { tag ->
            composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun ohnePin_bietetDerZugangNurDasEinrichtenAn() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneZugang()

        // Ohne gesetzte PIN bietet der Zugang nur das Einrichten an, kein Entfernen.
        composeTestRule.onNodeWithTag("parent_pin_manage_button").performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("parent_pin_remove_button").assertCountEquals(0)
    }

    @Test
    fun nachDemVergeben_fragtDiePinStattDerElternfrage() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.vergebePin(pin = "4711", frage = frage, antwort = "Meier")

        composeTestRule.verlasseDenElternbereich()
        composeTestRule.onNodeWithTag("island_eltern").performClick()

        composeTestRule.warteAuf("parent_pin_input")
        composeTestRule.onAllNodesWithTag("adult_gate_question").assertCountEquals(0)
    }

    @Test
    fun falschePin_sperrtNicht_undDerRueckwegFuehrtZuEinerNeuenPin() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.vergebePin(pin = "4711", frage = frage, antwort = "Meier")
        composeTestRule.verlasseDenElternbereich()
        composeTestRule.onNodeWithTag("island_eltern").performClick()
        composeTestRule.warteAuf("parent_pin_input")

        // Mehrfach daneben: Es darf weder eine Sperre noch eine Wartezeit geben.
        repeat(3) {
            composeTestRule.onNodeWithTag("parent_pin_input").performTextInput("0000")
            composeTestRule.onNodeWithTag("parent_pin_submit").performClick()
            composeTestRule.onNodeWithTag("parent_pin_error").assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("parent_pin_submit").assertIsDisplayed()

        composeTestRule.onNodeWithTag("parent_pin_forgot").performClick()
        composeTestRule.onNodeWithText(frage).assertIsDisplayed()

        // Andere Schreibweise, zusätzliche Leerzeichen – die Rettung darf daran nicht scheitern.
        composeTestRule.onNodeWithTag("parent_security_answer_input").performTextInput("  meier ")
        composeTestRule.onNodeWithTag("parent_security_answer_submit").performClick()

        // Richtige Antwort führt zum Vergeben einer neuen PIN, nie zum Anzeigen der alten.
        composeTestRule.warteAuf("parent_pin_new_input")
        composeTestRule.onNodeWithText("PIN ändern").assertIsDisplayed()
        // Die vorhandene Frage steht schon da – sie wird geändert, nicht neu erfunden.
        composeTestRule.onNodeWithTag("parent_pin_question_input").performScrollTo()
            .assertTextContains(frage)
        composeTestRule.speicherePin(pin = "1234", frage = "", antwort = "Meier")

        // Auch der Weg über die Sicherheitsfrage endet in der Übersicht – wie der aus dem Zugang.
        composeTestRule.warteAuf("parent_entry_zugang")
    }

    /**
     * Die Sicherheitsfrage anzufassen ist beim Ändern freiwillig: Wer sie stehen lässt und das
     * Antwortfeld leer, behält seinen bisherigen Rückweg – und die neue PIN gilt trotzdem.
     */
    @Test
    fun pinAendern_ohneDieFrageAnzufassen_behaeltDenAltenRueckweg() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.vergebePin(pin = "4711", frage = frage, antwort = "Meier")

        composeTestRule.oeffneZugang()
        composeTestRule.onNodeWithTag("parent_pin_manage_button").performScrollTo().performClick()
        composeTestRule.warteAuf("parent_pin_new_input")

        // ADR-0016 verlangt den Hinweis auf dem Einrichtungsbildschirm – auch beim Ändern, denn
        // hierher führt der Weg nach dem Rücksetzen, wo eine neue Frage formuliert wird.
        composeTestRule.onNodeWithTag("parent_pin_setup_hint").performScrollTo()
            .assertTextContains("dein Kind nicht weiß", substring = true)

        composeTestRule.onNodeWithTag("parent_pin_new_input").performScrollTo()
            .performTextInput("1234")
        composeTestRule.onNodeWithTag("parent_pin_repeat_input").performScrollTo()
            .performTextInput("1234")
        // Frage und Antwort bleiben unangetastet.
        composeTestRule.onNodeWithTag("parent_pin_save_button").performScrollTo().performClick()
        composeTestRule.warteAuf("parent_entry_zugang")

        composeTestRule.verlasseDenElternbereich()
        composeTestRule.onNodeWithTag("island_eltern").performClick()
        composeTestRule.warteAuf("parent_pin_input")

        // Die neue PIN gilt …
        composeTestRule.onNodeWithTag("parent_pin_input").performTextInput("1234")
        composeTestRule.onNodeWithTag("parent_pin_submit").performClick()
        composeTestRule.warteAuf("parent_entry_sicherung")

        // … und die alte Sicherheitsfrage steht noch.
        composeTestRule.verlasseDenElternbereich()
        composeTestRule.onNodeWithTag("island_eltern").performClick()
        composeTestRule.warteAuf("parent_pin_forgot")
        composeTestRule.onNodeWithTag("parent_pin_forgot").performClick()
        composeTestRule.onNodeWithText(frage).assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_security_answer_input").performTextInput("meier")
        composeTestRule.onNodeWithTag("parent_security_answer_submit").performClick()
        composeTestRule.warteAuf("parent_pin_new_input")
    }

    @Test
    fun entferntePin_bringtDieElternfrageZurueck() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.vergebePin(pin = "4711", frage = frage, antwort = "Meier")

        composeTestRule.oeffneZugang()
        composeTestRule.onNodeWithTag("parent_pin_remove_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("parent_pin_remove_confirm").performClick()
        composeTestRule.warteBis {
            onAllNodesWithTag("parent_pin_remove_button").fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.verlasseDenZugang()
        composeTestRule.verlasseDenElternbereich()
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
    }
}

/**
 * Aus der Übersicht heraus eine PIN vergeben und wieder in der Übersicht landen – dorthin führt
 * der Weg nach dem Speichern, aus dem Zugang wie über die Sicherheitsfrage.
 */
private fun ComposeTestRule.vergebePin(pin: String, frage: String, antwort: String) {
    oeffneZugang()
    onNodeWithTag("parent_pin_manage_button").performScrollTo().performClick()
    warteAuf("parent_pin_new_input")
    speicherePin(pin = pin, frage = frage, antwort = antwort)
    warteAuf("parent_entry_zugang")
}

/** Das Formular zum Vergeben einer PIN ausfüllen und speichern. */
private fun ComposeTestRule.speicherePin(pin: String, frage: String, antwort: String) {
    onNodeWithTag("parent_pin_new_input").performScrollTo().performTextInput(pin)
    onNodeWithTag("parent_pin_repeat_input").performScrollTo().performTextInput(pin)
    onNodeWithTag("parent_pin_question_input").performScrollTo().performTextInput(frage)
    onNodeWithTag("parent_pin_answer_input").performScrollTo().performTextInput(antwort)
    onNodeWithTag("parent_pin_save_button").performScrollTo().performClick()
}

