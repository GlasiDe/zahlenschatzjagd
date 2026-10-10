package io.github.glaside.zahlenschatzjagd.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.BADGE_CATEGORIES
import io.github.glaside.zahlenschatzjagd.GameSettings
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.Task
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.categoryForType
import io.github.glaside.zahlenschatzjagd.createTask
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.merkeFuerAktivesProfil
import io.github.glaside.zahlenschatzjagd.equation
import io.github.glaside.zahlenschatzjagd.expectedAnswer
import io.github.glaside.zahlenschatzjagd.isAnswerCorrect
import io.github.glaside.zahlenschatzjagd.newlyReachedTiers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Hält den kompletten Spielzustand einer Runde und kapselt die Zustandsübergänge, die
 * bisher als verschachtelte Funktionen in der GameScreen-Composable lagen. Die reine
 * Logik stammt unverändert aus [createTask]/[isAnswerCorrect] (GameLogic.kt).
 *
 * [random] ist injizierbar, damit das ViewModel in Unit-Tests deterministisch ist.
 */
class GameViewModel(
    private val settings: GameSettings,
    private val taskRepository: TaskRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val rundenRepository: RundenRepository? = null,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GameUiState(totalTasks = settings.count, showCoins = settings.showCoins)
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Abzeichen-Freischaltung race-frei: [baseline] ist der Gesamtstand je Kategorie zu
    // Rundenbeginn, [roundLocalCorrect] zählt die richtigen Antworten dieser Runde synchron.
    // Beim Rundenende ergibt baseline + roundLocal den neuen Stand – ohne die DB erneut zu
    // befragen und damit unabhängig vom asynchronen Speicher-Timing.
    private val baseline = mutableMapOf<String, Int>()
    private val roundLocalCorrect = mutableMapOf<String, Int>()
    private var baselineJob: Job? = null

    init {
        generateTask()
        baselineJob = loadBaselineIfPossible()
    }

    /** Lädt den Startstand je (Kategorie × Zahlenraum) der Runde, sofern ein Repo existiert. */
    private fun loadBaselineIfPossible(): Job? {
        val repo = taskRepository ?: return null
        return viewModelScope.launch {
            val user = userPreferencesRepository?.activeUser?.first()
                ?: UserPreferencesRepository.DEFAULT_USER
            val keys = settings.types
                .map { categoryForType(it, settings.multipleChoice, settings.showCoins) }
                .filter { key -> BADGE_CATEGORIES.any { it.key == key } }
                .distinct()
            for (key in keys) {
                baseline[key] = repo.summeErtrag(user, key, settings.range)
            }
        }
    }

    private fun generateTask() {
        val task = createTask(settings.types, settings.range, random, settings.multipleChoice, settings.showCoins)
        // Was zur vorigen Aufgabe noch aussteht, gehört nicht zur neuen.
        _uiState.update { it.copy(currentTask = task, userInput = "", feedback = null, ausstehenderTon = null) }
    }

    fun onDigit(digit: String) {
        _uiState.update {
            if (it.feedback != null || it.userInput.length >= 3) {
                it
            } else {
                it.copy(userInput = it.userInput + digit)
            }
        }
    }

    fun onClear() {
        _uiState.update { if (it.feedback != null) it else it.copy(userInput = "") }
    }

    /**
     * Gibt die Antwort ab – der ✓, gleich wie sie zustande kam: getippte Ziffern, ein gewähltes
     * Vergleichszeichen ([onCompare]) oder eine gewählte Zahl ([onSelectOption]). Ohne Eingabe
     * geschieht nichts.
     *
     * Beim Zahlvergleich wird das Zeichen mit [io.github.glaside.zahlenschatzjagd.Task.operation]
     * verglichen, und die Rückmeldung zeigt die vollständige richtige Ungleichung (z. B. „3 < 7").
     */
    fun onCheck() {
        val state = _uiState.value
        if (state.feedback != null) return
        val task = state.currentTask ?: return
        val eingabe = state.userInput

        if (task.category == "GROESSER_KLEINER") {
            if (eingabe.isEmpty()) return
            applyAnswer(
                task = task,
                userAnswer = eingabe,
                correct = eingabe == task.operation,
                feedbackAnswer = task.equation(task.operation),
            )
            return
        }

        if (eingabe.toIntOrNull() == null) return // wie bisher: nicht-numerische Eingabe ignorieren
        applyAnswer(
            task = task,
            userAnswer = eingabe,
            correct = isAnswerCorrect(eingabe, task.correctAnswer),
            feedbackAnswer = task.correctAnswer.toString(),
        )
    }

    /**
     * Wählt beim Zahlvergleich ein Relationszeichen („<", „=", „>") aus. Es steht dann in der
     * Lücke der Aufgabe und lässt sich bis zum Abgeben ([onCheck]) beliebig ändern – ein Tippen
     * wertet nicht mehr sofort aus.
     */
    fun onCompare(symbol: String) = waehleAus(symbol)

    /**
     * Wählt im leichten Verdoppeln-Modus eine der drei Antworten
     * ([io.github.glaside.zahlenschatzjagd.Task.answerOptions]) aus. Wie bei [onCompare] wird erst mit
     * [onCheck] abgegeben.
     */
    fun onSelectOption(value: Int) = waehleAus(value.toString())

    private fun waehleAus(eingabe: String) {
        _uiState.update { if (it.feedback != null) it else it.copy(userInput = eingabe) }
    }

    /**
     * Gemeinsamer Auswertungs-Kern von [onCheck]: zählt den Treffer, setzt das [Feedback] und
     * persistiert die Aufgabe. [feedbackAnswer] ist der im Feedback angezeigte
     * „Richtig ist: …"-Text. Bei „Verdoppeln" wird zusätzlich die Äquivalenz zu Addition und
     * Multiplikation angehängt (siehe LEHRPLAN.md, Abschnitt „Verdoppeln").
     */
    private fun applyAnswer(
        task: Task,
        userAnswer: String,
        correct: Boolean,
        feedbackAnswer: String,
    ) {
        if (correct) {
            roundLocalCorrect[task.category] = (roundLocalCorrect[task.category] ?: 0) + 1
        }
        val extraExplanation = if (task.category == "VERDOPPELN_LEICHT" || task.category == "VERDOPPELN_SCHWER") {
            // „2 · n" statt „n · 2": zweimal die Zahl, so wie Grundschulkinder Malaufgaben lesen.
            "${task.num1} + ${task.num1} = ${task.correctAnswer}   2 · ${task.num1} = ${task.correctAnswer}"
        } else {
            null
        }
        _uiState.update {
            it.copy(
                feedback = Feedback(correct, feedbackAnswer, extraExplanation),
                correctAnswers = if (correct) it.correctAnswers + 1 else it.correctAnswers,
                ausstehenderTon = if (correct) GameSound.CORRECT else GameSound.WRONG,
            )
        }
        recordTask(task, userAnswer, correct)
    }

    /** Die Oberfläche hat den [GameUiState.ausstehenderTon] gespielt. */
    fun tonGespielt() {
        _uiState.update { it.copy(ausstehenderTon = null) }
    }

    /**
     * Speichert die Aufgabe in der Historie (nur wenn ein Repository vorhanden ist; in reinen
     * Logik-Unit-Tests bleibt es null). Der aktive Nutzer kommt aus DataStore; ohne Profil-UI
     * ist das aktuell der Default-Nutzer.
     */
    private fun recordTask(task: Task, userAnswer: String, correct: Boolean) {
        val repo = taskRepository ?: return
        viewModelScope.launch {
            val user = userPreferencesRepository?.activeUser?.first()
                ?: UserPreferencesRepository.DEFAULT_USER
            repo.record(
                TaskHistory(
                    userName = user,
                    taskCategory = task.category,
                    numberRange = settings.range,
                    questionText = task.equation("_"),
                    expectedAnswer = task.expectedAnswer(),
                    userAnswer = userAnswer,
                    isCorrect = correct,
                )
            )
        }
    }

    fun onNext() {
        val state = _uiState.value
        if (state.feedback == null) return
        // Eine beendete Runde endet nicht zweimal. Bis die Oberfläche zur Auswertung wechselt,
        // steht der Knopf noch da – ein zweiter Tipp darauf schrieb sonst einen zweiten
        // Rundenabschluss, und der Übungskalender zählte am Tag eine Runde zu viel.
        if (state.isFinished) return
        if (state.taskNumber < settings.count) {
            _uiState.update { it.copy(taskNumber = it.taskNumber + 1) }
            generateTask()
        } else {
            finishRound()
        }
    }

    private fun finishRound() {
        // Der Punkt, den der Übungskalender an diesem Tag setzt. Hierher kommt nur, wer die Runde
        // **durchgezogen** hat: Ein Abbruch über ✕ verlässt den Bildschirm, ohne finishRound zu
        // erreichen (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`).
        rundenRepository?.let { repo ->
            viewModelScope.launch { repo.merkeFuerAktivesProfil(userPreferencesRepository) }
        }
        val job = baselineJob
        if (job == null) {
            // Kein Repository (reine Logik-Tests): sofort beenden, keine Abzeichen.
            _uiState.update { it.copy(isFinished = true) }
            return
        }
        viewModelScope.launch {
            job.join() // sicherstellen, dass der Startstand geladen ist
            _uiState.update {
                it.copy(newlyUnlockedBadges = computeUnlockedBadges(), isFinished = true)
            }
        }
    }

    /** Ermittelt die in dieser Runde neu erreichten Abzeichen (baseline → baseline+lokal). */
    private fun computeUnlockedBadges(): List<UnlockedBadge> {
        val result = mutableListOf<UnlockedBadge>()
        for ((key, localCount) in roundLocalCorrect) {
            if (localCount == 0) continue
            val previous = baseline[key] ?: 0
            newlyReachedTiers(previous, previous + localCount).forEach { tier ->
                result.add(UnlockedBadge(key, settings.range, tier))
            }
        }
        return result
    }
}
