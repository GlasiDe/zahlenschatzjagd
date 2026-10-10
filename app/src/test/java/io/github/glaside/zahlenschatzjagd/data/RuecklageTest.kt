package io.github.glaside.zahlenschatzjagd.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Die **Rücklage** – die Kopie, die die App vor einer Schema-Änderung selbst anlegt
 * (`docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`).
 *
 * Beides hier ist reine Dateiarbeit ohne Android und ohne SQLite: Die Schema-Version steht an
 * einer festen Stelle im Datei-Kopf, und das Kopieren ist gewöhnliches Kopieren. Deshalb liegt
 * der Test hier und nicht bei den instrumentierten – dass eine **echte** Room-Datenbank denselben
 * Kopf trägt, prüft `SchutzhaltInstrumentedTest`.
 */
class RuecklageTest {

    @get:Rule
    val ordner = TemporaryFolder()

    private fun sqliteDatei(name: String, version: Int, inhalt: String = ""): File =
        ordner.newFile(name).apply { writeBytes(sqliteKopf(version) + inhalt.toByteArray()) }

    @Test
    fun schemaVersion_stehtImKopfDerDatei() {
        assertEquals(6, leseSchemaVersion(sqliteDatei("db6", version = 6)))
        assertEquals(300, leseSchemaVersion(sqliteDatei("db300", version = 300)))
    }

    @Test
    fun schemaVersion_einerFrischenInstallation_istUnbekannt() {
        val fehlt = File(ordner.root, "gibt_es_nicht")

        assertEquals(SCHEMA_VERSION_UNBEKANNT, leseSchemaVersion(fehlt))
    }

    /**
     * Eine angefangene oder fremde Datei darf keine Zahl liefern, die wie eine Schema-Version
     * aussieht – sonst würde aus Zufallsbytes eine Entscheidung.
     */
    @Test
    fun schemaVersion_ohneSqliteKennung_istUnbekannt() {
        val fremd = ordner.newFile("fremd").apply { writeBytes(ByteArray(200) { 7 }) }
        val angefangen = ordner.newFile("kurz").apply { writeBytes(ByteArray(12)) }

        assertEquals(SCHEMA_VERSION_UNBEKANNT, leseSchemaVersion(fremd))
        assertEquals(SCHEMA_VERSION_UNBEKANNT, leseSchemaVersion(angefangen))
    }

    @Test
    fun ruecklage_wirdNurBeiAbweichenderVersionGebraucht() {
        assertFalse("gleiche Version: nichts steht an", brauchtRuecklage(gefunden = 6, erwartet = 6))
        assertTrue("ältere Version: es wird migriert", brauchtRuecklage(gefunden = 5, erwartet = 6))
        assertTrue("neuere Version: erst recht", brauchtRuecklage(gefunden = 7, erwartet = 6))
        assertFalse(
            "frische Installation: es gibt nichts zu retten",
            brauchtRuecklage(gefunden = SCHEMA_VERSION_UNBEKANNT, erwartet = 6)
        )
    }

    @Test
    fun ruecklage_nimmtDieDatenbankUndIhreBegleitdateienMit() {
        val datenbank = sqliteDatei("schatztruhe_db", version = 5, inhalt = "Fortschritt")
        File(datenbank.parentFile, "schatztruhe_db-wal").writeText("noch nicht geschrieben")
        File(datenbank.parentFile, "schatztruhe_db-shm").writeText("gemeinsamer Speicher")
        val ziel = File(ordner.root, RUECKLAGE_ORDNER)

        legeRuecklageAn(datenbank, ziel)

        assertArrayEquals(datenbank.readBytes(), File(ziel, "schatztruhe_db").readBytes())
        assertEquals("noch nicht geschrieben", File(ziel, "schatztruhe_db-wal").readText())
        assertEquals("gemeinsamer Speicher", File(ziel, "schatztruhe_db-shm").readText())
    }

    /**
     * Es gibt immer **höchstens eine** Rücklage: Die nächste Schema-Änderung ersetzt sie. Sie
     * nach dem ersten erfolgreichen Öffnen zu löschen, machte sie wertlos – der Fehler, gegen
     * den sie gebaut ist, fällt erst Tage später auf.
     */
    @Test
    fun ruecklage_wirdBeiDerNaechstenAenderungErsetzt() {
        val datenbank = sqliteDatei("schatztruhe_db", version = 5, inhalt = "alter Stand")
        val ziel = File(ordner.root, RUECKLAGE_ORDNER)
        legeRuecklageAn(datenbank, ziel)

        datenbank.writeBytes(sqliteKopf(6) + "neuer Stand".toByteArray())
        legeRuecklageAn(datenbank, ziel)

        assertTrue(File(ziel, "schatztruhe_db").readText().endsWith("neuer Stand"))
        assertEquals(1, ziel.listFiles()!!.size)
    }

    /**
     * Der gefährlichste Fall beim Ersetzen: Die neue Datenbank hat keine Begleitdateien mehr,
     * die alte Rücklage aber schon. Bliebe die alte `-wal` liegen, gehörte sie zu einer anderen
     * Datenbank – die Rücklage wäre in sich widersprüchlich und beim Auslesen wertlos.
     */
    @Test
    fun ruecklage_laesstKeineAltenBegleitdateienStehen() {
        val datenbank = sqliteDatei("schatztruhe_db", version = 5)
        val wal = File(datenbank.parentFile, "schatztruhe_db-wal").apply { writeText("alt") }
        val ziel = File(ordner.root, RUECKLAGE_ORDNER)
        legeRuecklageAn(datenbank, ziel)
        assertTrue(File(ziel, "schatztruhe_db-wal").exists())

        wal.delete()
        legeRuecklageAn(datenbank, ziel)

        assertFalse(
            "Eine -wal aus der vorigen Rücklage darf nicht zur neuen Datenbank wandern",
            File(ziel, "schatztruhe_db-wal").exists()
        )
    }
}
