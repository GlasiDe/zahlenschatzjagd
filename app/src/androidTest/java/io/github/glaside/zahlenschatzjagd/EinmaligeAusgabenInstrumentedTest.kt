package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.ui.HOME_WELCOME_SPOKEN
import io.github.glaside.zahlenschatzjagd.ui.Narrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ein **Ton** und ein vorgelesener Satz kommen so oft, wie sie gemeint sind – auch wenn die
 * Activity neu gebaut wird, weil das Kind das Gerät dreht. Das ViewModel übersteht das
 * Drehen, die Composition nicht; ein nackter `LaunchedEffect` spielte deshalb denselben Ton noch
 * einmal (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
 *
 * Gedreht wird echt über `ActivityScenario.recreate()`. Gezählt wird, wie oft [SoundPlayer.play]
 * und [Narrator.speak] gerufen werden, nicht was aus dem Lautsprecher kommt – das bleibt
 * gerätespezifisch (`docs/agents/testing.md`).
 */
@RunWith(AndroidJUnit4::class)
class EinmaligeAusgabenInstrumentedTest {

    /** Zählt ab dem ersten Frame mit, deshalb vor der Activity (`order = 0`). */
    @get:Rule(order = 0)
    val zaehler = AusgabenZaehler()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /** Wie oft die Startkarte bis zum Ende von [setUp] begrüßt hat. */
    private var begruessungenBeimStart = 0

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
        composeTestRule.waitForIdle()
        begruessungenBeimStart = zaehler.saetze.count { it == HOME_WELCOME_SPOKEN }
        zaehler.leeren()
    }

    @Test
    fun rechnen_nachDemDrehen_keinZweiterTon() {
        oeffneGrundrechenarten()
        scrolleUndTippe("start_button")

        beantworteAktuelleAufgabe(richtig = true)
        assertEquals(listOf(GameSound.CORRECT), zaehler.toene)

        drehe()
        assertEquals(listOf(GameSound.CORRECT), zaehler.toene)

        // Ohne Drehen weiterhin genau ein Ton je geprüfter Aufgabe.
        scrolleUndTippe("next_button")
        beantworteAktuelleAufgabe(richtig = false)
        assertEquals(listOf(GameSound.CORRECT, GameSound.WRONG), zaehler.toene)
    }

    @Test
    fun auswertung_nachDemDrehen_keinZweitesGeschafft() {
        oeffneGrundrechenarten()
        scrolleUndTippe("start_button")
        repeat(RUNDENLAENGE) {
            beantworteAktuelleAufgabe(richtig = true)
            scrolleUndTippe("next_button")
        }
        composeTestRule.onNodeWithTag("stats_result_text").assertExists()
        assertEquals(1, zaehler.toene.count { it == GameSound.FINISH })

        drehe()
        composeTestRule.onNodeWithTag("stats_result_text").assertExists()
        assertEquals(1, zaehler.toene.count { it == GameSound.FINISH })
    }

    @Test
    fun zahlenmauer_nachDemDrehen_keinZweiterTon() {
        tippe("island_rechnen")
        tippe("game_zahlenmauer")
        scrolleUndTippe("difficulty_LEICHT")
        scrolleUndTippe("start_button")

        mauerLuecken().forEach { luecke ->
            scrolleUndTippe(luecke)
            scrolleUndTippe("digit_1")
        }
        scrolleUndTippe("digit_check")
        composeTestRule.onNodeWithTag("zahlenmauer_result_text").assertExists()
        assertEquals(1, zaehler.toene.size)

        drehe()
        assertEquals(1, zaehler.toene.size)
    }

    @Test
    fun nachbarzahlen_nachDemDrehen_keinZweiterTonUndKeineZweiteAnsage() {
        tippe("island_zahlenspiele")
        tippe("game_nachbarzahlen")
        composeTestRule.waitForIdle()
        zaehler.leeren()
        scrolleUndTippe("start_button")
        composeTestRule.waitForIdle()
        assertEquals("Eine Ansage zu Rundenbeginn", 1, zaehler.saetze.size)

        drehe()
        assertEquals(1, zaehler.saetze.size)

        val mitte = composeTestRule.onNodeWithTag("nachbarzahlen_zahl_MITTE", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }.trim().toInt()
        fuelleNachbarLuecke("LINKS", mitte - 1)
        fuelleNachbarLuecke("RECHTS", mitte + 1)
        scrolleUndTippe("digit_check")
        assertEquals(listOf(GameSound.CORRECT), zaehler.toene)

        drehe()
        assertEquals(listOf(GameSound.CORRECT), zaehler.toene)
        assertEquals(1, zaehler.saetze.size)
    }

    /**
     * Startkarte und Menüs begrüßen bei **jedem Besuch** – nach dem Drehen aber nicht noch einmal.
     * Ein Besuch ist es, wenn zuvor ein anderer Bildschirm sichtbar war.
     */
    @Test
    fun startkarteUndMenue_begruessenJeBesuch_nichtNachDemDrehen() {
        assertTrue("Die Startkarte begrüßt beim App-Start", begruessungenBeimStart >= 1)

        drehe()
        assertEquals("Nach dem Drehen keine zweite Begrüßung", 0, zaehler.saetze.count { it == HOME_WELCOME_SPOKEN })

        tippe("island_rechnen")
        composeTestRule.waitForIdle()
        assertEquals(1, zaehler.saetze.count { it == RECHNEN_BEGRUESSUNG })

        drehe()
        assertEquals(1, zaehler.saetze.count { it == RECHNEN_BEGRUESSUNG })

        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.waitForIdle()
        assertEquals("Zurück auf der Startkarte ist ein neuer Besuch", 1, zaehler.saetze.count { it == HOME_WELCOME_SPOKEN })

        tippe("island_rechnen")
        composeTestRule.waitForIdle()
        assertEquals(2, zaehler.saetze.count { it == RECHNEN_BEGRUESSUNG })
    }

    @Test
    fun zahlenspieleMenue_begruesstNachDemDrehenNichtNochEinmal() {
        tippe("island_zahlenspiele")
        assertEquals(1, zaehler.saetze.count { it == ZAHLENSPIELE_BEGRUESSUNG })

        drehe()

        assertEquals(1, zaehler.saetze.count { it == ZAHLENSPIELE_BEGRUESSUNG })
    }

    /** Dreht das Gerät – genauer: baut die Activity neu, wie es Android beim Drehen tut. */
    private fun drehe() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    private fun oeffneGrundrechenarten() {
        tippe("island_rechnen")
        tippe("game_grundrechenarten")
    }

    /** Beantwortet die Plus-Aufgabe, die gerade dasteht, und gibt mit ✓ ab. */
    private fun beantworteAktuelleAufgabe(richtig: Boolean) {
        val text = composeTestRule.onNodeWithTag("equation_text").fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
        val (a, b) = Regex("""(\d+) \+ (\d+) = .*""").find(text)?.destructured
            ?: error("Unerwartetes Aufgabenformat: $text")
        val summe = a.toInt() + b.toInt()
        val antwort = if (richtig) summe else summe + 1
        antwort.toString().forEach { tippe("digit_$it") }
        scrolleUndTippe("digit_check")
    }

    private fun mauerLuecken(): List<String> =
        composeTestRule.onAllNodes(istMauerzelle and hasClickAction())
            .fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }

    private fun fuelleNachbarLuecke(position: String, wert: Int) {
        tippe("nachbarzahlen_feld_$position")
        tippe("digit_clear")
        wert.toString().forEach { tippe("digit_$it") }
    }

    /**
     * Tippt und wartet, bis Compose fertig ist. Der Ton entsteht in einem Effekt nach dem
     * Neuzeichnen; ohne das Warten zählte ein unmittelbar folgendes `assertEquals` zu früh.
     */
    private fun tippe(tag: String) {
        composeTestRule.onNodeWithTag(tag).performClick()
        composeTestRule.waitForIdle()
    }

    /** Für Knöpfe auf Bildschirmen, die bei Anzeigegröße „groß" länger sind als der Schirm. */
    private fun scrolleUndTippe(tag: String) {
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private companion object {
        /** Die Voreinstellung des Aufgabenzahl-Reglers. */
        const val RUNDENLAENGE = 10
        const val RECHNEN_BEGRUESSUNG = "Rechnen. Suche dir eine Übung aus!"
        const val ZAHLENSPIELE_BEGRUESSUNG = "Zahlenspiele. Suche dir ein Spiel aus!"

        val istMauerzelle = SemanticsMatcher("ist eine Zelle der Zahlenmauer") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("zahlenmauer_cell_") == true
        }
    }
}

/**
 * Hängt sich für die Dauer eines Tests an [SoundPlayer] und [Narrator] und schreibt mit, was sie
 * abspielen bzw. vorlesen sollten.
 */
class AusgabenZaehler : TestWatcher() {
    val toene: MutableList<GameSound> = CopyOnWriteArrayList()
    val saetze: MutableList<String> = CopyOnWriteArrayList()

    fun leeren() {
        toene.clear()
        saetze.clear()
    }

    override fun starting(description: Description) {
        SoundPlayer.beobachter = { toene += it }
        Narrator.beobachter = { saetze += it }
    }

    override fun finished(description: Description) {
        SoundPlayer.beobachter = null
        Narrator.beobachter = null
    }
}
