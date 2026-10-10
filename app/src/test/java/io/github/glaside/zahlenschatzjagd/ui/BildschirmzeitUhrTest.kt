package io.github.glaside.zahlenschatzjagd.ui

import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Prüft die Messung der **Bildschirmzeit** (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`).
 *
 * Die Uhr ist bewusst frei von Android: Jeder Zeitpunkt kommt als Parameter herein, deshalb
 * braucht kein einziger dieser Fälle ein Gerät oder eine wartende Testzeit.
 */
class BildschirmzeitUhrTest {

    private val pause = BildschirmzeitUhr.PAUSE_MILLIS

    private fun uhr() = BildschirmzeitUhr()

    // --- Der Normalfall ---

    @Test
    fun ohneProfil_wirdNichtsGemessen() {
        val uhr = uhr()

        assertNull(uhr.beobachte(profil = null, jetzt = 0))
        assertNull(uhr.tipp(jetzt = 1_000))
        assertNull("Zeit ohne aktives Profil wird verworfen", uhr.anhalten(jetzt = 5_000))
    }

    @Test
    fun tippenUndVerlassen_ergibtEinenAbschnitt() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 1_000)

        assertNull("Ein Tipp beendet nichts", uhr.tipp(jetzt = 20_000))
        val abschnitt = uhr.anhalten(jetzt = 30_000)

        assertEquals(Zeitabschnitt(profil = "Mira", beginn = 1_000, ende = 30_000), abschnitt)
    }

    @Test
    fun einAbschnittOhneDauer_wirdVerworfen() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 1_000)

        assertNull(uhr.anhalten(jetzt = 1_000))
    }

    @Test
    fun nachDemAnhalten_beginntDieMessungWiederVonVorn() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)
        uhr.anhalten(jetzt = 10_000)

        uhr.beobachte("Mira", jetzt = 100_000)
        val zweiter = uhr.anhalten(jetzt = 110_000)

        assertEquals(Zeitabschnitt(profil = "Mira", beginn = 100_000, ende = 110_000), zweiter)
    }

    // --- Der Inaktivitäts-Stopp ---

    @Test
    fun zweiMinutenOhneEingabe_beendenDenAbschnittRueckwirkendZumLetztenTipp() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)
        uhr.tipp(jetzt = 10_000)

        // Das Tablet liegt eine halbe Stunde auf dem Sofa, dann wird die App verlassen.
        val abschnitt = uhr.anhalten(jetzt = 10_000 + 30 * 60_000)

        assertEquals(
            "Das Ende gehört zum letzten Tipp plus Pause, nicht zum Verlassen",
            Zeitabschnitt(profil = "Mira", beginn = 0, ende = 10_000 + pause),
            abschnitt,
        )
    }

    @Test
    fun einTippNachLangerPause_schliesstDenAltenAbschnittUndBeginntEinenNeuen() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)
        uhr.tipp(jetzt = 10_000)

        val alter = uhr.tipp(jetzt = 10_000 + 10 * 60_000)

        assertEquals(Zeitabschnitt(profil = "Mira", beginn = 0, ende = 10_000 + pause), alter)
        assertEquals(
            "Der neue Abschnitt beginnt beim Tipp, nicht beim Ende des alten",
            Zeitabschnitt(profil = "Mira", beginn = 10_000 + 10 * 60_000, ende = 10_000 + 11 * 60_000),
            uhr.anhalten(jetzt = 10_000 + 11 * 60_000),
        )
    }

    @Test
    fun einTippGenauNachZweiMinuten_faelltSchonInDenNeuenAbschnitt() {
        // „Ende nach zwei Minuten ohne Eingabe": Ist die Pause voll, ist der Abschnitt zu Ende –
        // die volle Pause zählt nicht mehr als „noch dabei".
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)

        val alter = uhr.tipp(jetzt = pause)

        assertEquals(Zeitabschnitt(profil = "Mira", beginn = 0, ende = pause), alter)
        assertEquals(
            Zeitabschnitt(profil = "Mira", beginn = pause, ende = pause + 1_000),
            uhr.anhalten(jetzt = pause + 1_000),
        )
    }

    @Test
    fun einTippKurzVorAblaufDerPause_haeltDenAbschnittAmLeben() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)
        uhr.tipp(jetzt = pause - 1)

        assertNull(uhr.tipp(jetzt = 2 * pause - 2))
        assertEquals(
            Zeitabschnitt(profil = "Mira", beginn = 0, ende = 2 * pause - 2),
            uhr.anhalten(jetzt = 2 * pause - 2),
        )
    }

    @Test
    fun ohneJedenTipp_zaehltHoechstensDiePause() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)

        // Die App wird geöffnet und liegen gelassen: Der Beginn selbst gilt als Aktivität.
        assertEquals(
            Zeitabschnitt(profil = "Mira", beginn = 0, ende = pause),
            uhr.anhalten(jetzt = 60 * 60_000),
        )
    }

    // --- Der Schnitt beim Profilwechsel ---

    @Test
    fun profilwechsel_schneidetDenAbschnittDurch() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)
        uhr.tipp(jetzt = 30_000)

        val miras = uhr.beobachte("Chris", jetzt = 40_000)

        assertEquals(
            "Die Zeit gehört dem Profil, das sie verbraucht hat",
            Zeitabschnitt(profil = "Mira", beginn = 0, ende = 40_000),
            miras,
        )
        assertEquals(
            Zeitabschnitt(profil = "Chris", beginn = 40_000, ende = 50_000),
            uhr.anhalten(jetzt = 50_000),
        )
    }

    @Test
    fun dasselbeProfilErneutBeobachtet_schneidetNicht() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)

        assertNull(uhr.beobachte("Mira", jetzt = 5_000))
        assertEquals(
            Zeitabschnitt(profil = "Mira", beginn = 0, ende = 10_000),
            uhr.anhalten(jetzt = 10_000),
        )
    }

    // --- Der Elternbereich ---

    @Test
    fun elternbereich_beendetDenAbschnittUndSeineZeitZaehltNicht() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)
        uhr.tipp(jetzt = 20_000)

        // Der Elternbereich meldet sich als „kein zu messendes Profil".
        val miras = uhr.beobachte(profil = null, jetzt = 30_000)
        uhr.tipp(jetzt = 60_000)
        val ausDemElternbereich = uhr.beobachte("Mira", jetzt = 90_000)

        assertEquals(Zeitabschnitt(profil = "Mira", beginn = 0, ende = 30_000), miras)
        assertNull("Zeit im Elternbereich wird verworfen", ausDemElternbereich)
        assertEquals(
            "Nach dem Elternbereich beginnt die Messung neu",
            Zeitabschnitt(profil = "Mira", beginn = 90_000, ende = 95_000),
            uhr.anhalten(jetzt = 95_000),
        )
    }

    // --- Robustheit ---

    @Test
    fun eineRueckwaertsLaufendeUhr_erzeugtKeinenAbschnitt() {
        // Zeitumstellung oder von Hand gestellte Uhr: lieber nichts messen als Unsinn speichern.
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 100_000)

        assertNull(uhr.anhalten(jetzt = 90_000))
    }

    @Test
    fun anhaltenOhneLaufendeMessung_aenderteNichts() {
        val uhr = uhr()
        uhr.beobachte("Mira", jetzt = 0)
        uhr.anhalten(jetzt = 10_000)

        assertNull(uhr.anhalten(jetzt = 20_000))
    }
}
