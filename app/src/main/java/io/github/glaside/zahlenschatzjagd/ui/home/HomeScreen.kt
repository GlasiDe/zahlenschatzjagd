package io.github.glaside.zahlenschatzjagd.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.sp
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.BegruessungJeBesuch
import io.github.glaside.zahlenschatzjagd.ui.HOME_WELCOME_SPOKEN
import io.github.glaside.zahlenschatzjagd.ui.Narrator
import io.github.glaside.zahlenschatzjagd.ui.profile.AvatarImage
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges
import io.github.glaside.zahlenschatzjagd.ui.util.rememberSharedTitleFontSize

/** Anteil der verfügbaren Kartenbreite, den eine Insel-Grafik einnimmt - siehe [TreasureMap]. */
private const val ISLAND_SIZE_FRACTION = 0.30f
private val ISLAND_SIZE_MIN = 64.dp
private val ISLAND_SIZE_MAX = 108.dp

private const val TILE_WIDTH_FRACTION = 0.355f
private val TILE_WIDTH_MIN = 80.dp
private val TILE_WIDTH_MAX = 128.dp

/**
 * Sichtbare Darstellung einer Insel auf der Schatzkarte: die Grafik und ihre (normierte)
 * Position auf der Kartenfläche. **Bewusst getrennt** von [Island] (`Islands.kt`), damit
 * die Insel-Daten selbst Compose-/`R`-frei und rein unit-testbar bleiben. Die Positionen
 * (x/y in 0..1) verteilen die Inseln nur ansprechend über die Karte – sie sind **kein**
 * Lernpfad, nur Deko.
 */
internal data class IslandVisual(
    @param:DrawableRes val image: Int,
    val x: Float,
    val y: Float,
)

/**
 * Zuordnung Insel → Grafik + Kartenposition. Muss jede [IslandId] aus [HOME_ISLANDS] abdecken.
 * Die Inseln sitzen bewusst in den oberen zwei Dritteln: Unten zeigt `map_background.png` ein
 * Schiff und ein Seeungeheuer, die frei bleiben sollen.
 */
internal val ISLAND_VISUALS: Map<IslandId, IslandVisual> = mapOf(
    IslandId.RECHNEN to IslandVisual(R.drawable.island_rechnen, x = 0.27f, y = 0.10f),
    IslandId.ZAHLENSPIELE to IslandVisual(R.drawable.island_zahlenspiele, x = 0.73f, y = 0.14f),
    IslandId.ABZEICHEN to IslandVisual(R.drawable.island_abzeichen, x = 0.30f, y = 0.41f),
    IslandId.ELTERN to IslandVisual(R.drawable.island_eltern, x = 0.72f, y = 0.47f),
)

/**
 * Manuell korrekt getrennte Titel für die Anzeige auf der Titel-Pille: Compose bricht einzelne,
 * zusammenhängende Wörter (kein Leerzeichen) nicht automatisch an der richtigen Stelle um,
 * sondern irgendwo mitten im Wort ohne Trennstrich. "Für Eltern"/"Rechnen" brauchen das nicht
 * (Leerzeichen bzw. kurz genug).
 */
private val TITLE_LINE_BREAKS = mapOf(
    "Zahlenspiele" to "Zahlen-\nspiele",
    "Abzeichen" to "Abzei-\nchen",
)

private fun displayTitle(title: String): String = TITLE_LINE_BREAKS[title] ?: title

/**
 * Der Einstiegsbildschirm als **Schatzkarte**: ein Pergament-Hintergrund, auf dem das
 * Maskottchen Ella begrüßt (und per [Narrator] vorliest, für Nicht-Leser)
 * und die Inseln ([HOME_ISLANDS]) als echte Grafiken verstreut liegen, verbunden durch
 * eine gestrichelte Route – rein zur kindgerechten Optik, **kein** Lernpfad. Eine
 * X-Markierung am Ende der Route gibt es bewusst nicht: Sie wurde nicht als Schatz verstanden
 * und verdeckte das Seeungeheuer der Kartengrafik. Jede Insel führt in einen eigenen Bereich;
 * neue ROADMAP-Übungswelten kommen als weitere Insel hinzu.
 *
 * Gezeigt werden nur die Inseln, die die [freigabe] dieses Profils übrig lässt – Gesperrtes ist
 * unsichtbar, nicht gedämpft (siehe [sichtbareInseln]).
 */
@Composable
fun HomeScreen(
    freigabe: Freigabe,
    onOpenRechnen: () -> Unit,
    onOpenZahlenspiele: () -> Unit,
    onOpenBadges: () -> Unit,
    onOpenProfil: () -> Unit,
    onOpenEltern: () -> Unit,
    begruessungFaellig: () -> Boolean,
    activeAvatarId: String? = null,
    activeName: String? = null,
) {
    val context = LocalContext.current
    // Begrüßung bei jedem Besuch, nach dem Drehen nicht noch einmal. Der Narrator ist prozessweit
    // und wird bewusst NICHT beim Verlassen des Screens beendet, damit Ansagen nicht abgeschnitten
    // werden.
    BegruessungJeBesuch(HOME_WELCOME_SPOKEN, begruessungFaellig)

    fun openIsland(island: Island) {
        Narrator.speak(context, island.spoken)
        when (island.id) {
            IslandId.RECHNEN -> onOpenRechnen()
            IslandId.ZAHLENSPIELE -> onOpenZahlenspiele()
            IslandId.ABZEICHEN -> onOpenBadges()
            IslandId.ELTERN -> onOpenEltern()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.map_background),
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
            if (activeAvatarId != null && activeName != null) {
                HomeProfileBadge(
                    avatarId = activeAvatarId,
                    name = activeName,
                    onClick = onOpenProfil,
                )
                Spacer(Modifier.height(8.dp))
            }
            MapTitle()
            Spacer(Modifier.height(12.dp))
            TreasureMap(
                inseln = sichtbareInseln(freigabe),
                onOpenIsland = ::openIsland,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

/**
 * Kleines Profil-Abzeichen oben auf der Schatzkarte: zeigt den aktiven Piraten (Avatar +
 * Name) als Pergament-Pille und führt bei Tap direkt in die Profil-Verwaltung.
 */
@Composable
private fun HomeProfileBadge(
    avatarId: String,
    name: String,
    onClick: () -> Unit,
) {
    Surface(
        color = MapParchment.copy(alpha = 0.92f),
        shape = RoundedCornerShape(percent = 50),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("home_profile_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            AvatarImage(avatarId = avatarId, size = 40.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MapInk
            )
        }
    }
}

/**
 * Der Titel der Startkarte, ohne Sprechblase darunter: Die Aufforderung liest Ella ohnehin vor
 * ([HOME_WELCOME_SPOKEN]), und jede Zeile mehr kostete der Karte Höhe, die die Inseln brauchen,
 * um das Schiff und das Seeungeheuer der Kartengrafik frei zu lassen.
 *
 * Einzeilig erzwungen und automatisch schrumpfend: „Zahlenschatzjagd" bräche bei großer
 * Anzeigegröße sonst mitten im Wort um.
 */
@Composable
private fun MapTitle() {
    Text(
        text = "Zahlenschatzjagd 🏴‍☠️",
        style = MaterialTheme.typography.displaySmall,
        color = MapInk,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 36.sp)
    )
}

/**
 * Die Kartenfläche: zuerst die dekorative gestrichelte Route zwischen den Inseln, darüber
 * die Inseln als große Touch-Ziele an ihren normierten Positionen.
 */
@Composable
private fun TreasureMap(
    inseln: List<Island>,
    onOpenIsland: (Island) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // Dynamisch aus der verfügbaren Kartenbreite statt fest verdrahteter dp-Werte: verhindert,
        // dass sich Inseln bzw. ihre Titel-Pillen bei größerer Android-Anzeigegröße (kleinere
        // verfügbare Breite) überlappen.
        val islandSize = (maxWidth * ISLAND_SIZE_FRACTION).coerceIn(ISLAND_SIZE_MIN, ISLAND_SIZE_MAX)
        val tileWidth = (maxWidth * TILE_WIDTH_FRACTION).coerceIn(TILE_WIDTH_MIN, TILE_WIDTH_MAX)
        // Gemeinsame Schriftgröße für alle Titel-Pillen, damit sie einheitlich wirken (siehe
        // rememberSharedTitleFontSize) statt dass lange Titel wie "Zahlenspiele" auffällig
        // kleiner erscheinen als kurze wie "Rechnen".
        val titleFontSize = rememberSharedTitleFontSize(
            titles = inseln.map { displayTitle(it.title) },
            maxWidth = tileWidth,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        val routePoints = inseln.mapNotNull { ISLAND_VISUALS[it.id] }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = routePoints.map { Offset(it.x * size.width, it.y * size.height) }
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (p in points.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(
                    path = path,
                    color = MapInk.copy(alpha = 0.55f),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 16f))
                    )
                )
            }
        }

        for (island in inseln) {
            val visual = ISLAND_VISUALS[island.id] ?: continue
            IslandTile(
                island = island,
                image = visual.image,
                islandSize = islandSize,
                tileWidth = tileWidth,
                titleFontSize = titleFontSize,
                onClick = { onOpenIsland(island) },
                modifier = Modifier.offset(
                    x = maxWidth * visual.x - tileWidth / 2,
                    // Nie über den oberen Rand der Kartenfläche: Die oberen Inseln sitzen weit oben
                    // (siehe ISLAND_VISUALS), auf niedrigen Bildschirmen rutschten sie sonst über die
                    // Sprechblase.
                    y = (maxHeight * visual.y - islandSize / 2).coerceAtLeast(0.dp)
                )
            )
        }
    }
}

/**
 * Eine einzelne Insel als großes Touch-Ziel: die echte Insel-Grafik (mit weich
 * ausgeblendetem Rand, damit die quadratische Kachel nahtlos auf dem Pergament sitzt) und
 * darunter der kurze Titel in einer Pergament-Pille. „Kommt bald"-Inseln sind gedämpft und
 * nicht antippbar.
 */
@Composable
private fun IslandTile(
    island: Island,
    @DrawableRes image: Int,
    islandSize: Dp,
    tileWidth: Dp,
    titleFontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(tileWidth)
            .alpha(if (island.enabled) 1f else 0.45f)
            .clickable(enabled = island.enabled, onClick = onClick)
            .testTag("island_${island.id.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = island.title,
            modifier = Modifier
                .size(islandSize)
                .featherEdges()
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            color = MapParchment.copy(alpha = 0.88f),
            shape = RoundedCornerShape(percent = 50)
        ) {
            Text(
                text = displayTitle(island.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                color = MapInk,
                textAlign = TextAlign.Center,
                // Einheitliche Größe (siehe rememberSharedTitleFontSize) statt individuellem
                // Schrumpfen je Kachel - sonst wirken lange Titel neben kurzen unpassend klein.
                // maxLines = 2 erlaubt den manuell korrekt getrennten Titeln (TITLE_LINE_BREAKS)
                // umzubrechen; Ellipsis bleibt nur als Sicherheitsnetz für den unwahrscheinlichen
                // Fall, dass selbst zwei Zeilen nicht mehr reichen.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = tileWidth)
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
        if (!island.enabled) {
            Text(
                text = "kommt bald",
                style = MaterialTheme.typography.labelSmall,
                color = MapInk,
                textAlign = TextAlign.Center
            )
        }
    }
}
