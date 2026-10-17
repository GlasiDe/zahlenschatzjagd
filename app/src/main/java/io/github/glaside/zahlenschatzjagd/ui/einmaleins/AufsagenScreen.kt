package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.AUFGABEN_JE_REIHE
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.reihenAnsage
import io.github.glaside.zahlenschatzjagd.reihenName
import io.github.glaside.zahlenschatzjagd.ui.AusstehendeAnsage
import io.github.glaside.zahlenschatzjagd.ui.AusstehenderTon
import io.github.glaside.zahlenschatzjagd.ui.game.Ziffernblock
import io.github.glaside.zahlenschatzjagd.ui.theme.MapCorrect
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.theme.MapWood
import io.github.glaside.zahlenschatzjagd.ui.util.adaptiveElementSize

/** Fünf Kästchen je Zeile: Die zehn Zahlen einer Reihe stehen in zwei Zeilen zu je fünf. */
private const val KAESTCHEN_JE_ZEILE = 5

/** Wie klein die Zahl im Kästchen höchstens wird – „100" muss in ein schmales Kästchen passen. */
private val MIN_ZAHL_SCHRIFT = 14.sp

/**
 * Der Aufsagen-Bildschirm: oben die Reihe („7er-Reihe"), darunter zehn Kästchen ohne Aufgabe davor,
 * in die das Kind nacheinander `7, 14, 21 …` tippt. Immer ist das nächste Kästchen aktiv; der
 * gemeinsame [Ziffernblock] füllt es, sein ✓ prüft es. Ein falsches Kästchen wird rot und zeigt die
 * richtige Zahl, die getippte steht klein durchgestrichen darunter – sonst verschwände ein
 * Vertipper spurlos. Nach dem zehnten Kästchen geht es mit „Weiter" zur nächsten Reihe.
 */
@Composable
fun AufsagenScreen(
    viewModel: AufsagenViewModel,
    onFinish: (Int, Int, List<UnlockedBadge>) -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BildschirmWachHalten()
    // Beides einmal – nach dem Drehen steht nichts mehr aus
    // (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
    AusstehendeAnsage(state.ausstehendeAnsage?.let(::reihenAnsage), viewModel::ansageVorgelesen)
    AusstehenderTon(state.ausstehenderTon, viewModel::tonGespielt)

    LaunchedEffect(state.fertig) {
        if (state.fertig) onFinish(state.richtige, state.gepruefte, state.neueAbzeichen)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Sicherheitsnetz wie bei der Zahlenmauer: scrollt nur, wenn der Ziffernblock
                // bei sehr großer Android-Anzeigegröße sonst aus dem Bild rutschte.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Reihe ${state.reiheNummer} von ${state.reihenGesamt}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reihenName(state.reihe),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MapInk,
                modifier = Modifier.testTag("aufsagen_reihe_text")
            )
            Spacer(modifier = Modifier.height(16.dp))

            KaestchenRaster(state)

            Spacer(modifier = Modifier.height(20.dp))

            if (state.reiheFertig) {
                ReiheErgebnis(state = state, onWeiter = viewModel::onWeiter)
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

@Composable
private fun KaestchenRaster(state: AufsagenUiState) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val groesse = adaptiveElementSize(
            available = maxWidth,
            count = KAESTCHEN_JE_ZEILE,
            ideal = 60.dp,
            min = 44.dp,
            spacing = 8.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            for (zeile in (0 until AUFGABEN_JE_REIHE).chunked(KAESTCHEN_JE_ZEILE)) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    for (index in zeile) {
                        AufsagenKaestchen(index = index, state = state, groesse = groesse)
                    }
                }
            }
        }
    }
}

/**
 * Ein Kästchen: geprüft mit der **richtigen** Zahl (grün oder rot umrandet), aktiv mit der laufenden
 * Eingabe oder „?", noch nicht dran leer.
 */
@Composable
private fun AufsagenKaestchen(index: Int, state: AufsagenUiState, groesse: Dp) {
    val geprueft = state.kaestchen.getOrNull(index)
    val aktiv = index == state.aktivesKaestchen
    val fehler = MaterialTheme.colorScheme.error
    val (hintergrund, rahmen) = when {
        geprueft?.richtig == true -> MapCorrect.copy(alpha = 0.3f) to MapCorrect
        geprueft != null -> fehler.copy(alpha = 0.3f) to fehler
        aktiv -> MapParchment to MapWood
        else -> MapParchment to MapWood.copy(alpha = 0.4f)
    }
    val text = when {
        geprueft != null -> "${(index + 1) * state.reihe}"
        aktiv -> state.eingabe.ifEmpty { "?" }
        else -> ""
    }
    Surface(
        color = hintergrund,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (aktiv) 3.dp else 2.dp, rahmen),
        modifier = Modifier
            .padding(4.dp)
            .width(groesse)
            .heightIn(min = groesse)
            .testTag("aufsagen_kaestchen_$index")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Default),
                fontWeight = FontWeight.Bold,
                color = MapInk,
                // Eine Zeile, die schrumpft, statt „100" mitten in der Zahl umzubrechen.
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = MIN_ZAHL_SCHRIFT,
                    maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                ),
                modifier = Modifier.testTag("aufsagen_zahl_$index")
            )
            if (geprueft != null && !geprueft.richtig) {
                Text(
                    text = geprueft.getippt,
                    style = MaterialTheme.typography.labelSmall,
                    textDecoration = TextDecoration.LineThrough,
                    color = MapInk,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Nach dem zehnten Kästchen: wie viele saßen, und „Weiter" bzw. „Fertig!". */
@Composable
private fun ReiheErgebnis(state: AufsagenUiState, onWeiter: () -> Unit) {
    val richtigInDerReihe = state.kaestchen.count { it.richtig }
    val alle = richtigInDerReihe == AUFGABEN_JE_REIHE
    Text(
        text = if (alle) "Super! Alles richtig! 🎉" else "$richtigInDerReihe von $AUFGABEN_JE_REIHE richtig",
        style = MaterialTheme.typography.headlineSmall,
        color = if (alle) MapCorrect else MapInk,
        modifier = Modifier.testTag("aufsagen_ergebnis_text")
    )
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = onWeiter, modifier = Modifier.testTag("einmaleins_next_button")) {
        Text(if (state.reiheNummer < state.reihenGesamt) "Weiter" else "Fertig!")
    }
}
