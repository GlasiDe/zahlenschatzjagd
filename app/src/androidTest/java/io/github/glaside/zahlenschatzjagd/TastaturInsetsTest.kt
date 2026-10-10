package io.github.glaside.zahlenschatzjagd

import android.content.ComponentName
import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hält die halbe Miete gegen „die Tastatur liegt über dem Eingabefeld" fest: Ohne
 * `adjustResize` meldet Android vor Version 11 die Größe der Tastatur gar nicht, und das
 * `imePadding()` im `ZahlenschatzjagdNavHost` liefe ins Leere. Die App hat minSdk 26, der Fall ist
 * also real.
 *
 * Die andere Hälfte – dass der Platz auch tatsächlich freigehalten wird – hängt an der
 * eingeblendeten Tastatur und damit am System; sie wird von Hand geprüft (siehe
 * `docs/agents/testing.md`).
 */
@RunWith(AndroidJUnit4::class)
class TastaturInsetsTest {

    @Test
    fun mainActivity_gibtDerTastaturPlatzStattSieUeberDenInhaltZuLegen() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val info = context.packageManager.getActivityInfo(
            ComponentName(context, MainActivity::class.java),
            0,
        )

        assertEquals(
            "MainActivity braucht android:windowSoftInputMode=\"adjustResize\"",
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
            info.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST,
        )
    }
}
