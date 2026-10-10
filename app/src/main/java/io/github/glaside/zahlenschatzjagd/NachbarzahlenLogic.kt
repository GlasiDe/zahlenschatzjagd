package io.github.glaside.zahlenschatzjagd

import kotlin.random.Random

/**
 * Die Stelle, an der eine der drei aufeinanderfolgenden Zahlen einer Nachbarzahlen-Aufgabe steht.
 * Die Reihenfolge der Einträge ist die **Leserichtung** – darauf stützen sich [reihenText],
 * [loesungsText] und die Anordnung auf dem Bildschirm.
 */
enum class Nachbarposition { LINKS, MITTE, RECHTS }

/**
 * Die Schwierigkeitsstufen der Übungsform **Nachbarzahlen**: Sie unterscheiden sich allein darin,
 * **welche** der drei Zahlen vorgegeben sein kann ([moeglichePositionen]).
 *
 * [kennung] ist die Fortschrittskennung dieser Schwierigkeitsstufe – jede sammelt eigene Abzeichen
 * und wird eigens freigegeben. **Persistiert** (`TaskHistory.taskCategory`): Der Enum-Name ist
 * damit eingefroren, siehe `docs/adr/0005-persistierte-namen-entkoppelt.md`.
 */
enum class NachbarzahlenSchwierigkeitsstufe(val moeglichePositionen: List<Nachbarposition>) {
    /** Immer die mittlere Zahl vorgegeben – das Kind ergänzt nur davor und danach. */
    LEICHT(listOf(Nachbarposition.MITTE)),

    /**
     * Je Aufgabe zufällig die linke, die mittlere oder die rechte Zahl vorgegeben. Damit muss ein
     * Kind auch **rückwärts** denken („die 9 ist die kleinste, welche kommen danach?") und arbeitet
     * nicht nur ein Muster ab.
     */
    SCHWER(Nachbarposition.entries),
    ;

    val kennung: String get() = "NACHBARZAHLEN_$name"
}

/**
 * Eine Aufgabe: drei aufeinanderfolgende Zahlen, von denen genau eine vorgegeben ist. Gespeichert
 * wird nur die [mitte] – die beiden äußeren ergeben sich daraus, und so kann eine Reihe gar nicht
 * erst in einen Zustand geraten, in dem ihre Zahlen nicht aufeinanderfolgen.
 */
data class Nachbarzahlenaufgabe(val mitte: Int, val vorgegeben: Nachbarposition) {

    val links: Int get() = mitte - 1
    val rechts: Int get() = mitte + 1

    /** Die beiden Stellen, die das Kind füllt – in Leserichtung. */
    val luecken: List<Nachbarposition> get() = Nachbarposition.entries.filter { it != vorgegeben }

    fun zahlAn(position: Nachbarposition): Int = when (position) {
        Nachbarposition.LINKS -> links
        Nachbarposition.MITTE -> mitte
        Nachbarposition.RECHTS -> rechts
    }
}

/** Der Platzhalter, der in der Historie an der Stelle einer Lücke steht. */
const val NACHBARZAHLEN_PLATZHALTER: String = "_"

/**
 * Die Reihe, wie sie in der Historie steht: die vorgegebene Zahl im Klartext, die Lücken als
 * [platzhalter] – etwa `_ 7 _`. So sieht ein Erwachsener im Übungsstand, welche Form das Kind vor
 * sich hatte.
 */
fun Nachbarzahlenaufgabe.reihenText(platzhalter: String = NACHBARZAHLEN_PLATZHALTER): String =
    Nachbarposition.entries.joinToString(" ") { position ->
        if (position == vorgegeben) zahlAn(position).toString() else platzhalter
    }

/** Die beiden gesuchten Zahlen in Leserichtung, etwa `6, 8`. */
fun Nachbarzahlenaufgabe.loesungsText(): String =
    luecken.joinToString(", ") { zahlAn(it).toString() }

/**
 * Was Ella zu dieser Aufgabe sagt (Wortlaut abgenommen).
 *
 * Der Satz hängt an der vorgegebenen Stelle und nicht an der Stufe: In der leichten Stufe ist das
 * immer [Nachbarposition.MITTE], in der schweren wechselt er je Aufgabe. So hört ein Kind, das
 * nicht liest, welche Zahl feststeht.
 */
fun Nachbarzahlenaufgabe.ansage(): String {
    val zahl = zahlAn(vorgegeben)
    return when (vorgegeben) {
        Nachbarposition.MITTE -> "Hier steht die $zahl. Welche Zahl kommt davor? Und welche kommt danach?"
        Nachbarposition.LINKS -> "Hier steht die $zahl. Welche zwei Zahlen kommen danach?"
        Nachbarposition.RECHTS -> "Hier steht die $zahl. Welche zwei Zahlen kommen davor?"
    }
}

/**
 * Alle Aufgaben, die es in dieser [schwierigkeitsstufe] und diesem [zahlenraum] gibt.
 *
 * Die Ränder folgen aus der einen Regel „alle drei Zahlen liegen im Zahlenraum": Die mittlere Zahl
 * liegt in `1..zahlenraum-1`, eine vorgegebene linke Zahl damit in `0..zahlenraum-2`, eine
 * vorgegebene rechte in `2..zahlenraum`.
 *
 * Gleichverteilt und vollständig – **keine** Bevorzugung von Zehnerübergängen (im Zahlenraum 20
 * gäbe es nur 3 solche Reihen von 19, eine Quote wäre also wirkungslos und erzwänge
 * Wiederholungen) und **keine** 0/1-Reduktion wie beim Rechnen: `0 | 1 | 2` ist hier keine
 * triviale Aufgabe, sondern der Anfang der Zahlreihe. Siehe `LEHRPLAN.md`.
 */
fun moeglicheAufgaben(
    schwierigkeitsstufe: NachbarzahlenSchwierigkeitsstufe,
    zahlenraum: Int,
): List<Nachbarzahlenaufgabe> =
    (1 until zahlenraum).flatMap { mitte ->
        schwierigkeitsstufe.moeglichePositionen.map { Nachbarzahlenaufgabe(mitte, it) }
    }

/**
 * Der Vorrat, aus dem eine Runde ihre Aufgaben zieht: **ohne Zurücklegen**, damit sich innerhalb
 * einer Runde nichts wiederholt, solange der Vorrat reicht. Ist er leer, beginnt er von vorn (neu
 * gemischt) – eine Runde darf länger sein als der Vorrat.
 *
 * [random] ist injizierbar, damit Runden in Tests deterministisch sind.
 */
class Nachbarzahlenvorrat(
    schwierigkeitsstufe: NachbarzahlenSchwierigkeitsstufe,
    zahlenraum: Int,
    private val random: Random = Random.Default,
) {
    private val moeglich = moeglicheAufgaben(schwierigkeitsstufe, zahlenraum)
    private val uebrig = ArrayDeque<Nachbarzahlenaufgabe>()

    init {
        require(moeglich.isNotEmpty()) {
            "Im Zahlenraum $zahlenraum gibt es keine Nachbarzahlen-Aufgabe"
        }
    }

    fun ziehe(): Nachbarzahlenaufgabe {
        if (uebrig.isEmpty()) uebrig.addAll(neueFuellung())
        return uebrig.removeFirst()
    }

    /**
     * Eine frische Füllung: gemischt, aber mit je **einer** Aufgabe pro möglicher Position ganz
     * vorn. So sieht ein Kind in der schweren Stufe alle drei Formen, auch wenn seine Runde nur
     * fünf Aufgaben lang ist – rein zufällig gezogen fehlte dort in gut einem Drittel der Runden
     * eine Position, und die Übungsform verlöre gerade das, was sie schwer macht. Welche Reihe je
     * Position vorn steht und in welcher Reihenfolge die drei kommen, bleibt Zufall; dahinter ist
     * der Vorrat unverändert gemischt. In der leichten Stufe gibt es nur eine Position – dort
     * ändert die Schichtung nichts.
     */
    private fun neueFuellung(): List<Nachbarzahlenaufgabe> {
        val gemischt = moeglich.shuffled(random)
        val eroeffnung = gemischt.distinctBy { it.vorgegeben }
        return eroeffnung + (gemischt - eroeffnung.toSet())
    }
}
