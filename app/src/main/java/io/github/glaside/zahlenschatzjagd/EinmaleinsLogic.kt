package io.github.glaside.zahlenschatzjagd

import kotlin.random.Random

/**
 * Die beiden Schwierigkeitsstufen des **Einmaleins** (`CONTEXT.md`): die Reihe der Reihe nach
 * aufsagen oder ihre Aufgaben durcheinander beantworten. Die Reihenfolge der Einträge ist die
 * Anzeige-Reihenfolge, die leichtere zuerst.
 *
 * Der Enum-Name steckt in der Fortschrittskennung ([einmaleinsKennung]) und ist damit
 * **eingefroren**, siehe `docs/adr/0005-persistierte-namen-entkoppelt.md`. [beschriftung] ist das
 * Wort auf dem Bildschirm – für das Kind wie in der Freigabe dasselbe.
 */
enum class EinmaleinsSchwierigkeitsstufe(val beschriftung: String) {
    AUFSAGEN("Aufsagen"),
    DURCHEINANDER("Durcheinander"),
}

/** So viele Aufgaben hat eine Reihe: `1 · n` bis `10 · n`. */
const val AUFGABEN_JE_REIHE: Int = 10

/** Die wählbaren Reihen: die 2er- bis 10er-Reihe. Die 1er-Reihe ist Zählen, nicht Einmaleins. */
val EINMALEINS_REIHEN: List<Int> = (2..AUFGABEN_JE_REIHE).toList()

/**
 * Der feste Zahlenraum aller Einmaleins-Kennungen – auch der 2er-Reihe. Er steht mit jeder Aufgabe
 * in der Historie und ist damit eingefroren
 * (`docs/adr/0034-einmaleins-mit-kennungen-je-reihe-und-stufe.md`).
 */
const val EINMALEINS_ZAHLENRAUM: Int = 100

/**
 * Die Fortschrittskennung einer Reihe in einer Stufe, etwa `EINMALEINS_7_AUFSAGEN`. **Persistiert**
 * und eingefroren – siehe ADR-0034; `EinmaleinsLogicTest` hält das Schema gegen von Hand
 * geschriebene Kennungen fest.
 */
fun einmaleinsKennung(reihe: Int, stufe: EinmaleinsSchwierigkeitsstufe): String =
    "EINMALEINS_${reihe}_${stufe.name}"

/** Wie eine Reihe heißt – „7er-Reihe". */
fun reihenName(reihe: Int): String = "${reihe}er-Reihe"

/**
 * Eine Malaufgabe des Einmaleins: [faktor] mal [reihe]. Die Reihe steht immer **hinten**
 * (`8 · 7`, nie `7 · 8`) – so lesen sich die zehn Aufgaben einer Reihe als eine Folge.
 */
data class Malaufgabe(val faktor: Int, val reihe: Int) {
    val ergebnis: Int get() = faktor * reihe

    /** Die Aufgabe als Gleichung mit [luecke] an der Stelle des Ergebnisses: `8 · 7 = _`. */
    fun text(luecke: String = "_"): String = "$faktor · $reihe = $luecke"
}

/**
 * Die zehn Aufgaben einer Reihe in ihrer Reihenfolge. **Ohne 0**, weil die Reihe beim Faktor selbst
 * beginnt, und **mit** `1 · n`, das anders als bei Mal in den Grundrechenarten nicht seltener
 * vorkommt – siehe `LEHRPLAN.md`, Abschnitt „Einmaleins".
 */
fun aufgabenDerReihe(reihe: Int): List<Malaufgabe> =
    (1..AUFGABEN_JE_REIHE).map { Malaufgabe(it, reihe) }

/** In welcher Reihenfolge die gewählten Reihen aufgesagt werden: jede einmal, aufsteigend. */
fun aufsageReihenfolge(reihen: Collection<Int>): List<Int> = reihen.distinct().sorted()

/**
 * Der Vorrat einer **Durcheinander**-Runde: alle Aufgaben der gewählten [reihen], **ohne
 * Zurücklegen** gezogen. Erst wenn jede einmal dran war, wird neu gemischt – und dabei nie so, dass
 * die letzte Aufgabe der alten Füllung gleich wieder vorn steht.
 *
 * [random] ist injizierbar, damit Runden in Tests deterministisch sind.
 */
class Einmaleinsvorrat(
    reihen: Collection<Int>,
    private val random: Random = Random.Default,
) {
    private val moeglich = reihen.distinct().flatMap { aufgabenDerReihe(it) }
    private val uebrig = ArrayDeque<Malaufgabe>()
    private var zuletzt: Malaufgabe? = null

    init {
        require(moeglich.isNotEmpty()) { "Ohne Reihe gibt es keine Einmaleins-Aufgabe" }
    }

    fun ziehe(): Malaufgabe {
        if (uebrig.isEmpty()) uebrig.addAll(neueFuellung())
        return uebrig.removeFirst().also { zuletzt = it }
    }

    /**
     * Frisch gemischt. Stünde vorn ausgerechnet die zuletzt gezogene Aufgabe, tauscht sie mit einer
     * beliebigen anderen – eine Reihe hat zehn Aufgaben, es gibt also immer eine andere.
     */
    private fun neueFuellung(): List<Malaufgabe> {
        val gemischt = moeglich.shuffled(random).toMutableList()
        if (gemischt.first() == zuletzt) {
            val tausch = 1 + random.nextInt(gemischt.size - 1)
            gemischt[0] = gemischt[tausch].also { gemischt[tausch] = gemischt[0] }
        }
        return gemischt
    }
}
