package io.github.glaside.zahlenschatzjagd.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Zustand und Aktionen der Profilverwaltung (Kinder-Profile). Kapselt Anlegen, Wechseln,
 * Umbenennen, Löschen und Avatar-Wechsel.
 *
 * Die **Liste** der Profile und der aktive Name kommen bewusst nicht von hier: Beide hingen
 * hier an einem Startwert („keine Profile", der Vorgabe-Name), der sich nicht davon
 * unterscheiden ließ, dass es wirklich so ist. Genau daran hängt aber die Ausnahme vom Tor beim
 * Erststart. Der `ZahlenschatzjagdNavHost` wartet deshalb auf die erste echte Antwort und reicht sie
 * herein (siehe dort `geladen()`).
 *
 * Namensvalidierung passiert hier (nicht leer, nicht doppelt) und wird als [error] für eine
 * freundliche Meldung im UI ausgespielt – kein Absturz bei ungültiger Eingabe.
 */
class ProfileViewModel(
    private val profileRepository: UserProfileRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _firstProfileCreated = MutableStateFlow(false)

    /** True direkt nachdem das allererste Profil (aus leerer Liste) angelegt wurde. */
    val firstProfileCreated: StateFlow<Boolean> = _firstProfileCreated.asStateFlow()

    fun dismissFirstProfileHint() {
        _firstProfileCreated.value = false
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Legt ein neues Profil an. Der erste je angelegte Nutzer wird automatisch aktiv, damit
     * es nach dem Anlegen sofort „jemanden" gibt.
     */
    fun createProfile(name: String, avatarId: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _error.value = "Bitte gib einen Namen ein."
            return
        }
        _error.value = null
        viewModelScope.launch {
            if (profileRepository.nameExists(trimmed)) {
                _error.value = "Diesen Namen gibt es schon."
                return@launch
            }
            val wasEmpty = profileRepository.count() == 0
            profileRepository.createProfile(trimmed, avatarId)
            if (wasEmpty) {
                userPreferencesRepository.setActiveUser(trimmed)
                _firstProfileCreated.value = true
            }
        }
    }

    fun switchProfile(name: String) {
        viewModelScope.launch { userPreferencesRepository.setActiveUser(name) }
    }

    /** Benennt ein Profil um (Fortschritt folgt via DAO-Kaskade); ist es aktiv, folgt der aktive Name. */
    fun renameProfile(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            _error.value = "Bitte gib einen Namen ein."
            return
        }
        if (trimmed == oldName) {
            _error.value = null
            return
        }
        _error.value = null
        viewModelScope.launch {
            if (profileRepository.nameExists(trimmed)) {
                _error.value = "Diesen Namen gibt es schon."
                return@launch
            }
            val wasActive = userPreferencesRepository.activeUser.first() == oldName
            profileRepository.renameProfile(oldName, trimmed)
            if (wasActive) userPreferencesRepository.setActiveUser(trimmed)
        }
    }

    fun changeAvatar(name: String, avatarId: String) {
        viewModelScope.launch { profileRepository.changeAvatar(name, avatarId) }
    }

    /** Löscht ein Profil samt Fortschritt. War es aktiv, wird auf ein verbleibendes gewechselt. */
    fun deleteProfile(name: String) {
        viewModelScope.launch {
            val wasActive = userPreferencesRepository.activeUser.first() == name
            profileRepository.deleteProfile(name)
            if (wasActive) {
                val remaining = profileRepository.profiles.first()
                userPreferencesRepository.setActiveUser(
                    remaining.firstOrNull()?.name ?: UserPreferencesRepository.DEFAULT_USER
                )
            }
        }
    }
}
