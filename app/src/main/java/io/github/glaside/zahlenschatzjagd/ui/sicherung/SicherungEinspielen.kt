package io.github.glaside.zahlenschatzjagd.ui.sicherung

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.io.IOException

/**
 * Das Einspielen einer Sicherung, wie es **beide** Wege dorthin brauchen: der Elternbereich und
 * der Erststart ohne Profil. Es steht hier und nicht zweimal, weil sonst auch die drei
 * Fehlerzweige doppelt stünden – und genau dort verrottet Doppeltes.
 *
 * Zurück kommt die Aktion, die den Dateidialog öffnet; das Ergebnis landet als Meldung im
 * [viewModel] und wird mit [SicherungMeldung] angezeigt.
 */
@Composable
fun rememberSicherungEinspielen(viewModel: BackupViewModel): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackup { context.readText(uri) }
        }
    }
    return {
        // Der Doppelklick-Schutz sitzt hier und nicht nur am Knopf: Ein zweiter Dateidialog,
        // während der erste noch eingelesen wird, räumte die Meldung weg – und das Ergebnis der
        // zweiten Wahl fiele in [BackupViewModel.restoreBackup] still hinten runter. Der Nutzer
        // hätte eine Datei ausgewählt und bekäme weder Daten noch Rückmeldung.
        if (!viewModel.busy.value) {
            viewModel.clearMessage()
            // Bewusst kein enger MIME-Filter: Manche Dateimanager melden .json-Dateien als
            // "application/octet-stream", die Sicherung wäre dann gar nicht auswählbar. Der
            // Inhalt wird ohnehin geprüft und eine falsche Datei freundlich abgelehnt
            // (`docs/adr/0010-dateidialog-der-sicherung-ohne-mime-filter.md`).
            launcher.launch(arrayOf("*/*"))
        }
    }
}

internal fun Context.readText(uri: Uri): String {
    val stream = contentResolver.openInputStream(uri)
        ?: throw IOException("Kein Lesezugriff auf die gewählte Datei")
    return stream.use { it.readBytes().decodeToString() }
}

/** Rückmeldung nach dem Sichern/Einspielen – Fehler heben sich farblich ab. */
@Composable
fun SicherungMeldung(message: BackupMessage, modifier: Modifier = Modifier) {
    Surface(
        color = if (message.isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .testTag("sicherung_meldung")
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (message.isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.padding(12.dp)
        )
    }
}
