package io.github.glaside.zahlenschatzjagd.data

import java.io.File

/**
 * Wie der Start der App ausgegangen ist: Entweder liegt der Fortschritt bereit, oder die App
 * geht in den **Schutzhalt** und lässt ihn unangetastet liegen.
 *
 * Siehe `docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`.
 */
sealed interface Datenbankstart {

    /** Die Datenbank ist offen; die App läuft wie immer. */
    data object Bereit : Datenbankstart

    /**
     * Die App kann den vorgefundenen Fortschritt nicht sicher weiterverwenden und fasst ihn
     * deshalb nicht an. [ursache] hält die technische Meldung fest – sie steht klein auf dem
     * Bildschirm, weil sie genau das ist, was beim Nachbauen weiterhilft.
     */
    data class Schutzhalt(
        val grund: Schutzhaltgrund,
        val gefunden: Int,
        val erwartet: Int,
        val ursache: String? = null,
    ) : Datenbankstart
}

/** Warum die App im Schutzhalt steht – davon hängt ab, was ein Erwachsener tun kann. */
enum class Schutzhaltgrund {

    /** Gespeichert ist eine ältere Form, für die kein Weg nach oben hinterlegt ist. */
    WEG_FEHLT,

    /** Gespeichert ist eine **neuere** Form: Auf dem Gerät lief schon eine neuere App. */
    DATEN_NEUER,

    /** Die Form passt, die Datei geht trotzdem nicht auf – beschädigt oder unlesbar. */
    NICHT_LESBAR,
}

/**
 * Der Ablauf beim Start, in dieser Reihenfolge:
 *
 * 1. Die gespeicherte Schema-Version aus dem Datei-Kopf lesen, **ohne** die Datenbank zu öffnen.
 * 2. Weicht sie ab, eine [Rücklage][legeRuecklageAn] anlegen – vor dem Öffnen, denn danach ist
 *    die Umstellung bereits gelaufen.
 * 3. Öffnen versuchen. Geht das schief, bleibt die Datei liegen und die App meldet den
 *    Schutzhalt.
 *
 * [oeffne] kommt als Lambda herein, damit der Ablauf ohne Gerät prüfbar bleibt; in der App
 * steckt dahinter Room.
 */
fun starteDatenbank(
    datenbank: File,
    ruecklage: File,
    erwartet: Int,
    oeffne: () -> Unit,
): Datenbankstart {
    val gefunden = leseSchemaVersion(datenbank)
    if (brauchtRuecklage(gefunden, erwartet)) {
        // Scheitert das Kopieren – voller Speicher etwa –, ist das kein Grund, die App
        // anzuhalten: Ohne Rücklage geht nichts verloren, es fehlt nur das Netz für den einen
        // Fall, den sie abdeckt (eine Umstellung, die durchläuft und trotzdem Daten verliert).
        runCatching { legeRuecklageAn(datenbank, ruecklage) }
    }
    return try {
        oeffne()
        Datenbankstart.Bereit
    } catch (
        // Bewusst weit gefasst: Was auch immer beim Öffnen schiefgeht, es darf nie in einem
        // Absturz enden, der den Fortschritt fraglich macht. Der Schutzhalt ist die Antwort
        // auf **jeden** Fehlschlag, nicht nur auf den fehlenden Migrationsweg.
        @Suppress("TooGenericExceptionCaught") fehler: Exception
    ) {
        Datenbankstart.Schutzhalt(
            grund = grundFuer(gefunden, erwartet),
            gefunden = gefunden,
            erwartet = erwartet,
            ursache = fehler.message,
        )
    }
}

/**
 * Der Grund hängt allein an den beiden Versionen. Passen sie zueinander und es geht trotzdem
 * nicht auf, wäre „es fehlt eine Migration" gelogen – dann ist die Datei schlicht nicht lesbar.
 */
internal fun grundFuer(gefunden: Int, erwartet: Int): Schutzhaltgrund = when {
    gefunden > erwartet -> Schutzhaltgrund.DATEN_NEUER
    gefunden in 1 until erwartet -> Schutzhaltgrund.WEG_FEHLT
    else -> Schutzhaltgrund.NICHT_LESBAR
}
