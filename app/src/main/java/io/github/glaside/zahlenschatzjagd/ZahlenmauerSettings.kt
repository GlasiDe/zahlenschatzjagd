package io.github.glaside.zahlenschatzjagd

/**
 * Einstellungen einer Zahlenmauer-Runde. Bewusst **nicht** [GameSettings] wiederverwendet:
 * Zahlenmauer hat kein "types"-Konzept, dafür eine Schwierigkeitsstufe, die Etagenzahl,
 * Zahlenraum und Aufgabenform (Aufbau/Umkehr) fest bündelt (siehe [ZahlenmauerDifficulty]).
 */
data class ZahlenmauerSettings(
    val difficulty: ZahlenmauerDifficulty,
    val wallCount: Int,
)
