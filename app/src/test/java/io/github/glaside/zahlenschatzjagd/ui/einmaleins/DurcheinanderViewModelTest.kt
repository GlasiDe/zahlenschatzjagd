package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.EinmaleinsSettings
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
import io.github.glaside.zahlenschatzjagd.data.FakeRundenabschlussDao
import io.github.glaside.zahlenschatzjagd.data.FakeTaskHistoryDao
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
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

/** Eine **Durcheinander**-Runde (Regeln: `LEHRPLAN.md`, Abschnitt „Einmaleins"). */
@OptIn(ExperimentalCoroutinesApi::class)
class DurcheinanderViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        reihen: List<Int> = listOf(3, 7),
        aufgabenzahl: Int = 5,
        taskRepository: TaskRepository? = null,
        rundenRepository: RundenRepository? = null,
    ) = DurcheinanderViewModel(
        settings = EinmaleinsSettings(EinmaleinsSchwierigkeitsstufe.DURCHEINANDER, reihen, aufgabenzahl),
        taskRepository = taskRepository,
        rundenRepository = rundenRepository,
        random = Random(4),
    )

    private fun DurcheinanderViewModel.antworte(wert: Int) {
        onLoeschen()
        wert.toString().forEach { onZiffer(it.toString()) }
        onPruefen()
    }

    private fun DurcheinanderViewModel.antworteRichtig() = antworte(uiState.value.aufgabe.ergebnis)

    @Test
    fun dieAufgabenStammenAusDenGewaehltenReihen_unddieReiheStehtHinten() {
        val vm = viewModel(aufgabenzahl = 20)
        repeat(20) {
            val aufgabe = vm.uiState.value.aufgabe
            assertTrue("${aufgabe.text()}", aufgabe.reihe in listOf(3, 7))
            assertTrue(aufgabe.faktor in 1..10)
            vm.antworteRichtig()
            vm.onWeiter()
        }
    }

    @Test
    fun eineRichtigeAntwort_zaehlt() {
        val vm = viewModel()

        vm.antworteRichtig()

        assertEquals(true, vm.uiState.value.richtig)
        assertEquals(1, vm.uiState.value.richtige)
        assertEquals(GameSound.CORRECT, vm.uiState.value.ausstehenderTon)
    }

    @Test
    fun eineFalscheAntwort_zaehltNicht_undLaesstSichNichtMehrAendern() {
        val vm = viewModel()
        vm.antworte(vm.uiState.value.aufgabe.ergebnis + 1)

        vm.onZiffer("4")
        vm.onLoeschen()
        vm.onPruefen()

        assertEquals(false, vm.uiState.value.richtig)
        assertEquals(0, vm.uiState.value.richtige)
        assertEquals(GameSound.WRONG, vm.uiState.value.ausstehenderTon)
    }

    @Test
    fun ohneEingabe_bewirktDerHakenNichts() {
        val vm = viewModel()

        vm.onPruefen()

        assertNull(vm.uiState.value.richtig)
    }

    @Test
    fun weiterVorDemPruefen_bewirktNichts() {
        val vm = viewModel()
        val aufgabe = vm.uiState.value.aufgabe

        vm.onWeiter()

        assertEquals(aufgabe, vm.uiState.value.aufgabe)
        assertEquals(1, vm.uiState.value.aufgabenNummer)
    }

    @Test
    fun nachDerGewaehltenAnzahl_istDieRundeFertig_mitGenauEinemRundenabschluss() = runTest {
        val rundenDao = FakeRundenabschlussDao()
        val vm = viewModel(
            aufgabenzahl = 5,
            taskRepository = TaskRepository(FakeTaskHistoryDao()),
            rundenRepository = RundenRepository(rundenDao),
        )

        repeat(5) {
            assertFalse(vm.uiState.value.fertig)
            vm.antworteRichtig()
            vm.onWeiter()
        }
        vm.onWeiter()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.fertig)
        assertEquals(5, vm.uiState.value.richtige)
        assertEquals(1, rundenDao.eingefuegt.size)
    }

    /** Jede Aufgabe zählt auf die Kennung **ihrer** Reihe – eine Runde füllt mehrere Truhen. */
    @Test
    fun jedeAufgabeWirdUnterDerKennungIhrerReiheFestgehalten() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(aufgabenzahl = 20, taskRepository = TaskRepository(dao))

        repeat(20) {
            vm.antworteRichtig()
            vm.onWeiter()
        }
        advanceUntilIdle()

        assertEquals(20, dao.eingefuegt.size)
        assertEquals(
            setOf("EINMALEINS_3_DURCHEINANDER", "EINMALEINS_7_DURCHEINANDER"),
            dao.eingefuegt.map { it.taskCategory }.toSet(),
        )
        dao.eingefuegt.forEach { zeile ->
            assertEquals(100, zeile.numberRange)
            val reihe = zeile.taskCategory.split("_")[1]
            assertTrue(zeile.questionText, zeile.questionText.endsWith(" · $reihe = _"))
        }
    }
}
