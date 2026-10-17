package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.BADGE_CATEGORIES
import io.github.glaside.zahlenschatzjagd.BADGE_RANGES
import io.github.glaside.zahlenschatzjagd.BADGE_TABS
import io.github.glaside.zahlenschatzjagd.BadgeCategory
import io.github.glaside.zahlenschatzjagd.BadgeTab
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.util.umschaltbar

/**
 * Der Eintrag **Freigabe** des Elternbereichs: Was ein Kind üben darf.
 *
 * Zwei Listen, wie das Modell selbst (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`):
 * die Fortschrittskennungen – gruppiert nach den Reitern der Abzeichen-Übersicht, damit hier
 * dieselbe Einteilung gilt wie dort – und darunter die vier Zahlenräume, die überall dort wirken,
 * wo das Kind den Zahlenraum selbst wählt. Den Erwachsenen gegenüber heißen die Kennungen
 * schlicht „Übungen"; das Wort Fortschrittskennung gehört ins Projekt, nicht auf einen Bildschirm
 * für Eltern.
 *
 * **Ohne Untergrenze**: Alles abzuwählen ist erlaubt und wird nur erklärt, nicht verhindert.
 *
 * Zur offenen Frage aus dem Vorhaben, ob 12 Kennungen plus 4 Zahlenräume auf einem Bildschirm
 * handhabbar bleiben: ja. Sechs der acht Reiter tragen genau eine Kennung und damit genau einen
 * Schalter; nur Verdoppeln und Zahlenmauer klappen ihre Stufen auf. Ein Gruppenschalter je
 * Reiter oder ein „alles an/aus" wären zusätzliche Bedienelemente für einen Fall, den es in
 * einer Familie einmal gibt – beim Einrichten.
 */
@Composable
fun FreigabeScreen(
    viewModel: FreigabeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ParentPage(
        title = "🗝️ Freigabe",
        onBack = onBack,
        backTestTag = "freigabe_back_button",
    ) {
        val freigabe = state.freigabe
        if (freigabe == null) {
            Text(
                text = "Sobald ein Kind ein Profil hat, lässt sich hier einstellen, was es üben darf.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("freigabe_ohne_profil"),
            )
            return@ParentPage
        }

        Text(
            text = "Ein Kind sieht nur, was in beidem freigegeben ist: die Übung und ihr " +
                "Zahlenraum. Alles andere erscheint ihm gar nicht erst.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        if (state.profile.size > 1) {
            ProfilWahl(
                namen = state.profile.map { it.name },
                gewaehlt = state.gewaehltesProfil,
                testTagPrefix = "freigabe_profil_",
                onWaehle = viewModel::waehleProfil,
            )
            Spacer(Modifier.height(16.dp))
        }

        Abschnitt("Übungen")
        for (tab in BADGE_TABS) {
            ReiterGruppe(
                tab = tab,
                freigabe = freigabe,
                onSetze = viewModel::setzeKennung,
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        ZahlenraumAbschnitt(freigabe = freigabe, onSetze = viewModel::setzeZahlenraum)

        if (freigabe.erlaubtNichts()) LeerHinweis()
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Die vier Zahlenräume. Sie wirken nur auf Kennungen, bei denen das Kind den Zahlenraum selbst
 * wählt; Kennungen mit festem hängen allein an ihrem Kästchen
 * (`docs/adr/0026-zahlenraum-wirkt-nur-wo-gewaehlt-wird.md`).
 */
@Composable
private fun ZahlenraumAbschnitt(freigabe: Freigabe, onSetze: (Int, Boolean) -> Unit) {
    Abschnitt("Zahlenräume")
    Text(
        text = "Der Zahlenraum gilt dort, wo das Kind ihn selbst wählt. Verdoppeln, Verliebte " +
            "Zahlen und die Zahlenmauer rechnen in einem festen Zahlenraum – sie stellst du " +
            "oben über ihr eigenes Kästchen ein.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(4.dp))
    for (zahlenraum in BADGE_RANGES) {
        Schalter(
            label = "bis $zahlenraum",
            checked = zahlenraum in freigabe.zahlenraeume,
            testTag = "freigabe_zahlenraum_$zahlenraum",
            onCheckedChange = { onSetze(zahlenraum, it) },
        )
    }
}

/** Gesagt, nicht verhindert: Alles abzuwählen ist ein erlaubter Zustand. */
@Composable
private fun LeerHinweis() {
    Spacer(Modifier.height(16.dp))
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = "Gerade ist nichts zum Üben freigegeben. Auf der Startkarte bleiben " +
                "für dieses Kind nur noch die Abzeichen und Für Eltern.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(12.dp)
                .testTag("freigabe_leer_hinweis"),
        )
    }
}

/**
 * Ein Reiter der Abzeichen-Übersicht als Gruppe. Die meisten tragen genau eine
 * Fortschrittskennung – dann steht dort nur ein Schalter. „Verdoppeln" und „Zahlenmauer" bündeln
 * mehrere Stufen; die stehen eingerückt darunter, mit der Beschriftung ihrer Schatztruhe.
 */
@Composable
private fun ReiterGruppe(
    tab: BadgeTab,
    freigabe: Freigabe,
    onSetze: (String, Boolean) -> Unit,
) {
    val kategorien = tab.categoryKeys.mapNotNull { key -> BADGE_CATEGORIES.firstOrNull { it.key == key } }
    if (kategorien.size == 1) {
        val kategorie = kategorien.single()
        Schalter(
            label = "${tab.symbol}  ${tab.label}",
            checked = kategorie.key in freigabe.kennungen,
            testTag = "freigabe_kennung_${kategorie.key}",
            hinweis = zahlenraumHinweis(kategorie, freigabe),
            onCheckedChange = { onSetze(kategorie.key, it) },
        )
        return
    }
    Text(
        text = "${tab.symbol}  ${tab.label}",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
    for (kategorie in kategorien) {
        Schalter(
            label = kategorie.chestLabel ?: kategorie.label,
            checked = kategorie.key in freigabe.kennungen,
            testTag = "freigabe_kennung_${kategorie.key}",
            hinweis = zahlenraumHinweis(kategorie, freigabe),
            eingerueckt = true,
            onCheckedChange = { onSetze(kategorie.key, it) },
        )
    }
}

/**
 * Erklärt den Fall, der sonst wie ein Fehler aussähe: Der Haken steht, die Übung ist trotzdem
 * weg, weil alle ihre Zahlenräume abgewählt sind. Bei Übungen mit festem Zahlenraum kommt das
 * nicht mehr vor – sie hängen allein am Haken (ADR-0026).
 */
private fun zahlenraumHinweis(kategorie: BadgeCategory, freigabe: Freigabe): String? =
    if (kategorie.key in freigabe.kennungen && !freigabe.erlaubt(kategorie.key)) {
        val zahlenraeume = kategorie.ranges.joinToString(" oder ") { "bis $it" }
        "Zurzeit gesperrt – dafür fehlt der Zahlenraum $zahlenraeume."
    } else {
        null
    }

/** Eine Zeile mit Kästchen, Beschriftung und optionaler Erklärung darunter. */
@Composable
private fun Schalter(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    hinweis: String? = null,
    eingerueckt: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .umschaltbar(checked, Role.Checkbox, testTag, onCheckedChange),
    ) {
        if (eingerueckt) Spacer(Modifier.width(24.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.minimumInteractiveComponentSize(),
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontSize = 16.sp)
            if (hinweis != null) {
                Text(
                    text = hinweis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
