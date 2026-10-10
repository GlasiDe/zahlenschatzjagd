package io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen

import io.github.glaside.zahlenschatzjagd.BadgeTier
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
import io.github.glaside.zahlenschatzjagd.Nachbarposition
import io.github.glaside.zahlenschatzjagd.NachbarzahlenSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.NachbarzahlenSettings
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.data.FakeRundenabschlussDao
import io.github.glaside.zahlenschatzjagd.data.FakeTaskHistoryDao
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/**
 * Die Wertung einer Nachbarzahlen-Runde (Vorbild: `ZahlenmauerViewModelTest`). Geprüft wird das
 * äußere Verhalten: was gewertet wird, was in der Historie landet, wann eine Runde endet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NachbarzahlenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zahlenraum = 20

    private fun viewModel(
        aufgabenzahl: Int = 1,
        seed: Int = 1,
        schwierigkeitsstufe: NachbarzahlenSchwierigkeitsstufe = NachbarzahlenSchwierigkeitsstufe.LEICHT,
        zahlenraum: Int = this.zahlenraum,
        taskRepository: TaskRepository? = null,
    ) = NachbarzahlenViewModel(
        settings = NachbarzahlenSettings(
            schwierigkeitsstufe = schwierigkeitsstufe,
            zahlenraum = zahlenraum,
            aufgabenzahl = aufgabenzahl,
        ),
        taskRepository = taskRepository,
        random = Random(seed),
    )

    /** Tippt [wert] in die Lücke an [position] – wie ein Kind: antippen, dann Ziffern. */
    private fun NachbarzahlenViewModel.tippe(position: Nachbarposition, wert: Int) {
        waehleLuecke(position)
        onLoeschen()
        wert.toString().forEach { onZiffer(it.toString()) }
    }

    /** Füllt beide Lücken mit der richtigen Zahl. */
    private fun NachbarzahlenViewModel.loeseRichtig() {
        val aufgabe = uiState.value.aufgabe!!
        aufgabe.luecken.forEach { tippe(it, aufgabe.zahlAn(it)) }
    }

    @Test
    fun pruefenIstGesperrt_solangeEineLueckeLeerIst() {
        val vm = viewModel()
        val aufgabe = vm.uiState.value.aufgabe!!

        assertFalse("Ohne Eingabe darf nicht geprüft werden", vm.uiState.value.pruefenMoeglich)

        vm.tippe(aufgabe.luecken.first(), aufgabe.zahlAn(aufgabe.luecken.first()))
        assertFalse("Mit nur einer gefüllten Lücke darf nicht geprüft werden", vm.uiState.value.pruefenMoeglich)

        vm.tippe(aufgabe.luecken.last(), aufgabe.zahlAn(aufgabe.luecken.last()))
        assertTrue(vm.uiState.value.pruefenMoeglich)
    }

    /**
     * Der ✓ im Ziffernblock ist nie gesperrt.
     * Ein Tippen darauf mit einer leeren Lücke darf die Reihe deshalb nicht werten.
     */
    @Test
    fun pruefen_mitLeererLuecke_bewirktNichts() {
        val vm = viewModel()
        val aufgabe = vm.uiState.value.aufgabe!!
        vm.tippe(aufgabe.luecken.first(), aufgabe.zahlAn(aufgabe.luecken.first()))

        vm.onPruefen()

        assertFalse(vm.uiState.value.geprueft)
        assertEquals(0, vm.uiState.value.gepruefte)
    }

    /** Ein Vertipper darf die Aufgabe nicht kosten: Vor dem Prüfen ist beliebiges Korrigieren möglich. */
    @Test
    fun vorDemPruefen_laesstSichEineEingabeBeliebigOftKorrigieren() {
        val vm = viewModel()
        val aufgabe = vm.uiState.value.aufgabe!!
        val luecke = aufgabe.luecken.first()

        vm.tippe(luecke, 99)
        vm.tippe(luecke, 5)
        vm.tippe(luecke, aufgabe.zahlAn(luecke))
        vm.tippe(aufgabe.luecken.last(), aufgabe.zahlAn(aufgabe.luecken.last()))
        vm.onPruefen()

        assertEquals(1, vm.uiState.value.richtige)
    }

    @Test
    fun nachDemPruefenNimmtDerBildschirmKeineEingabeMehrEntgegen() {
        val vm = viewModel()
        val aufgabe = vm.uiState.value.aufgabe!!
        vm.loeseRichtig()
        vm.onPruefen()

        val vorher = vm.uiState.value.eingaben
        vm.tippe(aufgabe.luecken.first(), 3)

        assertEquals("Nach dem Prüfen gibt es keinen zweiten Versuch", vorher, vm.uiState.value.eingaben)
    }

    @Test
    fun beideZahlenRichtig_zaehlenAlsEineRichtigeAufgabe() {
        val vm = viewModel()

        vm.loeseRichtig()
        vm.onPruefen()

        val state = vm.uiState.value
        assertTrue(state.eingaben.values.all { it.istRichtig == true })
        assertEquals(1, state.richtige)
        assertEquals(1, state.gepruefte)
    }

    @Test
    fun eineZahlFalsch_machtDieGanzeReiheFalsch() {
        val vm = viewModel()
        val aufgabe = vm.uiState.value.aufgabe!!

        vm.tippe(aufgabe.luecken.first(), aufgabe.zahlAn(aufgabe.luecken.first()) + 1)
        vm.tippe(aufgabe.luecken.last(), aufgabe.zahlAn(aufgabe.luecken.last()))
        vm.onPruefen()

        val state = vm.uiState.value
        assertEquals(false, state.eingaben.getValue(aufgabe.luecken.first()).istRichtig)
        assertEquals(true, state.eingaben.getValue(aufgabe.luecken.last()).istRichtig)
        // Die ganze Reihe ist EINE Aufgabe – eine falsche Zahl macht sie insgesamt falsch.
        assertEquals(0, state.richtige)
        assertEquals(1, state.gepruefte)
    }

    /**
     * Die Form, in der ein Erwachsener die Aufgabe im Übungsstand wiederfindet – gegen von Hand
     * geschriebene Werte geprüft, nicht reihum (siehe `docs/agents/testing.md`).
     */
    @Test
    fun dieGanzeReiheGehtAlsEineZeileInDieHistorieEin() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(taskRepository = TaskRepository(dao))
        val aufgabe = vm.uiState.value.aufgabe!!

        vm.loeseRichtig()
        vm.onPruefen()
        advanceUntilIdle()

        assertEquals(1, dao.eingefuegt.size)
        val zeile: TaskHistory = dao.eingefuegt.single()
        assertEquals("NACHBARZAHLEN_LEICHT", zeile.taskCategory)
        assertEquals(zahlenraum, zeile.numberRange)
        // Von Hand geschrieben, nicht aus dem Platzhalter des Codes zusammengesetzt: Ein Reihum-Test
        // ließe eine geänderte Form unbemerkt mitwandern (`docs/agents/testing.md`).
        assertEquals("_ ${aufgabe.mitte} _", zeile.questionText)
        assertEquals("${aufgabe.links}, ${aufgabe.rechts}", zeile.expectedAnswer)
        assertEquals("${aufgabe.links}, ${aufgabe.rechts}", zeile.userAnswer)
        assertTrue(zeile.isCorrect)
    }

    @Test
    fun dieHistorieHaeltAuchDieFalscheEingabeFest() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(taskRepository = TaskRepository(dao))
        val aufgabe = vm.uiState.value.aufgabe!!

        vm.tippe(Nachbarposition.LINKS, 3)
        vm.tippe(Nachbarposition.RECHTS, aufgabe.rechts)
        vm.onPruefen()
        advanceUntilIdle()

        val zeile = dao.eingefuegt.single()
        assertEquals("3, ${aufgabe.rechts}", zeile.userAnswer)
        assertEquals("${aufgabe.links}, ${aufgabe.rechts}", zeile.expectedAnswer)
        assertFalse(zeile.isCorrect)
    }

    @Test
    fun eineRundeLaeuftUeberMehrereAufgabenUndZaehltJedeEinzeln() {
        val vm = viewModel(aufgabenzahl = 2, seed = 3)

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        assertEquals(2, vm.uiState.value.aufgabenNummer)
        assertFalse("Die neue Aufgabe startet ungeprüft", vm.uiState.value.geprueft)

        val zweite = vm.uiState.value.aufgabe!!
        vm.tippe(Nachbarposition.LINKS, zweite.links + 1)
        vm.tippe(Nachbarposition.RECHTS, zweite.rechts)
        vm.onPruefen()

        assertEquals(1, vm.uiState.value.richtige)
        assertEquals(2, vm.uiState.value.gepruefte)
    }

    /**
     * Der Punkt des Übungskalenders steht erst am Ende der ganzen Runde
     * (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`) – und nur einmal.
     */
    @Test
    fun abgeschlosseneRunde_wirdErstNachDerLetztenAufgabeFestgehalten() = runTest {
        val rundenDao = FakeRundenabschlussDao()
        // Der einzige Test mit Runden-Repository – deshalb steht es hier und nicht als weiterer
        // Vorgabewert in [viewModel].
        val vm = NachbarzahlenViewModel(
            settings = NachbarzahlenSettings(
                schwierigkeitsstufe = NachbarzahlenSchwierigkeitsstufe.LEICHT,
                zahlenraum = zahlenraum,
                aufgabenzahl = 2,
            ),
            rundenRepository = RundenRepository(rundenDao),
            random = Random(4),
        )

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        assertTrue(rundenDao.eingefuegt.isEmpty())

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        // Und ein zweiter Tipp auf „Weiter" beendet die Runde nicht noch einmal.
        vm.onWeiter()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.fertig)
        assertEquals(1, rundenDao.eingefuegt.size)
        assertEquals(UserPreferencesRepository.DEFAULT_USER, rundenDao.eingefuegt.first().profil)
    }

    /** Die Abzeichen gehören der Kennung der Stufe und dem geübten Zahlenraum. */
    @Test
    fun eineNeueAbzeichenStufe_traegtKennungUndZahlenraumDerRunde() = runTest {
        val bisher = List(BadgeTier.BRONZE.threshold - 1) {
            TaskHistory(
                userName = UserPreferencesRepository.DEFAULT_USER,
                taskCategory = "NACHBARZAHLEN_LEICHT",
                numberRange = zahlenraum,
                questionText = "_ 7 _",
                expectedAnswer = "6, 8",
                userAnswer = "6, 8",
                isCorrect = true,
            )
        }
        val vm = viewModel(taskRepository = TaskRepository(FakeTaskHistoryDao(bisher)))
        advanceUntilIdle()

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        advanceUntilIdle()

        assertEquals(
            listOf(UnlockedBadge("NACHBARZAHLEN_LEICHT", zahlenraum, BadgeTier.BRONZE)),
            vm.uiState.value.neueAbzeichen,
        )
    }

    /**
     * Die schwere Stufe hält in der Historie fest, **welche** Zahl vorgegeben war – daran erkennt
     * ein Erwachsener im Übungsstand die Form, die das Kind vor sich hatte. Geprüft an der Gestalt
     * der Frage (Zahl gegen Platzhalter), nicht an konkreten Zahlen: Die hängen am Startwert des
     * Zufallsgenerators, die Gestalt nicht.
     */
    @Test
    fun dieSchwereStufeHaeltDieVorgegebenePositionInDerHistorieFest() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(
            aufgabenzahl = 15,
            seed = 5,
            schwierigkeitsstufe = NachbarzahlenSchwierigkeitsstufe.SCHWER,
            taskRepository = TaskRepository(dao),
        )

        repeat(15) {
            vm.loeseRichtig()
            vm.onPruefen()
            vm.onWeiter()
        }
        advanceUntilIdle()

        assertEquals(listOf("NACHBARZAHLEN_SCHWER"), dao.eingefuegt.map { it.taskCategory }.distinct())
        assertEquals(
            setOf("# _ _", "_ # _", "_ _ #"),
            dao.eingefuegt.map { it.questionText.replace(Regex("\\d+"), "#") }.toSet(),
        )
    }

    /** Die schwere Stufe füllt ihre **eigene** Truhe – sie ist eine andere Leistung als die leichte. */
    @Test
    fun dieAbzeichenDerSchwerenStufeGehoerenIhrerEigenenKennung() = runTest {
        val bisher = List(BadgeTier.BRONZE.threshold - 1) {
            TaskHistory(
                userName = UserPreferencesRepository.DEFAULT_USER,
                taskCategory = "NACHBARZAHLEN_SCHWER",
                numberRange = zahlenraum,
                questionText = "9 _ _",
                expectedAnswer = "10, 11",
                userAnswer = "10, 11",
                isCorrect = true,
            )
        }
        val vm = viewModel(
            schwierigkeitsstufe = NachbarzahlenSchwierigkeitsstufe.SCHWER,
            taskRepository = TaskRepository(FakeTaskHistoryDao(bisher)),
        )
        advanceUntilIdle()

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        advanceUntilIdle()

        assertEquals(
            listOf(UnlockedBadge("NACHBARZAHLEN_SCHWER", zahlenraum, BadgeTier.BRONZE)),
            vm.uiState.value.neueAbzeichen,
        )
    }

    /**
     * Der Fortschritt der leichten Stufe zählt nicht für die schwere: Beide Kennungen werden
     * getrennt gezählt, sonst ließe sich die schwere Truhe mit leichten Aufgaben füllen.
     */
    @Test
    fun derFortschrittDerLeichtenStufeZaehltNichtFuerDieSchwere() = runTest {
        val leichteRunden = List(BadgeTier.BRONZE.threshold) {
            TaskHistory(
                userName = UserPreferencesRepository.DEFAULT_USER,
                taskCategory = "NACHBARZAHLEN_LEICHT",
                numberRange = zahlenraum,
                questionText = "_ 7 _",
                expectedAnswer = "6, 8",
                userAnswer = "6, 8",
                isCorrect = true,
            )
        }
        val vm = viewModel(
            schwierigkeitsstufe = NachbarzahlenSchwierigkeitsstufe.SCHWER,
            taskRepository = TaskRepository(FakeTaskHistoryDao(leichteRunden)),
        )
        advanceUntilIdle()

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.neueAbzeichen.isEmpty())
    }

    @Test
    fun ohneRichtigeAufgabe_gibtEsKeinAbzeichen() = runTest {
        val vm = viewModel(taskRepository = TaskRepository(FakeTaskHistoryDao()))
        val aufgabe = vm.uiState.value.aufgabe!!
        advanceUntilIdle()

        vm.tippe(Nachbarposition.LINKS, aufgabe.links + 1)
        vm.tippe(Nachbarposition.RECHTS, aufgabe.rechts + 1)
        vm.onPruefen()
        vm.onWeiter()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.neueAbzeichen.isEmpty())
    }

    /**
     * Vorgelesen wird nur die **erste** Aufgabe einer Runde. Jede einzelne vorzulesen war am Gerät
     * lästig, und keine andere Übungsform tut es; ganz weglassen geht aber nicht, weil ein Kind,
     * das noch nicht sicher liest, sonst nicht erführe, was zu tun ist.
     */
    @Test
    fun vorgelesenWirdNurDieErsteAufgabeEinerRunde() {
        val vm = viewModel(aufgabenzahl = 3, seed = 7)

        assertEquals(vm.uiState.value.aufgabe, vm.uiState.value.ausstehendeAnsage)

        repeat(2) {
            vm.loeseRichtig()
            vm.onPruefen()
            vm.onWeiter()

            assertNull(
                "Aufgabe ${vm.uiState.value.aufgabenNummer} darf nicht mehr vorgelesen werden",
                vm.uiState.value.ausstehendeAnsage,
            )
        }
    }

    /** Einmal vorgelesen heißt erledigt – auch wenn der Bildschirm neu aufgebaut wird. */
    @Test
    fun dieQuittierteAnsageStehtNichtMehrAus() {
        val vm = viewModel(aufgabenzahl = 3, seed = 7)

        vm.ansageVorgelesen()

        assertNull(vm.uiState.value.ausstehendeAnsage)
    }

    @Test
    fun nachDemPruefenStehtDerTonAus_bisErQuittiertIst() {
        val vm = viewModel(aufgabenzahl = 3)
        assertNull("Vor dem Prüfen steht kein Ton aus", vm.uiState.value.ausstehenderTon)

        vm.loeseRichtig()
        vm.onPruefen()
        assertEquals(GameSound.CORRECT, vm.uiState.value.ausstehenderTon)

        vm.tonGespielt()
        assertNull(vm.uiState.value.ausstehenderTon)
        assertTrue("Die Auswertung bleibt sichtbar", vm.uiState.value.geprueft)
    }

    @Test
    fun eineFalscheZahlInDerReihe_klingtFalsch() {
        val vm = viewModel()
        val aufgabe = vm.uiState.value.aufgabe!!
        aufgabe.luecken.forEachIndexed { index, position ->
            vm.tippe(position, aufgabe.zahlAn(position) + if (index == 0) 1 else 0)
        }

        vm.onPruefen()

        assertEquals(GameSound.WRONG, vm.uiState.value.ausstehenderTon)
    }

    @Test
    fun zuBeginnIstDieErsteLueckeGewaehlt_damitSofortGetipptWerdenKann() {
        val vm = viewModel()

        assertEquals(Nachbarposition.LINKS, vm.uiState.value.gewaehlteLuecke)
        assertNull(vm.uiState.value.eingaben.getValue(Nachbarposition.LINKS).istRichtig)
    }

    /** Die vorgegebene Zahl ist keine Lücke – sie lässt sich nicht antippen und nicht überschreiben. */
    @Test
    fun dieVorgegebeneZahlLaesstSichNichtAnwaehlen() {
        val vm = viewModel()

        vm.waehleLuecke(Nachbarposition.MITTE)

        assertEquals(Nachbarposition.LINKS, vm.uiState.value.gewaehlteLuecke)
        assertFalse(Nachbarposition.MITTE in vm.uiState.value.eingaben)
    }

    /** Im Zahlenraum 20 sind alle Zahlen höchstens zweistellig – mehr nimmt die Lücke nicht an. */
    @Test
    fun eineLueckeNimmtHoechstensSoVieleZiffernWieDerZahlenraumBraucht() {
        val vm = viewModel()

        vm.waehleLuecke(Nachbarposition.LINKS)
        repeat(5) { vm.onZiffer("7") }

        assertEquals("77", vm.uiState.value.eingaben.getValue(Nachbarposition.LINKS).eingabe)
    }

    /**
     * Im Zahlenraum 100 gehört die 100 selbst zu den möglichen Lösungen – die Lücke muss also drei
     * Ziffern annehmen, und ein Vertipper darin muss sich löschen und neu tippen lassen.
     */
    @Test
    fun imZahlenraum100LaesstSichEineDreistelligeZahlEingebenUndKorrigieren() {
        val vm = viewModel(zahlenraum = 100)

        vm.tippe(Nachbarposition.LINKS, 111)
        assertEquals("111", vm.uiState.value.eingaben.getValue(Nachbarposition.LINKS).eingabe)

        vm.tippe(Nachbarposition.LINKS, 100)
        assertEquals("100", vm.uiState.value.eingaben.getValue(Nachbarposition.LINKS).eingabe)

        // Und keine vierte Ziffer – die gäbe es im Zahlenraum 100 nicht.
        vm.onZiffer("7")
        assertEquals("100", vm.uiState.value.eingaben.getValue(Nachbarposition.LINKS).eingabe)
    }

    /**
     * Der obere Rand des Zahlenraums 100 als ganze Aufgabe: `98 | 99 | 100` wird richtig gewertet
     * und geht mit dem Zahlenraum 100 in die Historie ein – dort füllt sie eine **andere** Truhe
     * als dieselbe Stufe bis 20.
     */
    @Test
    fun eineReiheAmOberenRandDesZahlenraums100_zaehltFuerDenZahlenraum100() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(aufgabenzahl = 99, zahlenraum = 100, taskRepository = TaskRepository(dao))

        // Bis zur Reihe 98 | 99 | 100 durchspielen – ohne Zurücklegen kommt sie im vollen Vorrat vor.
        repeat(99) {
            vm.loeseRichtig()
            vm.onPruefen()
            vm.onWeiter()
        }
        advanceUntilIdle()

        val obererRand = dao.eingefuegt.single { it.questionText == "_ 99 _" }
        assertEquals("98, 100", obererRand.expectedAnswer)
        assertEquals("98, 100", obererRand.userAnswer)
        assertTrue(obererRand.isCorrect)
        assertEquals(listOf(100), dao.eingefuegt.map { it.numberRange }.distinct())
    }

    /** Die Truhe des Zahlenraums 100 füllt sich nicht mit Runden bis 20 – und umgekehrt. */
    @Test
    fun derFortschrittImZahlenraum20ZaehltNichtFuerDenZahlenraum100() = runTest {
        val bis20 = List(BadgeTier.BRONZE.threshold) {
            TaskHistory(
                userName = UserPreferencesRepository.DEFAULT_USER,
                taskCategory = "NACHBARZAHLEN_LEICHT",
                numberRange = 20,
                questionText = "_ 7 _",
                expectedAnswer = "6, 8",
                userAnswer = "6, 8",
                isCorrect = true,
            )
        }
        val vm = viewModel(zahlenraum = 100, taskRepository = TaskRepository(FakeTaskHistoryDao(bis20)))
        advanceUntilIdle()

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.neueAbzeichen.isEmpty())
    }

    /** Das Abzeichen einer Runde bis 100 trägt den Zahlenraum 100. */
    @Test
    fun eineNeueAbzeichenStufeImZahlenraum100_traegtDiesenZahlenraum() = runTest {
        val bisher = List(BadgeTier.BRONZE.threshold - 1) {
            TaskHistory(
                userName = UserPreferencesRepository.DEFAULT_USER,
                taskCategory = "NACHBARZAHLEN_LEICHT",
                numberRange = 100,
                questionText = "_ 40 _",
                expectedAnswer = "39, 41",
                userAnswer = "39, 41",
                isCorrect = true,
            )
        }
        val vm = viewModel(zahlenraum = 100, taskRepository = TaskRepository(FakeTaskHistoryDao(bisher)))
        advanceUntilIdle()

        vm.loeseRichtig()
        vm.onPruefen()
        vm.onWeiter()
        advanceUntilIdle()

        assertEquals(
            listOf(UnlockedBadge("NACHBARZAHLEN_LEICHT", 100, BadgeTier.BRONZE)),
            vm.uiState.value.neueAbzeichen,
        )
    }
}
