package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.data.ParentAccessRepository
import kotlinx.coroutines.launch

/**
 * Die schreibenden Aktionen rund um den Zugang. Bewusst dünn: Das Prüfen liegt in
 * `ParentAccess` (ohne Android testbar), das Lesen läuft als Flow direkt aus dem Repository in
 * die Navigation – ein ViewModel dazwischen brächte nur eine weitere Kopie desselben Zustands.
 */
class ParentAccessViewModel(
    private val parentAccessRepository: ParentAccessRepository,
) : ViewModel() {

    /** Vergibt eine neue PIN samt Sicherheitsfrage; eine vorhandene wird ersetzt. */
    fun setPin(pin: String, question: String, answer: String) {
        viewModelScope.launch {
            parentAccessRepository.setPin(pin = pin, question = question, answer = answer)
        }
    }

    /** Tauscht nur die PIN aus; Sicherheitsfrage und Antwort bleiben bestehen. */
    fun changePin(pin: String) {
        viewModelScope.launch { parentAccessRepository.changePin(pin) }
    }

    /** Entfernt die PIN; danach gilt wieder die Elternfrage. */
    fun clearPin() {
        viewModelScope.launch { parentAccessRepository.clearPin() }
    }

    /** Gibt den Profilwechsel frei oder legt auch ihn hinter das Tor. */
    fun setzeProfilwechselOhnePin(erlaubt: Boolean) {
        viewModelScope.launch { parentAccessRepository.setzeProfilwechselOhnePin(erlaubt) }
    }
}
