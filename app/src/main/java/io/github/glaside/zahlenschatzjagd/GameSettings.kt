package io.github.glaside.zahlenschatzjagd

data class GameSettings(
    val types: List<String>,
    val range: Int,
    val count: Int,
    // Die Hilfe bei „verliebten Zahlen": zeigt die Münz-Veranschaulichung (Zehnerfeld) an und
    // wählt damit die Schwierigkeitsstufe „Mit Hilfe" samt ihrer Kennung (siehe categoryForType).
    // Für die klassischen Rechenarten ohne Bedeutung.
    val showCoins: Boolean = false,
    // Leichter Schwierigkeitsgrad mit Auswahl-Antworten statt Zifferneingabe (aktuell nur für
    // "Verdoppeln" genutzt, generisch benannt für künftige Multiple-Choice-Spiele).
    val multipleChoice: Boolean = false,
)
