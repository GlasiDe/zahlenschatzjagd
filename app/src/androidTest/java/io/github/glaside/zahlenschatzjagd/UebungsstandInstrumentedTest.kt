package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
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

/**
 * Der ganze Weg des Übungsstands: eine Runde durchziehen, danach steht sie im Elternbereich.
 *
 * Bewusst **ohne** Aufräumen der Historie – sie ist die Quelle des Fortschritts und hat keine
 * Lösch-Methode. Die Behauptungen halten trotzdem: Wer heute zehn Plus-Aufgaben löst, hat danach
 * Plus geübt, gleichgültig was vorher in der Historie stand.
 */
@RunWith(AndroidJUnit4::class)
class UebungsstandInstrumentedTest {

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

    /**
     * Die drei Ebenen: Übungsform → Fortschrittskennung → Zahlenraum. Zugeklappt ist nur die
     * oberste zu sehen.
     */
    @Test
    fun eineZeileKlapptZweimalAuf_aufDieRechenartenUndDerenZahlenraeume() {
        oeffneDenUebungsstand()

        composeTestRule.onNodeWithTag("uebungsstand_zeile_PLUS").assertDoesNotExist()

        composeTestRule.onNodeWithTag("uebungsstand_zeile_GRUNDRECHENARTEN")
            .performScrollTo().performClick()
        composeTestRule.warteAuf("uebungsstand_zeile_PLUS")

        composeTestRule.onNodeWithTag("uebungsstand_zeile_PLUS").performScrollTo().performClick()
        composeTestRule.warteAuf("uebungsstand_zeile_PLUS_10")
        composeTestRule.onNodeWithTag("uebungsstand_zeile_PLUS_10").assertIsDisplayed()
    }

    /** Die Schwelle steht auf dem Bildschirm – sonst liest sich „zu wenig geübt" wie ein Fehler. */
    @Test
    fun derBildschirmNenntAbWannEineProzentzahlDasteht() {
        oeffneDenUebungsstand()

        composeTestRule.onNodeWithTag("uebungsstand_schwellen_hinweis")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun nachEinerDurchgezogenenRunde_istPlusNichtMehrUngeuebt() {
        spieleEineRundeDurch()

        // Neu gestartet: Der Stand kommt aus der Datenbank, nicht aus einem gemerkten Zustand.
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.warteAuf("island_eltern")

        oeffneDenUebungsstand()
        composeTestRule.onNodeWithTag("uebungsstand_zeile_GRUNDRECHENARTEN")
            .performScrollTo().performClick()
        composeTestRule.warteAuf("uebungsstand_zeile_PLUS")

        // Die Zeile führt die Semantik ihrer Texte zusammen, also steht ihre Aussage vollständig
        // an ihr selbst – „noch nicht geübt" darf danach nicht mehr dabei sein.
        composeTestRule.onNode(
            hasTestTag("uebungsstand_zeile_PLUS") and hasText("noch nicht geübt")
        ).assertDoesNotExist()
        composeTestRule.onNodeWithTag("uebungsstand_zeile_PLUS")
            .performScrollTo().assertIsDisplayed()
    }

    /** Von der Startkarte über das Tor in den Übungsstand. */
    private fun oeffneDenUebungsstand() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.onNodeWithTag("parent_entry_uebungsstand").performScrollTo().performClick()
        composeTestRule.warteAuf("uebungsstand_zeile_GRUNDRECHENARTEN")
    }

    /**
     * Eine vollständige Plus-Runde mit den Voreinstellungen (nur „Plus", Zahlenraum 0–10,
     * 10 Aufgaben) – wie im [UebungskalenderInstrumentedTest], nur dass es hier auf die
     * aufgezeichneten **Aufgaben** ankommt statt auf den Rundenabschluss.
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
