package io.github.glaside.zahlenschatzjagd.ui

import android.speech.tts.TextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sichert die reine Entscheidungslogik des [Narrator] ab – also genau die beiden Stellen,
 * an denen die früheren Bugs saßen (abgeschnittene / fehlende Sprachausgabe), und seit dem
 * Schalter im Elternbereich auch, dass eine ausgeschaltete Vorlesefunktion wirklich schweigt.
 * Das eigentliche `TextToSpeech` (Android-Framework) wird hier nicht instanziiert.
 */
class NarratorLogicTest {

    @Test
    fun leererTextWirdIgnoriert() {
        assertEquals(
            SpeakAction.DROP,
            decideSpeak("", ttsCreated = false, ready = false, vorlesenAn = true)
        )
        assertEquals(
            SpeakAction.DROP,
            decideSpeak("   ", ttsCreated = true, ready = true, vorlesenAn = true)
        )
    }

    @Test
    fun ohneEngineWirdInitGestartetUndTextGemerkt() {
        assertEquals(
            SpeakAction.START_INIT_AND_QUEUE,
            decideSpeak("Hallo", ttsCreated = false, ready = false, vorlesenAn = true)
        )
    }

    @Test
    fun waehrendDerInitialisierungWirdDerTextGemerktStattVerworfen() {
        // Das ist der Race-Fix: früher wurde die Begrüßung verworfen, jetzt wird sie
        // vorgemerkt und nach abgeschlossener Initialisierung nachgeholt.
        assertEquals(
            SpeakAction.QUEUE_PENDING,
            decideSpeak("Willkommen", ttsCreated = true, ready = false, vorlesenAn = true)
        )
    }

    @Test
    fun beiBereiterEngineWirdSofortGesprochen() {
        assertEquals(
            SpeakAction.SPEAK_NOW,
            decideSpeak("Rechnen üben", ttsCreated = true, ready = true, vorlesenAn = true)
        )
    }

    @Test
    fun engineGiltNachErfolgreichemInitAlsBereit() {
        assertTrue(shouldMarkReady(TextToSpeech.SUCCESS))
    }

    @Test
    fun ohneErfolgreichenInitBleibtDieEngineStumm() {
        assertFalse(shouldMarkReady(TextToSpeech.ERROR))
    }

    @Test
    fun ausgeschalteteVorlesefunktionSchweigtInJedemZustand() {
        assertEquals(
            SpeakAction.DROP,
            decideSpeak("Willkommen", ttsCreated = true, ready = true, vorlesenAn = false)
        )
        assertEquals(
            SpeakAction.DROP,
            decideSpeak("Willkommen", ttsCreated = false, ready = false, vorlesenAn = false)
        )
    }

    /**
     * Der Schalter kommt aus dem DataStore und damit **nach** dem ersten Frame – die Begrüßung
     * der Startkarte ist da längst losgeschickt. Würde sie in dieser Lücke gesprochen, wäre der
     * Schalter bei jedem App-Start genau einmal wirkungslos. Sie wird deshalb vorgemerkt wie ein
     * Text während der Initialisierung und erst nachgeholt, wenn der Schalter „an" sagt.
     */
    @Test
    fun vorDerAntwortDesSchaltersWirdVorgemerktStattGesprochen() {
        assertEquals(
            SpeakAction.QUEUE_PENDING,
            decideSpeak("Willkommen", ttsCreated = true, ready = true, vorlesenAn = null)
        )
    }

    /**
     * Die Engine darf trotzdem schon starten: Ihre Initialisierung dauert selbst am längsten, und
     * ohne sie käme die vorgemerkte Begrüßung erst spürbar später.
     */
    @Test
    fun vorDerAntwortDesSchaltersDarfDieEngineSchonStarten() {
        assertEquals(
            SpeakAction.START_INIT_AND_QUEUE,
            decideSpeak("Willkommen", ttsCreated = false, ready = false, vorlesenAn = null)
        )
    }
}
