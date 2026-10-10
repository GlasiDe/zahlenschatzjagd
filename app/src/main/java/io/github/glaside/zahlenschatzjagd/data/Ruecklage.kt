package io.github.glaside.zahlenschatzjagd.data

import java.io.File
import java.io.RandomAccessFile

/**
 * Die **Rücklage**: die Kopie des gespeicherten Fortschritts, die die App selbst anlegt, kurz
 * bevor sie ihn auf eine neue Form bringt. Siehe
 * `docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`.
 *
 * Bewusst **ohne Android und ohne SQLite**: Die Schema-Version steht an einer festen Stelle im
 * Datei-Kopf, und eine Kopie ist eine Kopie. Damit ist die Entscheidung „muss gesichert werden?"
 * ohne Gerät prüfbar – und, wichtiger, sie fällt **bevor** irgendetwas die Datenbank öffnet.
 * Genau das ist der Moment, um den es geht.
 */

/** Ordnername der Rücklage, angelegt neben den übrigen App-Dateien – nicht bei den Datenbanken. */
const val RUECKLAGE_ORDNER = "ruecklage"

/** Es liegt keine lesbare Datenbank vor: frische Installation, angefangene oder fremde Datei. */
const val SCHEMA_VERSION_UNBEKANNT = 0

/** Die ersten Bytes jeder SQLite-Datei, inklusive des abschließenden Null-Bytes. */
private val SQLITE_KENNUNG = "SQLite format 3".toByteArray(Charsets.US_ASCII) + 0.toByte()

/** Position der Schema-Version im Datei-Kopf – `PRAGMA user_version`, 4 Byte, big-endian. */
private const val POSITION_SCHEMA_VERSION = 60
private const val LAENGE_SCHEMA_VERSION = 4
private const val NOETIGE_KOPFLAENGE = POSITION_SCHEMA_VERSION + LAENGE_SCHEMA_VERSION

/** Zum Zusammensetzen der vier Bytes zu einer Zahl. */
private const val BITS_JE_BYTE = 8
private const val BYTE_MASKE = 0xFF

/**
 * Die Datenbank und die beiden Dateien, die SQLite im WAL-Modus daneben führt. Die `-wal` kann
 * Schreibvorgänge enthalten, die noch nicht in der Hauptdatei stehen; ohne sie wäre die Rücklage
 * womöglich älter als der letzte Übungsabend.
 */
private val ENDUNGEN = listOf("", "-wal", "-shm")

/**
 * Liest die gespeicherte Schema-Version direkt aus dem Datei-Kopf – ohne die Datenbank zu
 * öffnen, denn genau das Öffnen ist ja der Vorgang, der schiefgehen kann.
 *
 * Fehlt die SQLite-Kennung, liefert die Funktion [SCHEMA_VERSION_UNBEKANNT] statt einer Zahl aus
 * Zufallsbytes. Eine Datei ohne erkennbaren Kopf ist ohnehin nichts, was sich retten ließe: Die
 * App geht dann in den Schutzhalt und lässt sie unangetastet liegen.
 */
fun leseSchemaVersion(datenbank: File): Int {
    val kopf = liesKopf(datenbank)
        ?.takeIf { it.copyOf(SQLITE_KENNUNG.size).contentEquals(SQLITE_KENNUNG) }
        ?: return SCHEMA_VERSION_UNBEKANNT
    return (0 until LAENGE_SCHEMA_VERSION).fold(0) { wert, stelle ->
        (wert shl BITS_JE_BYTE) or (kopf[POSITION_SCHEMA_VERSION + stelle].toInt() and BYTE_MASKE)
    }
}

/**
 * Die ersten Bytes der Datei, oder `null`, wenn es sie nicht gibt, sie zu kurz ist oder das
 * Lesen scheitert. Ein Fehler wäre hier das falsche Mittel: Die Frage lautet ja gerade, ob
 * überhaupt etwas Brauchbares vorliegt.
 */
private fun liesKopf(datenbank: File): ByteArray? = runCatching {
    RandomAccessFile(datenbank, "r").use { datei ->
        if (datei.length() < NOETIGE_KOPFLAENGE) {
            null
        } else {
            ByteArray(NOETIGE_KOPFLAENGE).also { datei.readFully(it) }
        }
    }
}.getOrNull()

/**
 * Ob vor dem Öffnen eine Rücklage anzulegen ist. Nur die Abweichung zählt – der `versionCode`
 * der App wäre der falsche Auslöser, denn der springt auch bei einem reinen Grafik-Fix.
 *
 * Eine frische Installation braucht keine: Es gibt nichts, was verloren gehen könnte.
 */
fun brauchtRuecklage(gefunden: Int, erwartet: Int): Boolean =
    gefunden != SCHEMA_VERSION_UNBEKANNT && gefunden != erwartet

/**
 * Kopiert die Datenbank samt Begleitdateien nach [ziel]. Eine vorhandene Rücklage wird dabei
 * **ersetzt** – es gibt immer höchstens eine, die von der letzten Schema-Änderung.
 *
 * Fehlt eine Begleitdatei bei der Quelle, wird sie im Ziel gelöscht. Bliebe sie liegen, gehörte
 * sie zu einer früheren Datenbank, und die Rücklage widerspräche sich selbst.
 */
fun legeRuecklageAn(datenbank: File, ziel: File) {
    ziel.mkdirs()
    ENDUNGEN.forEach { endung ->
        val quelle = File(datenbank.parentFile, datenbank.name + endung)
        val kopie = File(ziel, datenbank.name + endung)
        if (quelle.exists()) quelle.copyTo(kopie, overwrite = true) else kopie.delete()
    }
}
