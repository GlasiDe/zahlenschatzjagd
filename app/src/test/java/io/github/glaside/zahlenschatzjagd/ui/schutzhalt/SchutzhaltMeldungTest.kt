package io.github.glaside.zahlenschatzjagd.ui.schutzhalt

import io.github.glaside.zahlenschatzjagd.data.Datenbankstart
import io.github.glaside.zahlenschatzjagd.data.Schutzhaltgrund
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Texte des **Schutzhalts**. Sie sind das Einzige, was ein Erwachsener in diesem Zustand zu
 * sehen bekommt – auf dem eigenen Gerät wie auf dem eines Freundes, wo niemand `adb` hat.
 *
 * Getrennt vom Bildschirm geprüft, wie bei `BackupMessage`: Was gesagt wird, ist eine
 * Entscheidung; wie es aussieht, ist eine andere.
 */
class SchutzhaltMeldungTest {

    private companion object {
        const val MELDESTELLE = "github.com/GlasiDe/zahlenschatzjagd/issues"
    }

    private fun meldung(
        grund: Schutzhaltgrund,
        gefunden: Int = 3,
        erwartet: Int = 6,
        ursache: String? = null,
    ) = schutzhaltMeldung(
        Datenbankstart.Schutzhalt(grund, gefunden, erwartet, ursache),
        appVersion = "1.7",
    )

    /**
     * Der gesprochene Satz gilt für alle Ursachen: Er richtet sich an ein Kind, das nicht liest
     * und dem der Unterschied zwischen den Ursachen nichts sagt.
     */
    @Test
    fun dasKind_hoertImmerDenselbenSatz_undErSchicktEsZuEinemErwachsenen() {
        val saetze = Schutzhaltgrund.entries.map { meldung(it).gesprochen }

        assertEquals(1, saetze.distinct().size)
        assertTrue(saetze.first().contains("Erwachsenen"))
    }

    /**
     * Die wichtigste Zusage überhaupt – und der Grund, warum es den Schutzhalt gibt: Es ist
     * nichts verloren. Sie muss in **jeder** Ursache stehen.
     */
    @Test
    fun jedeMeldung_sagtDassNichtsVerlorenIst() {
        Schutzhaltgrund.entries.forEach { grund ->
            val text = meldung(grund).erklaerung
            assertTrue(
                "Die Meldung zu $grund muss beruhigen, sonst löscht jemand aus Panik die App",
                text.contains("nichts verändert") || text.contains("unberührt")
            )
        }
    }

    /**
     * Der einzige Fall, den ein Erwachsener selbst lösen kann. Er bekommt deshalb eine
     * Handlungsanweisung statt einer Bitte, sich zu melden.
     */
    @Test
    fun neuereDaten_schickenNichtZurMeldestelle_sondernZurNeuerenApp() {
        val handlung = meldung(Schutzhaltgrund.DATEN_NEUER, gefunden = 9, erwartet = 6).handlung

        assertTrue(handlung.contains("neuere"))
        assertTrue("Hier hilft keine Meldung", !handlung.contains(MELDESTELLE))
    }

    /** Die Meldung nennt die App beim Namen, unter dem sie auf dem Gerät steht. */
    @Test
    fun neuereDaten_nennenDieAppBeimNamen() {
        val text = meldung(Schutzhaltgrund.DATEN_NEUER, gefunden = 9, erwartet = 6).erklaerung

        assertTrue(text.contains("Zahlenschatzjagd"))
    }

    /**
     * Nicht jede Familie kennt den Entwickler persönlich.
     * Wer die App nicht lesen kann, bekommt deshalb eine Adresse, die jeder erreicht – und die
     * Bitte um ein Bildschirmfoto statt ums Vorlesen am Telefon.
     */
    @Test
    fun fehlenderWegUndUnlesbareDaten_nennenDieMeldestelle() {
        listOf(Schutzhaltgrund.WEG_FEHLT, Schutzhaltgrund.NICHT_LESBAR).forEach { grund ->
            val handlung = meldung(grund, gefunden = 3).handlung

            assertTrue("$grund: $handlung", handlung.contains(MELDESTELLE))
            assertTrue("$grund: $handlung", handlung.contains("Bildschirmfoto"))
            assertTrue("$grund setzt noch voraus, den Entwickler zu kennen", !handlung.contains("Entwickler"))
            assertTrue("$grund verlangt noch Vorlesen am Telefon", !handlung.contains("lies"))
        }
    }

    /** Die unlesbare Datei darf auch nach der Meldung niemand löschen. */
    @Test
    fun unlesbareDaten_wehrenDemLoeschen() {
        val handlung = meldung(Schutzhaltgrund.NICHT_LESBAR, gefunden = 0).handlung

        assertTrue(handlung.contains("nicht löschen"))
    }

    /** Die drei Zahlen sind das, was eine Meldung zum Nachbauen des Falls braucht. */
    @Test
    fun dieZahlen_nennenGefundenErwartetUndAppVersion() {
        val zahlen = meldung(Schutzhaltgrund.WEG_FEHLT, gefunden = 3, erwartet = 6).zahlen

        assertTrue(zahlen.contains("3"))
        assertTrue(zahlen.contains("6"))
        assertTrue(zahlen.contains("1.7"))
    }

    /** Eine unlesbare Datei hat keine Version – „0" wäre eine Zahl, die es so nicht gibt. */
    @Test
    fun eineUnbekannteVersion_wirdAlsSolcheGenannt_nichtAlsNull() {
        val zahlen = meldung(Schutzhaltgrund.NICHT_LESBAR, gefunden = 0).zahlen

        assertTrue(zahlen.contains("unbekannt"))
        assertTrue("Eine 0 wäre eine erfundene Schema-Version", !zahlen.contains(" 0"))
    }

    /**
     * Die technische Meldung aus Room ist englisch und für Eltern nicht lesbar. Bei den beiden
     * Versions-Fällen sagt sie ohnehin nichts Neues – „A migration from 3 to 6" ist wörtlich
     * „Gefunden: 3 · Erwartet: 6". Sie bleibt deshalb dem einen Fall vorbehalten, in dem sie der
     * einzige Hinweis ist, was kaputt ist.
     */
    @Test
    fun dieEnglischeMeldung_erscheintNurWennSieEtwasBeitraegt() {
        val technisch = "A migration from 3 to 6 was required but not found."

        assertEquals(null, meldung(Schutzhaltgrund.WEG_FEHLT, ursache = technisch).ursache)
        assertEquals(null, meldung(Schutzhaltgrund.DATEN_NEUER, ursache = technisch).ursache)
        assertEquals(
            "file is not a database",
            meldung(Schutzhaltgrund.NICHT_LESBAR, ursache = "file is not a database").ursache
        )
    }
}
