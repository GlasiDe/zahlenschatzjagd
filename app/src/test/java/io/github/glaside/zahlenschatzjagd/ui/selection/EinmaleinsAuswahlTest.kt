package io.github.glaside.zahlenschatzjagd.ui.selection

import io.github.glaside.zahlenschatzjagd.EINMALEINS_REIHEN
import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe.AUFSAGEN
import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe.DURCHEINANDER
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import org.junit.Assert.assertEquals
import org.junit.Test

class EinmaleinsAuswahlTest {

    @Test
    fun ohneEinschraenkung_stehenBeideStufenUndAlleReihenDa_AufsagenZuerst() {
        assertEquals(listOf(AUFSAGEN, DURCHEINANDER), einmaleinsStufen(Freigabe.ALLES))
        assertEquals(EINMALEINS_REIHEN, einmaleinsReihen(Freigabe.ALLES, DURCHEINANDER))
    }

    @Test
    fun gesperrteReihenErscheinenNicht_jeStufeGetrennt() {
        val freigabe = Freigabe(
            kennungen = Freigabe.ALLES.kennungen - "EINMALEINS_9_AUFSAGEN" - "EINMALEINS_2_DURCHEINANDER",
            zahlenraeume = Freigabe.ALLES.zahlenraeume,
        )

        assertEquals(listOf(2, 3, 4, 5, 6, 7, 8, 10), einmaleinsReihen(freigabe, AUFSAGEN))
        assertEquals(listOf(3, 4, 5, 6, 7, 8, 9, 10), einmaleinsReihen(freigabe, DURCHEINANDER))
    }

    @Test
    fun eineStufeOhneFreigegebeneReihe_entfaellt() {
        val nurDurcheinander = Freigabe(
            kennungen = setOf("EINMALEINS_3_DURCHEINANDER", "EINMALEINS_4_DURCHEINANDER"),
            zahlenraeume = emptySet(),
        )

        assertEquals(listOf(DURCHEINANDER), einmaleinsStufen(nurDurcheinander))
        assertEquals(listOf(3, 4), einmaleinsReihen(nurDurcheinander, DURCHEINANDER))
    }

    /** Der Zahlenraum der Freigabe wirkt nicht aufs Einmaleins – sein Zahlenraum ist fest (ADR-0026). */
    @Test
    fun abgewaehlteZahlenraeume_sperrenKeineReihe() {
        val ohneZahlenraum = Freigabe(kennungen = Freigabe.ALLES.kennungen, zahlenraeume = emptySet())

        assertEquals(EINMALEINS_REIHEN, einmaleinsReihen(ohneZahlenraum, AUFSAGEN))
    }

    @Test
    fun beimStufenwechsel_bleibenNurHakenDieDortFreigegebenSind_undNichtsKommtDazu() {
        assertEquals(setOf(3, 7), reihenNachStufenwechsel(setOf(3, 7, 9), listOf(2, 3, 7)))
        assertEquals(emptySet<Int>(), reihenNachStufenwechsel(emptySet(), listOf(5)))
    }
}
