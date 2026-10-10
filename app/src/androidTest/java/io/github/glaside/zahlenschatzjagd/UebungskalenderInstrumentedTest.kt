package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Der ganze Weg des Übungskalenders: eine Runde durchziehen, danach steht der Punkt im
 * Elternbereich.
 *
 * Bewusst **ohne** Aufräumen der gezählten Runden – die Tabelle hat aus gutem Grund keine
 * Lösch-Methode ([io.github.glaside.zahlenschatzjagd.data.Rundenabschluss] ist unveränderlich). Die
 * Behauptung hält trotzdem: Wer heute eine Runde beendet, färbt heute – ob dort vorher schon ein
 * Punkt stand oder nicht.
 */
@RunWith(AndroidJUnit4::class)
class UebungskalenderInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

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
    fun derKalenderZeigtBeideBloecke_MusterUndBildschirmzeit() {
        oeffneDenKalender()

        composeTestRule.onNodeWithTag("kalender_tag_${LocalDate.now()}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kalender_tageszeit_${LocalDate.now()}")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun nachEinerDurchgezogenenRunde_istHeuteGefaerbt() {
        spieleEineRundeDurch()

        // Neu gestartet: Der Punkt kommt aus der Datenbank, nicht aus einem gemerkten Zustand.
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.warteAuf("island_eltern")

        oeffneDenKalender()

        composeTestRule.onNodeWithTag("kalender_tag_${LocalDate.now()}")
            .assertContentDescriptionContains("geübt", substring = true)
    }

    /** Von der Startkarte über das Tor in den Übungskalender. */
    private fun oeffneDenKalender() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.onNodeWithTag("parent_entry_uebungskalender").performScrollTo().performClick()
        composeTestRule.warteAuf("kalender_tag_${LocalDate.now()}")
    }

    /**
     * Eine vollständige Plus-Runde mit den Voreinstellungen (nur „Plus", Zahlenraum 0–10,
     * 10 Aufgaben) – wie im [GameFlowInstrumentedTest], nur dass es hier auf das **Ende** ankommt.
     */
    private fun spieleEineRundeDurch() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("game_grundrechenarten")
        composeTestRule.onNodeWithTag("game_grundrechenarten").performClick()
        composeTestRule.warteAuf("start_button")
        composeTestRule.onNodeWithTag("start_button").performClick()
        composeTestRule.warteAuf("equation_text")

        repeat(AUFGABEN_JE_RUNDE) {
            val (num1, num2) = lieseAufgabe()
            (num1 + num2).toString().forEach { ziffer ->
                composeTestRule.onNodeWithTag("digit_$ziffer").performClick()
            }
            composeTestRule.onNodeWithTag("digit_check").performClick()
            composeTestRule.onNodeWithTag("next_button").performClick()
        }
        composeTestRule.warteAuf("stats_result_text")
    }

    private fun lieseAufgabe(): Pair<Int, Int> {
        val node = composeTestRule.onNodeWithTag("equation_text").fetchSemanticsNode()
        val text = node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val match = Regex("""(\d+) \+ (\d+) = .*""").find(text)
            ?: error("Unerwartetes Aufgabenformat: $text")
        val (a, b) = match.destructured
        return a.toInt() to b.toInt()
    }
}

/** Die Voreinstellung des Auswahlbildschirms. */
private const val AUFGABEN_JE_RUNDE = 10
