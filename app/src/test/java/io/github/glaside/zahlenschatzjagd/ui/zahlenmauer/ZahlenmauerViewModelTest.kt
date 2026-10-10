package io.github.glaside.zahlenschatzjagd.ui.zahlenmauer

import io.github.glaside.zahlenschatzjagd.BadgeTier
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.ZahlenmauerDifficulty
import io.github.glaside.zahlenschatzjagd.ZahlenmauerPos
import io.github.glaside.zahlenschatzjagd.ZahlenmauerSettings
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

@OptIn(ExperimentalCoroutinesApi::class)
class ZahlenmauerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        difficulty: ZahlenmauerDifficulty = ZahlenmauerDifficulty.MITTEL,
        wallCount: Int = 1,
        seed: Int = 1,
        taskRepository: TaskRepository? = null,
    ): ZahlenmauerViewModel = ZahlenmauerViewModel(
        settings = ZahlenmauerSettings(difficulty = difficulty, wallCount = wallCount),
        taskRepository = taskRepository,
        random = Random(seed),
    )

    /**
     * Befüllt alle Lücken richtig, nur die oberste falsch. Von der obersten Lücke hängt keine
     * andere ab, so ist **genau eine** Lücke falsch – auch bei lokaler Prüfung gegen die Eingaben.
     */
    private fun ZahlenmauerViewModel.loeseBisAufDieObersteLuecke() {
        val gaps = uiState.value.wall!!.gaps
        val oberste = gaps.maxBy { it.pos.row }
        gaps.forEach { cell -> enter(cell.pos, if (cell == oberste) cell.solution + 1 else cell.solution) }
    }

    private fun ZahlenmauerViewModel.enter(pos: ZahlenmauerPos, value: Int) {
        selectCell(pos)
        onClear()
        value.toString().forEach { onDigit(it.toString()) }
    }

    @Test
    fun onCheck_followUpDerivedFromOwnMistake_isCorrect_butTheMistakeItselfStaysWrong() {
        // Szenario aus dem Zahlenmauer-Folgeprompt: Zelle g wird bewusst falsch beantwortet
        // (Rechenfehler), die direkt darüberliegende Zelle "parent" wird korrekt AUS DIESEM
        // FALSCHEN WERT abgeleitet beantwortet - g muss falsch, parent muss richtig gewertet
        // werden (lokale statt globale Prüfung, siehe ZahlenmauerViewModel.effectiveValue).
        val vm = viewModel()
        val wall = vm.uiState.value.wall!!

        val g = ZahlenmauerPos(1, 0) // Aufbau-Lücke mit eigenem Rechenfehler
        val sibling = ZahlenmauerPos(1, 1) // korrekt beantwortete Nachbarzelle
        val parent = ZahlenmauerPos(2, 0) // hängt direkt von g und sibling ab

        val wrongValue = wall.cellAt(g.row, g.col).solution + 1
        val siblingValue = wall.cellAt(sibling.row, sibling.col).solution
        val derivedFromMistake = wrongValue + siblingValue

        vm.enter(g, wrongValue)
        vm.enter(sibling, siblingValue)
        vm.enter(parent, derivedFromMistake)
        // Übrige Lücken mit der echten Lösung befüllen, nur damit "Prüfen" freigeschaltet wird.
        wall.gaps.filter { it.pos !in setOf(g, sibling, parent) }.forEach { cell ->
            vm.enter(cell.pos, cell.solution)
        }

        vm.onCheck()

        val state = vm.uiState.value
        assertEquals(false, state.entries.getValue(g).isCorrect)
        assertEquals(true, state.entries.getValue(sibling).isCorrect)
        assertEquals(true, state.entries.getValue(parent).isCorrect)
        // g bleibt falsch -> die ganze Mauer zählt trotz des korrekt abgeleiteten Folgeschritts
        // nicht als (komplett) richtig gelöst.
        assertEquals(0, state.correctWalls)
        assertEquals(1, state.totalWallsChecked)
    }

    @Test
    fun onCheck_allEntriesCorrect_countsTheWholeWallAsOneCorrectTask() {
        val vm = viewModel(seed = 2)
        val wall = vm.uiState.value.wall!!

        wall.gaps.forEach { cell -> vm.enter(cell.pos, cell.solution) }
        vm.onCheck()

        val state = vm.uiState.value
        assertTrue(state.entries.values.all { it.isCorrect == true })
        // Eine Mauer zählt als GENAU EINE Aufgabe, nicht als eine Aufgabe je Zelle.
        assertEquals(1, state.correctWalls)
        assertEquals(1, state.totalWallsChecked)
    }

    @Test
    fun pruefen_ganzRichtigeMauer_klingtRichtig() {
        val vm = viewModel(seed = 2)
        vm.uiState.value.wall!!.gaps.forEach { cell -> vm.enter(cell.pos, cell.solution) }

        vm.onCheck()

        assertEquals(GameSound.CORRECT, vm.uiState.value.ausstehenderTon)
    }

    /** Ein Ton für die ganze Mauer: Auch eine fast richtige Mauer klingt „falsch". */
    @Test
    fun pruefen_eineLueckeFalsch_klingtFalsch() {
        val vm = viewModel(seed = 4)
        vm.uiState.value.wall!!.gaps.forEachIndexed { index, cell ->
            vm.enter(cell.pos, if (index == 0) cell.solution + 1 else cell.solution)
        }

        vm.onCheck()

        assertEquals(GameSound.WRONG, vm.uiState.value.ausstehenderTon)
    }

    /** `docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`: Teilerfolg füllt still die Truhe. */
    @Test
    fun eineTeilweiseRichtigeMauer_bringtDenErtrag1_undBleibtInDerHistorieFalsch() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(difficulty = ZahlenmauerDifficulty.MITTEL, taskRepository = TaskRepository(dao))

        vm.loeseBisAufDieObersteLuecke()
        vm.onCheck()
        advanceUntilIdle()

        val zeile = dao.eingefuegt.single()
        assertEquals(1, zeile.ertrag)
        assertEquals(false, zeile.isCorrect)
        // Die Sterne der Auswertung zählen weiter nur ganz richtige Mauern.
        assertEquals(0, vm.uiState.value.correctWalls)
    }

    /**
     * Die Truhe rechnet mit dem Ertrag, nicht mit ganz richtigen Mauern: Der Bestand bringt 19
     * (neun ganze Mauern zu 2, eine teilweise zu 1), die teilweise richtige Mauer dieser Runde
     * noch 1 dazu – das ist Bronze. Mit gezählten richtigen Mauern stünde es bei 9.
     */
    @Test
    fun dieNeuenAbzeichenDerAuswertungRechnenMitDemErtrag() = runTest {
        val kennung = ZahlenmauerDifficulty.MITTEL.kennung
        val zahlenraum = ZahlenmauerDifficulty.MITTEL.range
        fun mauer(richtig: Boolean, ertrag: Int) = TaskHistory(
            userName = UserPreferencesRepository.DEFAULT_USER,
            taskCategory = kennung,
            numberRange = zahlenraum,
            questionText = "Zahlenmauer (MITTEL, 6 Lücken)",
            expectedAnswer = "",
            userAnswer = "",
            isCorrect = richtig,
            ertrag = ertrag,
        )
        val bisher = List(9) { mauer(richtig = true, ertrag = 2) } + mauer(richtig = false, ertrag = 1)
        val vm = viewModel(
            difficulty = ZahlenmauerDifficulty.MITTEL,
            taskRepository = TaskRepository(FakeTaskHistoryDao(bisher)),
        )
        advanceUntilIdle()

        vm.loeseBisAufDieObersteLuecke()
        vm.onCheck()
        vm.onNextWall()
        advanceUntilIdle()

        assertEquals(
            listOf(UnlockedBadge(kennung, zahlenraum, BadgeTier.BRONZE)),
            vm.uiState.value.newlyUnlockedBadges,
        )
    }

    /** Quittiert heißt gespielt: Nach dem Neuaufbau des Bildschirms kommt kein zweiter Ton. */
    @Test
    fun quittierterTon_stehtNichtMehrAus() {
        val vm = viewModel(seed = 2)
        vm.uiState.value.wall!!.gaps.forEach { cell -> vm.enter(cell.pos, cell.solution) }
        vm.onCheck()

        vm.tonGespielt()

        assertNull(vm.uiState.value.ausstehenderTon)
        assertTrue("Die Auswertung bleibt sichtbar", vm.uiState.value.checked)
    }

    @Test
    fun vorDemPruefen_keinTon() {
        val vm = viewModel()
        vm.uiState.value.wall!!.gaps.forEach { cell -> vm.enter(cell.pos, cell.solution) }

        assertNull(vm.uiState.value.ausstehenderTon)
    }

    /**
     * Der ✓ im Ziffernblock ist nie gesperrt.
     * Ein Tippen darauf, solange eine Lücke leer ist, darf die Mauer deshalb nicht werten.
     */
    @Test
    fun pruefen_mitLeererLuecke_bewirktNichts() {
        val vm = viewModel()
        val gaps = vm.uiState.value.wall!!.gaps
        gaps.drop(1).forEach { cell -> vm.enter(cell.pos, cell.solution) }

        vm.onCheck()

        assertFalse(vm.uiState.value.checked)
        assertEquals(0, vm.uiState.value.totalWallsChecked)
        assertNull(vm.uiState.value.ausstehenderTon)
    }

    /**
     * Der Bildschirm spielt den Ton, sobald [ZahlenmauerUiState.totalWallsChecked] steigt. Ein
     * zweites „Prüfen" auf derselben Mauer darf ihn deshalb nicht weiterzählen.
     */
    @Test
    fun erneutesPruefen_derselbenMauer_zaehltNichtNochEinmal() {
        val vm = viewModel()
        vm.uiState.value.wall!!.gaps.forEach { cell -> vm.enter(cell.pos, cell.solution) }
        vm.onCheck()

        vm.onCheck()

        assertEquals(1, vm.uiState.value.totalWallsChecked)
    }

    @Test
    fun onCheck_oneWrongCellInWall_doesNotCountWallAsCorrect_evenThoughMostCellsAreRight() {
        val vm = viewModel(seed = 4)
        val wall = vm.uiState.value.wall!!

        wall.gaps.forEachIndexed { index, cell ->
            val value = if (index == 0) cell.solution + 1 else cell.solution
            vm.enter(cell.pos, value)
        }
        vm.onCheck()

        val state = vm.uiState.value
        assertEquals(0, state.correctWalls)
        assertEquals(1, state.totalWallsChecked)
    }

    /**
     * Auch die Zahlenmauer setzt ihren Punkt im Übungskalender erst am Ende der ganzen Runde –
     * nicht nach jeder Mauer (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`).
     */
    @Test
    fun abgeschlosseneRunde_wirdErstNachDerLetztenMauerFestgehalten() = runTest {
        val rundenDao = FakeRundenabschlussDao()
        val vm = ZahlenmauerViewModel(
            settings = ZahlenmauerSettings(
                difficulty = ZahlenmauerDifficulty.MITTEL,
                wallCount = 2,
            ),
            rundenRepository = RundenRepository(rundenDao),
            random = Random(5),
        )

        repeat(2) {
            vm.uiState.value.wall!!.gaps.forEach { cell -> vm.enter(cell.pos, cell.solution) }
            vm.onCheck()
            if (it == 0) {
                vm.onNextWall()
                assertTrue(rundenDao.eingefuegt.isEmpty())
            }
        }
        vm.onNextWall()
        // Und ein zweiter Tipp auf „Weiter" beendet die Runde nicht noch einmal.
        vm.onNextWall()

        assertTrue(vm.uiState.value.isFinished)
        assertEquals(1, rundenDao.eingefuegt.size)
        assertEquals(UserPreferencesRepository.DEFAULT_USER, rundenDao.eingefuegt.first().profil)
    }

    @Test
    fun onCheck_acrossMultipleWalls_accumulatesPerWallNotPerCell() {
        val vm = viewModel(wallCount = 2, seed = 5)

        val firstWall = vm.uiState.value.wall!!
        firstWall.gaps.forEach { cell -> vm.enter(cell.pos, cell.solution) }
        vm.onCheck()
        vm.onNextWall()

        val secondWall = vm.uiState.value.wall!!
        secondWall.gaps.forEachIndexed { index, cell ->
            val value = if (index == 0) cell.solution + 1 else cell.solution
            vm.enter(cell.pos, value)
        }
        vm.onCheck()

        val state = vm.uiState.value
        // Erste Mauer komplett richtig, zweite Mauer hat eine falsche Zelle -> über beide
        // Mauern hinweg zählt nur 1 richtige Aufgabe, nicht die Summe der richtigen Zellen.
        assertEquals(1, state.correctWalls)
        assertEquals(2, state.totalWallsChecked)
    }

    @Test
    fun ultraBaseGaps_haveOnlyGivenNeighbors_soNoFollowUpChainCanOccurThere() {
        // Absicherung der Annahme hinter dem unveränderten row==0-Zweig in onCheck(): die
        // stützende Elternzelle und die Nachbar-Basiszelle einer Ultra-Basislücke sind laut
        // applyUltraMask() in ZahlenmauerLogic.kt immer isGiven/fix - es gibt dort strukturell
        // keine Kette aus mehreren Lücken, gegen die lokal statt global geprüft werden müsste.
        repeat(50) { seed ->
            val wall = viewModel(difficulty = ZahlenmauerDifficulty.ULTRA, seed = seed).uiState.value.wall!!
            val floors = wall.difficulty.floors
            wall.gaps.filter { it.pos.row == 0 }.forEach { gap ->
                val col = gap.pos.col
                val leftParentGiven = col > 0 && wall.cellAt(1, col - 1).isGiven
                val rightParentGiven = col < floors - 1 && wall.cellAt(1, col).isGiven
                assertTrue(
                    "Basis-Lücke $col sollte eine aufgedeckte Stützzelle haben",
                    leftParentGiven || rightParentGiven
                )
                val sibling = if (leftParentGiven) wall.cellAt(0, col - 1) else wall.cellAt(0, col + 1)
                assertTrue("Nachbar-Basiszelle sollte given sein", sibling.isGiven)
            }
        }
    }
}
