package io.github.glaside.zahlenschatzjagd

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.data.AppDatabase
import io.github.glaside.zahlenschatzjagd.data.BackupFile
import io.github.glaside.zahlenschatzjagd.data.BackupProfile
import io.github.glaside.zahlenschatzjagd.data.BackupSerializer
import io.github.glaside.zahlenschatzjagd.data.BackupTask
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.ui.profile.Avatars
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Simuliert eine Frischinstallation (keine Profile in der echten App-Datenbank). Von dort führen
 * **zwei** Wege weiter: ein Profil anlegen, oder eine Sicherung einspielen – der Moment, für den
 * die Sicherung gebaut ist (`docs/adr/0023-erststart-bietet-die-sicherung-an.md`). Beide stehen
 * ohne Tor da, weil es weder ein Profil zu schützen noch einen anderen Weg hinein gibt.
 *
 * Die Datenbank wird bewusst über [ActivityScenario.launch] statt über
 * `createAndroidComposeRule<MainActivity>()` befüllt/geleert, damit das Löschen sicher
 * *vor* dem allerersten Compose-Frame von `MainActivity` passiert.
 *
 * Der Android-Dateidialog gehört dem System und wird deshalb gestubbt (Espresso-Intents): Der
 * Test beantwortet `ACTION_OPEN_DOCUMENT` selbst und legt die Datei dazu ins Cache-Verzeichnis.
 * Geprüft wird damit die Verdrahtung – dass der Dialog überhaupt und ohne Tor aufgeht und was
 * die App mit dem Ergebnis macht. Das Zusammenführen selbst prüfen `BackupViewModelTest` und
 * `BackupMergeTest` ohne Gerät.
 */
@RunWith(AndroidJUnit4::class)
class ProfileOnboardingInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val app get() = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()

    @Before
    fun leereAlleProfile() {
        loescheAlleProfile()
        Intents.init()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun raeumeAuf() {
        Intents.release()
        scenario?.close()
        // Was dieser Test angelegt oder eingespielt hat, darf nicht in andere Testklassen
        // hineinreichen – die Datenbank ist die echte des Geräts.
        loescheAlleProfile()
    }

    private fun loescheAlleProfile() {
        val dao = AppDatabase.getDatabase(context).userProfileDao()
        runBlocking {
            dao.getAllProfiles().first().forEach { dao.delete(it.name) }
            app.container.userPreferencesRepository
                .setActiveUser(UserPreferencesRepository.DEFAULT_USER)
        }
    }

    @Test
    fun frischinstallation_zeigtBeideWege_anlegenUndSicherung() {
        composeTestRule.warteAuf("profile_add_button")

        composeTestRule.onNodeWithTag("profile_add_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_restore_button").performScrollTo()
            .assertIsDisplayed()
        // Kein Dialog, der sich von selbst über den zweiten Weg legt.
        composeTestRule.onAllNodesWithTag("profile_name_input").assertCountEquals(0)
        // Und keine Zeile, die auf Piraten zeigt, die es noch gar nicht gibt.
        composeTestRule.onAllNodesWithText("Tippe auf einen Piraten", substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun frischinstallation_profilAnlegen_fuehrtZurStartkarte() {
        composeTestRule.warteAuf("profile_add_button")
        composeTestRule.onNodeWithTag("profile_add_button").performClick()
        composeTestRule.onNodeWithTag("profile_name_input").performTextInput(LINA)
        composeTestRule.onNodeWithTag("profile_create_confirm").performClick()

        composeTestRule.warteAuf("first_profile_hint_confirm")
        composeTestRule.onNodeWithTag("first_profile_hint_confirm").performClick()

        composeTestRule.warteAuf("home_profile_badge")
        composeTestRule.onNodeWithText(LINA).assertIsDisplayed()
    }

    /** Der ganze Punkt: kein Wegwerf-Profil, bevor die Sicherung überhaupt gewählt werden kann. */
    @Test
    fun frischinstallation_sicherungEinspielen_erreichtDieDateiauswahlOhneTor() {
        beantworteDenDateidialogMit(Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null))
        composeTestRule.warteAuf("profile_restore_button")

        composeTestRule.onNodeWithTag("profile_restore_button").performClick()

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        composeTestRule.onAllNodesWithTag("adult_gate_question").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("parent_pin_input").assertCountEquals(0)
    }

    @Test
    fun untauglicheDatei_zeigtIhreMeldung_undLaesstBeideWegeStehen() {
        beantworteDenDateidialogMit(ergebnisMitDatei("das ist ein Urlaubsfoto"))
        composeTestRule.warteAuf("profile_restore_button")

        composeTestRule.onNodeWithTag("profile_restore_button").performClick()

        // Der Bildschirm scrollt, und die Meldung steht ganz unten – bei Anzeigegröße „groß"
        // liegt sie unter dem Rand (`docs/agents/testing.md`).
        composeTestRule.warteAuf("sicherung_meldung")
        composeTestRule.onNodeWithText("konnte nicht gelesen werden", substring = true)
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_add_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_restore_button").performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Nach dem Einspielen wird gewählt, nicht geraten: Das aktive Profil steht bewusst nicht in
     * der Sicherung, und ohne Wahl gälte auf der Startkarte „alles offen".
     */
    @Test
    fun eingespielteSicherung_laesstDasProfilWaehlen() {
        beantworteDenDateidialogMit(ergebnisMitDatei(BackupSerializer.encode(sicherungMitEinemKind())))
        composeTestRule.warteAuf("profile_restore_button")

        composeTestRule.onNodeWithTag("profile_restore_button").performClick()

        composeTestRule.warteAuf("profile_card_$SICHERUNGSKIND")
        composeTestRule.onNodeWithText("Tippe auf einen Piraten", substring = true)
            .performScrollTo().assertIsDisplayed()
        // Es gibt jetzt etwas zu schützen – damit endet die Ausnahme für beide Wege.
        composeTestRule.onAllNodesWithTag("profile_restore_button").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("profile_add_button").assertCountEquals(0)
        composeTestRule.onNodeWithTag("profile_manage_button").performScrollTo()
            .assertIsDisplayed()
        // Solange niemand spielt, führt „← Zurück" nirgendwohin und steht deshalb nicht da.
        composeTestRule.onAllNodesWithTag("profile_back_button").assertCountEquals(0)
    }

    private fun beantworteDenDateidialogMit(ergebnis: Instrumentation.ActivityResult) {
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(ergebnis)
    }

    /** Legt den Inhalt als Datei ab und beantwortet den Dialog damit, als hätte jemand sie gewählt. */
    private fun ergebnisMitDatei(inhalt: String): Instrumentation.ActivityResult {
        val datei = File(context.cacheDir, "test-sicherung.json")
        datei.writeText(inhalt)
        return Instrumentation.ActivityResult(
            Activity.RESULT_OK,
            Intent().setData(Uri.fromFile(datei))
        )
    }

    private fun sicherungMitEinemKind() = BackupFile(
        exportedAt = 1,
        profiles = listOf(BackupProfile(SICHERUNGSKIND, Avatars.DEFAULT_ID, createdAt = 1)),
        history = listOf(
            BackupTask(
                userName = SICHERUNGSKIND,
                taskCategory = "PLUS",
                numberRange = 10,
                questionText = "3 + 4",
                expectedAnswer = "7",
                userAnswer = "7",
                isCorrect = true,
                timestamp = 2,
            )
        ),
    )

    private companion object {
        const val LINA = "Lina"
        const val SICHERUNGSKIND = "Sicherungskind"
    }
}
