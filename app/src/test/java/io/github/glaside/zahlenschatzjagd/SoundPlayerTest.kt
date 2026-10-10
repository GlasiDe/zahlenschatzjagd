package io.github.glaside.zahlenschatzjagd

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testet die reine Datenschicht der Sound-Zuordnung ([GameSound]) und den Schalter, der die
 * Töne stummstellt. Das eigentliche Abspielen ([SoundPlayer.play]) hängt am Android-MediaPlayer
 * und wird hier nicht geprüft – wohl aber die Entscheidung davor, durch die es geht.
 */
class SoundPlayerTest {

    /** Der [SoundPlayer] ist prozessweit; ohne das Zurückstellen liefe der Schalter mit. */
    @After
    fun tonWiederAnschalten() {
        SoundPlayer.setzeTonAn(true)
    }

    @Test
    fun enthaeltGenauDieErwartetenSounds() {
        val namen = GameSound.entries.map { it.name }
        assertEquals(listOf("CORRECT", "WRONG", "FINISH"), namen)
    }

    @Test
    fun jederSoundHatEineEindeutigeGueltigeResId() {
        val ids = GameSound.entries.map { it.resId }
        assertTrue("Eine resId darf nicht 0 sein", ids.all { it != 0 })
        assertEquals("Die resIds müssen eindeutig sein", ids.size, ids.distinct().size)
    }

    @Test
    fun beiEingeschaltetemTonKommtDerKlangDesEreignisses() {
        SoundPlayer.setzeTonAn(true)

        for (sound in GameSound.entries) {
            assertEquals(sound.resId, SoundPlayer.zuSpielendeRessource(sound))
        }
    }

    /** Kein Klang bleibt übrig: Der Schalter gilt für alle drei, nicht nur für die Runde. */
    @Test
    fun beiAusgeschaltetemTonBleibtJederKlangStumm() {
        SoundPlayer.setzeTonAn(false)

        for (sound in GameSound.entries) {
            assertNull("$sound klingt trotz ausgeschaltetem Ton", SoundPlayer.zuSpielendeRessource(sound))
        }
    }
}
