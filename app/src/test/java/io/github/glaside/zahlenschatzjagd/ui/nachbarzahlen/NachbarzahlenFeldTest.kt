package io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen

import io.github.glaside.zahlenschatzjagd.Nachbarposition
import io.github.glaside.zahlenschatzjagd.Nachbarzahlenaufgabe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Die Regeln, nach denen ein einzelnes Feld der Reihe beschriftet wird – Compose-frei und deshalb
 * ohne Gerät prüfbar. Das Zeichnen selbst deckt der instrumentierte Flow-Test ab.
 */
class NachbarzahlenFeldTest {

    private val aufgabe = Nachbarzahlenaufgabe(mitte = 7, vorgegeben = Nachbarposition.MITTE)

    @Test
    fun ueberDenLueckenStehenPfeileUndKeineBuchstaben() {
        val links = feldPfeil(Feldzustand.OFFEN, Nachbarposition.LINKS, Nachbarposition.MITTE)
        val rechts = feldPfeil(Feldzustand.OFFEN, Nachbarposition.RECHTS, Nachbarposition.MITTE)

        assertEquals("←", links)
        assertEquals("→", rechts)
    }

    @Test
    fun ueberDerVorgegebenenZahlStehtKeinPfeil() {
        assertNull(feldPfeil(Feldzustand.VORGEGEBEN, Nachbarposition.MITTE, Nachbarposition.MITTE))
    }

    /**
     * Der Pfeil zeigt von der vorgegebenen Zahl weg. Für die schwere Stufe heißt das: Ist die linke
     * Zahl vorgegeben, zeigen **beide** Pfeile nach rechts.
     */
    @Test
    fun beiVorgegebenerLinkerZahlZeigenBeidePfeileNachRechts() {
        assertEquals("→", feldPfeil(Feldzustand.OFFEN, Nachbarposition.MITTE, Nachbarposition.LINKS))
        assertEquals("→", feldPfeil(Feldzustand.OFFEN, Nachbarposition.RECHTS, Nachbarposition.LINKS))
    }

    @Test
    fun eineLeereLueckeZeigtEinFragezeichen() {
        val leer = Lueckeneingabe()
        val zustand = feldzustand(leer, geprueft = false)

        assertEquals(Feldzustand.OFFEN, zustand)
        assertEquals("?", feldText(zustand, aufgabe, Nachbarposition.LINKS, leer))
    }

    @Test
    fun dieVorgegebeneZahlStehtImmerImKlartext() {
        val zustand = feldzustand(eingabe = null, geprueft = false)

        assertEquals(Feldzustand.VORGEGEBEN, zustand)
        assertEquals("7", feldText(zustand, aufgabe, Nachbarposition.MITTE, null))
    }

    /** Nach dem Prüfen bleibt die getippte Zahl stehen – ein Vertipper darf nicht spurlos verschwinden. */
    @Test
    fun nachDemPruefenStehtDieTatsaechlicheEingabeDa_nichtDieLoesung() {
        val eingabe = Lueckeneingabe(eingabe = "9", istRichtig = false)
        val zustand = feldzustand(eingabe, geprueft = true)

        assertEquals(Feldzustand.FALSCH, zustand)
        assertEquals("9", feldText(zustand, aufgabe, Nachbarposition.LINKS, eingabe))
    }
}
