package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft die **Bildschirmzeit** an der echten App: Wird auf den Kinder-Bildschirmen gemessen, und
 * schneidet der Weg in den Elternbereich den Abschnitt sauber durch?
 *
 * Die Regeln selbst prüft [io.github.glaside.zahlenschatzjagd.data.BildschirmzeitUhrTest] ohne Gerät;
 * hier geht es um die Verdrahtung, die ein Unit-Test nicht sehen kann – dass der Modifier
 * wirklich ganz außen hängt, dass er jeden Tipp mitbekommt, bevor ein Knopf ihn verbraucht, und
 * dass der fertige Abschnitt in der Datenbank landet.
 *
 * Die Activity wird wie im [ParentBackupInstrumentedTest] über [ActivityScenario.launch]
 * gestartet, damit eine zurückgebliebene PIN vor dem ersten Compose-Frame verschwindet.
 */
@RunWith(AndroidJUnit4::class)
class BildschirmzeitInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val app get() = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()

    private fun aktivesProfil(): String =
        runBlocking { app.container.userPreferencesRepository.activeUser.first() }

    private fun gemesseneAbschnitte(profil: String): List<Zeitabschnitt> =
        runBlocking { app.container.bildschirmzeitRepository.fuerProfil(profil) }

    @Before
    fun setUp() {
        clearParentPin()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.ensureActiveProfileExists()
        korrigiereVerwaistenAktivenNamen()
    }

    /**
     * Gemessen wird nur für ein Profil, das es wirklich gibt. Hat ein früherer Test das aktive
     * Profil gelöscht, zeigt der DataStore auf einen Namen ohne Profil – `ensureActiveProfileExists`
     * merkt das nicht, weil es nur den Anlage-Dialog der Frischinstallation abdeckt. Dann würde
     * hier gar nichts gemessen, und der Test hinge an einer Bedingung, die nie eintritt.
     */
    private fun korrigiereVerwaistenAktivenNamen() = runBlocking {
        val profile = app.container.userProfileRepository.profiles.first()
        val aktiv = app.container.userPreferencesRepository.activeUser.first()
        if (profile.none { it.name == aktiv }) {
            app.container.userPreferencesRepository.setActiveUser(profile.first().name)
        }
    }

    @After
    fun closeActivity() {
        scenario?.close()
    }

    @Test
    fun derWegInDenElternbereich_schliesstDenAbschnittDesKindes() {
        val profil = aktivesProfil()
        val vorher = gemesseneAbschnitte(profil).size

        // Ein paar echte Tipps auf Kinder-Bildschirmen …
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("rechnen_menu_back_button")
        composeTestRule.onNodeWithTag("rechnen_menu_back_button").performClick()
        composeTestRule.warteAuf("island_eltern")

        // … und dann durch das Tor: Ab hier gehört die Zeit keinem Kind mehr, der Abschnitt ist
        // fertig und muss geschrieben sein.
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.warteBis { gemesseneAbschnitte(profil).size > vorher }

        val neu = gemesseneAbschnitte(profil).first()
        assertEquals(profil, neu.profil)
        assertTrue("Ein Abschnitt ohne Dauer wäre keiner", neu.dauerMillis > 0)
    }

    @Test
    fun imElternbereich_kommtKeineZeitDazu() {
        val profil = aktivesProfil()
        val vorher = gemesseneAbschnitte(profil).size

        // Erst durch das Tor – dabei wird der eine Abschnitt fertig, den es zu schreiben gibt.
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.warteBis { gemesseneAbschnitte(profil).size > vorher }
        val nachDemTor = gemesseneAbschnitte(profil)

        // Im Elternbereich herumtippen – diese Zeit gehört den Erwachsenen.
        composeTestRule.oeffneZugang()
        composeTestRule.verlasseDenZugang()
        composeTestRule.oeffneSicherung()
        composeTestRule.warteAuf("parent_export_button")

        assertEquals(
            "Zeit im Elternbereich wird verworfen",
            nachDemTor,
            gemesseneAbschnitte(profil),
        )
    }
}
