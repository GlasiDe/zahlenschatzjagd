package io.github.glaside.zahlenschatzjagd.ui.parent

import io.github.glaside.zahlenschatzjagd.data.Rundenabschluss
import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Wie viele Wochen der Übungskalender zeigt. Sechs passen ohne Scrollen auf einen Bildschirm. */
const val KALENDER_WOCHEN: Int = 6

/**
 * Ein Tag im **Übungskalender**.
 *
 * [runden] ist die Zahl der an diesem Tag **abgeschlossenen** Runden – daran und an nichts anderem
 * hängt [geuebt]. [bildschirmzeitMillis] ist davon unabhängig: Zeit kann auch anfallen, ohne dass
 * eine Runde zu Ende gebracht wurde.
 */
data class Kalendertag(
    val datum: LocalDate,
    val runden: Int,
    val bildschirmzeitMillis: Long,
    /** Liegt der Tag noch vor uns? Dann ist „nicht geübt" keine Aussage, sondern nur noch offen. */
    val zukunft: Boolean,
) {
    /** Ein Tag gilt als geübt, sobald an ihm **eine Runde abgeschlossen** wurde. */
    val geuebt: Boolean get() = runden > 0
}

/**
 * Eine Woche im Übungskalender: immer sieben Tage, Montag bis Sonntag – auch wenn die letzte
 * Woche noch nicht zu Ende ist. Eine Zeile, die manchmal fünf und manchmal sieben Kästchen hat,
 * wäre als Muster nicht zu lesen.
 */
data class Kalenderwoche(val tage: List<Kalendertag>) {
    val beginn: LocalDate get() = tage.first().datum
    val ende: LocalDate get() = tage.last().datum
    val geuebteTage: Int get() = tage.count { it.geuebt }
    val bildschirmzeitMillis: Long get() = tage.sumOf { it.bildschirmzeitMillis }
}

/**
 * Baut den Übungskalender: [wochen] volle Wochen, die letzte ist die von [heute].
 *
 * [runden] und [zeitabschnitte] dürfen ruhig weiter zurückreichen als der gezeigte Zeitraum –
 * was außerhalb liegt, fällt hier heraus.
 *
 * [zone] ist ein Parameter, weil die Zuordnung zu einem Kalendertag von ihr abhängt: Dieselbe
 * Millisekunde ist je Zeitzone ein anderer Tag. Sie kommt aus der Uhr des ViewModels, damit die
 * Rechnung hier prüfbar bleibt.
 */
fun baueUebungskalender(
    runden: List<Rundenabschluss>,
    zeitabschnitte: List<Zeitabschnitt>,
    heute: LocalDate,
    zone: ZoneId,
    wochen: Int = KALENDER_WOCHEN,
): List<Kalenderwoche> {
    val letzterMontag = heute.minusDays((heute.dayOfWeek.value - 1).toLong())
    val ersterMontag = letzterMontag.minusWeeks((wochen - 1).toLong())
    val rundenJeTag = runden.groupingBy { tagVon(it.zeitpunkt, zone) }.eachCount()
    val zeitJeTag = bildschirmzeitJeTag(zeitabschnitte, zone)

    return List(wochen) { wocheNr ->
        val montag = ersterMontag.plusWeeks(wocheNr.toLong())
        Kalenderwoche(
            List(WOCHENTAGE.size) { tagNr ->
                val datum = montag.plusDays(tagNr.toLong())
                Kalendertag(
                    datum = datum,
                    runden = rundenJeTag[datum] ?: 0,
                    bildschirmzeitMillis = zeitJeTag[datum] ?: 0L,
                    zukunft = datum.isAfter(heute),
                )
            }
        )
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L

/**
 * Verteilt die Zeitabschnitte auf Kalendertage.
 *
 * Ein Abschnitt über Mitternacht wird an der Tagesgrenze **geteilt**, statt komplett dem Tag
 * seines Beginns zugeschlagen zu werden: Sonst schriebe ein Tag die Minuten des anderen auf sein
 * Konto, und in einem Kalender ist genau das die Frage. Die Grenze wird über
 * [LocalDate.atStartOfDay] bestimmt und ist damit auch an den beiden Umstellungstagen richtig.
 */
private fun bildschirmzeitJeTag(
    abschnitte: List<Zeitabschnitt>,
    zone: ZoneId,
): Map<LocalDate, Long> {
    val summen = mutableMapOf<LocalDate, Long>()
    for (abschnitt in abschnitte) {
        var von = abschnitt.beginn
        while (von < abschnitt.ende) {
            val tag = tagVon(von, zone)
            val tagesende = tag.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val bis = minOf(abschnitt.ende, tagesende)
            summen[tag] = (summen[tag] ?: 0L) + (bis - von)
            von = bis
        }
    }
    return summen
}

private fun tagVon(millis: Long, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

/**
 * Die Kürzel über den sieben Spalten des Punktemusters. Die Woche beginnt am Montag – so steht sie
 * in jedem deutschen Kalender.
 */
val WOCHENTAGE: List<String> = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

/**
 * Die Monatskürzel für die Beschriftungen. Von Hand statt über einen [java.time]-Formatierer:
 * dessen Abkürzungen hängen an den Sprachdaten der Laufzeit und sähen auf zwei Geräten
 * verschieden aus.
 */
private val MONATE = listOf(
    "Jan.", "Feb.", "März", "Apr.", "Mai", "Juni",
    "Juli", "Aug.", "Sep.", "Okt.", "Nov.", "Dez.",
)

private fun monat(datum: LocalDate): String = MONATE[datum.monthValue - 1]

/** Die Woche als Zeitraum: „3.–9. Aug." bzw. über den Monatswechsel „27. Juli–2. Aug.". */
fun wochenBeschriftung(woche: Kalenderwoche): String =
    if (woche.beginn.monthValue == woche.ende.monthValue) {
        "${woche.beginn.dayOfMonth}.–${woche.ende.dayOfMonth}. ${monat(woche.ende)}"
    } else {
        "${woche.beginn.dayOfMonth}. ${monat(woche.beginn)}–" +
            "${woche.ende.dayOfMonth}. ${monat(woche.ende)}"
    }

/** Ein einzelner Tag als Beschriftung: „Mo, 10. Aug.". */
fun tagesBeschriftung(datum: LocalDate): String =
    "${WOCHENTAGE[datum.dayOfWeek.value - 1]}, ${datum.dayOfMonth}. ${monat(datum)}"

/** „1 Runde" / „3 Runden" – damit die Zeile auch bei einer einzigen Runde Deutsch bleibt. */
fun rundenAnzahl(anzahl: Int): String = if (anzahl == 1) "1 Runde" else "$anzahl Runden"

/**
 * Was ein Tag des Punktemusters in Worten sagt – für die Vorlesefunktion und zugleich die Grundlage
 * der instrumentierten Prüfung.
 *
 * „keine abgeschlossene Runde" statt „nicht geübt": So enthält nur der gefärbte Tag das Wort
 * *geübt*, und ein Test kann darauf zeigen, ohne sich an der Dauer festzumachen. Die Dauer steht
 * **auch** an einem Tag ohne Runde, sofern welche anfiel – genau dieser Fall (die App war offen,
 * geübt wurde nicht) ist derjenige, den das Muster allein nicht zeigen kann.
 */
fun tagBeschreibung(tag: Kalendertag): String {
    val datum = tagesBeschriftung(tag.datum)
    val dauer = formatiereDauer(tag.bildschirmzeitMillis)
    return when {
        tag.zukunft -> "$datum: noch offen"
        tag.geuebt -> "$datum: geübt, ${rundenAnzahl(tag.runden)}, $dauer"
        tag.bildschirmzeitMillis > 0 -> "$datum: keine abgeschlossene Runde, $dauer"
        else -> "$datum: keine abgeschlossene Runde"
    }
}

/**
 * Eine Dauer, wie sie im Elternbereich dasteht: „–", „unter 1 Min", „12 Min", „1 Std",
 * „2 Std 5 Min".
 *
 * Sekunden kommen nicht vor – niemand liest hier auf die Sekunde. Eine angefangene Minute wird
 * aber ausdrücklich benannt statt zu „–" abgerundet, sonst sähe ein kurzer Besuch aus wie keiner.
 */
fun formatiereDauer(millis: Long): String {
    val minuten = millis / MILLIS_PER_MINUTE
    return when {
        millis <= 0L -> "–"
        minuten == 0L -> "unter 1 Min"
        minuten < MINUTES_PER_HOUR -> "$minuten Min"
        minuten % MINUTES_PER_HOUR == 0L -> "${minuten / MINUTES_PER_HOUR} Std"
        else -> "${minuten / MINUTES_PER_HOUR} Std ${minuten % MINUTES_PER_HOUR} Min"
    }
}
