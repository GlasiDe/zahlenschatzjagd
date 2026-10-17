package io.github.glaside.zahlenschatzjagd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Hält die vom Build erzeugte [AENDERUNGSHINWEISE] gegen ihre Quelle, die Änderungshinweise für
 * F-Droid: Was die App zeigt, soll genau das sein, was dort steht.
 */
class AenderungshinweiseErzeugtTest {

    private fun changelogs(): File? =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "CONTEXT.md").isFile }
            ?.let { File(it, "fastlane/metadata/android/de-DE/changelogs") }

    @Test
    fun jederHinweisTraegtDenTextAusFastlane() {
        val ordner = changelogs()
        // In exotischen Ausführungsumgebungen ohne Repo-Wurzel still überspringen.
        assumeTrue("Repo-Wurzel nicht gefunden", ordner != null)
        AENDERUNGSHINWEISE.forEach { hinweis ->
            val quelle = File(ordner, "${hinweis.versionCode}.txt").readText().replace("\r", "").trim()
            assertEquals("Text von ${hinweis.versionCode}", quelle, hinweis.text)
        }
    }

    @Test
    fun jederHinweisHatEinenVersionsnamen_undHaeltDieGrenzeVonFDroid() {
        AENDERUNGSHINWEISE.forEach { hinweis ->
            assertTrue("Versionsname von ${hinweis.versionCode}", hinweis.versionName.isNotBlank())
            assertTrue(
                "Hinweis ${hinweis.versionCode} ist länger als 500 Zeichen",
                hinweis.text.length <= F_DROID_GRENZE,
            )
        }
    }

    private companion object {
        const val F_DROID_GRENZE = 500
    }
}
