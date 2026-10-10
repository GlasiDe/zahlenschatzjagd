package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Der Eintrag **Übungsstand** des Elternbereichs: **was** ein Kind zuletzt geübt hat und **wie
 * sicher** es sitzt.
 *
 * Eine Zeile je Übungsform, die sich zweimal aufklappen lässt – auf die Fortschrittskennungen und
 * von dort auf die Zahlenräume. Aufgeklappt wird nur, wo es darunter mehr als eins gibt; die Regel
 * steht bei [baueUebungsstand]. Zugeklappt sind alle: Wer den Bildschirm öffnet, will erst sehen,
 * wo es hakt, und dann dorthin hineinsehen.
 *
 * Was hier **nicht** steht, wurde beim Entwurf erwogen und verworfen: die einzelnen falsch
 * beantworteten Aufgaben (eine Fehlerliste über das eigene Kind) und ein Knopf, der aus einer Zeile
 * heraus die Freigabe setzt (die hat ihren eigenen Eintrag).
 *
 * Der Bildschirm bekommt seinen [state] herein statt sich ein ViewModel zu holen – wie
 * [UebungskalenderScreen] und [ZugangScreen].
 */
@Composable
fun UebungsstandScreen(
    state: UebungsstandUiState,
    onWaehleProfil: (String) -> Unit,
    onBack: () -> Unit,
) {
    ParentPage(
        title = "🧭 Übungsstand",
        onBack = onBack,
        backTestTag = "uebungsstand_back_button",
    ) {
        // Vor der ersten Antwort der Datenbank bleibt die Seite leer: Ein leerer Übungsstand sähe
        // aus wie „noch nie geübt" und wäre eine Aussage, die niemand geprüft hat.
        if (!state.geladen) return@ParentPage

        if (state.gewaehltesProfil == null) {
            Text(
                text = "Sobald ein Kind ein Profil hat, steht hier, was es geübt hat und wie " +
                    "sicher es sitzt.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("uebungsstand_ohne_profil"),
            )
            return@ParentPage
        }

        if (state.profile.size > 1) {
            ProfilWahl(
                namen = state.profile.map { it.name },
                gewaehlt = state.gewaehltesProfil,
                testTagPrefix = "uebungsstand_profil_",
                onWaehle = onWaehleProfil,
            )
            Spacer(Modifier.height(16.dp))
        }

        Erklaerung(state.zeilen)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Zeilen(zeilen = state.zeilen, profil = state.gewaehltesProfil)
        Spacer(Modifier.height(16.dp))
    }
}

/** Woraus die Zahlen kommen – und ab wann überhaupt eine dasteht. */
@Composable
private fun Erklaerung(zeilen: List<Uebungsstandzeile>) {
    // Kein Wort darüber, *warum* gefenstert wird – das ist eine Entwurfsfrage und steht im ADR.
    // Hier steht nur, worauf die Zahlen daneben sich stützen.
    Text(
        text = "Gezählt werden die letzten $UEBUNGSSTAND_AUFGABEN Aufgaben je Übung und " +
            "höchstens die letzten $UEBUNGSSTAND_TAGE Tage – der Übungsstand zeigt also, wie es " +
            "gerade sitzt, nicht wie es über die Jahre war.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(8.dp))
    // Der Nachsatz erklärt die Ausnahme, die der Hinweis davor schon genannt hat – deshalb ohne
    // erneute Nennung der Zahlenmauer, sonst stünde sie zweimal in zwei Sätzen. Der letzte Satz
    // beantwortet die Frage, die sonst beim Aufklappen entsteht: warum die Zahl oben nicht zu den
    // Zahlen darunter zu passen scheint.
    Text(
        text = schwellenHinweis(zeilen) + " Eine ganze Mauer zählt dabei als eine Aufgabe. " +
            "Eine Zeile rechnet nur mit dem, was darunter selbst eine Prozentzahl trägt.",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("uebungsstand_schwellen_hinweis"),
    )
}

/**
 * Der aufklappbare Baum. Welche Zeilen offen sind, merkt sich der Bildschirm und nicht das
 * ViewModel: Es ist eine Frage der Ansicht, keine des Übungsstands. Beim Wechsel des Profils fängt
 * es von vorn an – die aufgeklappte Zeile des einen Kindes sagt über das andere nichts.
 */
@Composable
private fun Zeilen(zeilen: List<Uebungsstandzeile>, profil: String?) {
    var offen by remember(profil) { mutableStateOf(emptySet<String>()) }

    for (zeile in zeilen) {
        StandZeile(
            zeile = zeile,
            tiefe = 0,
            offen = offen,
            onKlappe = { schluessel ->
                offen = if (schluessel in offen) offen - schluessel else offen + schluessel
            },
        )
    }
}

/** Ein Schritt Einrückung je Ebene – gerade genug, um die Zugehörigkeit zu sehen. */
private val EINRUECKUNG = 20.dp

@Composable
private fun StandZeile(
    zeile: Uebungsstandzeile,
    tiefe: Int,
    offen: Set<String>,
    onKlappe: (String) -> Unit,
) {
    val aufklappbar = zeile.unterzeilen.isNotEmpty()
    val istOffen = zeile.schluessel in offen

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (aufklappbar) Modifier.clickable { onKlappe(zeile.schluessel) } else Modifier)
            .padding(start = EINRUECKUNG * tiefe, top = 6.dp, bottom = 6.dp)
            .testTag("uebungsstand_zeile_${zeile.schluessel}"),
    ) {
        // Das Zeichen steht auch bei nicht aufklappbaren Zeilen als Platzhalter, sonst rückten
        // deren Beschriftungen gegenüber den Geschwisterzeilen nach links aus.
        Text(
            text = if (!aufklappbar) " " else if (istOffen) "⌄" else "›",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = zeile.beschriftung, style = MaterialTheme.typography.bodyLarge)
            if (zeile.geuebt) {
                Text(
                    text = aufgabenText(zeile.aufgaben),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        // Ohne eigene Testmarke: Eine aufklappbare Zeile ist antippbar und führt damit die
        // Semantik ihrer Kinder zusammen – eine Marke hier wäre nur an den nicht aufklappbaren
        // Zeilen zu finden. Geprüft wird stattdessen der Text der ganzen Zeile.
        Text(text = quotenText(zeile), style = MaterialTheme.typography.bodyLarge)
    }

    if (!istOffen) return
    for (unterzeile in zeile.unterzeilen) {
        StandZeile(zeile = unterzeile, tiefe = tiefe + 1, offen = offen, onKlappe = onKlappe)
    }
}
