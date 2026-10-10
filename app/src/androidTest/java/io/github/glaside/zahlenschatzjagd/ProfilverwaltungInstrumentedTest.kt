package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.ui.profile.Avatars
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Die Trennung aus `docs/adr/0019-profilverwaltung-hinter-dem-tor.md`: **Wechseln bleibt frei**,
 * alles andere liegt hinter Elternfrage bzw. PIN – und der Schalter im Elternbereich zieht auch
 * das Wechseln dahinter.
 *
 * Profile und Schalter liegen in der echten Datenbank bzw. im echten DataStore und reichen sonst
 * in andere Testklassen hinein; deshalb wird beides vor **und** nach jedem Test gesetzt. Der
 * Aufbau passiert vor dem ersten Compose-Frame, die Activity startet dafür über
 * [ActivityScenario.launch] (siehe `docs/agents/testing.md`).
 */
@RunWith(AndroidJUnit4::class)
class ProfilverwaltungInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val app get() = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()

    @Before
    fun setUp() {
        clearParentPin()
        setzeProfilwechselOhnePin(true)
        runBlocking {
            val profile = app.container.userProfileRepository
            for (name in TESTPROFILE) {
                if (!profile.nameExists(name)) profile.createProfile(name, Avatars.DEFAULT_ID)
            }
            app.container.userPreferencesRepository.setActiveUser(ERSTPIRAT)
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.warteAuf("home_profile_badge")
    }

    @After
    fun tearDown() {
        scenario?.close()
        clearParentPin()
        setzeProfilwechselOhnePin(true)
        runBlocking {
            val profile = app.container.userProfileRepository
            for (name in TESTPROFILE + NEUPIRAT + UMBENANNT) profile.deleteProfile(name)
            val verbleibend = profile.profiles.first().firstOrNull()?.name
            app.container.userPreferencesRepository
                .setActiveUser(verbleibend ?: UserPreferencesRepository.DEFAULT_USER)
        }
    }

    /** Der Alltagsvorgang: Geschwister wechseln sich ab, ohne dass ein Erwachsener dabei ist. */
    @Test
    fun wechseln_gehtOhneElternfrage() {
        composeTestRule.onNodeWithTag("home_profile_badge").performClick()
        composeTestRule.warteAuf("profile_back_button")
        composeTestRule.onAllNodesWithTag("adult_gate_question").assertCountEquals(0)

        composeTestRule.onNodeWithTag("profile_card_$ZWEITPIRAT").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("profile_back_button").performClick()

        composeTestRule.warteAuf("home_profile_badge")
        composeTestRule.warteBis {
            onAllNodesWithText(ZWEITPIRAT).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(ZWEITPIRAT).assertIsDisplayed()
    }

    /** Was ein Kind nicht darf, ist auch nicht da – nicht bloß wirkungslos. */
    @Test
    fun ohneTor_zeigtDerBildschirmKeineVerwaltung() {
        composeTestRule.onNodeWithTag("home_profile_badge").performClick()
        composeTestRule.warteAuf("profile_back_button")

        composeTestRule.onAllNodesWithTag("profile_add_button").assertCountEquals(0)
        for (aktion in AKTIONEN) {
            composeTestRule.onAllNodesWithTag("profile_action_${aktion}_$ERSTPIRAT")
                .assertCountEquals(0)
        }
        composeTestRule.onNodeWithTag("profile_manage_button").performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun verwalten_erstNachDerElternfrage() {
        composeTestRule.onNodeWithTag("home_profile_badge").performClick()
        composeTestRule.warteAuf("profile_manage_button")
        composeTestRule.onNodeWithTag("profile_manage_button").performScrollTo().performClick()

        composeTestRule.warteAuf("adult_gate_question")
        composeTestRule.loeseDieElternfrage()

        // Dahinter steht alles offen – und zwar nicht bloß sichtbar: Anlegen, Umbenennen und
        // Löschen werden hier wirklich ausgeführt.
        composeTestRule.warteAuf("profile_add_button")
        for (aktion in AKTIONEN) {
            composeTestRule.onNodeWithTag("profile_action_${aktion}_$ERSTPIRAT")
                .performScrollTo().assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag("profile_add_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("profile_name_input").performTextInput(NEUPIRAT)
        composeTestRule.onNodeWithTag("profile_create_confirm").performClick()
        composeTestRule.warteAuf("profile_card_$NEUPIRAT")

        composeTestRule.onNodeWithTag("profile_action_umbenennen_$NEUPIRAT").performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("profile_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_name_input").performTextInput(UMBENANNT)
        composeTestRule.onNodeWithTag("profile_rename_confirm").performClick()
        composeTestRule.warteAuf("profile_card_$UMBENANNT")

        composeTestRule.onNodeWithTag("profile_action_löschen_$UMBENANNT").performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("profile_delete_confirm").performClick()
        composeTestRule.warteBis {
            onAllNodesWithTag("profile_card_$UMBENANNT").fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * Ist eine PIN gesetzt, tritt sie auch vor der Profilverwaltung an die Stelle der
     * Elternfrage – dasselbe Tor, dieselbe Regel (`docs/adr/0016-…`).
     */
    @Test
    fun mitPin_verlangtDieVerwaltungDiePin() {
        scenario?.close()
        setzeParentPin(pin = "4711", frage = "Erste Chefin?", antwort = "Meier")
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.warteAuf("home_profile_badge")

        composeTestRule.onNodeWithTag("home_profile_badge").performClick()
        composeTestRule.warteAuf("profile_manage_button")
        composeTestRule.onNodeWithTag("profile_manage_button").performScrollTo().performClick()

        composeTestRule.warteAuf("parent_pin_input")
        composeTestRule.onAllNodesWithTag("adult_gate_question").assertCountEquals(0)
    }

    /**
     * Mit ausgeschaltetem Schalter steht das Tor schon vor dem Wechseln – und wer es passiert,
     * kommt gleich auf den vollen Bildschirm; ein zweites Mal fragen wäre reine Schikane.
     */
    @Test
    fun schalterAus_verlangtDasTorSchonZumWechseln() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneZugang()
        composeTestRule.onNodeWithTag("parent_profilwechsel_switch").performScrollTo()
            .performClick()
        // Erst wenn der Hinweis umschlägt, ist der Schalter wirklich geschrieben – auf den
        // DataStore wartet keine virtuelle Zeit (`docs/agents/testing.md`).
        composeTestRule.warteBis {
            onAllNodesWithText("Auch das Wechseln verlangt", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.verlasseDenZugang()
        composeTestRule.verlasseDenElternbereich()

        composeTestRule.onNodeWithTag("home_profile_badge").performClick()

        composeTestRule.warteAuf("adult_gate_question")
        composeTestRule.onAllNodesWithTag("profile_card_$ERSTPIRAT").assertCountEquals(0)
        composeTestRule.loeseDieElternfrage()

        composeTestRule.warteAuf("profile_add_button")
    }

    private companion object {
        const val ERSTPIRAT = "Erstpirat"
        const val ZWEITPIRAT = "Zweitpirat"

        /** Werden erst im Test angelegt und müssen trotzdem wieder verschwinden. */
        const val NEUPIRAT = "Neupirat"
        const val UMBENANNT = "Umbenannt"

        val TESTPROFILE = listOf(ERSTPIRAT, ZWEITPIRAT)

        /** Die drei Karten-Aktionen, wie sie in den Testmarken stehen. */
        val AKTIONEN = listOf("umbenennen", "avatar", "löschen")
    }
}
