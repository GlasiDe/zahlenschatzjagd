package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.UserProfile
import io.github.glaside.zahlenschatzjagd.data.UserProfileRepository
import io.github.glaside.zahlenschatzjagd.data.freigabe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Was der Freigabe-Bildschirm zeigt. [freigabe] ist `null`, solange noch nichts geladen ist
 * oder es gar kein Profil gibt – dann steht auf dem Bildschirm ein Hinweis statt einer Liste
 * von Schaltern, die niemandem gehören.
 */
data class FreigabeUiState(
    val profile: List<UserProfile> = emptyList(),
    val gewaehltesProfil: String? = null,
    val freigabe: Freigabe? = null,
)

/**
 * Zustand und Aktionen des Eintrags **Freigabe** im Elternbereich: Für ein gewähltes Profil
 * lassen sich Fortschrittskennungen und Zahlenräume einzeln an- und abwählen. Erreichbar bleibt,
 * was in beiden Mengen vorkommt (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`).
 *
 * Es gibt **keine Untergrenze**: Alles abzuwählen ist ein erlaubter Zustand und wird nicht
 * abgefangen.
 */
class FreigabeViewModel(
    private val profileRepository: UserProfileRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /** Das im Elternbereich handverlesene Profil – siehe [bestimmeGewaehltesProfil]. */
    private val handverlesenesProfil = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FreigabeUiState> = combine(
        profileRepository.profiles,
        userPreferencesRepository.activeUser,
        handverlesenesProfil,
    ) { profile, aktivesProfil, handverlesen ->
        val gewaehlt = bestimmeGewaehltesProfil(
            namen = profile.map { it.name },
            handverlesen = handverlesen,
            aktivesProfil = aktivesProfil,
        )
        FreigabeUiState(
            profile = profile,
            gewaehltesProfil = gewaehlt,
            freigabe = profile.firstOrNull { it.name == gewaehlt }?.freigabe,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FreigabeUiState())

    fun waehleProfil(name: String) {
        handverlesenesProfil.value = name
    }

    /** Gibt eine Fortschrittskennung frei oder nimmt sie zurück. */
    fun setzeKennung(kennung: String, freigegeben: Boolean) {
        aendere { freigabe ->
            freigabe.copy(
                kennungen = if (freigegeben) freigabe.kennungen + kennung else freigabe.kennungen - kennung
            )
        }
    }

    /** Gibt einen Zahlenraum frei oder nimmt ihn zurück – er wirkt über alle Kennungen hinweg. */
    fun setzeZahlenraum(zahlenraum: Int, freigegeben: Boolean) {
        aendere { freigabe ->
            freigabe.copy(
                zahlenraeume = if (freigegeben) {
                    freigabe.zahlenraeume + zahlenraum
                } else {
                    freigabe.zahlenraeume - zahlenraum
                }
            )
        }
    }

    private fun aendere(wandle: (Freigabe) -> Freigabe) {
        val state = uiState.value
        val profil = state.gewaehltesProfil ?: return
        val freigabe = state.freigabe ?: return
        viewModelScope.launch { profileRepository.setFreigabe(profil, wandle(freigabe)) }
    }
}
