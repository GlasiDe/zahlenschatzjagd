package io.github.glaside.zahlenschatzjagd

/**
 * Einstellungen einer Einmaleins-Runde. Bewusst **nicht** [GameSettings] wiederverwendet: Das
 * Einmaleins zählt je Aufgabe auf die Kennung ihrer **Reihe**, und Aufsagen stellt zehn Kästchen
 * auf einmal (siehe `docs/adr/0006-zahlenmauer-mit-eigenem-bildschirm.md`, dessen Begründung hier
 * ebenso gilt).
 *
 * [aufgabenzahl] gilt nur für [EinmaleinsSchwierigkeitsstufe.DURCHEINANDER]; beim Aufsagen ergibt
 * sich die Länge der Runde aus den [reihen] – jede einmal, zehn Kästchen.
 */
data class EinmaleinsSettings(
    val schwierigkeitsstufe: EinmaleinsSchwierigkeitsstufe,
    val reihen: List<Int>,
    val aufgabenzahl: Int,
)
