package io.github.glaside.zahlenschatzjagd.ui.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.BADGE_CATEGORIES
import io.github.glaside.zahlenschatzjagd.BadgeCategory
import io.github.glaside.zahlenschatzjagd.BadgeTier
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.highestTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Stand einer einzelnen (Kategorie × Zahlenraum)-Kombination in der Abzeichen-Übersicht.
 * [ertrag] ist der summierte **Ertrag** – bei den meisten Übungsformen die Zahl richtig
 * gelöster Aufgaben, bei der Zahlenmauer gestaffelt (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
 */
data class BadgeCell(
    val category: BadgeCategory,
    val numberRange: Int,
    val ertrag: Int,
) {
    /** Höchste bereits erreichte Stufe (null = noch keine). */
    val currentTier: BadgeTier? get() = highestTier(ertrag)

    /** Nächste noch nicht erreichte Stufe (null = alles geschafft). */
    val nextTier: BadgeTier? get() = BadgeTier.entries.firstOrNull { ertrag < it.threshold }
}

data class BadgeUiState(
    val loading: Boolean = true,
    val cells: List<BadgeCell> = emptyList(),
)

/**
 * Lädt für den aktiven Nutzer je (Kategorie × Zahlenraum) den summierten Ertrag und leitet
 * daraus den Abzeichen-Stand ab. Rein lesend – die Abzeichen selbst
 * werden nicht gespeichert, sondern aus [TaskRepository.summeErtrag] berechnet.
 */
class BadgeViewModel(
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BadgeUiState())
    val uiState: StateFlow<BadgeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val user = userPreferencesRepository.activeUser.first()
            val cells = buildList {
                for (category in BADGE_CATEGORIES) {
                    for (range in category.ranges) {
                        val ertrag = taskRepository.summeErtrag(user, category.key, range)
                        add(BadgeCell(category, range, ertrag))
                    }
                }
            }
            _uiState.value = BadgeUiState(loading = false, cells = cells)
        }
    }
}
