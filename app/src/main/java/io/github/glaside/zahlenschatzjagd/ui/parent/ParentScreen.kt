package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Der Elternbereich als **Übersicht**: sechs Einträge, jeder auf einem eigenen Bildschirm.
 *
 * Bewusst schlichtes Material statt der Insel-Optik der Kinder-Bildschirme – dieser Teil der App
 * richtet sich an Erwachsene, und er wird gelesen, nicht angetippt wie eine Schatzkarte.
 *
 * Erreichbar ist er nur hinter der Elternfrage ([AdultGate]) oder, sobald eine gesetzt ist,
 * hinter der PIN ([ParentPinGate]).
 */
@Composable
fun ParentScreen(
    onOpenEntry: (ParentEntry) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("parent_back_button")) {
            Text("← Zurück")
        }
        Spacer(Modifier.height(8.dp))
        Text(text = "⚓", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Für Eltern",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))

        for (entry in ParentEntry.entries) {
            EntryRow(entry = entry, onClick = { onOpenEntry(entry) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Die sechs Einträge des Elternbereichs. Reihenfolge und Beschriftung stammen aus `CONTEXT.md`.
 *
 * **Ein Eintrag steht an genau einer Stelle.** [slug] trägt sowohl das Navigationsziel als auch
 * die Testmarke. Vorher standen dieselben Einträge zusätzlich in einer `when`-Kaskade und einer
 * Ausschlussliste; wer eine davon vergaß, registrierte dieselbe Route zweimal. Bis zum Übungsstand
 * gab es hier zusätzlich ein `showsPlaceholder`, das den noch leeren Einträgen einen gemeinsamen
 * „entsteht gerade"-Bildschirm gab; seit alle sechs gebaut sind, hat jeder sein eigenes Ziel.
 *
 * **[ZUGANG] steht an erster Stelle.** Er war einmal kein Eintrag, sondern ein Textblock über der
 * Liste – die Tür gehört nicht neben die Räume, hieß es. Mit dem Schalter für den Profilwechsel
 * wuchs der Block aber auf 43 % der Bildschirmhöhe, und von der Liste blieb beim Öffnen nur noch
 * der obere Rand des ersten Eintrags sichtbar. Als erste Zeile trägt er dieselbe Sorge (wer die
 * PIN nicht sucht, findet sie trotzdem) und lässt die Übersicht wieder eine Übersicht sein
 * (`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`).
 */
enum class ParentEntry(
    val icon: String,
    val title: String,
    val hint: String,
    val slug: String,
) {
    ZUGANG(
        icon = "🔒",
        title = "Zugang",
        hint = "Elternfrage oder PIN, und wer das Profil wechseln darf.",
        slug = "zugang",
    ),
    UEBUNGSSTAND(
        icon = "🧭",
        title = "Übungsstand",
        hint = "Was zuletzt geübt wurde und wie sicher es sitzt.",
        slug = "uebungsstand",
    ),
    UEBUNGSKALENDER(
        icon = "📅",
        title = "Übungskalender",
        hint = "An welchen Tagen und wie lange geübt wurde.",
        slug = "uebungskalender",
    ),
    FREIGABE(
        icon = "🗝️",
        title = "Freigabe",
        hint = "Was ein Kind üben darf.",
        slug = "freigabe",
    ),
    TON_UND_VORLESEN(
        icon = "🔊",
        title = "Ton & Vorlesen",
        hint = "Töne und die Vorlesestimme an- und ausschalten.",
        slug = "ton",
    ),
    SICHERUNG(
        icon = "💾",
        title = "Sicherung",
        hint = "Fortschritt als Datei speichern und wieder einspielen.",
        slug = "sicherung",
    ),
}

/** Ein Eintrag der Übersicht als großflächig antippbare Zeile. */
@Composable
private fun EntryRow(entry: ParentEntry, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("parent_entry_${entry.slug}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = entry.icon, fontSize = 28.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = entry.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(text = "›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/**
 * Der gemeinsame Rahmen aller Unter-Bildschirme des Elternbereichs: Zurück-Button, Überschrift,
 * scrollender Inhalt. Hält die sechs Bildschirme optisch zusammen, ohne dass jeder sein eigenes
 * Gerüst aufbaut.
 */
@Composable
internal fun ParentPage(
    title: String,
    onBack: () -> Unit,
    backTestTag: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, modifier = Modifier.testTag(backTestTag)) {
            Text("← Zurück")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** Eine Zwischenüberschrift innerhalb eines Eltern-Bildschirms. */
@Composable
internal fun Abschnitt(titel: String) {
    Text(text = titel, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
}

/**
 * Ein Schalter mit Beschriftung und Erklärung darunter – die Bauform, in der der Elternbereich
 * eine Ja/Nein-Einstellung stellt ([ZugangScreen], [TonUndVorlesenScreen]).
 *
 * Die Beschriftung steht **neben** dem Schalter, die Erklärung über die volle Breite darunter.
 * Zusammen in einer Spalte bricht die Beschriftung bei Anzeigegröße „groß" um, und ihre zweite
 * Zeile klebt ohne Abstand auf der Erklärung.
 *
 * Die [erklaerung] ist ein Inhalts-Block und kein Text, weil sie je nach Bildschirm aus einem
 * oder zwei Absätzen besteht. Nicht zu verwechseln mit dem `Schalter` der [FreigabeScreen]: Der
 * zeigt eine **Checkbox** in einer langen Liste, hier steht eine einzelne Einstellung.
 */
@Composable
internal fun SchalterZeile(
    beschriftung: String,
    an: Boolean,
    testTag: String,
    onSetze: (Boolean) -> Unit,
    erklaerung: @Composable ColumnScope.() -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = beschriftung,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = an,
            onCheckedChange = onSetze,
            modifier = Modifier.testTag(testTag),
        )
    }
    Spacer(Modifier.height(8.dp))
    Column(content = erklaerung)
}
