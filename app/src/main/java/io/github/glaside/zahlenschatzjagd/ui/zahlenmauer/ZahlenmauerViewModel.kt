package io.github.glaside.zahlenschatzjagd.ui.zahlenmauer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.Zahlenmauer
import io.github.glaside.zahlenschatzjagd.ZahlenmauerPos
import io.github.glaside.zahlenschatzjagd.ZahlenmauerSettings
import io.github.glaside.zahlenschatzjagd.createZahlenmauer
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.merkeFuerAktivesProfil
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
 * Hält den Zustand einer Zahlenmauer-Runde (mehrere Mauern nacheinander). Anders als
 * [io.github.glaside.zahlenschatzjagd.ui.game.GameViewModel] ist eine "Aufgabe" hier eine ganze Mauer
 * mit mehreren gleichzeitigen Lücken statt einer einzelnen Gleichung; entsprechend wird auch
 * die Historie pro ganzer Mauer geschrieben, nicht pro Zelle (siehe [recordWall]).
 *
 * [random] ist injizierbar, damit das ViewModel in Unit-Tests deterministisch ist.
 */
class ZahlenmauerViewModel(
    private val settings: ZahlenmauerSettings,
    private val taskRepository: TaskRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val rundenRepository: RundenRepository? = null,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val badgeKey = settings.difficulty.kennung

    private val _uiState = MutableStateFlow(ZahlenmauerUiState(totalWalls = settings.wallCount))
    val uiState: StateFlow<ZahlenmauerUiState> = _uiState.asStateFlow()

    // Abzeichen-Freischaltung race-frei, analog GameViewModel: [baseline] ist der summierte
    // Ertrag zu Rundenbeginn, [rundenErtrag] zählt synchron den Ertrag der geprüften Mauern
    // dieser Runde (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
    private var baseline = 0
    private var rundenErtrag = 0
    private var baselineJob: Job? = null

    init {
        generateWall()
        baselineJob = loadBaselineIfPossible()
    }

    private fun loadBaselineIfPossible(): Job? {
        val repo = taskRepository ?: return null
        return viewModelScope.launch {
            val user = userPreferencesRepository?.activeUser?.first()
                ?: UserPreferencesRepository.DEFAULT_USER
            baseline = repo.summeErtrag(user, badgeKey, settings.difficulty.range)
        }
    }

    private fun generateWall() {
        val wall = createZahlenmauer(settings.difficulty, random)
        // Was zur vorigen Aufgabe noch aussteht, gehört nicht zur neuen.
        _uiState.update {
            it.copy(
                wall = wall,
                entries = wall.gaps.associate { cell -> cell.pos to ZahlenmauerEntryState() },
                selectedPos = wall.gaps.firstOrNull()?.pos,
                checked = false,
                ausstehenderTon = null,
            )
        }
    }

    fun selectCell(pos: ZahlenmauerPos) {
        _uiState.update { state ->
            if (state.checked || pos !in state.entries) state else state.copy(selectedPos = pos)
        }
    }

    fun onDigit(digit: String) {
        _uiState.update { state ->
            val pos = state.selectedPos
            val entry = pos?.let { state.entries[it] }
            val maxLength = state.wall?.difficulty?.range?.toString()?.length ?: 3
            if (state.checked || entry == null || entry.input.length >= maxLength) {
                state
            } else {
                state.copy(entries = state.entries + (pos to entry.copy(input = entry.input + digit)))
            }
        }
    }

    fun onClear() {
        _uiState.update { state ->
            val pos = state.selectedPos
            val entry = pos?.let { state.entries[it] }
            if (state.checked || entry == null) {
                state
            } else {
                state.copy(entries = state.entries + (pos to entry.copy(input = "")))
            }
        }
    }

    /** Prüft alle Lücken der aktuellen Mauer gleichzeitig - erst möglich, wenn alle befüllt sind. */
    fun onCheck() {
        val state = _uiState.value
        val wall = state.wall
        if (state.checked || wall == null || state.entries.values.any { it.input.isEmpty() }) return

        val checkedEntries = state.entries.mapValues { (pos, entry) ->
            val cell = wall.gaps.first { it.pos == pos }
            val correct = if (pos.row > 0) {
                val left = ZahlenmauerPos(pos.row - 1, pos.col)
                val right = ZahlenmauerPos(pos.row - 1, pos.col + 1)
                effectiveValue(wall, state.entries, pos) ==
                    effectiveValue(wall, state.entries, left) + effectiveValue(wall, state.entries, right)
            } else {
                // Ultra-Basislücke: die stützende Elternzelle und die Nachbar-Basiszelle sind
                // laut Generierungslogik in ZahlenmauerLogic.kt immer isGiven/fix, es gibt hier
                // strukturell keine Kette aus mehreren Lücken - weiterhin fest gegen die
                // ursprüngliche Lösung prüfen.
                isAnswerCorrect(entry.input, cell.solution)
            }
            entry.copy(isCorrect = correct)
        }
        // Eine Mauer ist EINE Aufgabe (nicht eine je Zelle) und nur dann richtig gelöst, wenn
        // ALLE Lücken stimmen – so zählen Sterne und Übungsstand. Zur Truhe trägt sie dagegen
        // gestaffelt bei, auch teilweise richtig (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
        val wallCorrect = checkedEntries.values.all { it.isCorrect == true }
        val ertrag = settings.difficulty.ertrag(
            richtige = checkedEntries.values.count { it.isCorrect == true },
            luecken = checkedEntries.size,
        )
        rundenErtrag += ertrag

        _uiState.update {
            it.copy(
                entries = checkedEntries,
                checked = true,
                correctWalls = it.correctWalls + if (wallCorrect) 1 else 0,
                totalWallsChecked = it.totalWallsChecked + 1,
                ausstehenderTon = if (wallCorrect) GameSound.CORRECT else GameSound.WRONG,
            )
        }
        recordWall(wall, checkedEntries, wallCorrect, ertrag)
    }

    /** Die Oberfläche hat den [ZahlenmauerUiState.ausstehenderTon] gespielt. */
    fun tonGespielt() {
        _uiState.update { it.copy(ausstehenderTon = null) }
    }

    /**
     * Speichert die ganze Mauer als EINEN Historien-Eintrag mit ihrem [ertrag] (nur wenn ein
     * Repository vorhanden ist).
     */
    private fun recordWall(
        wall: Zahlenmauer,
        entries: Map<ZahlenmauerPos, ZahlenmauerEntryState>,
        wallCorrect: Boolean,
        ertrag: Int,
    ) {
        val repo = taskRepository ?: return
        viewModelScope.launch {
            val user = userPreferencesRepository?.activeUser?.first()
                ?: UserPreferencesRepository.DEFAULT_USER
            repo.record(
                TaskHistory(
                    userName = user,
                    taskCategory = badgeKey,
                    numberRange = settings.difficulty.range,
                    questionText = "Zahlenmauer (${settings.difficulty.name}, ${wall.gaps.size} Lücken)",
                    expectedAnswer = wall.gaps.joinToString(", ") { it.solution.toString() },
                    userAnswer = wall.gaps.joinToString(", ") { entries[it.pos]?.input.orEmpty() },
                    isCorrect = wallCorrect,
                    ertrag = ertrag,
                )
            )
        }
    }

    fun onNextWall() {
        val state = _uiState.value
        if (!state.checked) return
        // Wie im GameViewModel: Eine beendete Runde endet nicht zweimal, sonst zählte der
        // Übungskalender am Tag eine Runde zu viel.
        if (state.isFinished) return
        if (state.wallNumber < settings.wallCount) {
            _uiState.update { it.copy(wallNumber = it.wallNumber + 1) }
            generateWall()
        } else {
            finishRound()
        }
    }

    private fun finishRound() {
        // Der Punkt, den der Übungskalender an diesem Tag setzt. Hierher kommt nur, wer die Runde
        // durchgezogen hat – wer abbricht, verlässt den Bildschirm ohne finishRound.
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
            _uiState.update { it.copy(newlyUnlockedBadges = computeUnlockedBadges(), isFinished = true) }
        }
    }

    private fun computeUnlockedBadges(): List<UnlockedBadge> {
        if (rundenErtrag == 0) return emptyList()
        return newlyReachedTiers(baseline, baseline + rundenErtrag)
            .map { tier -> UnlockedBadge(badgeKey, settings.difficulty.range, tier) }
    }
}

/**
 * Der Wert, mit dem [pos] beim Prüfen tatsächlich rechnet: bei vorgegebenen Zellen die feste
 * `solution`, bei Lücken die tatsächlich eingegebene Zahl (nicht deren "wahre" Lösung). So
 * werden Aufbau-Lücken lokal gegen das geprüft, was das Kind wirklich hingeschrieben hat - ein
 * Folgefehler, der korrekt aus einem vorherigen eigenen Fehler abgeleitet wurde, wird dadurch
 * nicht mehr zusätzlich bestraft.
 */
private fun effectiveValue(
    wall: Zahlenmauer,
    entries: Map<ZahlenmauerPos, ZahlenmauerEntryState>,
    pos: ZahlenmauerPos,
): Int {
    val cell = wall.cellAt(pos.row, pos.col)
    return if (cell.isGiven) cell.solution else entries.getValue(pos).input.toInt()
}
