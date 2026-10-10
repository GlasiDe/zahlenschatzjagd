package io.github.glaside.zahlenschatzjagd.ui.sicherung

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.ui.parent.ParentPage
import java.io.IOException
import java.time.LocalDate

/**
 * Die Sicherung des Fortschritts – einer der Einträge des Elternbereichs: als Datei exportieren
 * und wieder einspielen, gedacht für Handywechsel, Neuinstallation und als regelmäßige Sicherung.
 *
 * Dass man diesen Bildschirm über den Elternbereich erreicht, ist Navigation und keine
 * Zugehörigkeit: Er ist der Bildschirm der Sicherung. Das Einspielen gibt es deshalb auch ohne
 * ihn – beim Erststart ohne Profil (siehe [rememberSicherungEinspielen]).
 *
 * Der Speicherort wird über den Android-Dateidialog gewählt (Storage Access Framework); die App
 * braucht dadurch keinerlei Speicherberechtigung.
 */
@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var askRestore by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.createBackup { text -> context.writeText(uri, text) }
        }
    }

    val einspielen = rememberSicherungEinspielen(viewModel)

    ParentPage(title = "Sicherung", onBack = onBack, backTestTag = "parent_backup_back_button") {
        BackupSection(
            busy = busy,
            onExport = { exportLauncher.launch(defaultBackupFileName()) },
            onImport = { askRestore = true },
        )
        message?.let {
            Spacer(Modifier.height(16.dp))
            SicherungMeldung(it)
        }
    }

    if (askRestore) {
        RestoreConfirmDialog(
            onConfirm = {
                askRestore = false
                einspielen()
            },
            onDismiss = { askRestore = false }
        )
    }
}

private fun Context.writeText(uri: Uri, text: String) {
    val stream = contentResolver.openOutputStream(uri)
        ?: throw IOException("Kein Schreibzugriff auf die gewählte Datei")
    stream.use { it.write(text.toByteArray()) }
}

/** Erklärung und die beiden Aktionen rund um die Sicherung. */
@Composable
private fun BackupSection(busy: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    Text(
        text = "Speichert alle Profile und gerechneten Aufgaben in einer Datei. " +
            "Praktisch beim Wechsel auf ein neues Handy oder vor einer Neuinstallation.",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onExport,
        enabled = !busy,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_export_button")
    ) {
        Text("💾 Sicherung erstellen")
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onImport,
        enabled = !busy,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_import_button")
    ) {
        Text("📥 Sicherung einspielen")
    }
}

/**
 * Die Rückfrage vor dem Einspielen. Sie steht **hier** und nicht im gemeinsamen Baustein: Im
 * Elternbereich gibt es Fortschritt, der schon dasteht, und die Frage erklärt, was mit ihm
 * geschieht. Beim Erststart ohne Profil gäbe es dazu nichts zu sagen.
 */
@Composable
private fun RestoreConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sicherung einspielen?") },
        text = {
            Text(
                "Der vorhandene Fortschritt bleibt erhalten und wird um die Daten aus der " +
                    "Sicherung ergänzt. Profile mit gleichem Namen werden zusammengeführt."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("parent_import_confirm")
            ) { Text("Datei auswählen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/** z. B. `zahlenschatzjagd-sicherung-2026-07-26.json` – nach Datum sortierbar. */
internal fun defaultBackupFileName(today: LocalDate = LocalDate.now()): String =
    "zahlenschatzjagd-sicherung-$today.json"
