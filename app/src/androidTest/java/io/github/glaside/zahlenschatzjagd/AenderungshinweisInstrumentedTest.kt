package io.github.glaside.zahlenschatzjagd

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.data.Aenderungshinweis
import io.github.glaside.zahlenschatzjagd.ui.HOME_WELCOME_SPOKEN
import io.github.glaside.zahlenschatzjagd.ui.Narrator
import io.github.glaside.zahlenschatzjagd.ui.ZahlenschatzjagdNavHost
import io.github.glaside.zahlenschatzjagd.ui.aenderungshinweis.AENDERUNGSHINWEIS_FREIGABE
import io.github.glaside.zahlenschatzjagd.ui.aenderungshinweis.AENDERUNGSHINWEIS_GESPROCHEN
import io.github.glaside.zahlenschatzjagd.ui.profile.Avatars
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Der Änderungshinweis nach einem Update, vom Start bis zum Wegtippen (#8).
 *
 * Die App zeigt nur markierte Versionen, und solange keine markiert ist, gäbe es nichts zu sehen.
 * Der Test reicht deshalb eigene Hinweise und eine erfundene Versionsnummer in den
 * `ZahlenschatzjagdNavHost` – Datenbank, DataStore und Vorlesefunktion sind die echten.
 */
@RunWith(AndroidJUnit4::class)
class AenderungshinweisInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val app = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()
    private val saetze = mutableListOf<String>()

    private val hinweise = listOf(
        Aenderungshinweis(AKTUELL, "9.9.9", "Neu: eine erfundene Übung für diesen Test."),
        Aenderungshinweis(AKTUELL - 1, "9.9.8", "Eine übersprungene Version."),
    )

    @Before
    fun setUp() {
        clearParentPin()
        runBlocking {
            val profile = app.container.userProfileRepository
            if (!profile.nameExists(PROFIL)) profile.createProfile(PROFIL, Avatars.DEFAULT_ID)
            app.container.userPreferencesRepository.setActiveUser(PROFIL)
            app.container.aenderungshinweisRepository.vergiss()
            app.container.aenderungshinweisRepository.merkeGezeigt(AKTUELL - 2)
        }
        Narrator.beobachter = { saetze += it }
    }

    @After
    fun tearDown() {
        Narrator.beobachter = null
        // Zurück auf den Stand der echten App, damit der Hinweis in keine andere Testklasse
        // hineinreicht.
        runBlocking {
            app.container.aenderungshinweisRepository.vergiss()
            app.container.aenderungshinweisRepository.merkeGezeigt(BuildConfig.VERSION_CODE)
        }
    }

    private fun starte(neuaufbau: StateRestorationTester? = null) {
        val inhalt: @Composable () -> Unit = {
            ZahlenschatzjagdTheme {
                ZahlenschatzjagdNavHost(aenderungshinweise = hinweise, aktuelleVersion = AKTUELL)
            }
        }
        if (neuaufbau != null) neuaufbau.setContent(inhalt) else composeTestRule.setContent(inhalt)
        warteAufDenHinweis()
    }

    private fun warteAufDenHinweis() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag("aenderungshinweis_dialog")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun nachDemUpdateErscheinenAlleNeuenHinweise_derNeuesteOben() {
        starte()

        val neu = composeTestRule.onNodeWithText("Neu in Version 9.9.9").fetchSemanticsNode()
        val alt = composeTestRule.onNodeWithText("Neu in Version 9.9.8").fetchSemanticsNode()
        assertTrue(
            "Der neueste Hinweis gehört nach oben",
            neu.boundsInRoot.top < alt.boundsInRoot.top,
        )
        composeTestRule.onNodeWithText(AENDERUNGSHINWEIS_FREIGABE).performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("aenderungshinweis_verstanden").assertIsDisplayed()
    }

    @Test
    fun verstandenSchliesstDenHinweis_undMerktDieVersion() {
        starte()

        composeTestRule.onNodeWithTag("aenderungshinweis_verstanden").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTag("aenderungshinweis_dialog")
                .fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithTag("home_profile_badge").assertIsDisplayed()
        val gemerkt = runBlocking {
            app.container.aenderungshinweisRepository.zuletztGezeigt.first { it == AKTUELL }
        }
        assertEquals(AKTUELL, gemerkt)
    }

    /** Ella holt die Eltern, statt das Kind wie sonst zu begrüßen – und liest den Text nicht vor. */
    @Test
    fun ellaSchicktZuDenEltern_stattZuBegruessen() {
        starte()
        composeTestRule.waitForIdle()

        assertEquals(listOf(AENDERUNGSHINWEIS_GESPROCHEN), saetze.toList())
        assertFalse(saetze.contains(HOME_WELCOME_SPOKEN))
    }

    /**
     * Nach dem Drehen steht der Hinweis wieder da, Ella sagt ihren Satz aber nicht noch einmal
     * (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
     */
    @Test
    fun nachDemNeuaufbauBleibtDerHinweis_ohneNochEinmalVorzulesen() {
        val neuaufbau = StateRestorationTester(composeTestRule)
        starte(neuaufbau)
        composeTestRule.waitForIdle()

        neuaufbau.emulateSavedInstanceStateRestore()
        warteAufDenHinweis()
        composeTestRule.waitForIdle()

        assertEquals(listOf(AENDERUNGSHINWEIS_GESPROCHEN), saetze.toList())
    }

    private companion object {
        const val AKTUELL = 9_999
        const val PROFIL = "Hinweispirat"
    }
}
