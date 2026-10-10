package io.github.glaside.zahlenschatzjagd

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.ui.badges.RewardItem
import io.github.glaside.zahlenschatzjagd.ui.badges.TreasureChest
import org.junit.Rule
import org.junit.Test

/**
 * Smoke-Test der gemalten Truhen-Bildserie: stellt sicher, dass `TreasureChest` für **jede**
 * Stufe (leer + Bronze…Platin) und `RewardItem` für jede Stufe ohne Absturz rendern und
 * damit alle sieben `chest_stage_*`-Drawables aufgelöst werden.
 */
class BadgeVisualsRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun treasureChest_rendersEveryTier() {
        val tiers: List<BadgeTier?> = listOf(null) + BadgeTier.entries.toList()
        composeRule.setContent {
            Row { for (tier in tiers) TreasureChest(highestTier = tier, size = 96.dp) }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun rewardItem_rendersEveryTier() {
        composeRule.setContent {
            Row { for (tier in BadgeTier.entries) RewardItem(tier = tier, size = 64.dp) }
        }
        composeRule.waitForIdle()
    }
}
