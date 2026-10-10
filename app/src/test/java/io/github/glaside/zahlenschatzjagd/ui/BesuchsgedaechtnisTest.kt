package io.github.glaside.zahlenschatzjagd.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Startkarte und Menüs begrüßen bei jedem Besuch, nach dem Drehen aber nicht noch einmal.
 * Ein Besuch beginnt, wenn zuvor ein anderer Eintrag des Verlaufs sichtbar war.
 */
class BesuchsgedaechtnisTest {

    @Test
    fun derErsteSichtbareEintrag_wirdBegruesst_undNurEinmal() {
        val gedaechtnis = Besuchsgedaechtnis()
        gedaechtnis.wirdSichtbar("startkarte")

        assertTrue(gedaechtnis.begruessungFaellig("startkarte"))
        assertFalse(gedaechtnis.begruessungFaellig("startkarte"))
    }

    /** Nach dem Drehen meldet die Navigation denselben Eintrag noch einmal. */
    @Test
    fun derselbeEintragErneutGemeldet_istKeinNeuerBesuch() {
        val gedaechtnis = Besuchsgedaechtnis()
        gedaechtnis.wirdSichtbar("startkarte")
        gedaechtnis.begruessungFaellig("startkarte")

        gedaechtnis.wirdSichtbar("startkarte")

        assertFalse(gedaechtnis.begruessungFaellig("startkarte"))
    }

    @Test
    fun zurueckVonEinemAnderenBildschirm_istEinNeuerBesuch() {
        val gedaechtnis = Besuchsgedaechtnis()
        gedaechtnis.wirdSichtbar("startkarte")
        gedaechtnis.begruessungFaellig("startkarte")

        gedaechtnis.wirdSichtbar("rechnen")
        gedaechtnis.wirdSichtbar("startkarte")

        assertTrue(gedaechtnis.begruessungFaellig("startkarte"))
    }

    /** Ein Bildschirm, der gerade verlassen wird, begrüßt nicht mehr. */
    @Test
    fun einNichtSichtbarerEintrag_wirdNichtBegruesst() {
        val gedaechtnis = Besuchsgedaechtnis()
        gedaechtnis.wirdSichtbar("rechnen")

        assertFalse(gedaechtnis.begruessungFaellig("startkarte"))
    }

    /** So kommt es nach dem Drehen aus dem gespeicherten Zustand zurück. */
    @Test
    fun wiederhergestellt_bleibtEsBeiDerSchonErfolgtenBegruessung() {
        val gedaechtnis = Besuchsgedaechtnis(sichtbar = "startkarte", begruesst = true)

        gedaechtnis.wirdSichtbar("startkarte")

        assertFalse(gedaechtnis.begruessungFaellig("startkarte"))
    }
}
