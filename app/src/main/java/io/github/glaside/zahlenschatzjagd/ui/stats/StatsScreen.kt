package io.github.glaside.zahlenschatzjagd.ui.stats

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.BADGE_CATEGORIES
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.PerformanceTier
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.earnedStars
import io.github.glaside.zahlenschatzjagd.performanceTier
import io.github.glaside.zahlenschatzjagd.ui.TonBeimErstenErscheinen
import io.github.glaside.zahlenschatzjagd.ui.badges.RewardItem
import io.github.glaside.zahlenschatzjagd.ui.theme.TreasureGold
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import kotlinx.coroutines.delay

/** Referenzhöhe für [densityScale]: der Platz, der dem Auswertungsteil bei normaler
 * Anzeigegröße zur Verfügung steht (Bildschirmhöhe abzüglich der fest verankerten Buttons).
 * Bleibt weniger übrig, schrumpfen die Abstände mit, damit möglichst gar nicht erst gescrollt
 * werden muss. */
private val NATURAL_CONTENT_HEIGHT = 660.dp

@Composable
fun StatsScreen(
    correct: Int,
    total: Int,
    unlockedBadges: List<UnlockedBadge> = emptyList(),
    onOpenBadges: () -> Unit = {},
    onPlayAgain: () -> Unit,
    onBackToSelection: () -> Unit
) {
    // 1. Ton „geschafft" einmal je Runde – nicht nach dem Drehen und nicht nach der Rückkehr von
    // den Abzeichen.
    TonBeimErstenErscheinen(GameSound.FINISH)

    // 2. Text und Farbe je nach Leistung festlegen
    val (message, color, icon) = when (performanceTier(correct, total)) {
        PerformanceTier.PERFECT -> Triple(
            "Wahnsinn! Du bist ein Mathe-Genie! 🏆",
            MaterialTheme.colorScheme.primary,
            "🥇"
        )
        PerformanceTier.GREAT -> Triple("Super Ergebnis! Fast perfekt! ⭐", MaterialTheme.colorScheme.primary, "🌟")
        PerformanceTier.GOOD -> Triple(
            "Gut gemacht! Übung macht den Meister! 💪",
            MaterialTheme.colorScheme.secondary,
            "👍"
        )
        PerformanceTier.KEEP_GOING -> Triple(
            "Kopf hoch! Beim nächsten Mal schaffst du mehr! 🚀",
            MaterialTheme.colorScheme.tertiary,
            "🏃"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Der Auswertungsteil bekommt nur den Platz, der nach den beiden Aktions-Buttons übrig
        // bleibt, und scrollt bei Bedarf. Ohne das wurden die Buttons bei großer Anzeigegröße
        // auf Höhe 0 zusammengedrückt, sobald die Abzeichen-Karte erschien - das Kind kam dann
        // weder zu einer neuen Runde noch zurück zur Auswahl. Die Karte kann beliebig hoch
        // werden (mehrere Abzeichen in einer Runde), deshalb reicht Verkleinern hier nicht.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val scale = densityScale(available = maxHeight, natural = NATURAL_CONTENT_HEIGHT)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    // Mindestens so hoch wie der sichtbare Bereich, damit der Inhalt bei
                    // genügend Platz weiterhin mittig steht statt oben zu kleben.
                    .heightIn(min = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = icon, style = MaterialTheme.typography.displayLarge)

                Spacer(modifier = Modifier.height(16.dp * scale))

                Text(
                    text = message,
                    style = MaterialTheme.typography.headlineMedium,
                    color = color,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp * scale))

                StarRating(stars = earnedStars(correct, total))

                Spacer(modifier = Modifier.height(24.dp * scale))

                Text(
                    text = "Du hast $correct von $total richtig!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("stats_result_text")
                )

                if (unlockedBadges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp * scale))
                    NewBadgesCard(badges = unlockedBadges, onOpenBadges = onOpenBadges)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Deine beiden Buttons
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.testTag("play_again_button").fillMaxWidth(0.8f)
        ) {
            Text("Gleich nochmal!")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBackToSelection,
            modifier = Modifier.testTag("back_to_selection_button").fillMaxWidth(0.8f)
        ) {
            Text("Andere Aufgaben")
        }
    }
}

/**
 * Zeigt immer drei Stern-Plätze; die verdienten [stars] werden gefüllt dargestellt und
 * fliegen beim Erscheinen nacheinander mit einem kleinen Pop ein. Leere Plätze bleiben als
 * Ansporn statisch sichtbar.
 */
@Composable
private fun StarRating(stars: Int) {
    // Zählt die bereits eingeflogenen (gefüllten) Sterne schrittweise hoch.
    var visibleStars by remember(stars) { mutableIntStateOf(0) }
    LaunchedEffect(stars) {
        visibleStars = 0
        for (i in 1..stars) {
            delay(250)
            visibleStars = i
        }
    }

    Row(
        modifier = Modifier.testTag("stars_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until 3) {
            val filled = i < stars
            // Gefüllte Sterne starten bei 0 und poppen ein, sobald sie an der Reihe sind;
            // leere Sterne sind konstant sichtbar.
            val targetScale = if (!filled || i < visibleStars) 1f else 0f
            val scale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "starScale"
            )
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = if (filled) {
                    TreasureGold
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                },
                modifier = Modifier
                    .size(56.dp)
                    .scale(scale)
            )
        }
    }
}

/**
 * Hervorgehobene Karte für in dieser Runde frisch freigeschaltete Abzeichen. Poppt beim
 * Erscheinen leicht ein und bietet einen direkten Weg zur Abzeichen-Übersicht.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewBadgesCard(badges: List<UnlockedBadge>, onOpenBadges: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badgeCardScale"
    )

    Card(modifier = Modifier.scale(scale).testTag("new_badges_card")) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Deine Schatztruhe füllt sich! 🎉",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (badge in badges) {
                    val categoryLabel = BADGE_CATEGORIES
                        .firstOrNull { it.key == badge.categoryKey }?.label ?: ""
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RewardItem(tier = badge.tier)
                        Text(
                            text = "$categoryLabel bis ${badge.numberRange}",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenBadges,
                modifier = Modifier.testTag("view_all_badges_button")
            ) {
                Text("Alle Abzeichen ansehen")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    ZahlenschatzjagdTheme {
        StatsScreen(
            correct = 4,
            total = 10,
            onPlayAgain = { },
            onBackToSelection = { }
        )
    }
}
