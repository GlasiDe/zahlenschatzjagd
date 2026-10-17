package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.data.ALLE_KENNUNGEN
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Was die Freigabe dem Kind wegnimmt, ist **weg** – nicht gedämpft, nicht ausgegraut
 * (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`).
 *
 * Die Freigabe liegt am Profil in der echten Datenbank und überdauert sonst in andere
 * Testklassen hinein; deshalb wird sie nach jedem Test zurückgesetzt. Gesetzt wird sie
 * **vor** dem ersten Compose-Frame, die Activity startet dafür über [ActivityScenario.launch]
 * (siehe `docs/agents/testing.md`).
 */
@RunWith(AndroidJUnit4::class)
class FreigabeInstrumentedTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        clearParentPin()
        setzeFreigabe(Freigabe.ALLES)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.ensureActiveProfileExists()
    }

    @After
    fun tearDown() {
        scenario?.close()
        setzeFreigabe(Freigabe.ALLES)
        clearParentPin()
    }

    /** Setzt die Freigabe des spielenden Profils und startet die App damit neu. */
    private fun starteNeuMit(freigabe: Freigabe) {
        scenario?.close()
        setzeFreigabe(freigabe)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.warteAuf("island_abzeichen")
    }

    @Test
    fun eineInselOhneErlaubteUebungVerschwindetVonDerStartkarte() {
        starteNeuMit(Freigabe(kennungen = setOf("VERLIEBTE_ZAHLEN"), zahlenraeume = setOf(10)))

        composeTestRule.onNodeWithTag("island_zahlenspiele").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("island_rechnen").assertCountEquals(0)
    }

    @Test
    fun ohneJedeFreigabeBleibenNurAbzeichenUndFuerEltern() {
        starteNeuMit(Freigabe(kennungen = emptySet(), zahlenraeume = emptySet()))

        composeTestRule.onNodeWithTag("island_abzeichen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("island_eltern").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("island_rechnen").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("island_zahlenspiele").assertCountEquals(0)
    }

    @Test
    fun abzeichenBleibenVollstaendigSichtbar() {
        starteNeuMit(Freigabe(kennungen = emptySet(), zahlenraeume = emptySet()))

        composeTestRule.onNodeWithTag("island_abzeichen").performClick()
        composeTestRule.warteAuf("badge_tab_PLUS")

        // Erreichtes wird nie weggenommen – die Truhen stehen auch für Gesperrtes. Der Reiter
        // liegt am Ende der scrollenden Reiterleiste, muss also erst herangeholt werden.
        composeTestRule.onNodeWithTag("badge_tab_ZAHLENMAUER").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun eineGesperrteKachelVerschwindetAusDemSpielmenue() {
        // Nur noch Grundrechenarten: Verdoppeln und Zahlenmauer fallen weg.
        starteNeuMit(
            Freigabe(kennungen = RECHENART_KENNUNGEN, zahlenraeume = setOf(10, 20, 50, 100))
        )

        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("game_grundrechenarten")

        composeTestRule.onAllNodesWithTag("game_verdoppeln").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("game_zahlenmauer").assertCountEquals(0)
    }

    @Test
    fun gesperrteRechenartUndZahlenraumFehlenInDerAuswahl() {
        starteNeuMit(
            Freigabe(kennungen = ALLE_KENNUNGEN - "MINUS", zahlenraeume = setOf(10, 20))
        )

        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("game_grundrechenarten")
        composeTestRule.onNodeWithTag("game_grundrechenarten").performClick()
        composeTestRule.warteAuf("checkbox_plus")

        composeTestRule.onAllNodesWithTag("checkbox_minus").assertCountEquals(0)
        composeTestRule.onNodeWithText("0-20").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0-100").assertCountEquals(0)
    }

    @Test
    fun eineAbgewaehlteSchwierigkeitsstufeFehltInDerAuswahl() {
        // Zahlenmauer (mittel) hat fest den Zahlenraum 50. Sie hängt allein an ihrem Kästchen –
        // ein abgewählter Zahlenraum 50 sperrt sie nicht mit
        // (`docs/adr/0026-zahlenraum-wirkt-nur-wo-gewaehlt-wird.md`).
        starteNeuMit(
            Freigabe(kennungen = ALLE_KENNUNGEN - "ZAHLENMAUER_SCHWER", zahlenraeume = setOf(10, 20, 100))
        )

        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("game_zahlenmauer")
        composeTestRule.onNodeWithTag("game_zahlenmauer").performClick()
        composeTestRule.warteAuf("difficulty_LEICHT")

        composeTestRule.onNodeWithTag("difficulty_MITTEL").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("difficulty_SCHWER").assertCountEquals(0)
    }

    /** Der ganze Weg: im Elternbereich abwählen, danach ist es für das Kind verschwunden. */
    @Test
    fun imElternbereichAbgewaehltesVerschwindetFuerDasKind() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.onNodeWithTag("parent_entry_freigabe").performScrollTo().performClick()
        composeTestRule.warteAuf("freigabe_kennung_MINUS")

        composeTestRule.onNodeWithTag("freigabe_kennung_MINUS").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("freigabe_back_button").performScrollTo().performClick()
        composeTestRule.warteAuf("parent_back_button")
        // Die Übersicht steht noch dort, wohin sie für den Eintrag „Freigabe" gescrollt wurde –
        // ihr Zurück-Knopf liegt dann über dem Bildschirmrand und muss erst herangeholt werden.
        composeTestRule.verlasseDenElternbereich()
        composeTestRule.warteAuf("island_rechnen")

        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("game_grundrechenarten")
        composeTestRule.onNodeWithTag("game_grundrechenarten").performClick()
        composeTestRule.warteAuf("checkbox_plus")

        composeTestRule.onAllNodesWithTag("checkbox_minus").assertCountEquals(0)
    }

    /**
     * Beim Einmaleins steht je Reihe und Stufe ein Kästchen im Raster. Eine einzeln abgewählte
     * Reihe fehlt dem Kind nur in dieser Stufe (`docs/adr/0034-einmaleins-mit-kennungen-je-reihe-und-stufe.md`).
     */
    @Test
    fun eineImRasterAbgewaehlteReiheFehltNurInIhrerStufe() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.onNodeWithTag("parent_entry_freigabe").performScrollTo().performClick()
        composeTestRule.warteAuf("freigabe_kennung_EINMALEINS_9_AUFSAGEN")

        composeTestRule.onNodeWithTag("freigabe_kennung_EINMALEINS_9_AUFSAGEN").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("freigabe_back_button").performScrollTo().performClick()
        composeTestRule.warteAuf("parent_back_button")
        composeTestRule.verlasseDenElternbereich()
        composeTestRule.warteAuf("island_rechnen")

        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("game_einmaleins")
        composeTestRule.onNodeWithTag("game_einmaleins").performClick()
        composeTestRule.warteAuf("einmaleins_reihe_8")

        composeTestRule.onAllNodesWithTag("einmaleins_reihe_9").assertCountEquals(0)
        composeTestRule.onNodeWithText("Durcheinander").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("einmaleins_reihe_9").performScrollTo().assertIsDisplayed()
    }

    /**
     * Eltern wählen bei Verliebte Zahlen „Mit Hilfe" ab. Das Kind sieht danach
     * keinen Schalter mehr und übt ohne Münzen
     * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
     */
    @Test
    fun ohneMitHilfeImElternbereichUebtDasKindOhneMuenzen() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.onNodeWithTag("parent_entry_freigabe").performScrollTo().performClick()
        composeTestRule.warteAuf("freigabe_kennung_VERLIEBTE_ZAHLEN")

        composeTestRule.onNodeWithTag("freigabe_kennung_VERLIEBTE_ZAHLEN").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("freigabe_back_button").performScrollTo().performClick()
        composeTestRule.warteAuf("parent_back_button")
        composeTestRule.verlasseDenElternbereich()
        composeTestRule.warteAuf("island_zahlenspiele")

        composeTestRule.onNodeWithTag("island_zahlenspiele").performClick()
        composeTestRule.warteAuf("game_verliebte")
        composeTestRule.onNodeWithTag("game_verliebte").performClick()
        composeTestRule.warteAuf("hilfe_stufe")

        composeTestRule.onNodeWithTag("hilfe_stufe").assertTextEquals("Ohne Hilfe")
        composeTestRule.onAllNodesWithTag("toggle_hilfe").assertCountEquals(0)

        composeTestRule.onNodeWithTag("start_button").performScrollTo().performClick()
        composeTestRule.warteAuf("equation_text")
        composeTestRule.onAllNodesWithTag("coin_ten_frame").assertCountEquals(0)
    }

    /**
     * „bis 10" abwählen, weil das Kind über die kleinen Aufgaben hinaus ist.
     * Plus bis 10 verschwindet, Verdoppeln mit seinem festen Zahlenraum 10 bleibt
     * (`docs/adr/0026-zahlenraum-wirkt-nur-wo-gewaehlt-wird.md`).
     */
    @Test
    fun ohneBis10ImElternbereichBleibtVerdoppelnFuerDasKind() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.onNodeWithTag("parent_entry_freigabe").performScrollTo().performClick()
        composeTestRule.warteAuf("freigabe_zahlenraum_10")

        composeTestRule.onNodeWithTag("freigabe_zahlenraum_10").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("freigabe_back_button").performScrollTo().performClick()
        composeTestRule.warteAuf("parent_back_button")
        composeTestRule.verlasseDenElternbereich()
        composeTestRule.warteAuf("island_rechnen")

        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.warteAuf("game_verdoppeln")
        composeTestRule.onNodeWithTag("game_verdoppeln").assertIsDisplayed()

        composeTestRule.onNodeWithTag("game_grundrechenarten").performClick()
        composeTestRule.warteAuf("checkbox_plus")
        composeTestRule.onNodeWithText("0-20").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("0-10").assertCountEquals(0)
    }

    /** Nicht nur das Kästchen, auch seine Beschriftung schaltet (#7). */
    @Test
    fun einTippAufDieBeschriftungSchaltetDasKaestchen() {
        composeTestRule.oeffneElternbereichUeberDieElternfrage()
        composeTestRule.onNodeWithTag("parent_entry_freigabe").performScrollTo().performClick()
        composeTestRule.warteAuf("freigabe_zahlenraum_10")
        composeTestRule.onNodeWithTag("freigabe_zahlenraum_10").performScrollTo().assertIsOn()

        composeTestRule.onNodeWithText("bis 10", useUnmergedTree = true).performClick()
        composeTestRule.warteBis { istAn("freigabe_zahlenraum_10") == false }

        composeTestRule.onNodeWithText("bis 10", useUnmergedTree = true).performClick()
        composeTestRule.warteBis { istAn("freigabe_zahlenraum_10") == true }
    }
}

/** Der Zustand eines Kästchens; erst nach der Antwort der Datenbank aktuell. */
private fun ComposeTestRule.istAn(testTag: String): Boolean? =
    onAllNodesWithTag(testTag).fetchSemanticsNodes().singleOrNull()
        ?.config?.getOrNull(SemanticsProperties.ToggleableState)
        ?.let { it == ToggleableState.On }

/** Die Kennungen der vier Grundrechenarten. */
private val RECHENART_KENNUNGEN = setOf("PLUS", "MINUS", "MAL", "GETEILT")

/** Schreibt die Freigabe des gerade spielenden Profils direkt in die Datenbank. */
private fun setzeFreigabe(freigabe: Freigabe) {
    val app = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()
    runBlocking {
        val aktiv = app.container.userPreferencesRepository.activeUser.first()
        app.container.userProfileRepository.setFreigabe(aktiv, freigabe)
    }
}
