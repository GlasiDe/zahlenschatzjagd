package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Hält die Dokumentation davon ab, tote Dateinamen zu tragen: Nennt ein Dokument eine
 * `.kt`-Datei, die es nicht (mehr) gibt, wird dieser Test rot. Er läuft bei
 * jedem `gradlew test` mit.
 *
 * **Nur eine Richtung wird geprüft.** Dass eine Datei nirgends erwähnt wird, ist kein Fehler:
 * Die Doku beschreibt bewusst keinen Dateibaum, weil das ein `ls`
 * ohnehin besser kann.
 */
class DocsReferenceTest {

    /**
     * Dateinamen, die in der Doku als Beispiel oder Muster vorkommen, ohne echte Dateien zu
     * sein. Bewusst kurz halten – wer hier etwas einträgt, sollte zuerst prüfen, ob sich der
     * Text nicht besser umformulieren lässt.
     */
    private val erlaubteBeispiele = setOf(
        "MyApp.kt",
    )

    private fun repoRoot(): File? =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "CONTEXT.md").isFile }

    private fun markdownDateien(root: File): List<File> =
        listOf("docs", ".claude")
            .map { File(root, it) }
            .filter { it.isDirectory }
            .flatMap { it.walkTopDown().filter { f -> f.extension == "md" }.toList() }
            .plus(root.listFiles { f: File -> f.extension == "md" }?.toList() ?: emptyList())

    @Test
    fun dokumentationNenntNurExistierendeKotlinDateien() {
        val gefunden = repoRoot()
        // In exotischen Ausführungsumgebungen ohne Repo-Wurzel still überspringen.
        assumeTrue("Repo-Wurzel nicht gefunden", gefunden != null)
        val root = gefunden!!

        val vorhanden = File(root, "app/src").walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.name }
            .toSet()

        val muster = Regex("""\b[A-Za-z0-9_]+\.kt\b""")
        val tot = mutableListOf<String>()

        markdownDateien(root).forEach { doc ->
            muster.findAll(doc.readText()).map { it.value }.distinct().forEach { genannt ->
                if (genannt !in vorhanden && genannt !in erlaubteBeispiele) {
                    tot += "${doc.relativeTo(root).path.replace('\\', '/')}: $genannt"
                }
            }
        }

        assertEquals(
            "Dokumentation nennt Kotlin-Dateien, die es nicht (mehr) gibt. Verweis " +
                "korrigieren oder entfernen:\n" + tot.sorted().joinToString("\n"),
            emptyList<String>(),
            tot.sorted(),
        )
    }
}
