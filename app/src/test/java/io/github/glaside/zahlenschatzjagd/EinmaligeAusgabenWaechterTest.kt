package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Hält die Regel aus `docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md` fest: Ein
 * **Ton** oder ein vorgelesener Satz, den ein Bildschirm von sich aus auslöst, läuft über einen
 * der Wege in `EinmaligeAusgaben.kt` – nie über einen nackten Effekt. Der liefe nach dem Drehen des
 * Geräts neu an und spielte dasselbe noch einmal.
 *
 * Eine Text-Suche, keine Code-Analyse: Sie findet [SoundPlayer.play] und `Narrator.speak` innerhalb
 * des Blocks von `LaunchedEffect`, `DisposableEffect` und `SideEffect`. Was auf ein Tippen hin
 * erklingt, steht in einem `onClick` und ist erlaubt – eine Wiederholung nach dem Tippen ist
 * kein Fehler.
 */
class EinmaligeAusgabenWaechterTest {

    private fun repoRoot(): File? =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "CONTEXT.md").isFile }

    @Test
    fun keinBildschirmSpieltTonOderVorlesenAusEinemNacktenEffekt() {
        val gefunden = repoRoot()
        // In exotischen Ausführungsumgebungen ohne Repo-Wurzel still überspringen.
        assumeTrue("Repo-Wurzel nicht gefunden", gefunden != null)
        val quellen = File(gefunden!!, "app/src/main/java")

        val dateien = quellen.walkTopDown()
            .filter { it.extension == "kt" && it.name != ERLAUBTE_DATEI }
            .toList()
        assumeTrue("Keine Quellen gefunden", dateien.isNotEmpty())

        val verstoesse = dateien.flatMap { datei ->
            effekteMitAusgabe(datei.readText()).map { zeile ->
                "${datei.relativeTo(quellen).path.replace('\\', '/')}:$zeile"
            }
        }

        assertEquals(
            "Ton oder Vorlesen aus einem nackten Effekt – nach dem Drehen kommt es doppelt. Einen " +
                "der Wege aus $ERLAUBTE_DATEI nehmen (ADR 0030):\n" + verstoesse.joinToString("\n"),
            emptyList<String>(),
            verstoesse,
        )
    }

    /** Damit der Wächter nicht still nichts findet, prüft er sich an Beispielen selbst. */
    @Test
    fun derWaechterErkenntDenNacktenEffekt_undLaesstTippenDurch() {
        val quelltext = """
            fun Bildschirm() {
                LaunchedEffect(state.feedback) {
                    state.feedback?.let { SoundPlayer.play(context, GameSound.CORRECT) }
                }
                LaunchedEffect(Unit) { Narrator.speak(context, "Hallo") }
                LaunchedEffect(state.isFinished) { if (state.isFinished) onFinish() }
                Button(onClick = { Narrator.speak(context, "Insel") }) {}
                DisposableEffect(view) {
                    SoundPlayer.play(context, GameSound.FINISH)
                    onDispose { }
                }
            }
        """.trimIndent()

        assertEquals(listOf(2, 5, 8), effekteMitAusgabe(quelltext))
    }

    private companion object {
        const val ERLAUBTE_DATEI = "EinmaligeAusgaben.kt"

        val effekt = Regex("""\b(LaunchedEffect|DisposableEffect|SideEffect)\b""")
        val ausgabe = Regex("""\b(SoundPlayer\.play|Narrator\.speak)\(""")

        /** Die Zeilen (ab 1), in denen ein Effekt beginnt, dessen Block eine Ausgabe enthält. */
        fun effekteMitAusgabe(quelltext: String): List<Int> =
            effekt.findAll(quelltext).mapNotNull { treffer ->
                val block = blockNach(quelltext, treffer.range.last + 1) ?: return@mapNotNull null
                if (ausgabe.containsMatchIn(block)) zeileVon(quelltext, treffer.range.first) else null
            }.toList()

        /** Der `{ … }`-Block hinter einer optionalen Argumentliste `( … )` ab [start]. */
        fun blockNach(text: String, start: Int): String? {
            var i = start
            while (i < text.length && text[i].isWhitespace()) i++
            if (i < text.length && text[i] == '(') i = schliessendes(text, i, '(', ')') + 1
            while (i < text.length && text[i].isWhitespace()) i++
            if (i >= text.length || text[i] != '{') return null
            return text.substring(i, schliessendes(text, i, '{', '}') + 1)
        }

        fun schliessendes(text: String, offen: Int, auf: Char, zu: Char): Int {
            var tiefe = 0
            for (i in offen until text.length) {
                when (text[i]) {
                    auf -> tiefe++
                    zu -> if (--tiefe == 0) return i
                }
            }
            return text.length - 1
        }

        fun zeileVon(text: String, index: Int): Int = text.substring(0, index).count { it == '\n' } + 1
    }
}
