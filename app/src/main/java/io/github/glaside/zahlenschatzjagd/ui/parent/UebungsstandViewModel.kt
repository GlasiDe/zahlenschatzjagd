package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.UserProfile
import io.github.glaside.zahlenschatzjagd.data.UserProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Wie lange der Zustand ohne Zuhörer weiterlebt – ein Bildschirmdreher soll den Übungsstand nicht
 * neu aus der Datenbank holen. Derselbe Wert wie im [UebungskalenderViewModel].
 */
private const val NACHLAUF_MILLIS = 5_000L

private const val TAG_MILLIS = 24L * 60 * 60 * 1000

/**
 * Was der Übungsstand zeigt.
 *
 * [geladen] unterscheidet „noch nichts gelesen" von „nichts geübt" – ohne diese Angabe stünde beim
 * Öffnen für einen Moment „noch nicht geübt" bei jeder Übungsform, und ein Erwachsener liest dort
 * etwas, das nicht stimmt (wie im [UebungskalenderUiState]).
 */
data class UebungsstandUiState(
    val profile: List<UserProfile> = emptyList(),
    val gewaehltesProfil: String? = null,
    val zeilen: List<Uebungsstandzeile> = emptyList(),
    val geladen: Boolean = false,
)

/**
 * Zustand des Eintrags **Übungsstand** im Elternbereich: was ein Kind zuletzt geübt hat und wie
 * sicher es sitzt.
 *
 * Gelesen wird nur der Ausschnitt, den der Bildschirm zeigt – die letzten [UEBUNGSSTAND_TAGE] Tage.
 * Das Fenster je Fortschrittskennung spannt dann [baueUebungsstand] darin auf.
 *
 * [jetzt] ist die Uhr als Parameter, damit das Fenster ohne Wartezeit und ohne die Uhr des Rechners
 * prüfbar ist (wie im [UebungskalenderViewModel]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UebungsstandViewModel(
    profileRepository: UserProfileRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val jetzt: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** Das im Elternbereich handverlesene Profil – siehe [bestimmeGewaehltesProfil]. */
    private val handverlesenesProfil = MutableStateFlow<String?>(null)

    /** Wessen Übungsstand gezeigt wird. */
    private data class Auswahl(
        val profile: List<UserProfile>,
        val gewaehltesProfil: String?,
    )

    val uiState: StateFlow<UebungsstandUiState> = combine(
        profileRepository.profiles,
        userPreferencesRepository.activeUser,
        handverlesenesProfil,
    ) { profile, aktivesProfil, handverlesen ->
        Auswahl(
            profile = profile,
            gewaehltesProfil = bestimmeGewaehltesProfil(
                namen = profile.map { it.name },
                handverlesen = handverlesen,
                aktivesProfil = aktivesProfil,
            ),
        )
        // mapLatest: Wechselt das Profil, während noch für das vorige gelesen wird, gilt das
        // spätere. Sonst könnte die alte Antwort die neue überschreiben.
    }.mapLatest { auswahl -> zustand(auswahl) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(NACHLAUF_MILLIS),
            UebungsstandUiState(),
        )

    fun waehleProfil(name: String) {
        handverlesenesProfil.value = name
    }

    private suspend fun zustand(auswahl: Auswahl): UebungsstandUiState {
        val profil = auswahl.gewaehltesProfil
        val nun = jetzt()
        return UebungsstandUiState(
            profile = auswahl.profile,
            gewaehltesProfil = profil,
            zeilen = if (profil == null) {
                emptyList()
            } else {
                baueUebungsstand(
                    aufgaben = taskRepository.aufgabenSeit(
                        profil = profil,
                        seit = nun - UEBUNGSSTAND_TAGE * TAG_MILLIS,
                    ),
                    jetztMillis = nun,
                )
            },
            geladen = true,
        )
    }
}
