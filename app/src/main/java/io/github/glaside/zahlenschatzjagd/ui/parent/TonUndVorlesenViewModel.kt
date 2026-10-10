package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.data.TonUndVorlesenRepository
import kotlinx.coroutines.launch

/**
 * Die schreibenden Aktionen der beiden Schalter. Bewusst dünn wie das [ParentAccessViewModel]:
 * Das Lesen läuft als Flow direkt aus dem Repository in die Navigation – ein ViewModel dazwischen
 * brächte nur eine weitere Kopie desselben Zustands.
 */
class TonUndVorlesenViewModel(
    private val repository: TonUndVorlesenRepository,
) : ViewModel() {

    /** Schaltet die Töne der Runde an oder aus. */
    fun setzeTonAn(an: Boolean) {
        viewModelScope.launch { repository.setzeTonAn(an) }
    }

    /** Schaltet die Vorlesefunktion an oder aus. */
    fun setzeVorlesenAn(an: Boolean) {
        viewModelScope.launch { repository.setzeVorlesenAn(an) }
    }
}
