package io.github.glaside.zahlenschatzjagd.data

import io.github.glaside.zahlenschatzjagd.BADGE_CATEGORIES
import io.github.glaside.zahlenschatzjagd.BADGE_RANGES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die Schnittbildung der Freigabe (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`)
 * und ihre Speicherung als Sperre (`docs/adr/0018-freigabe-als-sperre-gespeichert.md`).
 */
class FreigabeTest {

    @Test
    fun ohneEinschraenkungIstJedeKennungInIhrenZahlenraeumenErreichbar() {
        BADGE_CATEGORIES.forEach { kategorie ->
            kategorie.ranges.forEach { zahlenraum ->
                assertTrue(
                    "${kategorie.key} bis $zahlenraum sollte erreichbar sein",
                    Freigabe.ALLES.erlaubt(kategorie.key, zahlenraum),
                )
            }
        }
    }

    @Test
    fun erreichbarIstNurWasInBeidenMengenVorkommt() {
        val freigabe = Freigabe(kennungen = setOf("PLUS"), zahlenraeume = setOf(10, 20))

        assertTrue(freigabe.erlaubt("PLUS", 10))
        assertFalse("Zahlenraum nicht freigegeben", freigabe.erlaubt("PLUS", 50))
        assertFalse("Kennung nicht freigegeben", freigabe.erlaubt("MINUS", 10))
    }

    /** `docs/adr/0026-zahlenraum-wirkt-nur-wo-gewaehlt-wird.md` */
    @Test
    fun ohneBis10BleibenVerdoppelnUndVerliebteZahlen_nurPlusBis10IstWeg() {
        val ohneBis10 = Freigabe(kennungen = ALLE_KENNUNGEN, zahlenraeume = setOf(20, 50, 100))

        assertTrue(ohneBis10.erlaubt("VERDOPPELN_LEICHT"))
        assertTrue(ohneBis10.erlaubt("VERDOPPELN_SCHWER"))
        assertTrue(ohneBis10.erlaubt("VERLIEBTE_ZAHLEN"))
        assertTrue(ohneBis10.erlaubt("VERLIEBTE_ZAHLEN", 10))
        assertFalse(ohneBis10.erlaubt("PLUS", 10))
    }

    @Test
    fun ohneBis50BleibtZahlenmauerMittel() {
        val ohneBis50 = Freigabe(kennungen = ALLE_KENNUNGEN, zahlenraeume = setOf(10, 20, 100))

        assertTrue(ohneBis50.erlaubt("ZAHLENMAUER_MITTEL"))
    }

    /** Nachbarzahlen hat zwei Zahlenräume (20 und 100), also keinen festen. */
    @Test
    fun nachbarzahlenFolgenWeiterDemZahlenraum() {
        val ohne20Und100 = Freigabe(kennungen = ALLE_KENNUNGEN, zahlenraeume = setOf(10, 50))

        assertFalse(ohne20Und100.erlaubt("NACHBARZAHLEN_LEICHT"))
        assertFalse(ohne20Und100.erlaubt("NACHBARZAHLEN_SCHWER"))
    }

    @Test
    fun einAbgewaehltesKaestchenGiltAuchBeiFestemZahlenraum() {
        val ohneVerdoppelnLeicht = Freigabe(
            kennungen = ALLE_KENNUNGEN - "VERDOPPELN_LEICHT",
            zahlenraeume = BADGE_RANGES.toSet(),
        )

        assertFalse(ohneVerdoppelnLeicht.erlaubt("VERDOPPELN_LEICHT"))
        assertFalse(ohneVerdoppelnLeicht.erlaubt("VERDOPPELN_LEICHT", 10))
    }

    /** Die gespeicherte Sperre bleibt, ändert aber still ihre Wirkung (ADR-0026, gewollt). */
    @Test
    fun eineGespeicherteSperreVonBis10GibtVerdoppelnUndVerliebteZahlenWiederFrei() {
        val freigabe = freigabeAusSperren(gesperrteKennungen = "", gesperrteZahlenraeume = "10")

        assertTrue(freigabe.erlaubt("VERDOPPELN_LEICHT"))
        assertTrue(freigabe.erlaubt("VERDOPPELN_SCHWER"))
        assertTrue(freigabe.erlaubt("VERLIEBTE_ZAHLEN"))
        assertFalse(freigabe.erlaubt("PLUS", 10))
    }

    @Test
    fun leereMengenLassenNichtsUebrig() {
        val nichts = Freigabe(kennungen = emptySet(), zahlenraeume = emptySet())

        assertTrue(ALLE_KENNUNGEN.none { nichts.erlaubt(it) })
        assertFalse(nichts.erlaubtEineVon(ALLE_KENNUNGEN.toList()))
    }

    @Test
    fun eineFreigegebeneKennungOhneFreigegebenenZahlenraumIstNichtErreichbar() {
        val ohneZahlenraum = Freigabe(kennungen = setOf("PLUS"), zahlenraeume = emptySet())

        assertFalse(ohneZahlenraum.erlaubt("PLUS"))
        assertEquals(emptyList<Int>(), ohneZahlenraum.erlaubteZahlenraeume("PLUS"))
    }

    @Test
    fun erlaubteZahlenraeumeSindDerSchnittMitDenenDerKennung() {
        val freigabe = Freigabe(kennungen = setOf("PLUS", "VERLIEBTE_ZAHLEN"), zahlenraeume = setOf(20, 100))

        assertEquals(listOf(20, 100), freigabe.erlaubteZahlenraeume("PLUS"))
        // „Verliebte Zahlen" gibt es nur bis 10 – fest, also unabhängig von den freigegebenen.
        assertEquals(listOf(10), freigabe.erlaubteZahlenraeume("VERLIEBTE_ZAHLEN"))
    }

    /**
     * Nachbarzahlen kennt genau zwei Zahlenräume – die Auswahl bietet also nur die 20 an, wenn ein
     * Erwachsener die 100 gesperrt hat, und die Übungsform bleibt dabei erreichbar.
     */
    @Test
    fun nachbarzahlenBietenBeideZahlenraeume_undOhneDie100NurNochDie20() {
        assertEquals(listOf(20, 100), Freigabe.ALLES.erlaubteZahlenraeume("NACHBARZAHLEN_SCHWER"))

        val ohne100 = Freigabe(kennungen = ALLE_KENNUNGEN, zahlenraeume = setOf(10, 20, 50))

        assertEquals(listOf(20), ohne100.erlaubteZahlenraeume("NACHBARZAHLEN_SCHWER"))
        assertTrue(ohne100.erlaubt("NACHBARZAHLEN_SCHWER"))
    }

    @Test
    fun einZahlenraumDenDieKennungGarNichtKenntBleibtGesperrt() {
        // „Verliebte Zahlen" gibt es nur bis 10 – auch bei großzügigster Freigabe.
        assertFalse(Freigabe.ALLES.erlaubt("VERLIEBTE_ZAHLEN", 100))
        assertTrue(Freigabe.ALLES.erlaubt("VERLIEBTE_ZAHLEN", 10))
    }

    @Test
    fun erlaubtNichtsErkenntDenLeerenZustand() {
        assertTrue(Freigabe(kennungen = emptySet(), zahlenraeume = emptySet()).erlaubtNichts())
        // Kennungen mit wählbarem Zahlenraum zählen ohne Zahlenraum ebenso wenig wie keine Kennungen.
        assertTrue(Freigabe(kennungen = RECHENARTEN, zahlenraeume = emptySet()).erlaubtNichts())
        // Ein angehaktes Verdoppeln bleibt dagegen auch ohne jeden Zahlenraum (ADR-0026).
        assertFalse(Freigabe(kennungen = setOf("VERDOPPELN_LEICHT"), zahlenraeume = emptySet()).erlaubtNichts())
        assertFalse(Freigabe.ALLES.erlaubtNichts())
        assertFalse(Freigabe(kennungen = setOf("PLUS"), zahlenraeume = setOf(10)).erlaubtNichts())
    }

    @Test
    fun eineUnbekannteKennungIstNichtErreichbar() {
        assertFalse(Freigabe.ALLES.erlaubt("GIBT_ES_NICHT"))
        assertFalse(Freigabe.ALLES.erlaubt("GIBT_ES_NICHT", 10))
    }

    @Test
    fun eineNeueKennungIstFuerBestehendeProfileFreigegeben() {
        // Ein Profil, dessen Sperre gesetzt wurde, als es „PLUS" noch gar nicht gab: Die Sperre
        // nennt nur, was ausdrücklich abgewählt wurde – alles Übrige bleibt freigegeben.
        val freigabe = freigabeAusSperren(gesperrteKennungen = "MINUS", gesperrteZahlenraeume = "")

        assertTrue(freigabe.erlaubt("PLUS", 10))
        assertFalse(freigabe.erlaubt("MINUS", 10))
    }

    @Test
    fun leereSperreBedeutetAllesFreigegeben() {
        assertEquals(Freigabe.ALLES, freigabeAusSperren("", ""))
    }

    @Test
    fun sperreUndFreigabeSindGegenlaeufig() {
        val freigabe = Freigabe(
            kennungen = ALLE_KENNUNGEN - "MINUS" - "MAL",
            zahlenraeume = BADGE_RANGES.toSet() - 100,
        )

        assertEquals(setOf("MINUS", "MAL"), freigabe.gesperrteKennungen())
        assertEquals(setOf(100), freigabe.gesperrteZahlenraeume())
        assertEquals(
            freigabe,
            freigabeAusSperren(
                encodeSperre(freigabe.gesperrteKennungen()),
                encodeSperre(freigabe.gesperrteZahlenraeume()),
            ),
        )
    }

    @Test
    fun allesAbgewaehltIstEinErreichbarerZustand() {
        val nichts = Freigabe(kennungen = emptySet(), zahlenraeume = emptySet())

        val zurueck = freigabeAusSperren(
            encodeSperre(nichts.gesperrteKennungen()),
            encodeSperre(nichts.gesperrteZahlenraeume()),
        )

        assertEquals(nichts, zurueck)
    }

    @Test
    fun eineEntfallendeKennungInDerSperreStoertNicht() {
        // Wird eine Kennung je entfernt, bleibt sie in der Sperre alter Profile stehen.
        val freigabe = freigabeAusSperren("GAB_ES_MAL,MINUS", "7,50")

        assertEquals(ALLE_KENNUNGEN - "MINUS", freigabe.kennungen)
        assertEquals(BADGE_RANGES.toSet() - 50, freigabe.zahlenraeume)
    }

    @Test
    fun erlaubtEineVonFindetDieEinzigeErlaubteKennung() {
        val nurZahlenmauerSchwer = Freigabe(kennungen = setOf("ZAHLENMAUER_SCHWER"), zahlenraeume = setOf(100))

        assertTrue(nurZahlenmauerSchwer.erlaubtEineVon(listOf("ZAHLENMAUER_LEICHT", "ZAHLENMAUER_SCHWER")))
        assertFalse(nurZahlenmauerSchwer.erlaubtEineVon(listOf("PLUS", "MINUS")))
        assertFalse(nurZahlenmauerSchwer.erlaubtEineVon(emptyList()))
    }
}

/** Die Kennungen der vier Grundrechenarten – alle mit wählbarem Zahlenraum. */
private val RECHENARTEN = setOf("PLUS", "MINUS", "MAL", "GETEILT")
