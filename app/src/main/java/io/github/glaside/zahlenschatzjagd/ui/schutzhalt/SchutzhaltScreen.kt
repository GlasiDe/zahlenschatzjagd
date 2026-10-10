package io.github.glaside.zahlenschatzjagd.ui.schutzhalt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.data.Datenbankstart
import io.github.glaside.zahlenschatzjagd.ui.VorlesenBeimErstenErscheinen
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchmentMuted
import io.github.glaside.zahlenschatzjagd.ui.theme.ParchmentBackground

/**
 * Der **Schutzhalt**: Die App kann den vorgefundenen Fortschritt nicht sicher weiterverwenden
 * und fasst ihn deshalb nicht an (`docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`).
 *
 * Dieser Bildschirm hat zwei Adressaten. Das Kind hat die App geöffnet, liest aber nicht: Für
 * es spricht Ella einen Satz und es sieht eine **gefüllte** Truhe. Die Wahl ist Absicht – die
 * leere Truhe der Stufe 0 sagt in der Bildsprache dieser App „hier ist nichts", und das ist
 * genau die Angst, die dieser Bildschirm nehmen soll.
 *
 * Bewusst **ohne jeden Knopf**: Es gibt hier nichts zu entscheiden, und jeder Knopf wäre einer,
 * den ein Kind drückt. Der Weg heraus führt über ein neues Release, nicht über diesen Bildschirm.
 */
@Composable
fun SchutzhaltScreen(schutzhalt: Datenbankstart.Schutzhalt, appVersion: String) {
    val meldung = schutzhaltMeldung(schutzhalt, appVersion)

    VorlesenBeimErstenErscheinen(meldung.gesprochen)

    ParchmentBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .testTag("schutzhalt"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.chest_stage_1),
                contentDescription = null,
                modifier = Modifier.size(140.dp),
            )
            FuerDenErwachsenen(meldung)
        }
    }
}

/** Alles Geschriebene – das Kind hat mit diesem Teil nichts zu tun. */
@Composable
private fun FuerDenErwachsenen(meldung: SchutzhaltMeldung) {
    Text(
        text = meldung.ueberschrift,
        style = MaterialTheme.typography.headlineSmall,
        color = MapInk,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("schutzhalt_ueberschrift"),
    )
    Text(
        text = meldung.erklaerung,
        style = MaterialTheme.typography.bodyMedium,
        color = MapInk,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("schutzhalt_erklaerung"),
    )
    Text(
        text = meldung.handlung,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("schutzhalt_handlung"),
    )
    Text(
        text = meldung.zahlen,
        style = MaterialTheme.typography.bodySmall,
        color = MapInk,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MapParchmentMuted)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("schutzhalt_zahlen"),
    )
    meldung.ursache?.let { ursache ->
        Text(
            text = ursache,
            style = MaterialTheme.typography.bodySmall,
            color = MapInk.copy(alpha = URSACHE_DECKKRAFT),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("schutzhalt_ursache"),
        )
    }
}

/** Die technische Ursache tritt zurück – sie ist da, drängt sich aber nicht auf. */
private const val URSACHE_DECKKRAFT = 0.6f
