package io.github.glaside.zahlenschatzjagd.ui.parent

import io.github.glaside.zahlenschatzjagd.BADGE_CATEGORIES
import io.github.glaside.zahlenschatzjagd.BadgeCategory
import io.github.glaside.zahlenschatzjagd.MINDEST_AUFGABEN_STANDARD
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.ui.selection.UEBUNGSFORMEN
import io.github.glaside.zahlenschatzjagd.ui.selection.Uebungsform
import kotlin.math.roundToInt

/**
 * Wie viele Aufgaben je Fortschrittskennung höchstens in den Übungsstand eingehen – **die
 * jüngsten**. Über die ganze Historie gerechnet wäre die Trefferquote träge und würde sogar
 * besser, sobald ein Kind aufhört zu üben (`docs/adr/0025-uebungsstand-als-fenster.md`).
 */
const val UEBUNGSSTAND_AUFGABEN: Int = 50

/** Wie weit der Übungsstand höchstens zurückgreift, zusätzlich zu [UEBUNGSSTAND_AUFGABEN]. */
const val UEBUNGSSTAND_TAGE: Long = 30

private const val TAG_MILLIS = 24L * 60 * 60 * 1000
private const val PROZENT = 100

/**
 * Eine Zeile des **Übungsstands** – auf allen drei Ebenen dieselbe Bauform, weil auf allen dreien
 * dasselbe gefragt ist: was geübt wurde und wie sicher es sitzt.
 *
 * - Ebene 1 ist die **Übungsform** und fasst die Fenster ihrer Kennungen zusammen.
 * - Ebene 2 ist die **Fortschrittskennung** mit ihrem eigenen Fenster.
 * - Ebene 3 ist der **Zahlenraum** und teilt das Fenster seiner Kennung auf.
 *
 * Aufgeklappt wird nur, wo es darunter mehr als eins gibt: „Verliebte Zahlen" hat zwei Kennungen
 * mit je einem festen Zahlenraum und endet auf Ebene 2, „Zahlen vergleichen" hat eine Kennung mit
 * vier Zahlenräumen und überspringt Ebene 2.
 */
data class Uebungsstandzeile(
    /** Trägt die Testmarke und den Aufklapp-Zustand; **nicht** persistiert. */
    val schluessel: String,
    val beschriftung: String,
    /** Was im Fenster liegt – **alles**, auch was für eine Prozentzahl zu dünn ist. */
    val aufgaben: Int,
    /** Die richtigen unter den [gewerteten][gewertet] Aufgaben. */
    val richtig: Int,
    /** Worauf sich die Prozentzahl stützt. Siehe [trefferquote]. */
    val gewertet: Int,
    val mindestAufgaben: Int,
    val unterzeilen: List<Uebungsstandzeile> = emptyList(),
) {
    /** Ob im Fenster überhaupt etwas liegt. Siehe [quotenText]: „noch nicht" ist selbst ein Befund. */
    val geuebt: Boolean get() = aufgaben > 0

    /**
     * Die Trefferquote in Prozent – oder `null`, solange zu wenig geübt wurde. Eine Prozentzahl aus
     * drei Aufgaben sagt nichts, sieht aber aus, als sagte sie etwas.
     *
     * Gerechnet wird auf [gewertet] und nicht auf [aufgaben]: **Eine Zeile rechnet nur mit dem, was
     * darunter selbst eine Prozentzahl trägt.** Ohne diese Regel lag die Zahl der Übungsform
     * außerhalb des Bereichs aller aufgeklappten Zahlen — Grundrechenarten stand auf 95 %, während
     * darunter einzig Minus eine Zahl trug, nämlich 96 %, und der Rest „zu wenig geübt". Wer das
     * sieht, glaubt der Zeile zu Recht nicht mehr.
     */
    val trefferquote: Int?
        get() = if (gewertet >= mindestAufgaben) {
            (richtig * PROZENT.toFloat() / gewertet).roundToInt()
        } else {
            null
        }
}

/**
 * Baut den Übungsstand aus der Historie eines Profils.
 *
 * [aufgaben] darf ruhig weiter zurückreichen als [tage] – was außerhalb liegt, fällt hier heraus.
 * [jetztMillis] ist ein Parameter, damit die Rechnung ohne die Uhr des Rechners prüfbar bleibt
 * (wie [baueUebungskalender]).
 */
fun baueUebungsstand(
    aufgaben: List<TaskHistory>,
    jetztMillis: Long,
    fensterGroesse: Int = UEBUNGSSTAND_AUFGABEN,
    tage: Long = UEBUNGSSTAND_TAGE,
): List<Uebungsstandzeile> {
    val fensterJeKennung = fensterJeKennung(aufgaben, jetztMillis, fensterGroesse, tage)
    return UEBUNGSFORMEN.map { form -> zeileFuerUebungsform(form, fensterJeKennung) }
}

/**
 * Das Fenster je Fortschrittskennung: erst die letzten [tage] Tage, davon die jüngsten
 * [fensterGroesse] Aufgaben. **Je Kennung** und nicht über alle zusammen – sonst verdrängte
 * fleißiges Plus-Üben das Minus, und über Minus stünde nichts mehr da.
 */
private fun fensterJeKennung(
    aufgaben: List<TaskHistory>,
    jetztMillis: Long,
    fensterGroesse: Int,
    tage: Long,
): Map<String, List<TaskHistory>> {
    val aeltesteErlaubte = jetztMillis - tage * TAG_MILLIS
    return aufgaben
        .filter { it.timestamp >= aeltesteErlaubte }
        .groupBy { it.taskCategory }
        .mapValues { (_, aufgabenDerKennung) ->
            aufgabenDerKennung.sortedByDescending { it.timestamp }.take(fensterGroesse)
        }
}

private fun zeileFuerUebungsform(
    form: Uebungsform,
    fensterJeKennung: Map<String, List<TaskHistory>>,
): Uebungsstandzeile {
    // Eine Kennung ohne Katalog-Eintrag wird still übergangen – wie bei `freigabeAusSperren`:
    // Eine entfallene Übung soll den Bildschirm nicht unlesbar machen.
    val kategorien = form.kennungen.mapNotNull { kennung ->
        BADGE_CATEGORIES.firstOrNull { it.key == kennung }
    }
    val fenster = kategorien.flatMap { fensterJeKennung[it.key].orEmpty() }
    return zeile(
        schluessel = form.schluessel,
        beschriftung = form.name,
        fenster = fenster,
        // Das Maximum, nicht die Summe: Die Schwelle sagt „genug für eine Aussage", und das hängt
        // nicht daran, wie viele Kennungen in der Zeile zusammenkommen.
        mindestAufgaben = kategorien.maxOfOrNull { it.mindestAufgaben } ?: MINDEST_AUFGABEN_STANDARD,
        unterzeilen = if (kategorien.size > 1) {
            kategorien.map { zeileFuerKennung(it, fensterJeKennung[it.key].orEmpty()) }
        } else {
            // Nur eine Kennung: Ebene 2 wiederholte bloß die Zeile darüber.
            kategorien.singleOrNull()
                ?.let { zeilenFuerZahlenraeume(it, fensterJeKennung[it.key].orEmpty()) }
                .orEmpty()
        },
    )
}

private fun zeileFuerKennung(
    kategorie: BadgeCategory,
    fenster: List<TaskHistory>,
): Uebungsstandzeile = zeile(
    schluessel = kategorie.key,
    // Dieselbe Beschriftung wie auf der Schatztruhe und in der Freigabe: „Leicht" statt
    // „Zahlenmauer (leicht)", weil die Übungsform schon in der Zeile darüber steht.
    beschriftung = kategorie.chestLabel ?: kategorie.label,
    fenster = fenster,
    mindestAufgaben = kategorie.mindestAufgaben,
    unterzeilen = zeilenFuerZahlenraeume(kategorie, fenster),
)

/**
 * Baut eine Zeile und bestimmt dabei, **worauf ihre Prozentzahl sich stützt**.
 *
 * Hat sie [unterzeilen], zählt nur, was darunter selbst eine Prozentzahl trägt – siehe
 * [Uebungsstandzeile.trefferquote]. Ist sie ein Blatt, ist es ihr eigenes [fenster]. Die **Anzahl**
 * bleibt in beiden Fällen vollständig: „Was geübt wurde" ist die andere Frage dieses Bildschirms
 * und hat mit der Belastbarkeit der Quote nichts zu tun.
 */
private fun zeile(
    schluessel: String,
    beschriftung: String,
    fenster: List<TaskHistory>,
    mindestAufgaben: Int,
    unterzeilen: List<Uebungsstandzeile>,
): Uebungsstandzeile {
    val tragfaehige = unterzeilen.filter { it.trefferquote != null }
    return Uebungsstandzeile(
        schluessel = schluessel,
        beschriftung = beschriftung,
        aufgaben = fenster.size,
        richtig = if (unterzeilen.isEmpty()) {
            fenster.count { it.isCorrect }
        } else {
            tragfaehige.sumOf { it.richtig }
        },
        gewertet = if (unterzeilen.isEmpty()) fenster.size else tragfaehige.sumOf { it.gewertet },
        mindestAufgaben = mindestAufgaben,
        unterzeilen = unterzeilen,
    )
}

/**
 * Die Zahlenräume einer Kennung – aber nur, wenn sie mehr als einen hat. Jede Stufe der Zahlenmauer
 * rechnet in genau einem festen Zahlenraum; dort wäre die dritte Ebene eine Zeile, die dasselbe
 * noch einmal sagt.
 *
 * Auch nie geübte Zahlenräume stehen da: „bis 100 noch nicht geübt" ist die Hälfte dessen, wonach
 * hier gefragt wird.
 */
private fun zeilenFuerZahlenraeume(
    kategorie: BadgeCategory,
    fenster: List<TaskHistory>,
): List<Uebungsstandzeile> {
    if (kategorie.ranges.size < 2) return emptyList()
    return kategorie.ranges.map { zahlenraum ->
        zeile(
            schluessel = "${kategorie.key}_$zahlenraum",
            beschriftung = "bis $zahlenraum",
            fenster = fenster.filter { it.numberRange == zahlenraum },
            mindestAufgaben = kategorie.mindestAufgaben,
            unterzeilen = emptyList(),
        )
    }
}

/**
 * Was in einer Zeile rechts steht. Die beiden Fälle ohne Prozentzahl sind absichtlich verschieden
 * formuliert: „noch nicht geübt" ist eine Aussage über das Kind, „zu wenig geübt" eine über die
 * Grundlage der Rechnung.
 */
fun quotenText(zeile: Uebungsstandzeile): String {
    val quote = zeile.trefferquote
    return when {
        !zeile.geuebt -> "noch nicht geübt"
        quote == null -> "zu wenig geübt"
        else -> "$quote %"
    }
}

/** „1 Aufgabe" / „12 Aufgaben" – damit die Zeile auch bei einer einzigen Aufgabe Deutsch bleibt. */
fun aufgabenText(anzahl: Int): String = if (anzahl == 1) "1 Aufgabe" else "$anzahl Aufgaben"

/**
 * Der Satz, der die Schwelle auf dem Bildschirm nennt – aus den [zeilen] abgeleitet statt
 * hingeschrieben, damit er einer geänderten Schwelle folgt und nicht still falsch wird.
 *
 * Genannt werden der Regelfall und jede Übungsform, die davon abweicht.
 */
fun schwellenHinweis(zeilen: List<Uebungsstandzeile>): String {
    val regelfall = zeilen.groupingBy { it.mindestAufgaben }.eachCount()
        .maxByOrNull { it.value }?.key ?: MINDEST_AUFGABEN_STANDARD
    val satz = "Eine Prozentzahl steht erst ab $regelfall geübten Aufgaben."
    val abweichend = zeilen.filter { it.mindestAufgaben != regelfall }
    if (abweichend.isEmpty()) return satz
    val ausnahmen = abweichend.joinToString(" und ") { "${it.beschriftung} ab ${it.mindestAufgaben}" }
    return "$satz Bei $ausnahmen."
}
