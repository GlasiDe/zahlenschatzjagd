package io.github.glaside.zahlenschatzjagd.ui.selection

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.BegruessungJeBesuch
import io.github.glaside.zahlenschatzjagd.ui.Narrator
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges
import io.github.glaside.zahlenschatzjagd.ui.util.rememberSharedTitleFontSize

private const val GAME_ICON_SIZE_FRACTION = 0.33f
private val GAME_ICON_SIZE_MIN = 72.dp
private val GAME_ICON_SIZE_MAX = 120.dp

private const val GAME_TILE_WIDTH_FRACTION = 0.55f
private val GAME_TILE_WIDTH_MIN = 90.dp
private val GAME_TILE_WIDTH_MAX = 210.dp

/**
 * Die Spielauswahl der Rechnen-Insel als **vergrößerte Insel-Szene**, strukturell identisch zu
 * [ZahlenspieleMenuScreen] (dasselbe Bucht-Hintergrundbild `zahlenspiele_bay` wird bewusst
 * wiederverwendet – es ist eine neutrale Kulisse ohne zahlenspiele-spezifische Motive, siehe
 * `ASSETS.md`). Vier Objekte: „Grundrechenarten" (führt zum bestehenden `SelectionScreen`),
 * „Verdoppeln", „Zahlenmauer" und „Einmaleins" (führen zu ihren jeweils eigenen Selection-Screens). Neue
 * Rechenspiele kommen als Eintrag in [RECHNEN_GAMES] (Daten) plus [GAME_VISUALS] (Grafik +
 * Position) hinzu.
 *
 * Gezeigt wird nur, was die [freigabe] dieses Profils übrig lässt (siehe [sichtbareRechenspiele]);
 * bleibt gar nichts übrig, ist schon die Insel auf der Startkarte verschwunden.
 */
@Composable
fun RechnenMenuScreen(
    freigabe: Freigabe,
    onOpenGrundrechenarten: () -> Unit,
    onOpenVerdoppeln: () -> Unit,
    onOpenZahlenmauer: () -> Unit,
    onOpenEinmaleins: () -> Unit,
    begruessungFaellig: () -> Boolean,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    BegruessungJeBesuch("Rechnen. Suche dir eine Übung aus!", begruessungFaellig)

    fun openGame(game: Rechenspiel) {
        Narrator.speak(context, game.spoken)
        when (game.id) {
            RechenspielId.GRUNDRECHENARTEN -> onOpenGrundrechenarten()
            RechenspielId.VERDOPPELN -> onOpenVerdoppeln()
            RechenspielId.ZAHLENMAUER -> onOpenZahlenmauer()
            RechenspielId.EINMALEINS -> onOpenEinmaleins()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.zahlenspiele_bay),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .testTag("rechnen_menu_back_button")
            ) {
                Text("← Zurück")
            }

            Surface(
                color = MapParchment.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Was möchtest du üben?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MapInk,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            GameScene(
                spiele = sichtbareRechenspiele(freigabe),
                onOpenGame = ::openGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

/**
 * Sichtbare Zuordnung Spiel → Grafik + (normierte) Position auf der Bucht-Fläche. **Bewusst
 * getrennt** von [Rechenspiel] (`RechenspieleGames.kt`), damit die Spieldaten Compose-/`R`-frei
 * bleiben.
 */
private data class RechenGameVisual(
    @param:DrawableRes val image: Int,
    val x: Float,
    val y: Float,
    val testTag: String,
)

private val GAME_VISUALS: Map<RechenspielId, RechenGameVisual> = mapOf(
    // x-Werte bewusst weiter auseinandergezogen als ursprünglich (0.36/0.72): der lange Titel
    // "Grundrechenarten" braucht mehr Platz für die Titel-Pille, ohne die Nachbarkachel zu
    // berühren. Der Sandstrand in zahlenspiele_bay.png ist zwischen Ozean (links) und Palmen
    // (rechts) durchgehend offen, verschiebt also keine Kachel auf ein gemaltes Motiv.
    // Mit vier Kacheln stehen sie als zwei Paare übereinander, je Paar mit demselben Abstand wie
    // oben – darunter rückten die Titel-Pillen bei großer Anzeigegröße aneinander.
    RechenspielId.GRUNDRECHENARTEN to RechenGameVisual(
        R.drawable.game_grundrechenarten, x = 0.26f, y = 0.30f, testTag = "game_grundrechenarten"
    ),
    RechenspielId.VERDOPPELN to RechenGameVisual(
        R.drawable.game_verdoppeln, x = 0.26f, y = 0.66f, testTag = "game_verdoppeln"
    ),
    RechenspielId.ZAHLENMAUER to RechenGameVisual(
        R.drawable.game_zahlenmauer, x = 0.82f, y = 0.30f, testTag = "game_zahlenmauer"
    ),
    RechenspielId.EINMALEINS to RechenGameVisual(
        R.drawable.game_einmaleins, x = 0.82f, y = 0.66f, testTag = "game_einmaleins"
    ),
)

/**
 * Manuell korrekt getrennte Titel für die Anzeige auf der Titel-Pille: Compose bricht einzelne,
 * zusammenhängende Wörter (kein Leerzeichen) nicht automatisch an der richtigen Stelle um,
 * sondern irgendwo mitten im Wort ohne Trennstrich. Betrifft aktuell nur "Grundrechenarten" -
 * bei Bedarf hier um weitere lange Komposita ergänzen.
 */
private val TITLE_LINE_BREAKS = mapOf(
    "Grundrechenarten" to "Grundrechen-\narten",
)

private fun displayTitle(title: String): String = TITLE_LINE_BREAKS[title] ?: title

/** Die Bucht-Fläche mit den Spiel-Objekten an ihren normierten Positionen. */
@Composable
private fun GameScene(
    spiele: List<Rechenspiel>,
    onOpenGame: (Rechenspiel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // Dynamisch aus der verfügbaren Breite statt fest verdrahteter dp-Werte: verhindert
        // Überlappung der Titel-Pillen bei größerer Android-Anzeigegröße.
        val iconSize = (maxWidth * GAME_ICON_SIZE_FRACTION).coerceIn(GAME_ICON_SIZE_MIN, GAME_ICON_SIZE_MAX)
        val tileWidth = (maxWidth * GAME_TILE_WIDTH_FRACTION).coerceIn(GAME_TILE_WIDTH_MIN, GAME_TILE_WIDTH_MAX)
        // Gemeinsame Schriftgröße für alle Titel-Pillen, damit sie einheitlich wirken statt dass
        // "Grundrechenarten" auffällig kleiner erscheint als "Verdoppeln"/"Zahlenmauer".
        val titleFontSize = rememberSharedTitleFontSize(
            titles = spiele.map { displayTitle(it.title) },
            maxWidth = tileWidth,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        for (game in spiele) {
            val visual = GAME_VISUALS[game.id] ?: continue
            GameTile(
                title = game.title,
                image = visual.image,
                testTag = visual.testTag,
                iconSize = iconSize,
                tileWidth = tileWidth,
                titleFontSize = titleFontSize,
                onClick = { onOpenGame(game) },
                modifier = Modifier.offset(
                    x = maxWidth * visual.x - tileWidth / 2,
                    y = maxHeight * visual.y - iconSize / 2
                )
            )
        }
    }
}

/**
 * Ein einzelnes Spiel als großes Touch-Ziel: die gemalte Objekt-Grafik (mit weich
 * ausgeblendetem Rand, damit die quadratische Kachel nahtlos in der Bucht sitzt) und darunter
 * der kurze Titel in einer Pergament-Pille.
 */
@Composable
private fun GameTile(
    title: String,
    @DrawableRes image: Int,
    testTag: String,
    iconSize: Dp,
    tileWidth: Dp,
    titleFontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(tileWidth)
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = title,
            modifier = Modifier
                .size(iconSize)
                .featherEdges()
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            color = MapParchment.copy(alpha = 0.9f),
            shape = RoundedCornerShape(percent = 50)
        ) {
            Text(
                text = displayTitle(title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                color = MapInk,
                textAlign = TextAlign.Center,
                // maxLines = 2 statt 1: erlaubt den manuell korrekt getrennten Titeln (siehe
                // TITLE_LINE_BREAKS) tatsächlich umzubrechen. Ellipsis bleibt nur als
                // Sicherheitsnetz für den unwahrscheinlichen Fall, dass selbst zwei Zeilen nicht
                // reichen.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = tileWidth)
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
    }
}
