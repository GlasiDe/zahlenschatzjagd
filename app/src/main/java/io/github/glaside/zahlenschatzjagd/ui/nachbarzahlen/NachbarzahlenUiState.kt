package io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen

import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.Nachbarposition
import io.github.glaside.zahlenschatzjagd.Nachbarzahlenaufgabe
import io.github.glaside.zahlenschatzjagd.UnlockedBadge

/**
 * Was das Kind in **eine** Lücke getippt hat. Getrennt von [Nachbarzahlenaufgabe] (die die
 * Lösung kennt), damit die Oberfläche nie an die Lösung herankommt, bevor geprüft wurde.
 * [istRichtig] bleibt `null`, bis mit ✓ geprüft wurde – danach steht das Ergebnis fest, einen
 * zweiten Versuch gibt es nicht.
 */
data class Lueckeneingabe(val eingabe: String = "", val istRichtig: Boolean? = null)

/**
 * Ein vollständiger Schnappschuss des Nachbarzahlen-Bildschirms. Analog zu
 * [io.github.glaside.zahlenschatzjagd.ui.game.GameUiState], aber je Aufgabe mit **zwei** gleichzeitig
 * offenen Lücken statt einer einzelnen Antwort.
 */
data class NachbarzahlenUiState(
    val aufgabe: Nachbarzahlenaufgabe? = null,
    /** Nur die Lücken – die vorgegebene Zahl steht hier nie. */
    val eingaben: Map<Nachbarposition, Lueckeneingabe> = emptyMap(),
    val gewaehlteLuecke: Nachbarposition? = null,
    /** true = mit ✓ geprüft: Ergebnis sichtbar, keine Eingabe mehr möglich. */
    val geprueft: Boolean = false,
    val aufgabenNummer: Int = 1,
    val aufgabenGesamt: Int = 0,
    /** Ganze Reihen, die **vollständig** richtig waren – eine falsche Zahl macht die Reihe falsch. */
    val richtige: Int = 0,
    val gepruefte: Int = 0,
    val fertig: Boolean = false,
    val neueAbzeichen: List<UnlockedBadge> = emptyList(),
    /**
     * Die Aufgabe, die Ella vorliest – nur die **erste** einer Runde, und nur, bis die Oberfläche
     * mit [NachbarzahlenViewModel.ansageVorgelesen] quittiert hat.
     *
     * Jede einzelne vorzulesen war am Gerät störend (Prüfstand 14.08.2026), und keine andere
     * Übungsform tut es: Dort spricht Ella einmal beim Betreten eines Bildschirms. Der erste Satz
     * bleibt trotzdem, weil ein Kind, das noch nicht sicher liest, sonst nicht erführe, was hier zu
     * tun ist. Dass er je nach vorgegebener Stelle anders lautet, bleibt davon unberührt – er
     * beschreibt dann eben die erste Aufgabe. Quittiert wird, damit er nach dem Drehen nicht noch
     * einmal kommt (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
     */
    val ausstehendeAnsage: Nachbarzahlenaufgabe? = null,
    /**
     * Der **Ton** zur gerade geprüften Reihe – „richtig" nur, wenn beide Zahlen stimmen –, bis die
     * Oberfläche ihn mit [NachbarzahlenViewModel.tonGespielt] quittiert hat.
     */
    val ausstehenderTon: GameSound? = null,
) {
    /**
     * Ob ✓ die Reihe werten darf. Erst wenn **beide** Lücken gefüllt sind – sonst gäbe ein
     * Kind versehentlich eine halbe Antwort ab, und die zählte als ganze falsche Aufgabe.
     */
    val pruefenMoeglich: Boolean
        get() = !geprueft && eingaben.isNotEmpty() && eingaben.values.all { it.eingabe.isNotEmpty() }
}
