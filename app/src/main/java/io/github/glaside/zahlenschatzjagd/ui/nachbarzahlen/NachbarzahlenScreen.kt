package io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.Nachbarposition
import io.github.glaside.zahlenschatzjagd.Nachbarzahlenaufgabe
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.ansage
import io.github.glaside.zahlenschatzjagd.ui.AusstehendeAnsage
import io.github.glaside.zahlenschatzjagd.ui.AusstehenderTon
import io.github.glaside.zahlenschatzjagd.ui.game.Ziffernblock
import io.github.glaside.zahlenschatzjagd.ui.theme.MapCorrect
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.util.adaptiveElementSize

/**
 * Der Nachbarzahlen-Spielbildschirm: drei aufeinanderfolgende Zahlen nebeneinander, eine davon
 * vorgegeben, über den beiden Lücken je ein Pfeil (warum Pfeile und keine Buchstaben, steht bei
 * [PFEIL_DAVOR] in `NachbarzahlenFeld.kt`). Das Kind tippt eine Lücke an, füllt sie über den
 * gemeinsamen [Ziffernblock] und gibt beide Zahlen gemeinsam mit dessen ✓ ab –
 * derselbe Ablauf wie bei der Zahlenmauer
 * (`docs/adr/0006-zahlenmauer-mit-eigenem-bildschirm.md`), weil auch hier mehrere Lücken
 * gleichzeitig offen sind und der generische Spielbildschirm nur **eine** Antwort kennt.
 */
@Composable
fun NachbarzahlenScreen(
    viewModel: NachbarzahlenViewModel,
    onFinish: (Int, Int, List<UnlockedBadge>) -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val aufgabe = state.aufgabe

    BildschirmWachHalten()
    // Beides einmal – nach dem Drehen steht nichts mehr aus
    // (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
    AusstehendeAnsage(state.ausstehendeAnsage?.ansage(), viewModel::ansageVorgelesen)
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
                "Aufgabe ${state.aufgabenNummer} von ${state.aufgabenGesamt}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (aufgabe != null) {
                Reihe(aufgabe = aufgabe, state = state, onWaehleLuecke = viewModel::waehleLuecke)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.geprueft) {
                Ergebnis(state = state, aufgabe = aufgabe, onWeiter = viewModel::onWeiter)
            } else {
                Ziffernblock(
                    onZiffer = viewModel::onZiffer,
                    onLoeschen = viewModel::onLoeschen,
                    onPruefen = viewModel::onPruefen,
                )
            }
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 8.dp)
                .testTag("nachbarzahlen_cancel_button")
        ) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Solange geübt wird, geht der Bildschirm nicht von selbst aus – wie in den übrigen Übungsformen. */
@Composable
private fun BildschirmWachHalten() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Die drei Felder nebeneinander (siehe [Feld]). */
@Composable
private fun Reihe(
    aufgabe: Nachbarzahlenaufgabe,
    state: NachbarzahlenUiState,
    onWaehleLuecke: (Nachbarposition) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val feldGroesse = adaptiveElementSize(
            available = maxWidth,
            count = Nachbarposition.entries.size,
            ideal = 88.dp,
            min = 56.dp,
            spacing = 8.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("nachbarzahlen_reihe"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Nachbarposition.entries.forEach { position ->
                Feld(
                    position = position,
                    aufgabe = aufgabe,
                    state = state,
                    groesse = feldGroesse,
                    onClick = { onWaehleLuecke(position) },
                )
            }
        }
    }
}

/** Nach dem Prüfen: die Lösung im Klartext und „Weiter" – einen zweiten Versuch gibt es nicht. */
@Composable
private fun Ergebnis(
    state: NachbarzahlenUiState,
    aufgabe: Nachbarzahlenaufgabe?,
    onWeiter: () -> Unit,
) {
    val alleRichtig = state.eingaben.values.all { it.istRichtig == true }
    Text(
        text = if (alleRichtig) "Super! Richtig! 🎉" else "Leider falsch.",
        style = MaterialTheme.typography.headlineSmall,
        color = if (alleRichtig) MapCorrect else MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("nachbarzahlen_result_text")
    )
    if (!alleRichtig && aufgabe != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "So ist es richtig: ${aufgabe.links} ${aufgabe.mitte} ${aufgabe.rechts}",
            style = MaterialTheme.typography.bodyLarge,
            color = MapInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("nachbarzahlen_loesung_text")
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = onWeiter, modifier = Modifier.testTag("nachbarzahlen_next_button")) {
        Text(if (state.aufgabenNummer < state.aufgabenGesamt) "Weiter" else "Fertig!")
    }
}
