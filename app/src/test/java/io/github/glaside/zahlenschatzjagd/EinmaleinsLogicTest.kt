package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Die Aufgaben des Einmaleins und ihr Vorrat (Regeln: `LEHRPLAN.md`, Abschnitt „Einmaleins").
 */
class EinmaleinsLogicTest {

    /**
     * Die Kennungen sind persistiert und gegen **von Hand geschriebene** Werte geprüft, nicht
     * reihum (`docs/agents/testing.md`): Eine Umbenennung von Stufe oder Schema fiele sonst nicht auf.
     */
    @Test
    fun kennungen_folgenDemEingefrorenenSchema() {
        assertEquals("EINMALEINS_2_AUFSAGEN", einmaleinsKennung(2, EinmaleinsSchwierigkeitsstufe.AUFSAGEN))
        assertEquals("EINMALEINS_7_AUFSAGEN", einmaleinsKennung(7, EinmaleinsSchwierigkeitsstufe.AUFSAGEN))
        assertEquals(
            "EINMALEINS_10_DURCHEINANDER",
            einmaleinsKennung(10, EinmaleinsSchwierigkeitsstufe.DURCHEINANDER),
        )
    }

    @Test
    fun waehlbarSindDie2erBis10erReihe() {
        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8, 9, 10), EINMALEINS_REIHEN)
    }

    @Test
    fun eineReihe_gehtVonEinmalBisZehnmal_ohneNull_unddieReiheStehtHinten() {
        val aufgaben = aufgabenDerReihe(7)

        assertEquals((1..10).toList(), aufgaben.map { it.faktor })
        assertTrue(aufgaben.all { it.reihe == 7 })
        assertEquals(listOf(7, 14, 21, 28, 35, 42, 49, 56, 63, 70), aufgaben.map { it.ergebnis })
        assertEquals("8 · 7 = _", aufgaben[7].text())
    }

    @Test
    fun alleAufgabenBleibenImZahlenraum100() {
        val groesste = EINMALEINS_REIHEN.flatMap { aufgabenDerReihe(it) }.maxOf { it.ergebnis }
        assertEquals(EINMALEINS_ZAHLENRAUM, groesste)
    }

    /** Wortlaut abgenommen (#9). */
    @Test
    fun dieAnsageNenntDieReihe() {
        assertEquals("Jetzt die 7er-Reihe!", reihenAnsage(7))
        assertEquals("Jetzt die 10er-Reihe!", reihenAnsage(10))
    }

    @Test
    fun aufsagen_nimmtJedeReiheEinmal_aufsteigend() {
        assertEquals(listOf(3, 7, 9), aufsageReihenfolge(listOf(9, 3, 7, 3)))
    }

    @Test
    fun vorrat_ziehtOhneZuruecklegen_bisAlleAufgabenDerReihenDranWaren() {
        val vorrat = Einmaleinsvorrat(listOf(3, 7), Random(1))

        val ersteFuellung = List(20) { vorrat.ziehe() }

        assertEquals(
            (aufgabenDerReihe(3) + aufgabenDerReihe(7)).toSet(),
            ersteFuellung.toSet(),
        )
    }

    /** Auch über das Neumischen hinweg kommt keine Aufgabe zweimal direkt hintereinander. */
    @Test
    fun vorrat_wiederholtNieDieselbeAufgabeDirektHintereinander() {
        repeat(200) { seed ->
            val vorrat = Einmaleinsvorrat(listOf(4), Random(seed))
            val gezogen = List(60) { vorrat.ziehe() }
            gezogen.zipWithNext().forEach { (a, b) -> assertNotEquals("Seed $seed", a, b) }
        }
    }

    /** `1 · n` wird nicht seltener gezogen – sie gehört zur Reihe (`LEHRPLAN.md`). */
    @Test
    fun vorrat_ziehtEinmalNSoOftWieJedeAndereAufgabe() {
        val vorrat = Einmaleinsvorrat(listOf(6), Random(3))

        val gezogen = List(50) { vorrat.ziehe() }

        assertEquals(5, gezogen.count { it.faktor == 1 })
        assertEquals(5, gezogen.count { it.faktor == 10 })
    }
}
