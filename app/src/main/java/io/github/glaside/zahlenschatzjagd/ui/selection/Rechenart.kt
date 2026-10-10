package io.github.glaside.zahlenschatzjagd.ui.selection

/**
 * Eine der vier Grundrechenarten für die Auswahl im `SelectionScreen` – bewusst **reine Daten**
 * ohne Compose/Android (wie [Rechenspiel] und [Zahlenspiel]), damit sich das Filtern nach der
 * Freigabe ohne Gerät prüfen lässt.
 *
 * - [type] ist der Wert, der in `GameSettings.types` landet und die Aufgaben-Generierung steuert
 *   (`categoryForType`).
 * - [kennung] ist die zugehörige Fortschrittskennung, an der Abzeichen und Freigabe hängen.
 * - [testTag] bleibt bei den seit jeher verwendeten Marken (`checkbox_plus` …).
 */
data class Rechenart(
    val type: String,
    val label: String,
    val kennung: String,
) {
    val testTag: String get() = "checkbox_${type.lowercase()}"
}

/**
 * Die vier Grundrechenarten in Anzeige-Reihenfolge. **Zentrale Stelle**: Beschriftung,
 * Aufgaben-Typ und Kennung standen vorher an drei Stellen verteilt (Kästchen im
 * `SelectionScreen`, `categoryForType`, `BADGE_CATEGORIES`).
 */
val RECHENARTEN: List<Rechenart> = listOf(
    Rechenart(type = "Plus", label = "Plus (+)", kennung = "PLUS"),
    Rechenart(type = "Minus", label = "Minus (-)", kennung = "MINUS"),
    Rechenart(type = "Mal", label = "Mal (·)", kennung = "MAL"),
    Rechenart(type = "Geteilt", label = "Geteilt (:)", kennung = "GETEILT"),
)
