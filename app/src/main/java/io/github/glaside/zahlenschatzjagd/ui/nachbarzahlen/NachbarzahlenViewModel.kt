package io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.Nachbarposition
import io.github.glaside.zahlenschatzjagd.NachbarzahlenSettings
import io.github.glaside.zahlenschatzjagd.Nachbarzahlenaufgabe
import io.github.glaside.zahlenschatzjagd.Nachbarzahlenvorrat
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.merkeFuerAktivesProfil
import io.github.glaside.zahlenschatzjagd.isAnswerCorrect
import io.github.glaside.zahlenschatzjagd.loesungsText
import io.github.glaside.zahlenschatzjagd.newlyReachedTiers
import io.github.glaside.zahlenschatzjagd.reihenText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Hält den Zustand einer **Nachbarzahlen**-Runde. Eine „Aufgabe" ist hier eine ganze Reihe aus drei
 * Zahlen mit zwei gleichzeitig offenen Lücken; sie wird als **eine** Zeile in die Historie
 * geschrieben und ist nur richtig, wenn beide Zahlen stimmen – dieselbe Granularität wie bei der
 * Zahlenmauer.
 *
 * [random] ist injizierbar, damit das ViewModel in Unit-Tests deterministisch ist.
 */
class NachbarzahlenViewModel(
    private val settings: NachbarzahlenSettings,
    private val taskRepository: TaskRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null,
    private val rundenRepository: RundenRepository? = null,
    random: Random = Random.Default,
) : ViewModel() {

    private val kennung = settings.schwierigkeitsstufe.kennung
    private val vorrat = Nachbarzahlenvorrat(settings.schwierigkeitsstufe, settings.zahlenraum, random)

    /** Wie viele Ziffern eine Lücke höchstens aufnimmt – im Zahlenraum 100 sind es drei. */
    private val maxZiffern = settings.zahlenraum.toString().length

    private val _uiState = MutableStateFlow(NachbarzahlenUiState(aufgabenGesamt = settings.aufgabenzahl))
    val uiState: StateFlow<NachbarzahlenUiState> = _uiState.asStateFlow()

    // Abzeichen-Freischaltung race-frei, analog GameViewModel/ZahlenmauerViewModel: [baseline] ist
    // der Gesamtstand zu Rundenbeginn, [richtigeDieserRunde] zählt synchron mit.
    private var baseline = 0
    private var richtigeDieserRunde = 0
    private var baselineJob: Job? = null

    init {
        naechsteAufgabe()
        // Nur die erste Aufgabe einer Runde wird vorgelesen, siehe [NachbarzahlenUiState.ausstehendeAnsage].
        _uiState.update { it.copy(ausstehendeAnsage = it.aufgabe) }
        baselineJob = ladeBaseline()
    }

    private fun ladeBaseline(): Job? {
        val repo = taskRepository ?: return null
        return viewModelScope.launch {
            baseline = repo.summeErtrag(aktivesProfil(userPreferencesRepository), kennung, settings.zahlenraum)
        }
    }

    private fun naechsteAufgabe() {
        val aufgabe = vorrat.ziehe()
        // Was zur vorigen Aufgabe noch aussteht, gehört nicht zur neuen.
        _uiState.update {
            it.copy(
                aufgabe = aufgabe,
                eingaben = aufgabe.luecken.associateWith { _ -> Lueckeneingabe() },
                gewaehlteLuecke = aufgabe.luecken.first(),
                geprueft = false,
                ausstehendeAnsage = null,
                ausstehenderTon = null,
            )
        }
    }

    /** Eine Lücke antippen. Die vorgegebene Zahl steht nicht in [NachbarzahlenUiState.eingaben]
     * und lässt sich damit weder anwählen noch überschreiben. */
    fun waehleLuecke(position: Nachbarposition) {
        _uiState.update { state ->
            if (state.geprueft || position !in state.eingaben) state else state.copy(gewaehlteLuecke = position)
        }
    }

    fun onZiffer(ziffer: String) {
        _uiState.update { state ->
            val position = state.gewaehlteLuecke
            val eingabe = position?.let { state.eingaben[it] }
            if (state.geprueft || eingabe == null || eingabe.eingabe.length >= maxZiffern) {
                state
            } else {
                state.copy(
                    eingaben = state.eingaben + (position to eingabe.copy(eingabe = eingabe.eingabe + ziffer))
                )
            }
        }
    }

    fun onLoeschen() {
        _uiState.update { state ->
            val position = state.gewaehlteLuecke
            val eingabe = position?.let { state.eingaben[it] }
            if (state.geprueft || eingabe == null) {
                state
            } else {
                state.copy(eingaben = state.eingaben + (position to eingabe.copy(eingabe = "")))
            }
        }
    }

    /**
     * Wertet beide Lücken gleichzeitig aus – möglich, sobald beide gefüllt sind. Danach steht das
     * Ergebnis fest: **kein zweiter Versuch**, korrigiert wird ausschließlich davor.
     */
    fun onPruefen() {
        val state = _uiState.value
        val aufgabe = state.aufgabe
        if (!state.pruefenMoeglich || aufgabe == null) return

        val gewertet = state.eingaben.mapValues { (position, eingabe) ->
            eingabe.copy(istRichtig = isAnswerCorrect(eingabe.eingabe, aufgabe.zahlAn(position)))
        }
        // Die ganze Reihe ist EINE Aufgabe: nur wenn beide Zahlen stimmen, zählt sie als richtig.
        val reiheRichtig = gewertet.values.all { it.istRichtig == true }
        if (reiheRichtig) richtigeDieserRunde += 1

        _uiState.update {
            it.copy(
                eingaben = gewertet,
                geprueft = true,
                richtige = it.richtige + if (reiheRichtig) 1 else 0,
                gepruefte = it.gepruefte + 1,
                ausstehenderTon = if (reiheRichtig) GameSound.CORRECT else GameSound.WRONG,
            )
        }
        halteFest(aufgabe, gewertet, reiheRichtig)
    }

    /** Die Oberfläche hat den [NachbarzahlenUiState.ausstehenderTon] gespielt. */
    fun tonGespielt() {
        _uiState.update { it.copy(ausstehenderTon = null) }
    }

    /** Die Oberfläche hat die [NachbarzahlenUiState.ausstehendeAnsage] vorgelesen. */
    fun ansageVorgelesen() {
        _uiState.update { it.copy(ausstehendeAnsage = null) }
    }

    /** Schreibt die ganze Reihe als **einen** Historien-Eintrag (nur mit vorhandenem Repository). */
    private fun halteFest(
        aufgabe: Nachbarzahlenaufgabe,
        eingaben: Map<Nachbarposition, Lueckeneingabe>,
        reiheRichtig: Boolean,
    ) {
        val repo = taskRepository ?: return
        viewModelScope.launch {
            repo.record(
                TaskHistory(
                    userName = aktivesProfil(userPreferencesRepository),
                    taskCategory = kennung,
                    numberRange = settings.zahlenraum,
                    questionText = aufgabe.reihenText(),
                    expectedAnswer = aufgabe.loesungsText(),
                    userAnswer = aufgabe.luecken.joinToString(", ") { eingaben[it]?.eingabe.orEmpty() },
                    isCorrect = reiheRichtig,
                )
            )
        }
    }

    fun onWeiter() {
        val state = _uiState.value
        if (!state.geprueft) return
        // Wie im GameViewModel: Eine beendete Runde endet nicht zweimal, sonst zählte der
        // Übungskalender am Tag eine Runde zu viel.
        if (state.fertig) return
        if (state.aufgabenNummer < settings.aufgabenzahl) {
            _uiState.update { it.copy(aufgabenNummer = it.aufgabenNummer + 1) }
            naechsteAufgabe()
        } else {
            beendeRunde()
        }
    }

    private fun beendeRunde() {
        // Der Punkt, den der Übungskalender an diesem Tag setzt. Hierher kommt nur, wer die Runde
        // durchgezogen hat – wer abbricht, verlässt den Bildschirm ohne beendeRunde.
        rundenRepository?.let { repo ->
            viewModelScope.launch { repo.merkeFuerAktivesProfil(userPreferencesRepository) }
        }
        val job = baselineJob
        if (job == null) {
            // Kein Repository (reine Logik-Tests): sofort beenden, keine Abzeichen.
            _uiState.update { it.copy(fertig = true) }
            return
        }
        viewModelScope.launch {
            job.join() // sicherstellen, dass der Startstand geladen ist
            _uiState.update { it.copy(neueAbzeichen = neueAbzeichen(), fertig = true) }
        }
    }

    private fun neueAbzeichen(): List<UnlockedBadge> {
        if (richtigeDieserRunde == 0) return emptyList()
        return newlyReachedTiers(baseline, baseline + richtigeDieserRunde)
            .map { stufe -> UnlockedBadge(kennung, settings.zahlenraum, stufe) }
    }
}

/** Wessen Runde gerade läuft – ohne Repository (reine Logik-Tests) das Vorgabe-Profil. */
private suspend fun aktivesProfil(repository: UserPreferencesRepository?): String =
    repository?.activeUser?.first() ?: UserPreferencesRepository.DEFAULT_USER
