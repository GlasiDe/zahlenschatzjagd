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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.GameSettings
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.categoryForType
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges
import io.github.glaside.zahlenschatzjagd.ui.util.umschaltbar

private val NATURAL_CONTENT_HEIGHT = 520.dp

/** Die Zielzahl der verliebten Zahlen – fest, siehe LEHRPLAN.md. */
private const val VERLIEBTE_ZIELZAHL = 10

/**
 * Auswahl für die **verliebten Zahlen** (Zerlegung der 10). Bewusst schlank gehalten – ein
 * Schalter „Mit Hilfe" / „Ohne Hilfe" und die Aufgaben-Anzahl, dann direkt los. Die Zielzahl ist
 * fix 10 (`range = 10`), siehe LEHRPLAN.md.
 *
 * „Mit Hilfe" (Münzen zeigen, was fehlt) und „Ohne Hilfe" sind zwei Schwierigkeitsstufen mit
 * eigener Fortschrittskennung und werden getrennt freigegeben: Ist nur eine erlaubt, entfällt der
 * Schalter und die Runde läuft in dieser Stufe
 * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
 *
 * Ist keine der beiden freigegeben, lässt sich keine Runde starten. Normalerweise ist die
 * Kachel dann schon weg – nicht aber, wenn dieser Bildschirm beim Ändern der Freigabe noch im
 * Rückweg-Stapel lag.
 */
@Composable
fun ZahlenspieleSelectionScreen(
    freigabe: Freigabe,
    onStartGame: (GameSettings) -> Unit,
    onBack: () -> Unit = {},
) {
    val hilfen = remember(freigabe) { verliebteZahlenHilfen(freigabe) }
    var mitHilfe by remember(hilfen) { mutableStateOf(hilfen.firstOrNull() ?: true) }
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
                    .testTag("zahlenspiele_back_button")
            ) {
                Text("← Zurück")
            }

            Image(
                painter = painterResource(R.drawable.game_verliebte),
                contentDescription = "Verliebte Zahlen",
                modifier = Modifier
                    .size(96.dp * scale)
                    .featherEdges()
            )

            Text(
                text = "Verliebte Zahlen",
                style = MaterialTheme.typography.displaySmall,
                color = MapInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp * scale))

            Text(
                text = "Welche Zahl fehlt bis zur 10? Finde den verliebten Partner!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(36.dp * scale))

            val mitSchalter = hilfen.size > 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // Ist nur eine Hilfe-Stufe freigegeben, steht hier bloß ihr Name.
                modifier = if (mitSchalter) {
                    Modifier.umschaltbar(mitHilfe, Role.Switch, "toggle_hilfe") { mitHilfe = it }
                } else {
                    Modifier
                }
            ) {
                Text(
                    text = if (mitHilfe) "Mit Hilfe" else "Ohne Hilfe",
                    color = MapInk,
                    modifier = Modifier.testTag("hilfe_stufe")
                )
                if (mitSchalter) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = mitHilfe, onCheckedChange = null)
                }
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
                            types = listOf("Verliebte"),
                            range = VERLIEBTE_ZIELZAHL,
                            count = taskCount.toInt(),
                            showCoins = mitHilfe,
                        )
                    )
                },
                enabled = freigabe.erlaubt(
                    categoryForType("Verliebte", showCoins = mitHilfe),
                    VERLIEBTE_ZIELZAHL
                ),
                modifier = Modifier.testTag("start_button")
            ) {
                Text("Los geht's!")
            }
        }
    }
}
