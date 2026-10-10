package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Für welches Kind ein Eltern-Bildschirm gerade gilt. Erscheint erst, wenn es überhaupt eine Wahl
 * gibt – bei einem einzigen Profil wäre eine Reihe mit einem Knopf nur Beiwerk.
 *
 * [testTagPrefix] hält die Testmarken je Bildschirm auseinander (`freigabe_profil_Emma`,
 * `uebungskalender_profil_Emma`).
 */
@Composable
internal fun ProfilWahl(
    namen: List<String>,
    gewaehlt: String?,
    testTagPrefix: String,
    onWaehle: (String) -> Unit,
) {
    Text(text = "Für wen?", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (name in namen) {
            FilterChip(
                selected = name == gewaehlt,
                onClick = { onWaehle(name) },
                label = { Text(name) },
                modifier = Modifier.testTag("$testTagPrefix$name"),
            )
        }
    }
}

/**
 * Welches Profil ein Eltern-Bildschirm zeigt.
 *
 * Vorbelegt ist das Kind, das gerade spielt ([aktivesProfil]) – wer den Bildschirm öffnet, meint
 * meistens dieses. Eine eigene Wahl ([handverlesen]) hat Vorrang, ist aber bewusst getrennt vom
 * aktiven Profil: Erwachsene schauen hier auch für ein Geschwisterkind nach, ohne dass dessen
 * Profil dadurch das aktive wird. Ein gelöschtes oder umbenanntes Profil fällt auf das erste
 * zurück.
 */
internal fun bestimmeGewaehltesProfil(
    namen: List<String>,
    handverlesen: String?,
    aktivesProfil: String?,
): String? = handverlesen?.takeIf { it in namen }
    ?: aktivesProfil?.takeIf { it in namen }
    ?: namen.firstOrNull()
