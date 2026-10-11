package io.github.glaside.zahlenschatzjagd.ui.zahlenmauer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.Zahlenmauer
import io.github.glaside.zahlenschatzjagd.ZahlenmauerCell
import io.github.glaside.zahlenschatzjagd.ZahlenmauerPos
import io.github.glaside.zahlenschatzjagd.ui.AusstehenderTon
import io.github.glaside.zahlenschatzjagd.ui.game.Ziffernblock
import io.github.glaside.zahlenschatzjagd.ui.theme.MapCorrect
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchmentMuted
import io.github.glaside.zahlenschatzjagd.ui.theme.MapWood
import io.github.glaside.zahlenschatzjagd.ui.util.adaptiveElementSize
import io.github.glaside.zahlenschatzjagd.ui.util.fitsWithoutScrollFallback

/**
 * Zahlenmauer-Spielbildschirm: eine Pyramide je "Aufgabe" (statt einer einzelnen Gleichung wie
 * bei [io.github.glaside.zahlenschatzjagd.ui.game.GameScreen]). Der Spieler tippt eine Lücke an,
 * befüllt sie über den gemeinsamen [Ziffernblock] und prüft mit dessen ✓ alle Lücken der
 * Mauer gleichzeitig, sobald alle befüllt sind.
 */
@Composable
fun ZahlenmauerScreen(
    viewModel: ZahlenmauerViewModel,
    onFinish: (Int, Int, List<UnlockedBadge>) -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinish(state.correctWalls, state.totalWallsChecked, state.newlyUnlockedBadges)
    }
    AusstehenderTon(state.ausstehenderTon, viewModel::tonGespielt)

    val wall = state.wall

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Sicherheitsnetz für Extremfälle (z.B. "Ultra" mit 5 Etagen + sehr große
                // Android-Anzeigegröße): ohne dies wurden die unteren Zeilen des Ziffernblocks
                // nachweislich (Emulator-Test bei Dichte 560) aus dem
                // sichtbaren Bereich gedrückt, ohne dass gescrollt werden konnte.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Mauer ${state.wallNumber} von ${state.totalWalls}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (wall != null) {
                ZahlenmauerPyramid(
                    wall = wall,
                    entries = state.entries,
                    selectedPos = state.selectedPos,
                    checked = state.checked,
                    onSelectCell = viewModel::selectCell,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.checked) {
                ZahlenmauerResultSection(state = state, onNextWall = viewModel::onNextWall)
            } else {
                Ziffernblock(viewModel::onDigit, viewModel::onClear, viewModel::onCheck)
            }
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 8.dp)
                .testTag("zahlenmauer_cancel_button")
        ) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/** Ergebnis-Zusammenfassung nach dem "Prüfen": Trefferzahl + Weiter-Button für die nächste Mauer. */
@Composable
private fun ZahlenmauerResultSection(state: ZahlenmauerUiState, onNextWall: () -> Unit) {
    val correctInWall = state.entries.values.count { it.isCorrect == true }
    Text(
        text = "$correctInWall von ${state.entries.size} Zellen richtig",
        style = MaterialTheme.typography.headlineSmall,
        color = if (correctInWall == state.entries.size) MapCorrect else MapInk,
        modifier = Modifier.testTag("zahlenmauer_result_text")
    )
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = onNextWall, modifier = Modifier.testTag("zahlenmauer_next_button")) {
        Text(if (state.wallNumber < state.totalWalls) "Nächste Mauer" else "Fertig!")
    }
}

/**
 * Die Pyramide, Spitze oben (höchste Etage zuerst) bis Basis unten - natürliche Leserichtung.
 * `internal` nur für `ZahlenmauerLageTest`, der sie in einen Rahmen fester Breite setzt.
 */
@Composable
internal fun ZahlenmauerPyramid(
    wall: Zahlenmauer,
    entries: Map<ZahlenmauerPos, ZahlenmauerEntryState>,
    selectedPos: ZahlenmauerPos?,
    checked: Boolean,
    onSelectCell: (ZahlenmauerPos) -> Unit,
) {
    // Die unterste Etage hat die meisten Zellen (= wall.difficulty.floors) - sie bestimmt, wie
    // breit die Pyramide insgesamt wird.
    val widestRow = wall.difficulty.floors
    val cellSpacing = 6.dp // 3.dp Padding auf jeder Seite der Zelle, siehe ZahlenmauerCellView.

    // Die Box ist bildschirmbreit, die Column nur so breit wie die Mauer: ohne die Ausrichtung
    // säße die Mauer oben links.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val cellSize = adaptiveElementSize(
            available = maxWidth,
            count = widestRow,
            ideal = 52.dp,
            min = 40.dp,
            spacing = cellSpacing,
        )
        // Selbst die Mindestgröße passt bei sehr vielen Spalten (z.B. "Ultra" + große
        // Android-Anzeigegröße) nicht mehr nebeneinander: dann horizontal scrollbar machen,
        // statt die äußeren Zellen über den Bildschirmrand hinauslaufen zu lassen.
        val needsHorizontalScrollFallback = !fitsWithoutScrollFallback(
            available = maxWidth,
            count = widestRow,
            min = 40.dp,
            spacing = cellSpacing,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .testTag("zahlenmauer_pyramid")
                .then(if (needsHorizontalScrollFallback) Modifier.fillMaxWidth() else Modifier)
        ) {
            for (row in wall.difficulty.floors - 1 downTo 0) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = if (needsHorizontalScrollFallback) {
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    } else {
                        Modifier
                    }
                ) {
                    wall.cells.filter { it.pos.row == row }.sortedBy { it.pos.col }.forEach { cell ->
                        ZahlenmauerCellView(
                            cell = cell,
                            entry = entries[cell.pos],
                            selected = cell.pos == selectedPos,
                            checked = checked,
                            size = cellSize,
                            onClick = { onSelectCell(cell.pos) },
                        )
                    }
                }
            }
        }
    }
}

/** Die vier möglichen Anzeigezustände einer Zelle, aus [cell]/[entry]/[checked] klassifiziert. */
private enum class CellVisualState { GIVEN, EMPTY, CORRECT, WRONG }

private fun classifyCell(cell: ZahlenmauerCell, entry: ZahlenmauerEntryState?, checked: Boolean): CellVisualState =
    when {
        cell.isGiven -> CellVisualState.GIVEN
        checked && entry?.isCorrect == true -> CellVisualState.CORRECT
        checked && entry?.isCorrect == false -> CellVisualState.WRONG
        else -> CellVisualState.EMPTY
    }

/** Vorab berechnete Darstellung einer Zelle - hält [ZahlenmauerCellView] selbst schlank/linear. */
private data class CellVisuals(
    val background: Color,
    val border: Color,
    val borderWidth: Dp,
    val text: String,
    val subtitle: String?,
    val tappable: Boolean,
)

/** Nach dem Prüfen zeigt eine Lücke ihre TATSÄCHLICHE Eingabe (nicht mehr die überschreibende
 * Lösung) - sonst verschwindet ein unbemerkter Tippfehler des Kindes spurlos. */
private fun cellText(
    state: CellVisualState,
    cell: ZahlenmauerCell,
    entry: ZahlenmauerEntryState?,
    checked: Boolean,
): String =
    when {
        state == CellVisualState.GIVEN -> cell.solution.toString()
        checked -> entry?.input.orEmpty()
        else -> entry?.input?.takeIf { it.isNotEmpty() } ?: "?"
    }

/** Bei falscher Eingabe zusätzlich ein kleiner Hinweis auf die korrekte Lösung. */
private fun cellSubtitle(state: CellVisualState, cell: ZahlenmauerCell): String? =
    if (state == CellVisualState.WRONG) "richtig: ${cell.solution}" else null

private fun cellVisuals(
    state: CellVisualState,
    cell: ZahlenmauerCell,
    entry: ZahlenmauerEntryState?,
    selected: Boolean,
    checked: Boolean,
    errorColor: Color,
    woodColor: Color,
): CellVisuals {
    val isSelectedGap = selected && state == CellVisualState.EMPTY
    val background = when (state) {
        CellVisualState.GIVEN -> MapParchmentMuted
        CellVisualState.CORRECT -> MapCorrect.copy(alpha = 0.3f)
        CellVisualState.WRONG -> errorColor.copy(alpha = 0.3f)
        CellVisualState.EMPTY -> MapParchment
    }
    val border = when (state) {
        CellVisualState.CORRECT -> MapCorrect
        CellVisualState.WRONG -> errorColor
        else -> if (isSelectedGap) woodColor else woodColor.copy(alpha = 0.4f)
    }
    return CellVisuals(
        background,
        border,
        if (isSelectedGap) 3.dp else 2.dp,
        cellText(state, cell, entry, checked),
        cellSubtitle(state, cell),
        state == CellVisualState.EMPTY
    )
}

/**
 * Eine einzelne Pyramiden-Zelle. Vorgegebene Zellen sind nicht antippbar und zeigen immer die
 * Lösung. Lücken zeigen vor dem Prüfen die getippte Eingabe (oder "?"); nach dem Prüfen die
 * tatsächlich eingegebene Zahl, grün/rot eingefärbt je nachdem, ob sie als richtig gewertet
 * wurde - bei falscher Eingabe zusätzlich klein die korrekte Lösung darunter.
 */
@Composable
private fun ZahlenmauerCellView(
    cell: ZahlenmauerCell,
    entry: ZahlenmauerEntryState?,
    selected: Boolean,
    checked: Boolean,
    size: Dp = 52.dp,
    onClick: () -> Unit,
) {
    val state = classifyCell(cell, entry, checked)
    val visuals = cellVisuals(state, cell, entry, selected, checked, MaterialTheme.colorScheme.error, MapWood)

    Surface(
        color = visuals.background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(visuals.borderWidth, visuals.border),
        modifier = Modifier
            .padding(3.dp)
            .width(size)
            .heightIn(min = size)
            .then(if (visuals.tappable) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("zahlenmauer_cell_${cell.pos.row}_${cell.pos.col}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text(
                text = visuals.text,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Default),
                fontWeight = FontWeight.Bold,
                color = MapInk,
            )
            if (visuals.subtitle != null) {
                Text(
                    text = visuals.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MapInk,
                )
            }
        }
    }
}
