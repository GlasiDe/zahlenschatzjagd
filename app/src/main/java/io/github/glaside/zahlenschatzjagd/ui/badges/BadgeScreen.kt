package io.github.glaside.zahlenschatzjagd.ui.badges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.BADGE_TABS
import io.github.glaside.zahlenschatzjagd.truhenBeschriftung

/**
 * Übersicht aller Abzeichen des aktiven Nutzers als **Schatztruhen**, aufgeteilt in **Tabs
 * je Rechenart/Spiel**. Der aktive Tab zeigt für jeden Zahlenraum eine sich füllende Truhe
 * plus einen knappen Fortschrittstext. Es gibt keine Vorschau auf den nächsten Schatz – die
 * noch nicht volle Truhe zeigt selbst, dass es weitergeht.
 */
@Composable
fun BadgeScreen(
    viewModel: BadgeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("badges_back_button")) {
            Text("← Zurück")
        }

        Text(
            text = "Deine Schatztruhen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (state.loading) {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator()
            return@Column
        }

        Spacer(Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            BADGE_TABS.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text("${tab.label} ${tab.symbol}") },
                    modifier = Modifier.testTag("badge_tab_${tab.key}")
                )
            }
        }

        val selectedTabInfo = BADGE_TABS[selectedTab]
        val cells = state.cells.filter { it.category.key in selectedTabInfo.categoryKeys }

        for (cell in cells) {
            Spacer(Modifier.height(20.dp))
            ChestRow(cell)
        }
    }
}

/**
 * Eine Truhe für eine (Kategorie × Zahlenraum)-Kombination: gefüllt bis zur aktuell höchsten
 * Stufe, daneben ihre Beschriftung (siehe `truhenBeschriftung`) und ein kurzer Fortschrittstext.
 */
@Composable
private fun ChestRow(cell: BadgeCell) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TreasureChest(highestTier = cell.currentTier, size = 120.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = truhenBeschriftung(cell.category, cell.numberRange),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(2.dp))
            val currentTier = cell.currentTier
            Text(
                text = if (currentTier == null) {
                    "Noch leer – los geht's!"
                } else {
                    "Truhe: ${tierLabel(currentTier)}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = tierColor(currentTier)
            )
            Spacer(Modifier.height(2.dp))
            val nextTier = cell.nextTier
            Text(
                text = if (nextTier == null) {
                    "🏆 randvoll!"
                } else {
                    "noch ${nextTier.threshold - cell.ertrag} bis zum nächsten Schatz"
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Start
            )
        }
    }
}
