package io.github.glaside.zahlenschatzjagd.ui.aenderungshinweis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.glaside.zahlenschatzjagd.data.Aenderungshinweis
import io.github.glaside.zahlenschatzjagd.data.AppContainer
import io.github.glaside.zahlenschatzjagd.data.faelligeAenderungshinweise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Was Ella zum Kind sagt, wenn nach einem Update ein Änderungshinweis erscheint. Bewusst **nicht**
 * der Hinweis selbst – der richtet sich an Erwachsene. Das Kind soll nur merken, dass etwas anders
 * ist, und jemanden holen.
 */
const val AENDERUNGSHINWEIS_GESPROCHEN = "Hier ist etwas neu. Zeig das mal deinen Eltern!"

/**
 * Steht einmal unter allen Hinweisen, gleich was sie enthalten: Eine neue Übung ist für bestehende
 * Profile sofort freigegeben (`docs/adr/0018-freigabe-als-sperre-gespeichert.md`), und genau das
 * sollen Eltern nach einem automatischen Update erfahren.
 */
const val AENDERUNGSHINWEIS_FREIGABE =
    "Neue Übungen sind für alle Kinder sofort offen. Abwählen lassen sie sich im Elternbereich " +
        "unter Freigabe."

/**
 * Die Änderungshinweise, die jetzt anstehen. Hängt an zwei Quellen: dem gemerkten Stand und daran,
 * ob es Profile gibt – ohne beides lässt sich eine Neuinstallation nicht von einem Update auf die
 * erste Version unterscheiden, die sich überhaupt etwas merkt (siehe [faelligeAenderungshinweise]).
 */
@Composable
fun faelligeHinweiseFlow(
    container: AppContainer,
    hinweise: List<Aenderungshinweis>,
    aktuelleVersion: Int,
): Flow<List<Aenderungshinweis>> =
    remember(container, hinweise, aktuelleVersion) {
        combine(
            container.aenderungshinweisRepository.zuletztGezeigt,
            container.userProfileRepository.profiles,
        ) { zuletztGezeigt, profile ->
            faelligeAenderungshinweise(
                zuletztGezeigt = zuletztGezeigt,
                aktuelleVersion = aktuelleVersion,
                profileVorhanden = profile.isNotEmpty(),
                hinweise = hinweise,
            )
        }.distinctUntilChanged()
    }

/**
 * Die Änderungshinweise nach einem Update, der neueste oben. Jeder darf ihn schließen – ein Tor
 * davor sperrte das Kind aus, bis ein Erwachsener kommt. Ein Tipp daneben schließt ihn nicht:
 * Kinder tippen viel, und der Hinweis soll erst weg, wenn jemand ihn bewusst wegtippt.
 */
@Composable
fun AenderungshinweisDialog(hinweise: List<Aenderungshinweis>, onVerstanden: () -> Unit) {
    AlertDialog(
        onDismissRequest = onVerstanden,
        properties = DialogProperties(dismissOnClickOutside = false),
        modifier = Modifier.testTag("aenderungshinweis_dialog"),
        text = {
            // Scrollbar, weil übersprungene Versionen mehrere Hinweise bringen und bei großer
            // Anzeigegröße schon einer den Bildschirm füllen kann.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                hinweise.forEach { hinweis ->
                    Text(
                        text = "Neu in Version ${hinweis.versionName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(hinweis.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    text = AENDERUNGSHINWEIS_FREIGABE,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onVerstanden,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("aenderungshinweis_verstanden"),
            ) { Text("Verstanden") }
        },
    )
}
