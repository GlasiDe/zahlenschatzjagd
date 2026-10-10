package io.github.glaside.zahlenschatzjagd

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.ui.Narrator
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke-Test: [Narrator.speak] darf auf einem echten Gerät/Emulator unter keinen Umständen
 * crashen – auch nicht, wenn (wie z. B. auf manchen Geräten) keine deutsche TTS-Stimme
 * installiert ist. Ob tatsächlich hörbar gesprochen wird, hängt vom Gerät ab und wird hier
 * nicht geprüft.
 */
@RunWith(AndroidJUnit4::class)
class NarratorInstrumentedTest {

    @Test
    fun speak_laeuftOhneCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Narrator.speak(context, "Willkommen bei der Zahlenschatzjagd!")
        Narrator.speak(context, "")
    }
}
