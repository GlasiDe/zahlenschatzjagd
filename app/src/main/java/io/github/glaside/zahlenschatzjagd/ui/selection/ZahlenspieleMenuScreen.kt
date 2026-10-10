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

private const val GAME_TILE_WIDTH_FRACTION = 0.417f
private val GAME_TILE_WIDTH_MIN = 90.dp
private val GAME_TILE_WIDTH_MAX = 150.dp

/**
 * Die Spielauswahl der Zahlenspiele-Insel als **vergrößerte Insel-Szene**: eine gemalte Bucht
 * (`zahlenspiele_bay`), auf der die einzelnen Spiele als große, gemalte, antippbare Objekte
 * liegen – dasselbe kindgerechte Muster wie die Schatzkarte der Startseite
 * (`ui/home/HomeScreen.kt`, `TreasureMap`/`IslandTile`). Erst ein Objekt antippen, dann folgt
 * die Konfiguration des jeweiligen Spiels. Neue Zahlenspiele kommen als Eintrag in
 * [ZAHLENSPIELE_GAMES] (Daten) plus [GAME_VISUALS] (Grafik + Position) hinzu.
 *
 * Gezeigt wird nur, was die [freigabe] dieses Profils übrig lässt (siehe [sichtbareZahlenspiele]).
 */
@Composable
fun ZahlenspieleMenuScreen(
    freigabe: Freigabe,
    onOpenVerliebte: () -> Unit,
    onOpenVergleichen: () -> Unit,
    onOpenNachbarzahlen: () -> Unit,
    begruessungFaellig: () -> Boolean,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    BegruessungJeBesuch("Zahlenspiele. Suche dir ein Spiel aus!", begruessungFaellig)

    fun openGame(game: Zahlenspiel) {
        Narrator.speak(context, game.spoken)
        when (game.id) {
            ZahlenspielId.VERLIEBTE -> onOpenVerliebte()
            ZahlenspielId.VERGLEICHEN -> onOpenVergleichen()
            ZahlenspielId.NACHBARZAHLEN -> onOpenNachbarzahlen()
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
                    .testTag("zahlenspiele_menu_back_button")
            ) {
                Text("← Zurück")
            }

            Surface(
                color = MapParchment.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Welches Spiel möchtest du spielen?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MapInk,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            GameScene(
                spiele = sichtbareZahlenspiele(freigabe),
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
 * getrennt** von [Zahlenspiel] (`ZahlenspieleGames.kt`), damit die Spieldaten Compose-/`R`-frei
 * bleiben. Die Positionen (x/y in 0..1) verteilen die Objekte über die offene Sandfläche der
 * Bucht (Ozean links, Palmen rechts).
 */
private data class GameVisual(
    @param:DrawableRes val image: Int,
    val x: Float,
    val y: Float,
    val testTag: String,
)

private val GAME_VISUALS: Map<ZahlenspielId, GameVisual> = mapOf(
    ZahlenspielId.VERLIEBTE to GameVisual(R.drawable.game_verliebte, x = 0.36f, y = 0.30f, testTag = "game_verliebte"),
    ZahlenspielId.VERGLEICHEN to GameVisual(R.drawable.game_vergleich, x = 0.60f, y = 0.64f, testTag = "game_vergleich"),
    // Oben rechts: der einzige Platz, der sich weder mit den beiden bestehenden Kacheln
    // überschneidet noch am unteren Rand abgeschnitten wird – die Diagonale der ersten beiden
    // lässt darunter keinen Streifen mehr frei, der eine ganze Kachel samt Titel-Pille trägt.
    ZahlenspielId.NACHBARZAHLEN to
        GameVisual(R.drawable.game_nachbarzahlen, x = 0.76f, y = 0.16f, testTag = "game_nachbarzahlen"),
)

/** Die Bucht-Fläche mit den Spiel-Objekten an ihren normierten Positionen. */
@Composable
private fun GameScene(
    spiele: List<Zahlenspiel>,
    onOpenGame: (Zahlenspiel) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // Dynamisch aus der verfügbaren Breite statt fest verdrahteter dp-Werte: verhindert
        // Überlappung der Titel-Pillen bei größerer Android-Anzeigegröße.
        val iconSize = (maxWidth * GAME_ICON_SIZE_FRACTION).coerceIn(GAME_ICON_SIZE_MIN, GAME_ICON_SIZE_MAX)
        val tileWidth = (maxWidth * GAME_TILE_WIDTH_FRACTION).coerceIn(GAME_TILE_WIDTH_MIN, GAME_TILE_WIDTH_MAX)
        // Gemeinsame Schriftgröße für alle Titel-Pillen, damit sie einheitlich wirken.
        val titleFontSize = rememberSharedTitleFontSize(
            titles = spiele.map { it.title },
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                color = MapInk,
                textAlign = TextAlign.Center,
                // maxLines = 2: beide Titel enthalten ein Leerzeichen ("Verliebte Zahlen",
                // "Zahlen vergleichen"), Compose bricht dort also von selbst korrekt um - anders
                // als bei zusammengeschriebenen Komposita ohne Leerzeichen (siehe
                // RechnenMenuScreen.kt/HomeScreen.kt) braucht es hier keine manuelle Trennung.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = tileWidth)
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
    }
}
