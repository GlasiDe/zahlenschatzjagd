package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Jede Tondatei der App muss in `ASSETS.md` (Abschnitt „6. Töne") mit Quelle und der Lizenz
 * **CC0** belegt sein. Unfreie Medien bringen der App bei F-Droid die Markierung „Non-Free Assets"
 * ein; deshalb wurden die Pixabay-Töne 2026 gegen CC0-Töne getauscht. CC BY wäre zwar ebenfalls
 * frei, verlangt aber eine Namensnennung, die die App nirgends zeigt. Ohne diesen Test fiele ein
 * später ergänzter Ton erst auf, wenn jemand die Lizenzen von Hand nachprüft.
 */
class TonQuellenTest {

    private fun repoRoot(): File? =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "CONTEXT.md").isFile }

    /** Die Tabellenzeilen des Abschnitts „6. Töne", die eine Datei nennen, je Dateiname. */
    private fun belegteZeilen(root: File): Map<String, String> =
        File(root, "ASSETS.md").readText()
            .substringAfter("## 6. Töne", missingDelimiterValue = "")
            .substringBefore("\n## ")
            .lines()
            .filter { it.startsWith("| `") }
            .associateBy { it.substringAfter("| `").substringBefore("`") }

    private fun tondateien(root: File): List<String> =
        File(root, "app/src/main/res/raw").listFiles().orEmpty().map { it.name }.sorted()

    @Test
    fun jedeTondatei_istInAssetsMdAlsCc0Belegt() {
        val gefunden = repoRoot()
        // In exotischen Ausführungsumgebungen ohne Repo-Wurzel still überspringen.
        assumeTrue("Repo-Wurzel nicht gefunden", gefunden != null)
        val root = gefunden!!
        val zeilen = belegteZeilen(root)

        val unbelegt = tondateien(root).filter { zeilen[it]?.contains("CC0 1.0") != true }

        assertEquals(
            "Diese Tondateien fehlen in ASSETS.md, Abschnitt „6. Töne\", oder sind dort nicht CC0",
            emptyList<String>(),
            unbelegt,
        )
    }

    /** Die Gegenrichtung: Eine Zeile zu einer gelöschten Datei wäre eine falsche Auskunft. */
    @Test
    fun assetsMd_nenntKeineTondateiDieEsNichtGibt() {
        val gefunden = repoRoot()
        assumeTrue("Repo-Wurzel nicht gefunden", gefunden != null)
        val root = gefunden!!

        val tot = belegteZeilen(root).keys.sorted() - tondateien(root).toSet()

        assertEquals("ASSETS.md nennt Tondateien, die es nicht gibt", emptyList<String>(), tot)
    }
}
