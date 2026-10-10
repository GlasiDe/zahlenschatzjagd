package io.github.glaside.zahlenschatzjagd.ui.selection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.glaside.zahlenschatzjagd.BADGE_RANGES
import io.github.glaside.zahlenschatzjagd.GameSettings
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale

/** Referenzhöhe für [densityScale]: die bei normaler Anzeigegröße tatsächlich verfügbare Höhe
 * (auf einem 1080x2400-Gerät mit 420 dpi rund 830 dp, gemessen). Ist weniger Platz da - bei
 * Anzeigegröße "groß" sind es nur noch rund 650 dp -, schrumpfen die dekorativen Abstände
 * proportional mit, damit der Start-Button unten sichtbar bleibt. Der Wert liegt bewusst knapp
 * unter der Normalhöhe: bei normaler Anzeigegröße soll gar nicht erst verkleinert werden. */
private val NATURAL_CONTENT_HEIGHT = 820.dp

/**
 * „Es ist kein Zahlenraum freigegeben." Kein echter Zahlenraum, deshalb als eigener Wert
 * benannt – erreichbar ist dieser Zustand ohnehin kaum, weil vorher schon die Kachel
 * verschwindet.
 */
internal const val KEIN_ZAHLENRAUM = 0

/**
 * Die Konfiguration einer Runde mit den Grundrechenarten.
 *
 * Angeboten wird nur, was die [freigabe] dieses Profils übrig lässt – abgewählte Rechenarten und
 * Zahlenräume erscheinen gar nicht erst, statt gedämpft dazustehen
 * (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`).
 */
@Composable
fun SelectionScreen(
    freigabe: Freigabe,
    onStartGame: (GameSettings) -> Unit,
    onBack: () -> Unit = {},
) {
    val rechenarten = remember(freigabe) { RECHENARTEN.filter { freigabe.erlaubt(it.kennung) } }
    val zahlenraeume = remember(freigabe) { BADGE_RANGES.filter { it in freigabe.zahlenraeume } }

    // Vorbelegt ist die erste noch erlaubte Rechenart – „Plus" ist es nicht mehr zwangsläufig.
    var gewaehlteArten by remember(rechenarten) {
        mutableStateOf(setOfNotNull(rechenarten.firstOrNull()?.type))
    }
    var selectedRange by remember(zahlenraeume) {
        mutableIntStateOf(zahlenraeume.firstOrNull() ?: KEIN_ZAHLENRAUM)
    }
    var taskCount by remember { mutableFloatStateOf(10f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scale = densityScale(available = maxHeight, natural = NATURAL_CONTENT_HEIGHT)
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Sicherheitsnetz für Extremfälle: scrollt nur, wenn der Inhalt trotz der
                // Verkleinerung oben wirklich nicht mehr passt.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .testTag("selection_back_button")
            ) {
                Text("← Zurück")
            }

            // Einzeilig erzwungen und automatisch schrumpfend: "Grundrechenarten" würde bei
            // großer Anzeigegröße (schmale verfügbare Breite) sonst umbrechen und den
            // Start-Button ganz unten aus dem sichtbaren Bereich drücken.
            Text(
                text = "Grundrechenarten",
                style = MaterialTheme.typography.displaySmall,
                color = MapInk,
                textAlign = TextAlign.Center,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 36.sp)
            )

            Text(
                text = "Hier werden nur Grundrechenarten geübt.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp * scale)
            )
            Spacer(modifier = Modifier.height(20.dp * scale))
            Text(text = "Wähle die Rechenart:")

            ZweispaltigesRaster(rechenarten) { rechenart ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rechenart.type in gewaehlteArten,
                        onCheckedChange = { angehakt ->
                            gewaehlteArten = if (angehakt) {
                                gewaehlteArten + rechenart.type
                            } else {
                                gewaehlteArten - rechenart.type
                            }
                        },
                        modifier = Modifier.testTag(rechenart.testTag)
                    )
                    Text(rechenart.label)
                }
            }

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

            Spacer(modifier = Modifier.height(24.dp * scale))

            Button(
                onClick = {
                    val types = rechenarten.map { it.type }.filter { it in gewaehlteArten }
                    onStartGame(GameSettings(types, selectedRange, taskCount.toInt()))
                },
                enabled = gewaehlteArten.isNotEmpty() && selectedRange != KEIN_ZAHLENRAUM,
                modifier = Modifier.testTag("start_button")
            ) {
                Text("Los geht's!")
            }
        }
    }
}

/**
 * Zwei Spalten, zeilenweise gefüllt. Bewusst kein festes 2×2-Raster mehr: Fällt eine Rechenart
 * oder ein Zahlenraum durch die Freigabe weg, rücken die übrigen nach, statt eine Lücke zu lassen.
 */
@Composable
internal fun <T> ZweispaltigesRaster(eintraege: List<T>, inhalt: @Composable (T) -> Unit) {
    for (zeile in eintraege.chunked(2)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (eintrag in zeile) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    inhalt(eintrag)
                }
            }
            // Hält die letzte, halb gefüllte Zeile in derselben Spalteneinteilung.
            if (zeile.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun RangeButton(label: String, value: Int, currentSelection: Int, onSelect: (Int) -> Unit) {
    val isSelected = value == currentSelection
    Button(
        onClick = { onSelect(value) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(label)
    }
}
