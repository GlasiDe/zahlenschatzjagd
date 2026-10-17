package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.AUFGABEN_JE_REIHE
import io.github.glaside.zahlenschatzjagd.EinmaleinsSettings
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.aufgabenDerReihe
import io.github.glaside.zahlenschatzjagd.aufsageReihenfolge
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.isAnswerCorrect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Ein Kästchen beim Aufsagen, nachdem es geprüft ist. [getippt] ist, was das Kind eingegeben hat;
 * im Kästchen selbst steht danach immer die **richtige** Zahl, damit der Rest der Reihe darauf
 * aufbauen kann (`CONTEXT.md`, Aufsagen).
 */
data class Kaestchen(val getippt: String, val richtig: Boolean)

/**
 * Ein Schnappschuss des Aufsagen-Bildschirms. [kaestchen] hält nur die schon geprüften Kästchen der
 * aktuellen Reihe, in ihrer Reihenfolge; das nächste ist immer das aktive, und in ihm steht die
 * laufende [eingabe].
 */
data class AufsagenUiState(
    val reihe: Int,
    val reiheNummer: Int = 1,
    val reihenGesamt: Int,
    val kaestchen: List<Kaestchen> = emptyList(),
    val eingabe: String = "",
    /** Richtige **Kästchen** der ganzen Runde – ein Kästchen ist eine Aufgabe. */
    val richtige: Int = 0,
    val gepruefte: Int = 0,
    val fertig: Boolean = false,
    val neueAbzeichen: List<UnlockedBadge> = emptyList(),
    /**
     * Der **Ton** zum gerade geprüften Kästchen, bis die Oberfläche ihn mit
     * [AufsagenViewModel.tonGespielt] quittiert hat
     * (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
     */
    val ausstehenderTon: GameSound? = null,
    /**
     * Die Reihe, die Ella ansagt („Jetzt die 7er-Reihe!"), bis die Oberfläche mit
     * [AufsagenViewModel.ansageVorgelesen] quittiert hat. Gesetzt zu Beginn **jeder** Reihe, auch
     * der ersten – aber nur, wenn mehrere gewählt sind: Bei einer einzigen steht ohnehin fest,
     * welche dran ist. Quittiert wird, damit sie nach dem Drehen nicht noch einmal kommt.
     */
    val ausstehendeAnsage: Int? = null,
) {
    /** Alle zehn Kästchen der Reihe sind geprüft – jetzt geht es mit „Weiter" zur nächsten. */
    val reiheFertig: Boolean get() = kaestchen.size == AUFGABEN_JE_REIHE

    /** Das Kästchen, in das gerade getippt wird – `null`, wenn die Reihe fertig ist. */
    val aktivesKaestchen: Int? get() = kaestchen.size.takeIf { it < AUFGABEN_JE_REIHE }
}

/**
 * Hält eine **Aufsagen**-Runde: Jede gewählte Reihe kommt einmal dran, aufsteigend, mit zehn
 * Kästchen ohne Aufgabe davor. Das Kind tippt die nächste Zahl und gibt sie mit ✓ ab; eine falsche
 * wird sofort markiert und durch die richtige ersetzt, dann ist das nächste Kästchen dran. Einen
 * zweiten Versuch gibt es nicht. Jedes Kästchen ist eine Aufgabe und eine Zeile der Historie.
 */
class AufsagenViewModel(
    settings: EinmaleinsSettings,
    taskRepository: TaskRepository? = null,
    userPreferencesRepository: UserPreferencesRepository? = null,
    rundenRepository: RundenRepository? = null,
) : ViewModel() {

    private val reihen = aufsageReihenfolge(settings.reihen)
    private val fortschritt = Einmaleinsfortschritt(
        settings = settings,
        taskRepository = taskRepository,
        userPreferencesRepository = userPreferencesRepository,
        rundenRepository = rundenRepository,
        scope = viewModelScope,
    )

    private val _uiState = MutableStateFlow(
        AufsagenUiState(
            reihe = reihen.first(),
            reihenGesamt = reihen.size,
            ausstehendeAnsage = ansageNurBeiMehrerenReihen(reihen.first()),
        )
    )
    val uiState: StateFlow<AufsagenUiState> = _uiState.asStateFlow()

    private var rundeBeendet = false

    fun onZiffer(ziffer: String) {
        _uiState.update { state ->
            if (state.aktivesKaestchen == null || state.eingabe.length >= MAX_ZIFFERN) {
                state
            } else {
                state.copy(eingabe = state.eingabe + ziffer)
            }
        }
    }

    fun onLoeschen() {
        _uiState.update { if (it.aktivesKaestchen == null) it else it.copy(eingabe = "") }
    }

    /** Prüft das aktive Kästchen. Ohne Eingabe geschieht nichts – der ✓ ist nie gesperrt. */
    fun onPruefen() {
        val state = _uiState.value
        val index = state.aktivesKaestchen ?: return
        if (state.eingabe.isEmpty()) return

        val aufgabe = aufgabenDerReihe(state.reihe)[index]
        val richtig = isAnswerCorrect(state.eingabe, aufgabe.ergebnis)
        _uiState.update {
            it.copy(
                kaestchen = it.kaestchen + Kaestchen(getippt = it.eingabe, richtig = richtig),
                eingabe = "",
                richtige = it.richtige + if (richtig) 1 else 0,
                gepruefte = it.gepruefte + 1,
                ausstehenderTon = if (richtig) GameSound.CORRECT else GameSound.WRONG,
            )
        }
        fortschritt.halteFest(aufgabe, state.eingabe, richtig)
    }

    /** Nach der zehnten Zahl: zur nächsten Reihe oder – nach der letzten – zur Auswertung. */
    fun onWeiter() {
        val state = _uiState.value
        // Eine beendete Runde endet nicht zweimal, sonst zählte der Übungskalender doppelt. Am
        // Merker und nicht an `fertig`: Das wird erst gesetzt, wenn die Abzeichen geladen sind.
        if (!state.reiheFertig || rundeBeendet) return
        if (state.reiheNummer < reihen.size) {
            _uiState.update {
                it.copy(
                    reihe = reihen[it.reiheNummer],
                    reiheNummer = it.reiheNummer + 1,
                    kaestchen = emptyList(),
                    ausstehenderTon = null,
                    ausstehendeAnsage = ansageNurBeiMehrerenReihen(reihen[it.reiheNummer]),
                )
            }
        } else {
            rundeBeendet = true
            fortschritt.beende { abzeichen ->
                _uiState.update { it.copy(neueAbzeichen = abzeichen, fertig = true) }
            }
        }
    }

    /** Die Oberfläche hat die [AufsagenUiState.ausstehendeAnsage] vorgelesen. */
    fun ansageVorgelesen() {
        _uiState.update { it.copy(ausstehendeAnsage = null) }
    }

    /** Angesagt wird nur, wenn mehrere Reihen gewählt sind – siehe [AufsagenUiState.ausstehendeAnsage]. */
    private fun ansageNurBeiMehrerenReihen(reihe: Int): Int? = reihe.takeIf { reihen.size > 1 }

    /** Die Oberfläche hat den [AufsagenUiState.ausstehenderTon] gespielt. */
    fun tonGespielt() {
        _uiState.update { it.copy(ausstehenderTon = null) }
    }
}
