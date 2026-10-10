package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Der Eintrag **Übungskalender** des Elternbereichs: **wann** und **wie lange** ein Kind geübt hat.
 *
 * Zwei Blöcke, weil es zwei verschiedene Fragen sind:
 * - Das **Punktemuster** der letzten Wochen antwortet auf „wann". Ein Tag ist gefüllt, sobald an
 *   ihm eine Runde **abgeschlossen** wurde – nicht, sobald die App offen war. Sonst färbte sich der
 *   Tag schon, wenn ein Kind hineinschaut und einmal danebentippt.
 * - Die **Bildschirmzeit** je Woche und je Tag antwortet auf „wie lange". Sie steht hier und nicht
 *   beim Übungsstand: Zeit gehört auf eine Zeitachse.
 *
 * Solange der Profilwechsel ohne PIN erlaubt ist und es mehr als ein Kind gibt, steht darunter,
 * dass sich Zeiten vermischt haben können — hier ist die falsch zugeordnete Zeit zu sehen, nicht
 * beim Zugang (`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`).
 *
 * Der Bildschirm bekommt seinen [state] herein statt sich ein ViewModel zu holen — wie
 * [ZugangScreen]. Das ist hier kein Selbstzweck: Sieben Spalten sind das Dichteste, was diese App
 * quer hinstellt, und nur so lässt sich das Muster im `SmallScreenLayoutTest` auf 320 dp Breite
 * prüfen, ohne Datenbank und ohne Gerät in einer bestimmten Anzeigegröße.
 */
@Composable
fun UebungskalenderScreen(
    state: UebungskalenderUiState,
    onWaehleProfil: (String) -> Unit,
    onBack: () -> Unit,
) {
    ParentPage(
        title = "📅 Übungskalender",
        onBack = onBack,
        backTestTag = "uebungskalender_back_button",
    ) {
        // Vor der ersten Antwort der Datenbank bleibt die Seite leer: Ein leerer Kalender sähe aus
        // wie „noch nie geübt" und wäre eine Aussage, die niemand geprüft hat.
        if (!state.geladen) return@ParentPage

        if (state.gewaehltesProfil == null) {
            Text(
                text = "Sobald ein Kind ein Profil hat, steht hier, wann und wie lange es " +
                    "geübt hat.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("uebungskalender_ohne_profil"),
            )
            return@ParentPage
        }

        if (state.profile.size > 1) {
            ProfilWahl(
                namen = state.profile.map { it.name },
                gewaehlt = state.gewaehltesProfil,
                testTagPrefix = "uebungskalender_profil_",
                onWaehle = onWaehleProfil,
            )
            Spacer(Modifier.height(16.dp))
        }

        Punktemuster(state.wochen)

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        Bildschirmzeiten(state.wochen)

        if (state.geschwisterHinweis) GeschwisterHinweis()
        Spacer(Modifier.height(16.dp))
    }
}

/** Die letzten Wochen als Muster: eine Zeile je Woche, Montag bis Sonntag. */
@Composable
private fun Punktemuster(wochen: List<Kalenderwoche>) {
    Abschnitt("Geübte Tage")
    Text(
        text = "Ein gefüllter Tag heißt: An ihm wurde mindestens eine Runde zu Ende geübt.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        for (kuerzel in WOCHENTAGE) {
            Text(
                text = kuerzel,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    for (woche in wochen) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            for (tag in woche.tage) {
                TagPunkt(tag = tag, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Ein Tag als Kreis mit seiner Tageszahl. Die Beschreibung für die Vorlesefunktion sagt in Worten,
 * was die Farbe zeigt – ein Punktemuster allein ist nichts, was sich vorlesen ließe.
 */
@Composable
private fun TagPunkt(tag: Kalendertag, modifier: Modifier) {
    val schema = MaterialTheme.colorScheme
    val hintergrund: Color
    val schrift: Color
    when {
        tag.geuebt -> {
            hintergrund = schema.primary
            schrift = schema.onPrimary
        }
        tag.zukunft -> {
            hintergrund = Color.Transparent
            schrift = schema.onSurface.copy(alpha = ZUKUNFT_ALPHA)
        }
        else -> {
            hintergrund = schema.secondaryContainer
            schrift = schema.onSecondaryContainer
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(hintergrund)
                .testTag("kalender_tag_${tag.datum}")
                .semantics { contentDescription = tagBeschreibung(tag) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tag.datum.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = schrift,
            )
        }
    }
}

private const val ZUKUNFT_ALPHA = 0.38f

/**
 * Die gemessene Zeit: eine Zeile je Woche, und die **angetippte** Woche klappt ihre Tage auf.
 *
 * Ohne das Aufklappen hätte nur die laufende Woche Tageswerte – „je Tag und Woche" gilt aber für
 * jede gezeigte Woche. Alle 42 Tage untereinander wären dagegen eine Liste, die niemand liest.
 * Vorbelegt offen ist die laufende Woche: nach ihr wird zuerst gefragt.
 */
@Composable
private fun Bildschirmzeiten(wochen: List<Kalenderwoche>) {
    Abschnitt("Bildschirmzeit")
    Text(
        text = "Gezählt wird nur, was das Kind wirklich benutzt hat: Pausen ab zwei Minuten ohne " +
            "Eingabe zählen nicht mit, die Zeit im Elternbereich gar nicht. Eine Woche antippen " +
            "zeigt ihre einzelnen Tage.",
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(Modifier.height(8.dp))

    val laufendeWoche = wochen.lastOrNull()?.beginn
    var offeneWoche by remember(laufendeWoche) { mutableStateOf(laufendeWoche) }

    // Jüngste Woche oben: Danach wird zuerst gefragt.
    for (woche in wochen.asReversed()) {
        val offen = woche.beginn == offeneWoche
        ZeitZeile(
            beschriftung = "${if (offen) "⌄" else "›"}  ${wochenBeschriftung(woche)}",
            zusatz = "${woche.geuebteTage} von ${WOCHENTAGE.size} Tagen geübt",
            dauer = formatiereDauer(woche.bildschirmzeitMillis),
            modifier = Modifier
                .clickable { offeneWoche = if (offen) null else woche.beginn }
                .testTag("kalender_woche_${woche.beginn}"),
        )
        if (!offen) continue
        for (tag in woche.tage.filterNot { it.zukunft }) {
            ZeitZeile(
                beschriftung = tagesBeschriftung(tag.datum),
                zusatz = if (tag.geuebt) rundenAnzahl(tag.runden) else null,
                dauer = formatiereDauer(tag.bildschirmzeitMillis),
                modifier = Modifier
                    .padding(start = 20.dp)
                    .testTag("kalender_tageszeit_${tag.datum}"),
            )
        }
    }
}

/**
 * Eine Zeile „Zeitraum … Dauer", mit optionaler Erläuterung darunter.
 *
 * Testmarke, Einrückung und Antippbarkeit kommen über den [modifier] herein statt als eigene
 * Parameter: Wochen- und Tageszeile unterscheiden sich in genau diesen drei Äußerlichkeiten, und
 * eine Reihe von Schaltern dafür sagte weniger als der Modifier an der Aufrufstelle.
 */
@Composable
private fun ZeitZeile(
    beschriftung: String,
    zusatz: String?,
    dauer: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(modifier),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = beschriftung, style = MaterialTheme.typography.bodyLarge)
            if (zusatz != null) {
                Text(
                    text = zusatz,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(text = dauer, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Gesagt, nicht verhindert: Ohne PIN vor dem Wechseln können Zeiten beim falschen Kind landen. */
@Composable
private fun GeschwisterHinweis() {
    Spacer(Modifier.height(16.dp))
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = "Der Profilwechsel ist gerade ohne PIN erlaubt. Wechselt ein Kind auf ein " +
                "fremdes Profil, zählen seine Zeit und seine Runden dort. Unter Zugang lässt " +
                "sich das Wechseln hinter die PIN legen.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(12.dp)
                .testTag("uebungskalender_geschwister_hinweis"),
        )
    }
}
