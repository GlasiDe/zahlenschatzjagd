package io.github.glaside.zahlenschatzjagd.ui.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.GameSettings
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges

/** Siehe [SelectionScreen]s NATURAL_CONTENT_HEIGHT: grobe Referenzhöhe für [densityScale]. */
private val NATURAL_CONTENT_HEIGHT = 550.dp

/**
 * Konfiguration des Zahlenspiels „Zahlen vergleichen" (Zahlvergleich). Aufbau bewusst wie
 * `SelectionScreen`/`ZahlenspieleSelectionScreen`: gemaltes Spiel-Icon + kindgerechte Erklärung,
 * Zahlenraum-Wahl über den wiederverwendeten [RangeButton] (10/20/50/100, wie bei der
 * Recheninsel, siehe LEHRPLAN.md) und ein Slider für die Aufgabenanzahl. Startet dann direkt.
 *
 * Zur Wahl stehen nur die Zahlenräume, die die [freigabe] dieses Profils übrig lässt.
 */
@Composable
fun VergleichenSelectionScreen(
    freigabe: Freigabe,
    onStartGame: (GameSettings) -> Unit,
    onBack: () -> Unit = {},
) {
    val zahlenraeume = remember(freigabe) { freigabe.erlaubteZahlenraeume("GROESSER_KLEINER") }
    var selectedRange by remember(zahlenraeume) {
        mutableIntStateOf(zahlenraeume.firstOrNull() ?: KEIN_ZAHLENRAUM)
    }
    var taskCount by remember { mutableFloatStateOf(10f) }

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
                    .testTag("vergleichen_back_button")
            ) {
                Text("← Zurück")
            }

            Image(
                painter = painterResource(R.drawable.game_vergleich),
                contentDescription = "Zahlen vergleichen",
                modifier = Modifier
                    .size(96.dp * scale)
                    .featherEdges()
            )

            Text(
                text = "Zahlen vergleichen",
                style = MaterialTheme.typography.displaySmall,
                color = MapInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp * scale))

            Text(
                text = "Welche Zahl ist größer? Tippe das Zeichen < oder > in die Mitte!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(30.dp * scale))
            Text(text = "Zahlenraum: 0 bis $selectedRange")

            ZweispaltigesRaster(zahlenraeume) { zahlenraum ->
                RangeButton("0-$zahlenraum", zahlenraum, selectedRange) { selectedRange = it }
            }

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
                            types = listOf("Vergleichen"),
                            range = selectedRange,
                            count = taskCount.toInt(),
                            showCoins = false,
                        )
                    )
                },
                enabled = selectedRange != KEIN_ZAHLENRAUM,
                modifier = Modifier.testTag("start_button")
            ) {
                Text("Los geht's!")
            }
        }
    }
}
