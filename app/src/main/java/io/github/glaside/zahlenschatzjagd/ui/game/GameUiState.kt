package io.github.glaside.zahlenschatzjagd.ui.game

import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.Task
import io.github.glaside.zahlenschatzjagd.UnlockedBadge

/**
 * Ein einziger, konsistenter Snapshot des Spielbildschirms (eine Quelle der Wahrheit).
 * Das [GameViewModel] exponiert diesen als StateFlow, die UI rendert rein daraus.
 */
data class GameUiState(
    val currentTask: Task? = null,
    val userInput: String = "",
    val taskNumber: Int = 1,
    val totalTasks: Int = 0,
    val correctAnswers: Int = 0,
    val feedback: Feedback? = null, // null = es wird gerade eingegeben, kein Feedback sichtbar
    val isFinished: Boolean = false,
    val newlyUnlockedBadges: List<UnlockedBadge> = emptyList(),
    val showCoins: Boolean = false, // Münz-Veranschaulichung (Zehnerfeld) bei „verliebten Zahlen"
    /**
     * Der **Ton** zur gerade geprüften Aufgabe, bis die Oberfläche ihn gespielt und mit
     * [GameViewModel.tonGespielt] quittiert hat – danach `null`, auch wenn die Auswertung noch
     * sichtbar ist. So kommt er nach dem Drehen nicht noch einmal
     * (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
     */
    val ausstehenderTon: GameSound? = null,
)

/**
 * Auswertungsergebnis der aktuellen Aufgabe. [correctAnswer] ist bewusst ein **String** (nicht
 * `Int`), damit auch nicht-numerische Antworten dargestellt werden können: bei den Rechenarten
 * die Zahl, beim Zahlvergleich die vollständige richtige Ungleichung (z. B. „3 < 7").
 * [extraExplanation] ist nur bei „Verdoppeln" gesetzt: zeigt nach der Antwort die Äquivalenz
 * zu Addition und Multiplikation (z. B. „5 + 5 = 10  ·  2 · 5 = 10"), siehe LEHRPLAN.md.
 */
data class Feedback(val isCorrect: Boolean, val correctAnswer: String, val extraExplanation: String? = null)
