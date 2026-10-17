package io.github.glaside.zahlenschatzjagd.ui.selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.EinmaleinsSettings
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges
import io.github.glaside.zahlenschatzjagd.ui.util.umschaltbar

/** Siehe [SelectionScreen]s NATURAL_CONTENT_HEIGHT: grobe Referenzhöhe für [densityScale]. */
private val NATURAL_CONTENT_HEIGHT = 700.dp

private const val DEFAULT_AUFGABENZAHL = 10f

/** Drei Reihen je Zeile: Die neun Reihen stehen so als 3×3-Raster. */
private const val REIHEN_JE_ZEILE = 3

/** Was unter der Stufenwahl steht. */
private fun EinmaleinsSchwierigkeitsstufe.erklaerung(): String = when (this) {
    EinmaleinsSchwierigkeitsstufe.AUFSAGEN -> "Aufsagen: die Reihe der Reihe nach, Kästchen für Kästchen"
    EinmaleinsSchwierigkeitsstufe.DURCHEINANDER -> "Durcheinander: einzelne Aufgaben aus deinen Reihen"
}

/**
 * Auswahl für die Übungsform **Einmaleins**. Eine eigene Datei statt einer Erweiterung eines
 * bestehenden Auswahl-Bildschirms, nach
 * `docs/adr/0007-getrennte-dateien-statt-gemeinsamer-abstraktion.md`.
 *
 * Oben die Stufe, darunter die Reihen als 3×3-Raster, die Aufgabenzahl nur bei Durcheinander –
 * beim Aufsagen ergibt sich die Länge aus den Reihen. Angeboten wird nur, was die [freigabe] übrig
 * lässt (siehe [einmaleinsStufen] und [einmaleinsReihen]).
 *
 * **Keine Reihe ist vorgewählt**, auch nicht die einzige: Bei einer Mehrfachauswahl müsste das Kind
 * eine Vorwahl sonst erst abwählen. Der Start bleibt deshalb aus, bis eine Reihe angehakt ist. Die
 * Stufe ist dagegen eine Entweder-oder-Wahl und vorgewählt – die leichtere, sofern freigegeben.
 * Gemerkt wird nichts: Zurück aus einer Runde steht der Bildschirm wieder am Anfang, wie bei den
 * Grundrechenarten.
 */
@Composable
fun EinmaleinsSelectionScreen(
    freigabe: Freigabe,
    onStartGame: (EinmaleinsSettings) -> Unit,
    onBack: () -> Unit = {},
) {
    val stufen = remember(freigabe) { einmaleinsStufen(freigabe) }
    var stufe by remember(stufen) {
        mutableStateOf(stufen.firstOrNull() ?: EinmaleinsSchwierigkeitsstufe.AUFSAGEN)
    }
    val reihen = remember(freigabe, stufe) { einmaleinsReihen(freigabe, stufe) }
    var angehakt by remember(freigabe) { mutableStateOf(emptySet<Int>()) }
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
                    .testTag("einmaleins_back_button")
            ) {
                Text("← Zurück")
            }

            Image(
                painter = painterResource(R.drawable.game_einmaleins),
                contentDescription = "Einmaleins",
                modifier = Modifier
                    .size(96.dp * scale)
                    .featherEdges()
            )

            Text(
                text = "Einmaleins",
                style = MaterialTheme.typography.displaySmall,
                color = MapInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp * scale))

            if (stufen.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (wahl in stufen) {
                        RangeButton(wahl.beschriftung, wahl.ordinal, stufe.ordinal) {
                            stufe = wahl
                            angehakt = reihenNachStufenwechsel(angehakt, einmaleinsReihen(freigabe, wahl))
                        }
                    }
                }
            }
            Text(
                text = stufe.erklaerung(),
                style = MaterialTheme.typography.bodyMedium,
                color = MapInk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .testTag("einmaleins_stufe_text")
            )

            Spacer(modifier = Modifier.height(20.dp * scale))
            Text(text = "Wähle deine Reihen:")

            ReihenRaster(reihen = reihen, angehakt = angehakt) { reihe, an ->
                angehakt = if (an) angehakt + reihe else angehakt - reihe
            }

            if (stufe == EinmaleinsSchwierigkeitsstufe.DURCHEINANDER) {
                Spacer(modifier = Modifier.height(24.dp * scale))
                Text(text = "Anzahl der Aufgaben: ${aufgabenzahl.toInt()}")
                Slider(
                    value = aufgabenzahl,
                    onValueChange = { aufgabenzahl = it },
                    valueRange = 5f..25f,
                    steps = 3,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onStartGame(
                        EinmaleinsSettings(
                            schwierigkeitsstufe = stufe,
                            reihen = reihen.filter { it in angehakt },
                            aufgabenzahl = aufgabenzahl.toInt(),
                        )
                    )
                },
                enabled = reihen.any { it in angehakt },
                modifier = Modifier.testTag("start_button")
            ) {
                Text("Los geht's!")
            }
        }
    }
}

/**
 * Die Reihen als Kästchen, drei je Zeile. Fällt eine Reihe durch die Freigabe weg, rücken die
 * übrigen nach, statt eine Lücke zu lassen – wie im [ZweispaltigesRaster].
 */
@Composable
private fun ReihenRaster(
    reihen: List<Int>,
    angehakt: Set<Int>,
    onSetze: (Int, Boolean) -> Unit,
) {
    for (zeile in reihen.chunked(REIHEN_JE_ZEILE)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (reihe in zeile) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val an = reihe in angehakt
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.umschaltbar(an, Role.Checkbox, "einmaleins_reihe_$reihe") {
                            onSetze(reihe, it)
                        }
                    ) {
                        Checkbox(
                            checked = an,
                            onCheckedChange = null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        Text("${reihe}er")
                    }
                }
            }
            // Hält eine halb gefüllte letzte Zeile in derselben Spalteneinteilung.
            repeat(REIHEN_JE_ZEILE - zeile.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}
