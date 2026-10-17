package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.ui.AusstehenderTon
import io.github.glaside.zahlenschatzjagd.ui.game.Ziffernblock
import io.github.glaside.zahlenschatzjagd.ui.theme.MapCorrect
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.theme.MapWood

/**
 * Der Durcheinander-Bildschirm: eine Malaufgabe `8 · 7 = ?` nach der anderen, eingegeben über den
 * gemeinsamen [Ziffernblock] und mit dessen ✓ abgegeben. Danach steht fest, ob es stimmte, bei
 * einer falschen Antwort zusätzlich die richtige Gleichung – einen zweiten Versuch gibt es nicht.
 */
@Composable
fun DurcheinanderScreen(
    viewModel: DurcheinanderViewModel,
    onFinish: (Int, Int, List<UnlockedBadge>) -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BildschirmWachHalten()
    AusstehenderTon(state.ausstehenderTon, viewModel::tonGespielt)

    LaunchedEffect(state.fertig) {
        if (state.fertig) onFinish(state.richtige, state.aufgabenGesamt, state.neueAbzeichen)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Aufgabe ${state.aufgabenNummer} von ${state.aufgabenGesamt}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            AufgabenTafel(state)

            Spacer(modifier = Modifier.height(24.dp))

            if (state.geprueft) {
                Ergebnis(state = state, onWeiter = viewModel::onWeiter)
            } else {
                Ziffernblock(
                    onZiffer = viewModel::onZiffer,
                    onLoeschen = viewModel::onLoeschen,
                    onPruefen = viewModel::onPruefen,
                )
            }
        }
        Abbrechen(
            onCancel = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 8.dp)
        )
    }
}

/** Die Aufgabe `8 · 7 = ?`, in der Lücke die laufende Eingabe. */
@Composable
private fun AufgabenTafel(state: DurcheinanderUiState) {
    Surface(
        color = MapParchment,
        shape = RoundedCornerShape(12.dp),
        // Nach dem Prüfen wie beim Aufsagen: grün oder rot umrandet, darin weiter die
        // eigene Eingabe – sonst verschwände ein Vertipper spurlos.
        border = BorderStroke(
            width = if (state.geprueft) 3.dp else 2.dp,
            color = when (state.richtig) {
                null -> MapWood
                true -> MapCorrect
                false -> MaterialTheme.colorScheme.error
            },
        ),
    ) {
        Text(
            text = state.aufgabe.text(state.eingabe.ifEmpty { "?" }),
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Default),
            fontWeight = FontWeight.Bold,
            color = MapInk,
            // „10 · 10 = 100" muss auch bei Anzeigegröße „groß" in eine Zeile passen.
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 20.sp,
                maxFontSize = MaterialTheme.typography.displaySmall.fontSize,
            ),
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("durcheinander_aufgabe")
        )
    }
}

@Composable
private fun Ergebnis(state: DurcheinanderUiState, onWeiter: () -> Unit) {
    val richtig = state.richtig == true
    Text(
        text = if (richtig) "Super! Richtig! 🎉" else "Leider falsch.",
        style = MaterialTheme.typography.headlineSmall,
        color = if (richtig) MapCorrect else MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("durcheinander_result_text")
    )
    if (!richtig) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "So ist es richtig: ${state.aufgabe.text(state.aufgabe.ergebnis.toString())}",
            style = MaterialTheme.typography.bodyLarge,
            color = MapInk,
            modifier = Modifier.testTag("durcheinander_loesung_text")
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = onWeiter, modifier = Modifier.testTag("einmaleins_next_button")) {
        Text(if (state.aufgabenNummer < state.aufgabenGesamt) "Weiter" else "Fertig!")
    }
}
