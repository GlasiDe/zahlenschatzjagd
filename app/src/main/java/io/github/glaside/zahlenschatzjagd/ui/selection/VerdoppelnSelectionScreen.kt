package io.github.glaside.zahlenschatzjagd.ui.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.GameSettings
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.categoryForType
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges

private const val DEFAULT_TASK_COUNT = 10f
private val NATURAL_CONTENT_HEIGHT = 520.dp

/**
 * Auswahl für das Rechen-Spiel „Verdoppeln". Fester Zahlenraum 0..10 für die Ausgangszahl
 * (Kernaufgaben-Basis, siehe LEHRPLAN.md) – kein Zahlenraum-Wähler nötig. Statt dessen ein
 * Schwierigkeits-Umschalter: leicht (drei Auswahl-Antworten) oder schwer (Zifferneingabe).
 *
 * Die beiden Stufen sind getrennte Fortschrittskennungen und werden getrennt freigegeben: Ist nur
 * eine erlaubt, entfällt der Umschalter und die Runde läuft in dieser Stufe.
 */
@Composable
fun VerdoppelnSelectionScreen(
    freigabe: Freigabe,
    onStartGame: (GameSettings) -> Unit,
    onBack: () -> Unit = {},
) {
    // true = leichte Stufe (Antwort auswählen), false = schwere (Ergebnis eintippen); genau
    // diese Unterscheidung trägt `multipleChoice` durch die Runde.
    val stufen = remember(freigabe) {
        listOf(true, false).filter { istLeicht ->
            freigabe.erlaubt(categoryForType("Verdoppeln", istLeicht))
        }
    }
    var leicht by remember(stufen) { mutableStateOf(stufen.firstOrNull() ?: true) }
    var taskCount by remember { mutableFloatStateOf(DEFAULT_TASK_COUNT) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scale = densityScale(available = maxHeight, natural = NATURAL_CONTENT_HEIGHT)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .testTag("verdoppeln_back_button")
            ) {
                Text("← Zurück")
            }

            Image(
                painter = painterResource(R.drawable.game_verdoppeln),
                contentDescription = "Verdoppeln",
                modifier = Modifier
                    .size(96.dp * scale)
                    .featherEdges()
            )

            Text(
                text = "Verdoppeln",
                style = MaterialTheme.typography.displaySmall,
                color = MapInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp * scale))

            Text(
                text = "Wie viel ist das Doppelte der Zahl?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(36.dp * scale))

            if (stufen.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Leicht: Antwort auswählen")
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = leicht,
                        onCheckedChange = { leicht = it },
                        modifier = Modifier.testTag("toggle_difficulty")
                    )
                }
            }
            Text(
                text = if (leicht) "Leicht: drei Antworten zur Auswahl" else "Schwerer: Ergebnis eintippen",
                style = MaterialTheme.typography.bodyMedium,
                color = MapInk
            )

            Spacer(modifier = Modifier.height(30.dp * scale))
            Text(text = "Anzahl der Aufgaben: ${taskCount.toInt()}")
            Slider(
                value = taskCount,
                onValueChange = { taskCount = it },
                valueRange = 5f..25f,
                steps = 3,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onStartGame(
                        GameSettings(
                            types = listOf("Verdoppeln"),
                            range = 10,
                            count = taskCount.toInt(),
                            multipleChoice = leicht,
                        )
                    )
                },
                modifier = Modifier.testTag("start_button")
            ) {
                Text("Los geht's!")
            }
        }
    }
}
