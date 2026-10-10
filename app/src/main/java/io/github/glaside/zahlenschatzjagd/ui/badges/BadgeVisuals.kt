package io.github.glaside.zahlenschatzjagd.ui.badges

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.BadgeTier
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.ui.theme.BadgeBronze
import io.github.glaside.zahlenschatzjagd.ui.theme.BadgeDiamond
import io.github.glaside.zahlenschatzjagd.ui.theme.BadgePlatinum
import io.github.glaside.zahlenschatzjagd.ui.theme.BadgeSapphire
import io.github.glaside.zahlenschatzjagd.ui.theme.BadgeSilver
import io.github.glaside.zahlenschatzjagd.ui.theme.TreasureGold
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges

/** Farbe einer Abzeichen-Stufe (für Texte/Akzente). */
fun tierColor(tier: BadgeTier?): Color = when (tier) {
    BadgeTier.BRONZE -> BadgeBronze
    BadgeTier.SILBER -> BadgeSilver
    BadgeTier.GOLD -> TreasureGold
    BadgeTier.SAPHIR -> BadgeSapphire
    BadgeTier.DIAMANT -> BadgeDiamond
    BadgeTier.PLATIN -> BadgePlatinum
    null -> Color(0xFFBDBDBD)
}

/** Deutscher Anzeigename einer Stufe. */
fun tierLabel(tier: BadgeTier?): String = when (tier) {
    BadgeTier.BRONZE -> "Bronze"
    BadgeTier.SILBER -> "Silber"
    BadgeTier.GOLD -> "Gold"
    BadgeTier.SAPHIR -> "Saphir"
    BadgeTier.DIAMANT -> "Diamant"
    BadgeTier.PLATIN -> "Platin"
    null -> "Noch offen"
}

/** Kurzname des neu gewonnenen Schatzes einer Stufe (für die Ordensverleihung). */
fun rewardLabel(tier: BadgeTier): String = when (tier) {
    BadgeTier.BRONZE -> "Bronzemünzen"
    BadgeTier.SILBER -> "Silbermünzen"
    BadgeTier.GOLD -> "Goldmünzen"
    BadgeTier.SAPHIR -> "Saphir"
    BadgeTier.DIAMANT -> "Diamant"
    BadgeTier.PLATIN -> "Krone"
}

/**
 * Gemaltes Einzel-Bild des neu gewonnenen Schatzes einer Stufe (für die Ordensverleihung):
 * der konkrete Gegenstand allein (Münzhaufen/Edelstein/Krone), nicht die ganze Truhe.
 * Herkunft/Prompts/Seeds siehe `ASSETS.md`.
 */
@DrawableRes
fun rewardDrawable(tier: BadgeTier): Int = when (tier) {
    BadgeTier.BRONZE -> R.drawable.reward_coin_bronze
    BadgeTier.SILBER -> R.drawable.reward_coin_silver
    BadgeTier.GOLD -> R.drawable.reward_coin_gold
    BadgeTier.SAPHIR -> R.drawable.reward_gem_saphir
    BadgeTier.DIAMANT -> R.drawable.reward_gem_diamant
    BadgeTier.PLATIN -> R.drawable.reward_tiara
}

/**
 * Gemaltes Truhen-Bild für die erreichte Stufe. Sieben eigenständige KI-Illustrationen
 * derselben Truhe, die von Stufe zu Stufe sichtbar voller/reicher wird (leer → Bronze- →
 * Silber- → Goldmünzen → +Saphir → +Diamant → +Krone). [tier] == null = leere Truhe.
 * Herkunft/Prompts/Seeds siehe `ASSETS.md`.
 */
@DrawableRes
fun chestStageDrawable(tier: BadgeTier?): Int = when (tier) {
    null -> R.drawable.chest_stage_0
    BadgeTier.BRONZE -> R.drawable.chest_stage_1
    BadgeTier.SILBER -> R.drawable.chest_stage_2
    BadgeTier.GOLD -> R.drawable.chest_stage_3
    BadgeTier.SAPHIR -> R.drawable.chest_stage_4
    BadgeTier.DIAMANT -> R.drawable.chest_stage_5
    BadgeTier.PLATIN -> R.drawable.chest_stage_6
}

/**
 * Die sich füllende Schatztruhe als gemaltes Bild der aktuell erreichten Stufe. Die Ränder
 * werden – wie bei den Insel-Kacheln – per [featherEdges] weich in den Pergament-Hintergrund
 * ausgeblendet. [highestTier] == null zeigt die leere Truhe.
 */
@Composable
fun TreasureChest(
    highestTier: BadgeTier?,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
) {
    Image(
        painter = painterResource(chestStageDrawable(highestTier)),
        contentDescription = null,
        modifier = modifier.size(size).featherEdges(),
        contentScale = ContentScale.Fit,
    )
}

/**
 * Ein frisch gewonnener Schatz für die Ordensverleihung: das gemalte Einzel-Bild des neuen
 * Gegenstands (Münzhaufen/Edelstein/Krone) mit dem Namen des neuen Schatzes darunter.
 */
@Composable
fun RewardItem(
    tier: BadgeTier,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(rewardDrawable(tier)),
            contentDescription = rewardLabel(tier),
            modifier = Modifier.size(size).featherEdges(),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = rewardLabel(tier),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tierColor(tier),
            textAlign = TextAlign.Center,
        )
    }
}
