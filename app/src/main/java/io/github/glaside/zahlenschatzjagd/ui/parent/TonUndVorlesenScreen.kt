package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Der Eintrag **Ton & Vorlesen** des Elternbereichs: zwei Schalter für das, was aus dem Gerät
 * herauskommt.
 *
 * Beide gelten **für dieses Gerät und nicht für ein einzelnes Kind** – die einzige Ausnahme von
 * der Regel, dass Einstellungen am Profil hängen
 * (`docs/adr/0024-ton-und-vorlesen-am-geraet.md`). Sie stehen damit neben dem [ZugangScreen] und
 * nicht neben der [FreigabeScreen], die je Kind etwas anderes sagen kann.
 */
@Composable
fun TonUndVorlesenScreen(
    einstellung: TonUndVorlesen,
    onSetzeTon: (Boolean) -> Unit,
    onSetzeVorlesen: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    ParentPage(
        title = "🔊 Ton & Vorlesen",
        onBack = onBack,
        backTestTag = "ton_back_button",
    ) {
        Text(
            text = "Beides gilt für dieses Gerät – nicht für ein einzelnes Kind. „Ton aus\" " +
                "beschreibt den Ort (Wartezimmer, Auto, Schule) und muss deshalb nicht für " +
                "jedes Geschwisterkind einzeln umgelegt werden.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        SchalterZeile(
            beschriftung = "Töne abspielen",
            an = einstellung.tonAn,
            testTag = "ton_switch",
            onSetze = onSetzeTon,
        ) {
            Text(
                text = if (einstellung.tonAn) {
                    "Ein kurzer Ton sagt dem Kind, ob eine Aufgabe richtig war – und einer am " +
                        "Ende der Runde, dass es geschafft ist."
                } else {
                    "Die Runde läuft stumm. Richtig und falsch stehen weiterhin auf dem " +
                        "Bildschirm."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))
        SchalterZeile(
            beschriftung = "Ella liest vor",
            an = einstellung.vorlesenAn,
            testTag = "vorlesen_switch",
            onSetze = onSetzeVorlesen,
        ) {
            Text(
                text = if (einstellung.vorlesenAn) {
                    "Das Maskottchen liest die Beschriftungen vor, damit sich auch ein Kind " +
                        "ohne sichere Lesefähigkeit allein zurechtfindet. Fehlt die " +
                        "Sprachausgabe auf diesem Gerät, bleibt die App still – ohne " +
                        "Fehlermeldung."
                } else {
                    "Ella bleibt still. Ein Kind, das noch nicht sicher liest, braucht dann " +
                        "Hilfe beim Suchen."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Was auf diesem Gerät zu hören ist – die beiden Angaben, die dieser Bildschirm zeigt. Sie reisen
 * gemeinsam: Beide kommen aus demselben DataStore und beschreiben dasselbe Gerät.
 *
 * Frei von Compose, aber neben der Oberfläche statt in `data/`, weil nur sie es liest – wie
 * [ParentZugang] (`docs/agents/testing.md`, „Der Schnitt bestimmt, wo Code liegt").
 */
data class TonUndVorlesen(
    /** Ob die Töne der Runde zu hören sind. */
    val tonAn: Boolean,
    /** Ob das Maskottchen vorliest. */
    val vorlesenAn: Boolean,
)
