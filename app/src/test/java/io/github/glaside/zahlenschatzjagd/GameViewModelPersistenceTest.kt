package io.github.glaside.zahlenschatzjagd

import io.github.glaside.zahlenschatzjagd.data.FakeRundenabschlussDao
import io.github.glaside.zahlenschatzjagd.data.FakeTaskHistoryDao
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.TaskHistoryDao
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.ui.game.GameViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelPersistenceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(dao: TaskHistoryDao, count: Int = 3) = GameViewModel(
        settings = GameSettings(types = listOf("Plus"), range = 10, count = count),
        taskRepository = TaskRepository(dao),
        random = Random(1234),
    )

    /** Legt [n] bereits richtig gelöste Plus-Aufgaben (Zahlenraum 10) in den Fake-DAO. */
    private suspend fun FakeTaskHistoryDao.seedCorrectPlus(n: Int) {
        repeat(n) {
            insertTask(
                TaskHistory(
                    userName = UserPreferencesRepository.DEFAULT_USER,
                    taskCategory = "PLUS",
                    numberRange = 10,
                    questionText = "1 + 1 = _",
                    expectedAnswer = "2",
                    userAnswer = "2",
                    isCorrect = true,
                )
            )
        }
    }

    /** Spielt genau eine (richtig beantwortete) Aufgabe und beendet die Runde. */
    private fun GameViewModel.playOneCorrectAndFinish() {
        val task = uiState.value.currentTask!!
        task.correctAnswer.toString().forEach { onDigit(it.toString()) }
        onCheck()
        onNext()
    }

    @Test
    fun onCheck_recordsAnsweredTaskWithDefaultUser() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(dao)
        val task = vm.uiState.value.currentTask!!

        task.correctAnswer.toString().forEach { vm.onDigit(it.toString()) }
        vm.onCheck()

        assertEquals(1, dao.eingefuegt.size)
        val entry = dao.eingefuegt.first()
        assertEquals(UserPreferencesRepository.DEFAULT_USER, entry.userName)
        assertEquals(task.correctAnswer.toString(), entry.expectedAnswer)
        assertEquals(task.correctAnswer.toString(), entry.userAnswer)
        assertTrue(entry.isCorrect)
    }

    @Test
    fun onCheck_recordsWrongAnswer() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = viewModel(dao)
        val task = vm.uiState.value.currentTask!!

        (task.correctAnswer + 1).toString().forEach { vm.onDigit(it.toString()) }
        vm.onCheck()

        assertEquals(1, dao.eingefuegt.size)
        assertFalse(dao.eingefuegt.first().isCorrect)
    }

    @Test
    fun finishingRound_crossingThreshold_unlocksBronzeBadge() = runTest {
        val dao = FakeTaskHistoryDao()
        dao.seedCorrectPlus(19) // die 20. richtige Plus-Aufgabe löst Bronze aus
        val vm = viewModel(dao, count = 1)

        vm.playOneCorrectAndFinish()

        val state = vm.uiState.value
        assertTrue(state.isFinished)
        assertEquals(
            listOf(UnlockedBadge("PLUS", 10, BadgeTier.BRONZE)),
            state.newlyUnlockedBadges
        )
    }

    @Test
    fun finishingRound_alreadyPastThreshold_unlocksNothing() = runTest {
        val dao = FakeTaskHistoryDao()
        dao.seedCorrectPlus(20) // Bronze war schon vor der Runde erreicht
        val vm = viewModel(dao, count = 1)

        vm.playOneCorrectAndFinish()

        val state = vm.uiState.value
        assertTrue(state.isFinished)
        assertTrue(state.newlyUnlockedBadges.isEmpty())
    }

    /**
     * Der Punkt, den der Übungskalender an einem Tag setzt: **eine** abgeschlossene Runde, nicht
     * eine beantwortete Aufgabe (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`).
     */
    @Test
    fun abgeschlosseneRunde_wirdFestgehalten() = runTest {
        val rundenDao = FakeRundenabschlussDao()
        val vm = GameViewModel(
            settings = GameSettings(types = listOf("Plus"), range = 10, count = 1),
            taskRepository = TaskRepository(FakeTaskHistoryDao()),
            rundenRepository = RundenRepository(rundenDao),
            random = Random(1234),
        )

        vm.playOneCorrectAndFinish()

        assertEquals(1, rundenDao.eingefuegt.size)
        assertEquals(UserPreferencesRepository.DEFAULT_USER, rundenDao.eingefuegt.first().profil)
    }

    /**
     * Bis die Oberfläche zur Auswertung wechselt, steht „Weiter" noch da. Ein zweiter Tipp darauf
     * darf die Runde nicht ein zweites Mal beenden – sonst zählte der Übungskalender am Tag eine
     * Runde zu viel.
     */
    @Test
    fun zweiterTippAufWeiter_beendetDieRundeNichtZweimal() = runTest {
        val rundenDao = FakeRundenabschlussDao()
        val vm = GameViewModel(
            settings = GameSettings(types = listOf("Plus"), range = 10, count = 1),
            taskRepository = TaskRepository(FakeTaskHistoryDao()),
            rundenRepository = RundenRepository(rundenDao),
            random = Random(1234),
        )

        vm.playOneCorrectAndFinish()
        vm.onNext()
        vm.onNext()

        assertEquals(1, rundenDao.eingefuegt.size)
    }

    @Test
    fun beantworteteAufgabeOhneRundenende_wirdNichtFestgehalten() = runTest {
        val rundenDao = FakeRundenabschlussDao()
        val vm = GameViewModel(
            settings = GameSettings(types = listOf("Plus"), range = 10, count = 5),
            taskRepository = TaskRepository(FakeTaskHistoryDao()),
            rundenRepository = RundenRepository(rundenDao),
            random = Random(1234),
        )

        vm.playOneCorrectAndFinish() // erst 1 von 5 Aufgaben – die Runde läuft weiter

        assertFalse(vm.uiState.value.isFinished)
        assertTrue(rundenDao.eingefuegt.isEmpty())
    }

    private fun verliebteViewModel(dao: TaskHistoryDao, count: Int = 3) = GameViewModel(
        settings = GameSettings(types = listOf("Verliebte"), range = 10, count = count, showCoins = true),
        taskRepository = TaskRepository(dao),
        random = Random(1234),
    )

    @Test
    fun verliebteZahlen_speichernHistorieMitLueckeAnDerRichtigenStelle() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = verliebteViewModel(dao)
        val task = vm.uiState.value.currentTask!!

        task.correctAnswer.toString().forEach { vm.onDigit(it.toString()) }
        vm.onCheck()

        assertEquals(1, dao.eingefuegt.size)
        val entry = dao.eingefuegt.first()
        assertEquals("VERLIEBTE_ZAHLEN", entry.taskCategory)
        // Die Lücke steht je nach task.firstIsBlank vorne oder hinten (siehe Task.equation).
        val expected = if (task.firstIsBlank) "_ + ${task.num1} = 10" else "${task.num1} + _ = 10"
        assertEquals(expected, entry.questionText)
    }

    /**
     * Beim Vergleich wählt ein Tippen nur aus: Umentscheiden hinterlässt nichts, in die
     * Historie geht genau die mit ✓ abgegebene Antwort – und erst dann.
     */
    @Test
    fun vergleichen_nurDieAbgegebeneAntwortGehtInDieHistorie() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = GameViewModel(
            settings = GameSettings(types = listOf("Vergleichen"), range = 20, count = 3),
            taskRepository = TaskRepository(dao),
            random = Random(1234),
        )
        val task = vm.uiState.value.currentTask!!
        val falsch = if (task.operation == "<") ">" else "<"

        vm.onCompare(falsch)
        vm.onCompare(task.operation)
        assertTrue("Vor dem Abgeben steht nichts in der Historie", dao.eingefuegt.isEmpty())

        vm.onCheck()

        val eintrag = dao.eingefuegt.single()
        assertEquals(task.operation, eintrag.userAnswer)
        assertTrue(eintrag.isCorrect)
    }

    /** Die Schalterstellung bestimmt die Kennung, unter der die Runde in die Historie geht. */
    @Test
    fun verliebteZahlen_ohneHilfe_speichernUnterEigenerKennung() = runTest {
        val dao = FakeTaskHistoryDao()
        val vm = GameViewModel(
            settings = GameSettings(types = listOf("Verliebte"), range = 10, count = 3, showCoins = false),
            taskRepository = TaskRepository(dao),
            random = Random(1234),
        )

        vm.playOneCorrectAndFinish()

        assertEquals("VERLIEBTE_ZAHLEN_OHNE_HILFE", dao.eingefuegt.single().taskCategory)
    }

    @Test
    fun verliebteZahlen_kreuzenDerSchwelle_schaltetBronzeFrei() = runTest {
        // Regressions-Absicherung von categoryForType: die Baseline muss unter dem Key
        // "VERLIEBTE_ZAHLEN" laden, sonst würde nie ein Abzeichen freigeschaltet.
        val dao = FakeTaskHistoryDao()
        repeat(19) {
            dao.insertTask(
                TaskHistory(
                    userName = UserPreferencesRepository.DEFAULT_USER,
                    taskCategory = "VERLIEBTE_ZAHLEN",
                    numberRange = 10,
                    questionText = "6 + _ = 10",
                    expectedAnswer = "4",
                    userAnswer = "4",
                    isCorrect = true,
                )
            )
        }
        val vm = verliebteViewModel(dao, count = 1)

        vm.playOneCorrectAndFinish()

        val state = vm.uiState.value
        assertTrue(state.isFinished)
        assertEquals(
            listOf(UnlockedBadge("VERLIEBTE_ZAHLEN", 10, BadgeTier.BRONZE)),
            state.newlyUnlockedBadges
        )
    }
}
