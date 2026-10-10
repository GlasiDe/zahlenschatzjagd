package io.github.glaside.zahlenschatzjagd.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.SoundPlayer

/*
 * Die einzigen Stellen, an denen ein Bildschirm von sich aus einen **Ton** spielt oder etwas
 * vorliest (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`). Ein nackter
 * `LaunchedEffect` läuft nach dem Drehen des Geräts neu an, weil die Composition neu gebaut wird,
 * und spielt dann denselben Ton noch einmal. `EinmaligeAusgabenWaechterTest` hält alle
 * übrigen Dateien davon frei. Was auf ein Tippen hin erklingt, braucht nichts davon.
 *
 * Drei Wege, je nachdem, woher der Anlass kommt:
 * - aus dem Zustand eines ViewModels → [AusstehenderTon], [AusstehendeAnsage]: Das ViewModel legt
 *   die Ausgabe ab, die Oberfläche spielt sie und quittiert. Nach dem Drehen steht nichts mehr aus.
 * - beim ersten Erscheinen eines Bildschirms → [TonBeimErstenErscheinen],
 *   [VorlesenBeimErstenErscheinen]: ein Merker, der das Drehen und die Rückkehr aus dem Verlauf
 *   übersteht.
 * - bei jedem Besuch → [BegruessungJeBesuch] mit dem [Besuchsgedaechtnis] der Navigation.
 */

/** Spielt [ton], sobald einer aussteht, und meldet ihn über [onGespielt] als erledigt. */
@Composable
fun AusstehenderTon(ton: GameSound?, onGespielt: () -> Unit) {
    val context = LocalContext.current
    val quittiere by rememberUpdatedState(onGespielt)
    LaunchedEffect(ton) {
        if (ton == null) return@LaunchedEffect
        SoundPlayer.play(context, ton)
        quittiere()
    }
}

/** Liest [text] vor, sobald einer aussteht, und meldet ihn über [onVorgelesen] als erledigt. */
@Composable
fun AusstehendeAnsage(text: String?, onVorgelesen: () -> Unit) {
    val context = LocalContext.current
    val quittiere by rememberUpdatedState(onVorgelesen)
    LaunchedEffect(text) {
        if (text == null) return@LaunchedEffect
        Narrator.speak(context, text)
        quittiere()
    }
}

/**
 * Spielt [ton] einmal, solange der Bildschirm im Verlauf liegt – nicht nach dem Drehen und nicht,
 * wenn man von einem anderen Bildschirm zu ihm zurückkehrt.
 */
@Composable
fun TonBeimErstenErscheinen(ton: GameSound) {
    val context = LocalContext.current
    var gespielt by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (gespielt) return@LaunchedEffect
        gespielt = true
        SoundPlayer.play(context, ton)
    }
}

/** Wie [TonBeimErstenErscheinen], nur für einen vorgelesenen Satz. */
@Composable
fun VorlesenBeimErstenErscheinen(text: String) {
    val context = LocalContext.current
    var vorgelesen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (vorgelesen) return@LaunchedEffect
        vorgelesen = true
        Narrator.speak(context, text)
    }
}

/**
 * Liest [text] bei jedem Besuch des Bildschirms vor, nach dem Drehen aber nicht noch einmal. Ob
 * gerade ein Besuch beginnt, weiß das [Besuchsgedaechtnis] im `ZahlenschatzjagdNavHost`; es kommt als
 * [begruessungFaellig] herein.
 */
@Composable
fun BegruessungJeBesuch(text: String, begruessungFaellig: () -> Boolean) {
    val context = LocalContext.current
    val faellig by rememberUpdatedState(begruessungFaellig)
    LaunchedEffect(Unit) {
        if (faellig()) Narrator.speak(context, text)
    }
}

/**
 * Das [Besuchsgedaechtnis] der Navigation. Es muss das Drehen überstehen – sonst hielte es den
 * Neuaufbau danach für einen Besuch – und tut das über den gespeicherten Zustand der Activity.
 */
@Composable
fun rememberBesuchsgedaechtnis(): Besuchsgedaechtnis =
    rememberSaveable(saver = BesuchsgedaechtnisSaver) { Besuchsgedaechtnis() }

private val BesuchsgedaechtnisSaver = listSaver<Besuchsgedaechtnis, Any?>(
    save = { listOf(it.sichtbar, it.begruesst) },
    restore = { Besuchsgedaechtnis(sichtbar = it[0] as String?, begruesst = it[1] as Boolean) },
)
