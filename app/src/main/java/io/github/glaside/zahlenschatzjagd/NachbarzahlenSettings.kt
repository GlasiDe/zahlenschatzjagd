package io.github.glaside.zahlenschatzjagd

/**
 * Einstellungen einer Nachbarzahlen-Runde. Bewusst **nicht** [GameSettings] wiederverwendet:
 * Nachbarzahlen kennt kein "types"-Konzept und stellt je Aufgabe **zwei** Lücken gleichzeitig
 * (siehe `docs/adr/0006-zahlenmauer-mit-eigenem-bildschirm.md`, dessen Begründung hier ebenso
 * gilt).
 *
 * Anders als bei der Zahlenmauer bündelt die [schwierigkeitsstufe] den Zahlenraum **nicht** mit: Beide sind hier
 * frei kombinierbar, das Kind wählt sie getrennt.
 */
data class NachbarzahlenSettings(
    val schwierigkeitsstufe: NachbarzahlenSchwierigkeitsstufe,
    val zahlenraum: Int,
    val aufgabenzahl: Int,
)
