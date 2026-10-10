package io.github.glaside.zahlenschatzjagd.ui

import io.github.glaside.zahlenschatzjagd.Nachbarposition
import io.github.glaside.zahlenschatzjagd.Nachbarzahlenaufgabe
import io.github.glaside.zahlenschatzjagd.ansage
import io.github.glaside.zahlenschatzjagd.ui.home.HOME_ISLANDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die reine Text-Zuordnung der Vorlesefunktion. Das eigentliche Sprechen ([Narrator])
 * hängt am Android-TextToSpeech und wird hier nicht getestet.
 */
class NarratorTextTest {

    @Test
    fun begruessungIstNichtLeer() {
        assertTrue(HOME_WELCOME_SPOKEN.isNotBlank())
    }

    /**
     * Die Startkarte hat keine Sprechblase: Nur dieser Satz sagt dem Kind, was es tun soll. Er
     * bleibt deshalb wörtlich, wie er ist.
     */
    @Test
    fun begruessung_fordertZumTippenAufEineInselAuf() {
        assertEquals("Willkommen! Tippe auf eine Insel.", HOME_WELCOME_SPOKEN)
    }

    @Test
    fun jedeInselHatEinenVorlesbarenNamen() {
        assertTrue(HOME_ISLANDS.all { it.spoken.isNotBlank() })
    }

    /** Der Satz der leichten Nachbarzahlen-Stufe – die Zahl wird eingesetzt. */
    @Test
    fun nachbarzahlen_beiVorgegebenerMitte_fragtNachDavorUndDanach() {
        val aufgabe = Nachbarzahlenaufgabe(mitte = 7, vorgegeben = Nachbarposition.MITTE)

        assertEquals(
            "Hier steht die 7. Welche Zahl kommt davor? Und welche kommt danach?",
            aufgabe.ansage(),
        )
    }

    /**
     * Die beiden Sätze für eine vorgegebene äußere Zahl. Sie werden erst mit der schweren Stufe
     * erreichbar; die Zuordnung steht hier trotzdem fest, weil die vorgegebene Stelle ein Feld der
     * Aufgabe ist und der Satz sonst unbemerkt zur falschen Position gehören könnte.
     */
    @Test
    fun nachbarzahlen_beiVorgegebenerAeussererZahl_fragtNurInEineRichtung() {
        assertEquals(
            "Hier steht die 9. Welche zwei Zahlen kommen danach?",
            Nachbarzahlenaufgabe(mitte = 10, vorgegeben = Nachbarposition.LINKS).ansage(),
        )
        assertEquals(
            "Hier steht die 20. Welche zwei Zahlen kommen davor?",
            Nachbarzahlenaufgabe(mitte = 19, vorgegeben = Nachbarposition.RECHTS).ansage(),
        )
    }
}
