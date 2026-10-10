package io.github.glaside.zahlenschatzjagd.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.ui.util.MIN_TOUCH_TARGET
import io.github.glaside.zahlenschatzjagd.ui.util.adaptiveElementSize

/** Die Tasten des Ziffernblocks, Reihe für Reihe – ✓ unten rechts wie auf einem Telefon. */
private val ZIFFERNBLOCK_REIHEN = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("0", "C", "✓"),
)

/**
 * Der **eine** Ziffernblock aller Übungsformen mit Zifferneingabe: `0–9`, `C` zum Löschen und ✓
 * zum Abgeben. Gemeinsam, damit das Kind nicht zwei Bedienweisen für dasselbe lernen muss.
 *
 * ✓ ist nie gesperrt. Ob gerade abgegeben werden darf – etwa erst, wenn alle Lücken einer Mauer
 * gefüllt sind –, entscheidet das ViewModel hinter [onPruefen] und tut sonst nichts.
 *
 * Die Tastengröße folgt der verfügbaren Breite (drei Tasten je Reihe), damit die Reihe auch bei
 * Anzeigegröße „groß" nicht über den Rand läuft, ohne unter eine antippbare Mindestgröße zu fallen.
 */
@Composable
fun Ziffernblock(
    onZiffer: (String) -> Unit,
    onLoeschen: () -> Unit,
    onPruefen: () -> Unit,
    zeilenAbstand: Dp = 8.dp,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val groesse = tastenGroesse(maxWidth)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ZIFFERNBLOCK_REIHEN.forEach { reihe ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    reihe.forEach { taste ->
                        NumberButton(label = taste, size = groesse) {
                            when (taste) {
                                "C" -> onLoeschen()
                                "✓" -> onPruefen()
                                else -> onZiffer(taste)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(zeilenAbstand))
            }
        }
    }
}

/**
 * Nur der ✓ – für die Übungsformen, deren Antwort nicht getippt, sondern ausgewählt wird
 * (Zahlen vergleichen, Verdoppeln leicht). Er sitzt genau dort und ist genau so groß wie im
 * [Ziffernblock]: unten rechts. So gibt das Kind überall mit demselben Knopf an derselben Stelle
 * ab.
 */
@Composable
fun AbgebenZeile(onPruefen: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val groesse = tastenGroesse(maxWidth)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // Zwei leere Plätze statt der Tasten „0" und „C" halten den ✓ in der dritten Spalte.
            Spacer(modifier = Modifier.size(groesse))
            Spacer(modifier = Modifier.size(groesse))
            NumberButton(label = "✓", size = groesse, onClick = onPruefen)
        }
    }
}

/** Drei Tasten je Reihe, aus der verfügbaren Breite – nie unter die antippbare Mindestgröße. */
private fun tastenGroesse(verfuegbar: Dp): Dp = adaptiveElementSize(
    available = verfuegbar,
    count = 3,
    ideal = 80.dp,
    min = MIN_TOUCH_TARGET,
    spacing = 8.dp,
)
