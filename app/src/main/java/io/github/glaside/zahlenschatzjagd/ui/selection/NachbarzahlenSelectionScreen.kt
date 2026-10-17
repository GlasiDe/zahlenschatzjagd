package io.github.glaside.zahlenschatzjagd.ui.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.NachbarzahlenSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.NachbarzahlenSettings
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges
import io.github.glaside.zahlenschatzjagd.ui.util.umschaltbar

/** Siehe [SelectionScreen]s NATURAL_CONTENT_HEIGHT: grobe Referenzhöhe für [densityScale]. */
private val NATURAL_CONTENT_HEIGHT = 600.dp

private const val DEFAULT_AUFGABENZAHL = 10f

/** Was auf dem Bildschirm zur jeweiligen Schwierigkeitsstufe steht. */
private fun NachbarzahlenSchwierigkeitsstufe.erklaerung(): String = when (this) {
    NachbarzahlenSchwierigkeitsstufe.LEICHT -> "Leicht: die mittlere Zahl steht da"
    NachbarzahlenSchwierigkeitsstufe.SCHWER -> "Schwer: mal steht die erste, mal die mittlere, mal die letzte da"
}

/**
 * Auswahl für die Übungsform **Nachbarzahlen**. Eine eigene Datei statt einer Erweiterung eines
 * bestehenden Auswahl-Bildschirms, nach
 * `docs/adr/0007-getrennte-dateien-statt-gemeinsamer-abstraktion.md`.
 *
 * Zu wählen sind Schwierigkeitsstufe, Zahlenraum und Aufgabenzahl. Stufe und Zahlenraum sind
 * **frei kombinierbar** – anders als bei der Zahlenmauer bringt keine Stufe ihren Zahlenraum mit.
 *
 * Die beiden Stufen sind getrennte Fortschrittskennungen und werden getrennt freigegeben: Ist nur
 * eine erlaubt, entfällt der Umschalter und die Runde läuft in dieser Stufe (wie bei
 * [VerdoppelnSelectionScreen]). Zur Wahl stehen zudem nur die Zahlenräume, die die Freigabe der
 * **gewählten** Stufe übrig lässt. Ist die Übungsform gar nicht freigegeben, lässt sich keine Runde
 * starten. Normalerweise ist die Kachel dann schon weg – nicht aber, wenn dieser Bildschirm beim
 * Ändern der Freigabe noch im Rückweg-Stapel lag.
 */
@Composable
fun NachbarzahlenSelectionScreen(
    freigabe: Freigabe,
    onStartGame: (NachbarzahlenSettings) -> Unit,
    onBack: () -> Unit = {},
) {
    val stufen = remember(freigabe) {
        NachbarzahlenSchwierigkeitsstufe.entries.filter { freigabe.erlaubt(it.kennung) }
    }
    var schwierigkeitsstufe by remember(stufen) {
        mutableStateOf(stufen.firstOrNull() ?: NachbarzahlenSchwierigkeitsstufe.LEICHT)
    }
    val zahlenraeume = remember(freigabe, schwierigkeitsstufe) {
        freigabe.erlaubteZahlenraeume(schwierigkeitsstufe.kennung)
    }
    // Der Schlüssel ist die **Liste**, nicht die Stufe: Lassen beide Stufen dieselben Zahlenräume
    // übrig – der Regelfall –, behält ein Umschalten der Stufe die getroffene Wahl.
    var zahlenraum by remember(zahlenraeume) {
        mutableIntStateOf(zahlenraeume.firstOrNull() ?: KEIN_ZAHLENRAUM)
    }
    var aufgabenzahl by remember { mutableFloatStateOf(DEFAULT_AUFGABENZAHL) }

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
                    .testTag("nachbarzahlen_back_button")
            ) {
                Text("← Zurück")
            }

            Image(
                painter = painterResource(R.drawable.game_nachbarzahlen),
                contentDescription = "Nachbarzahlen",
                modifier = Modifier
                    .size(96.dp * scale)
                    .featherEdges()
            )

            Text(
                text = "Nachbarzahlen",
                style = MaterialTheme.typography.displaySmall,
                color = MapInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp * scale))

            Text(
                // Bewusst ohne „davor und danach": In der schweren Stufe kann die vorgegebene Zahl
                // auch außen stehen, dann liegen beide Lücken auf derselben Seite.
                text = "Drei Zahlen stehen nebeneinander. Eine ist schon da – fülle die beiden Lücken!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp * scale))

            if (stufen.size > 1) {
                val schwer = schwierigkeitsstufe == NachbarzahlenSchwierigkeitsstufe.SCHWER
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.umschaltbar(schwer, Role.Switch, "toggle_difficulty") { an ->
                        schwierigkeitsstufe = if (an) {
                            NachbarzahlenSchwierigkeitsstufe.SCHWER
                        } else {
                            NachbarzahlenSchwierigkeitsstufe.LEICHT
                        }
                    }
                ) {
                    Text("Schwerer üben")
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = schwer, onCheckedChange = null)
                }
            }
            Text(
                text = schwierigkeitsstufe.erklaerung(),
                style = MaterialTheme.typography.bodyMedium,
                color = MapInk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .testTag("nachbarzahlen_stufe_text")
            )

            Spacer(modifier = Modifier.height(24.dp * scale))
            Text(text = "Zahlenraum: 0 bis $zahlenraum")

            ZweispaltigesRaster(zahlenraeume) { wahl ->
                RangeButton("0-$wahl", wahl, zahlenraum) { zahlenraum = it }
            }

            Spacer(modifier = Modifier.height(30.dp * scale))
            Text(text = "Anzahl der Aufgaben: ${aufgabenzahl.toInt()}")
            Slider(
                value = aufgabenzahl,
                onValueChange = { aufgabenzahl = it },
                valueRange = 5f..25f,
                steps = 3,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onStartGame(
                        NachbarzahlenSettings(
                            schwierigkeitsstufe = schwierigkeitsstufe,
                            zahlenraum = zahlenraum,
                            aufgabenzahl = aufgabenzahl.toInt(),
                        )
                    )
                },
                enabled = freigabe.erlaubt(schwierigkeitsstufe.kennung, zahlenraum),
                modifier = Modifier.testTag("start_button")
            ) {
                Text("Los geht's!")
            }
        }
    }
}
