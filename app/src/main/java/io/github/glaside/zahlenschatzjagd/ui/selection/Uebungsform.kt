package io.github.glaside.zahlenschatzjagd.ui.selection

/**
 * Eine **Übungsform** – eine Art zu üben, mit den Fortschrittskennungen, die über sie erreichbar
 * sind (`CONTEXT.md`).
 *
 * Die Übungsformen liegen im Code auf zwei Inseln verteilt ([RECHNEN_GAMES], [ZAHLENSPIELE_GAMES]),
 * weil das Kind sie dort auswählt. Wer sie **ohne** Insel braucht – der Elternbereich zählt
 * Fortschritt entlang der Übungsform, nicht entlang der Insel –, nimmt [UEBUNGSFORMEN] und nicht
 * eine dritte, von Hand gepflegte Liste.
 *
 * [schluessel] ist die Kennung der Übungsform selbst (`GRUNDRECHENARTEN`, `VERLIEBTE`, …). Sie ist
 * **nicht** persistiert – sie trägt nur Testmarken und Navigation und darf sich ändern, anders als
 * die Fortschrittskennungen in [kennungen].
 */
data class Uebungsform(
    val schluessel: String,
    val name: String,
    val kennungen: List<String>,
)

/**
 * Alle Übungsformen in Anzeige-Reihenfolge: erst die Rechnen-Insel, dann die Zahlenspiele – so
 * stehen sie auch auf der Startkarte.
 *
 * Abgeleitet statt abgeschrieben: `UebungsformKennungenTest` hält bereits fest, dass jede
 * Fortschrittskennung zu genau einer der beiden Listen gehört. Eine eigene Liste hier wäre eine
 * zweite Wahrheit, die beim Ergänzen einer Übungsform vergessen würde.
 */
val UEBUNGSFORMEN: List<Uebungsform> =
    RECHNEN_GAMES.map { Uebungsform(it.id.name, it.title, it.kennungen) } +
        ZAHLENSPIELE_GAMES.map { Uebungsform(it.id.name, it.title, it.kennungen) }
