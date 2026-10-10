package io.github.glaside.zahlenschatzjagd.ui.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.ZahlenmauerDifficulty
import io.github.glaside.zahlenschatzjagd.ZahlenmauerSettings
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapWood
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges

private const val DEFAULT_WALL_COUNT = 5f
private val NATURAL_CONTENT_HEIGHT = 750.dp

/** Kurzbeschreibung je Stufe für die Auswahl-Kacheln (Etagenzahl + Zahlenraum + Aufgabenform). */
private fun describeDifficulty(difficulty: ZahlenmauerDifficulty): String {
    val form = if (difficulty.isUltra) "mit Umkehraufgaben" else "aufwärts rechnen"
    return "${difficulty.floors} Etagen, bis ${difficulty.range}, $form"
}

/**
 * Auswahl für das Rechen-Spiel „Zahlenmauer". Ein einziger Stufen-Regler (Leicht/Mittel/
 * Schwer/Ultra) statt separater Regler für Etagenzahl/Zahlenraum – jede Stufe bündelt beides zu
 * einer festen, sinnvollen Kombination (siehe LEHRPLAN.md, Abschnitt „Zahlenmauer"). Ultra zeigt
 * zusätzlich einen Warnhinweis, da es als einzige Stufe Umkehraufgaben enthält und damit deutlich
 * schwerer ist als die übrigen Stufen.
 *
 * Jede Stufe ist eine eigene Fortschrittskennung mit festem Zahlenraum – angeboten wird nur, was
 * die [freigabe] dieses Profils übrig lässt.
 */
@Composable
fun ZahlenmauerSelectionScreen(
    freigabe: Freigabe,
    onStartGame: (ZahlenmauerSettings) -> Unit,
    onBack: () -> Unit = {},
) {
    val stufen = remember(freigabe) {
        ZahlenmauerDifficulty.entries.filter { freigabe.erlaubt(it.kennung) }
    }
    var difficulty by remember(stufen) {
        mutableStateOf(stufen.firstOrNull() ?: ZahlenmauerDifficulty.LEICHT)
    }
    var wallCount by remember { mutableFloatStateOf(DEFAULT_WALL_COUNT) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scale = densityScale(available = maxHeight, natural = NATURAL_CONTENT_HEIGHT)
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Sicherheitsnetz für Extremfälle (sehr große Android-Anzeigegröße + viele
                // Schwierigkeitsstufen): scrollt nur, wenn der Inhalt trotz der Verkleinerung oben
                // wirklich nicht mehr passt - bei normaler Anzeigegröße bleibt scrollState.maxValue
                // 0 und es ist optisch kein Scrollen sichtbar/nötig.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .testTag("zahlenmauer_back_button")
            ) {
                Text("← Zurück")
            }

            Image(
                painter = painterResource(R.drawable.game_zahlenmauer),
                contentDescription = "Zahlenmauer",
                modifier = Modifier
                    .size(96.dp * scale)
                    .featherEdges()
            )

            Text(
                text = "Zahlenmauer",
                style = MaterialTheme.typography.displaySmall,
                color = MapInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp * scale))

            Text(
                text = "Zwei nebeneinanderliegende Steine ergeben zusammengezählt den Stein darüber!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(30.dp * scale))
            Text(text = "Schwierigkeit:")
            Spacer(modifier = Modifier.height(8.dp * scale))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                stufen.forEach { option ->
                    DifficultyButton(
                        difficulty = option,
                        isSelected = option == difficulty,
                        onSelect = { difficulty = option },
                    )
                    Spacer(modifier = Modifier.height(6.dp * scale))
                }
            }

            if (difficulty.isUltra) {
                Spacer(modifier = Modifier.height(12.dp * scale))
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("ultra_warning_banner")
                ) {
                    Text(
                        text = "Achtung: Ultra ist sehr schwer – hier fehlen Zahlen in der " +
                            "untersten Reihe, die du selbst zurückrechnen musst!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp * scale))
            Text(text = "Anzahl der Mauern: ${wallCount.toInt()}")
            Slider(
                value = wallCount,
                onValueChange = { wallCount = it },
                valueRange = 3f..10f,
                steps = 6,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onStartGame(ZahlenmauerSettings(difficulty = difficulty, wallCount = wallCount.toInt()))
                },
                modifier = Modifier.testTag("start_button")
            ) {
                Text("Los geht's!")
            }
        }
    }
}

@Composable
private fun DifficultyButton(
    difficulty: ZahlenmauerDifficulty,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Button(
        onClick = onSelect,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MapWood else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
        ),
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .testTag("difficulty_${difficulty.name}")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(difficulty.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
            Text(describeDifficulty(difficulty), style = MaterialTheme.typography.labelSmall)
        }
    }
}
