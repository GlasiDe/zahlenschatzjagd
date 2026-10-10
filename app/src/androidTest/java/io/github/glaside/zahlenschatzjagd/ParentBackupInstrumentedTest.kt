package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deckt den Weg zur Sicherung im Elternbereich ab: Sie ist einer der fünf Einträge der
 * Übersicht, beide Aktionen sind vorhanden, und das Einspielen fragt vorher nach. Der
 * Android-Dateidialog selbst gehört zum System und wird hier nicht automatisiert – die Logik
 * dahinter prüfen `BackupViewModelTest` und `BackupRepositoryTest`.
 *
 * Die Activity wird über [ActivityScenario.launch] gestartet statt über
 * `createAndroidComposeRule<MainActivity>()`, damit eine eventuell zurückgebliebene PIN sicher
 * *vor* dem allerersten Compose-Frame verschwindet – sonst stünde statt der Elternfrage das
 * PIN-Tor da.
 */
@RunWith(AndroidJUnit4::class)
class ParentBackupInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        clearParentPin()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.ensureActiveProfileExists()
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.oeffneSicherung()
    }

    @After
    fun closeActivity() {
        scenario?.close()
    }

    @Test
    fun elternbereich_zeigtBeideSicherungsAktionen() {
        composeTestRule.onNodeWithTag("parent_export_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_import_button").assertIsDisplayed()
    }

    @Test
    fun einspielen_fragtVorherNach_undErklaertDasZusammenfuehren() {
        composeTestRule.onNodeWithTag("parent_import_button").performClick()

        composeTestRule.onNodeWithText("Sicherung einspielen?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_import_confirm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Abbrechen").assertIsDisplayed()
    }

    @Test
    fun einspielen_laesstSichAbbrechen_ohneDassEtwasPassiert() {
        composeTestRule.onNodeWithTag("parent_import_button").performClick()
        composeTestRule.onNodeWithText("Abbrechen").performClick()

        composeTestRule.onNodeWithTag("parent_export_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_import_button").assertIsDisplayed()
    }
}
