package io.github.glaside.zahlenschatzjagd

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Name und Kennung der App, wie Android sie sieht. Die `applicationId` ist seit 2.0 eine andere
 * als vorher – für Android also eine andere App (siehe
 * `docs/adr/0032-neue-application-id-vor-der-veroeffentlichung.md`). Rutscht sie versehentlich
 * zurück, landet ein Update wieder in der alten App statt in der neuen.
 */
@RunWith(AndroidJUnit4::class)
class AppIdentitaetInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun applicationId_istDieNeue() {
        assertEquals("io.github.glaside.zahlenschatzjagd", context.packageName)
    }

    @Test
    fun appName_unterDemSymbol_istZahlenschatzjagd() {
        assertEquals(
            "Zahlenschatzjagd",
            context.applicationInfo.loadLabel(context.packageManager).toString(),
        )
    }
}
