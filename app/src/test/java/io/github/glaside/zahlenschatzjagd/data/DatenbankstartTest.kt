package io.github.glaside.zahlenschatzjagd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * Der Ablauf beim Start: Rücklage anlegen, öffnen versuchen, und bei einem Fehlschlag in den
 * **Schutzhalt** gehen, statt die Daten wegzuwerfen
 * (`docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`).
 *
 * Das Öffnen kommt als Lambda herein. Dadurch ist der ganze Ablauf ohne Gerät prüfbar – dass
 * Room bei einem fehlenden Migrationsweg tatsächlich wirft, ist eine eigene Aussage und steht
 * in `SchutzhaltInstrumentedTest`.
 */
class DatenbankstartTest {

    @get:Rule
    val ordner = TemporaryFolder()

    private val erwartet = 6

    private fun datenbankMitVersion(version: Int): File =
        File(ordner.root, "schatztruhe_db").apply { writeBytes(sqliteKopf(version)) }

    private fun ruecklage() = File(ordner.root, RUECKLAGE_ORDNER)

    private fun kopie() = File(ruecklage(), "schatztruhe_db")

    private fun starte(datenbank: File, oeffne: () -> Unit) =
        starteDatenbank(datenbank, ruecklage(), erwartet, oeffne)

    @Test
    fun frischeInstallation_legtKeineRuecklageAn_undIstBereit() {
        val nochNichts = File(ordner.root, "schatztruhe_db")

        val start = starte(nochNichts) { /* Room legt die Datenbank gleich selbst an */ }

        assertEquals(Datenbankstart.Bereit, start)
        assertFalse("Es gibt nichts zu retten", kopie().exists())
    }

    @Test
    fun gleicheVersion_legtKeineRuecklageAn_undIstBereit() {
        val start = starte(datenbankMitVersion(erwartet)) { }

        assertEquals(Datenbankstart.Bereit, start)
        assertFalse("Ohne Schema-Änderung wird nicht kopiert", kopie().exists())
    }

    @Test
    fun aeltereVersion_legtDieRuecklageAn_bevorGeoeffnetWird() {
        var lagBeimOeffnenSchonVor = false

        val start = starte(datenbankMitVersion(5)) { lagBeimOeffnenSchonVor = kopie().exists() }

        assertEquals(Datenbankstart.Bereit, start)
        assertTrue(
            "Die Rücklage muss stehen, bevor die Migration läuft – danach ist es zu spät",
            lagBeimOeffnenSchonVor
        )
    }

    /**
     * Der Kern des Schutzhalts: kein Migrationsweg. Statt alles zu löschen, bleibt die Datei
     * liegen, und ein Erwachsener bekommt die Zahlen zu sehen.
     */
    @Test
    fun ohneMigrationsweg_gehtDieAppInDenSchutzhalt_undLaesstDieDatenLiegen() {
        val datenbank = datenbankMitVersion(2)
        val vorher = datenbank.readBytes()

        val start = starte(datenbank) { error("A migration from 2 to 6 was required but not found.") }

        val schutzhalt = start as Datenbankstart.Schutzhalt
        assertEquals(Schutzhaltgrund.WEG_FEHLT, schutzhalt.grund)
        assertEquals(2, schutzhalt.gefunden)
        assertEquals(erwartet, schutzhalt.erwartet)
        assertTrue(schutzhalt.ursache!!.contains("was required but not found"))
        assertArrayEqualsBytes(vorher, datenbank.readBytes())
        assertTrue("Die Rücklage entsteht auch hier", kopie().exists())
    }

    /**
     * Die gespeicherten Daten sind neuer als die App. Das ist der einzige Fall, den ein
     * Erwachsener selbst lösen kann – deshalb bekommt er einen eigenen Grund.
     */
    @Test
    fun neuereDaten_bekommenIhrenEigenenGrund() {
        val start = starte(datenbankMitVersion(9)) { error("Migration nicht gefunden") }

        assertEquals(Schutzhaltgrund.DATEN_NEUER, (start as Datenbankstart.Schutzhalt).grund)
        assertEquals(9, start.gefunden)
    }

    /**
     * Passende Version, und trotzdem geht es nicht auf – eine beschädigte Datei etwa. Auch dann
     * wird nichts weggeworfen, aber „es fehlt eine Migration" wäre gelogen.
     */
    @Test
    fun beschaedigteDatei_meldetWederFehlendenWegNochNeuereDaten() {
        val start = starte(datenbankMitVersion(erwartet)) { error("file is not a database") }

        assertEquals(Schutzhaltgrund.NICHT_LESBAR, (start as Datenbankstart.Schutzhalt).grund)
    }

    /**
     * Voller Speicher darf die App nicht anhalten: Ohne Rücklage geht nichts verloren, es fehlt
     * nur das Netz. Der Start läuft weiter.
     */
    @Test
    fun scheiterndeRuecklage_haeltDieAppNichtAn() {
        val datenbank = datenbankMitVersion(5)
        // Eine Datei dort, wo der Ordner der Rücklage hin soll: Das Anlegen muss scheitern.
        ruecklage().writeText("im Weg")

        val start = starte(datenbank) { }

        assertEquals(Datenbankstart.Bereit, start)
    }

    @Test
    fun grundFuer_haengtNurAnDenBeidenVersionen() {
        assertEquals(Schutzhaltgrund.WEG_FEHLT, grundFuer(gefunden = 3, erwartet = 6))
        assertEquals(Schutzhaltgrund.DATEN_NEUER, grundFuer(gefunden = 7, erwartet = 6))
        assertEquals(Schutzhaltgrund.NICHT_LESBAR, grundFuer(gefunden = 6, erwartet = 6))
        assertEquals(
            Schutzhaltgrund.NICHT_LESBAR,
            grundFuer(gefunden = SCHEMA_VERSION_UNBEKANNT, erwartet = 6)
        )
    }

    private fun assertArrayEqualsBytes(erwartet: ByteArray, tatsaechlich: ByteArray) {
        assertTrue("Die Datenbank darf nicht angefasst worden sein", erwartet.contentEquals(tatsaechlich))
    }

    @Test
    fun einDateifehlerBeimOeffnen_fuehrtEbenfallsInDenSchutzhalt() {
        val start = starte(datenbankMitVersion(5)) { throw IOException("Speicher voll") }

        assertTrue(start is Datenbankstart.Schutzhalt)
    }
}
