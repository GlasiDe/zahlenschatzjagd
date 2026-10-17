package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.EinmaleinsSettings
import io.github.glaside.zahlenschatzjagd.Einmaleinsvorrat
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.Malaufgabe
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.isAnswerCorrect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/** Ein Schnappschuss des Durcheinander-Bildschirms. [richtig] ist `null`, bis mit ✓ geprüft ist. */
data class DurcheinanderUiState(
    val aufgabe: Malaufgabe,
    val eingabe: String = "",
    val richtig: Boolean? = null,
    val aufgabenNummer: Int = 1,
    val aufgabenGesamt: Int,
    val richtige: Int = 0,
    val fertig: Boolean = false,
    val neueAbzeichen: List<UnlockedBadge> = emptyList(),
    /**
     * Der **Ton** zur gerade geprüften Aufgabe, bis die Oberfläche ihn mit
     * [DurcheinanderViewModel.tonGespielt] quittiert hat
     * (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
     */
    val ausstehenderTon: GameSound? = null,
) {
    val geprueft: Boolean get() = richtig != null
}

/**
 * Hält eine **Durcheinander**-Runde: einzelne Aufgaben `8 · 7 = _` aus allen gewählten Reihen,
 * gezogen aus einem [Einmaleinsvorrat] ohne Zurücklegen. Abgegeben wird mit ✓, einen zweiten
 * Versuch gibt es nicht.
 *
 * [random] ist injizierbar, damit das ViewModel in Unit-Tests deterministisch ist.
 */
class DurcheinanderViewModel(
    private val settings: EinmaleinsSettings,
    taskRepository: TaskRepository? = null,
    userPreferencesRepository: UserPreferencesRepository? = null,
    rundenRepository: RundenRepository? = null,
    random: Random = Random.Default,
) : ViewModel() {

    private val vorrat = Einmaleinsvorrat(settings.reihen, random)
    private val fortschritt = Einmaleinsfortschritt(
        settings = settings,
        taskRepository = taskRepository,
        userPreferencesRepository = userPreferencesRepository,
        rundenRepository = rundenRepository,
        scope = viewModelScope,
    )

    private val _uiState = MutableStateFlow(
        DurcheinanderUiState(aufgabe = vorrat.ziehe(), aufgabenGesamt = settings.aufgabenzahl)
    )
    val uiState: StateFlow<DurcheinanderUiState> = _uiState.asStateFlow()

    private var rundeBeendet = false

    fun onZiffer(ziffer: String) {
        _uiState.update { state ->
            if (state.geprueft || state.eingabe.length >= MAX_ZIFFERN) {
                state
            } else {
                state.copy(eingabe = state.eingabe + ziffer)
            }
        }
    }

    fun onLoeschen() {
        _uiState.update { if (it.geprueft) it else it.copy(eingabe = "") }
    }

    /** Wertet die Eingabe. Ohne Eingabe geschieht nichts – der ✓ ist nie gesperrt. */
    fun onPruefen() {
        val state = _uiState.value
        if (state.geprueft || state.eingabe.isEmpty()) return
        val richtig = isAnswerCorrect(state.eingabe, state.aufgabe.ergebnis)
        _uiState.update {
            it.copy(
                richtig = richtig,
                richtige = it.richtige + if (richtig) 1 else 0,
                ausstehenderTon = if (richtig) GameSound.CORRECT else GameSound.WRONG,
            )
        }
        fortschritt.halteFest(state.aufgabe, state.eingabe, richtig)
    }

    fun onWeiter() {
        val state = _uiState.value
        // Eine beendete Runde endet nicht zweimal, sonst zählte der Übungskalender doppelt.
        if (!state.geprueft || rundeBeendet) return
        if (state.aufgabenNummer < settings.aufgabenzahl) {
            _uiState.update {
                it.copy(
                    aufgabe = vorrat.ziehe(),
                    eingabe = "",
                    richtig = null,
                    aufgabenNummer = it.aufgabenNummer + 1,
                    ausstehenderTon = null,
                )
            }
        } else {
            rundeBeendet = true
            fortschritt.beende { abzeichen ->
                _uiState.update { it.copy(neueAbzeichen = abzeichen, fertig = true) }
            }
        }
    }

    /** Die Oberfläche hat den [DurcheinanderUiState.ausstehenderTon] gespielt. */
    fun tonGespielt() {
        _uiState.update { it.copy(ausstehenderTon = null) }
    }
}
