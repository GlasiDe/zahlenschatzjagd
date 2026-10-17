package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import io.github.glaside.zahlenschatzjagd.BadgeTier
import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.EinmaleinsSettings
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
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

/**
 * Eine **Aufsagen**-Runde (Regeln: `LEHRPLAN.md`, Abschnitt „Einmaleins"). Geprüft wird das äußere
 * Verhalten: welches Kästchen dran ist, was gewertet wird, was in der Historie landet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AufsagenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        reihen: List<Int> = listOf(7),
        taskRepository: TaskRepository? = null,
        rundenRepository: RundenRepository? = null,
    ) = AufsagenViewModel(
        settings = EinmaleinsSettings(EinmaleinsSchwierigkeitsstufe.AUFSAGEN, reihen, aufgabenzahl = 10),
        taskRepository = taskRepository,
        rundenRepository = rundenRepository,
    )

    private fun AufsagenViewModel.tippe(wert: Int) {
        onLoeschen()
        wert.toString().forEach { onZiffer(it.toString()) }
        onPruefen()
    }

    /** Sagt die aktuelle Reihe richtig auf. */
    private fun AufsagenViewModel.sageAuf() {
        val reihe = uiState.value.reihe
        (1..10).forEach { tippe(it * reihe) }
    }

    @Test
    fun dasErsteKaestchenIstAktiv_undDieReiheStehtFest() {
        val vm = viewModel()

        assertEquals(7, vm.uiState.value.reihe)
        assertEquals(0, vm.uiState.value.aktivesKaestchen)
    }

    @Test
    fun einRichtigesKaestchen_zaehlt_unddasNaechsteIstDran() {
        val vm = viewModel()

        vm.tippe(7)

        val state = vm.uiState.value
        assertEquals(listOf(Kaestchen("7", richtig = true)), state.kaestchen)
        assertEquals(1, state.aktivesKaestchen)
        assertEquals("", state.eingabe)
        assertEquals(1, state.richtige)
        assertEquals(GameSound.CORRECT, state.ausstehenderTon)
    }

    /** Ein falsches Kästchen wird markiert, und es geht trotzdem weiter – ohne zweiten Versuch. */
    @Test
    fun einFalschesKaestchen_wirdMarkiert_undEsGehtWeiter() {
        val vm = viewModel()

        vm.tippe(7)
        vm.tippe(15)

        val state = vm.uiState.value
        assertEquals(Kaestchen("15", richtig = false), state.kaestchen[1])
        assertEquals(2, state.aktivesKaestchen)
        assertEquals(1, state.richtige)
        assertEquals(2, state.gepruefte)
        assertEquals(GameSound.WRONG, state.ausstehenderTon)
    }

    @Test
    fun ohneEingabe_bewirktDerHakenNichts() {
        val vm = viewModel()

        vm.onPruefen()

        assertTrue(vm.uiState.value.kaestchen.isEmpty())
    }

    @Test
    fun eineEingabeNimmtHoechstensDreiZiffernAuf() {
        val vm = viewModel(reihen = listOf(10))
        repeat(9) { vm.tippe((it + 1) * 10) }

        "1000".forEach { vm.onZiffer(it.toString()) }

        assertEquals("100", vm.uiState.value.eingabe)
    }

    @Test
    fun nachDemZehntenKaestchen_istDieReiheFertig_undNimmtNichtsMehrAn() {
        val vm = viewModel()
        vm.sageAuf()

        vm.onZiffer("5")
        vm.onPruefen()

        val state = vm.uiState.value
        assertTrue(state.reiheFertig)
        assertNull(state.aktivesKaestchen)
        assertEquals(10, state.kaestchen.size)
        assertEquals("", state.eingabe)
    }

    @Test
    fun weiter_vorDemEndeDerReihe_bewirktNichts() {
        val vm = viewModel(reihen = listOf(3, 7))
        vm.tippe(3)

        vm.onWeiter()

        assertEquals(3, vm.uiState.value.reihe)
        assertEquals(1, vm.uiState.value.kaestchen.size)
    }

    @Test
    fun dieReihenKommenAufsteigend_jedeEinmal_undDanachIstDieRundeFertig() {
        val vm = viewModel(reihen = listOf(9, 3))

        assertEquals(3, vm.uiState.value.reihe)
        assertEquals(2, vm.uiState.value.reihenGesamt)
        vm.sageAuf()
        vm.onWeiter()

        assertEquals(9, vm.uiState.value.reihe)
        assertEquals(2, vm.uiState.value.reiheNummer)
        assertTrue(vm.uiState.value.kaestchen.isEmpty())
        vm.sageAuf()
        vm.onWeiter()

        assertTrue(vm.uiState.value.fertig)
        assertEquals(20, vm.uiState.value.richtige)
        assertEquals(20, vm.uiState.value.gepruefte)
    }

    /** Jedes Kästchen ist eine Zeile der Historie – unter der Kennung seiner Reihe, im Zahlenraum 100. */
    @Test
    fun jedesKaestchenWirdAlsAufgabeSeinerReiheFestgehalten() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(taskRepository = TaskRepository(dao))

        vm.tippe(7)
        vm.tippe(15)
        advanceUntilIdle()

        val (erste, zweite) = dao.eingefuegt
        assertEquals("EINMALEINS_7_AUFSAGEN", erste.taskCategory)
        assertEquals(100, erste.numberRange)
        assertEquals("1 · 7 = _", erste.questionText)
        assertEquals("7", erste.expectedAnswer)
        assertTrue(erste.isCorrect)
        assertEquals("2 · 7 = _", zweite.questionText)
        assertEquals("14", zweite.expectedAnswer)
        assertEquals("15", zweite.userAnswer)
        assertFalse(zweite.isCorrect)
        assertEquals(0, zweite.ertrag)
    }

    @Test
    fun eineDurchgezogeneRunde_setztGenauEinenRundenabschluss() = runTest {
        val rundenDao = FakeRundenabschlussDao()
        val vm = viewModel(
            taskRepository = TaskRepository(FakeTaskHistoryDao()),
            rundenRepository = RundenRepository(rundenDao),
        )
        vm.sageAuf()

        vm.onWeiter()
        vm.onWeiter()
        advanceUntilIdle()

        assertEquals(1, rundenDao.eingefuegt.size)
    }

    /** Die erste Truhe füllt sich bei 20 – nur die Reihe, deren Stand die Schwelle überschreitet. */
    @Test
    fun neueAbzeichen_jeReihe() = runTest {
        val bisher = List(15) {
            TaskHistory(
                userName = UserPreferencesRepository.DEFAULT_USER,
                taskCategory = "EINMALEINS_7_AUFSAGEN",
                numberRange = 100,
                questionText = "1 · 7 = _",
                expectedAnswer = "7",
                userAnswer = "7",
                isCorrect = true,
            )
        }
        val vm = viewModel(reihen = listOf(3, 7), taskRepository = TaskRepository(FakeTaskHistoryDao(bisher)))
        advanceUntilIdle()

        vm.sageAuf()
        vm.onWeiter()
        vm.sageAuf()
        vm.onWeiter()
        advanceUntilIdle()

        assertEquals(
            listOf(UnlockedBadge("EINMALEINS_7_AUFSAGEN", 100, BadgeTier.BRONZE)),
            vm.uiState.value.neueAbzeichen,
        )
    }
}
