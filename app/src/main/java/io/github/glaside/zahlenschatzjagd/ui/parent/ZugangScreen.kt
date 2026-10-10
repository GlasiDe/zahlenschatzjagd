package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Der Eintrag **Zugang** des Elternbereichs: die Tür und wer durch sie muss.
 *
 * Zwei Dinge, die zusammengehören: die optionale PIN
 * (`docs/adr/0016-pin-optional-neben-der-elternfrage.md`) und der Schalter, ob auch der
 * Profilwechsel durch dieselbe Tür muss
 * (`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`). Beide beschreiben denselben Zugang, beide
 * gelten für dieses Gerät und nicht für ein einzelnes Kind – anders als die [FreigabeScreen].
 */
@Composable
fun ZugangScreen(
    zugang: ParentZugang,
    onManagePin: () -> Unit,
    onRemovePin: () -> Unit,
    onSetzeProfilwechsel: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var askRemovePin by remember { mutableStateOf(false) }

    ParentPage(
        title = "🔒 Zugang",
        onBack = onBack,
        backTestTag = "zugang_back_button",
    ) {
        PinAbschnitt(
            pinIsSet = zugang.pinIsSet,
            onManagePin = onManagePin,
            onRemovePin = { askRemovePin = true },
        )
        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))
        ProfilwechselSchalter(
            profilwechselOhnePin = zugang.profilwechselOhnePin,
            onSetze = onSetzeProfilwechsel,
        )
    }

    if (askRemovePin) {
        RemovePinDialog(
            onConfirm = {
                askRemovePin = false
                onRemovePin()
            },
            onDismiss = { askRemovePin = false },
        )
    }
}

/** Die optionale PIN: einrichten, ändern, entfernen. */
@Composable
private fun PinAbschnitt(pinIsSet: Boolean, onManagePin: () -> Unit, onRemovePin: () -> Unit) {
    Text(
        text = if (pinIsSet) {
            "Dieser Bereich ist mit einer PIN gesichert."
        } else {
            "Dieser Bereich ist durch die Rechenaufgabe geschützt. Sobald ein Kind sicher " +
                "rechnet, reicht sie nicht mehr – dann hilft eine PIN."
        },
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onManagePin,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_pin_manage_button")
    ) {
        Text(if (pinIsSet) "🔒 PIN ändern" else "🔒 PIN einrichten")
    }
    if (pinIsSet) {
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onRemovePin,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("parent_pin_remove_button")
        ) {
            Text("PIN entfernen")
        }
    }
}

/**
 * Der Schalter, der das Wechseln vom Verwalten trennt: Anlegen, Umbenennen, Avatar und Löschen
 * liegen immer hinter dem Zugang, das bloße Wechseln je nach Einstellung
 * (`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`).
 *
 * Die Bauform – Beschriftung neben dem Schalter, Erklärung darunter – steckt in [SchalterZeile];
 * dort steht auch, warum sie so aussieht.
 */
@Composable
private fun ProfilwechselSchalter(profilwechselOhnePin: Boolean, onSetze: (Boolean) -> Unit) {
    SchalterZeile(
        beschriftung = "Profilwechsel ohne PIN erlauben",
        an = profilwechselOhnePin,
        testTag = "parent_profilwechsel_switch",
        onSetze = onSetze,
    ) {
        Text(
            text = if (profilwechselOhnePin) {
                "Die Kinder wechseln ihr Profil selbst. Wer sich vertippt, übt unter fremdem " +
                    "Namen – Übungszeit und Fortschritt landen dann beim falschen Kind."
            } else {
                "Auch das Wechseln verlangt die PIN – oder die Rechenaufgabe, solange keine " +
                    "PIN gesetzt ist."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Anlegen, Umbenennen, Avatar ändern und Löschen sind immer geschützt.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun RemovePinDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN entfernen?") },
        text = {
            Text(
                "Der Elternbereich ist danach wieder nur durch die Rechenaufgabe geschützt. " +
                    "Eine neue PIN lässt sich jederzeit vergeben."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("parent_pin_remove_confirm")
            ) { Text("Entfernen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/**
 * Wie der Zugang auf diesem Gerät geregelt ist – die beiden Angaben, die dieser Bildschirm zeigt.
 * Sie reisen ohnehin gemeinsam: Beide kommen aus demselben DataStore und beschreiben dieselbe Tür.
 *
 * Frei von Compose, aber neben der Oberfläche statt in `data/`, weil nur sie es liest – wie
 * [io.github.glaside.zahlenschatzjagd.ui.sicherung.BackupMessage] (`docs/agents/testing.md`, „Der
 * Schnitt bestimmt, wo Code liegt").
 */
data class ParentZugang(
    /** Ob eine PIN gesetzt ist; sonst gilt die Elternfrage. */
    val pinIsSet: Boolean,
    /** Ob der Profilwechsel ohne das Tor möglich bleibt. */
    val profilwechselOhnePin: Boolean,
)
