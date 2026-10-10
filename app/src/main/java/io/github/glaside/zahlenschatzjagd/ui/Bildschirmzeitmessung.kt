package io.github.glaside.zahlenschatzjagd.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt

/**
 * Misst die **Bildschirmzeit** des Kindes, solange dieser Bereich sichtbar ist.
 *
 * Der Modifier gehört an die **äußerste** Fläche der App (den Scaffold im
 * [ZahlenschatzjagdNavHost]): Nur dort liegen alle Bildschirme darunter, und nur dort sieht die
 * Messung jeden Tipp.
 *
 * Drei Dinge halten die Zahl ehrlich – die Regeln selbst stecken in der [BildschirmzeitUhr]:
 * - **Vordergrund.** Verschwindet die App, endet der Abschnitt (`ON_STOP`), und beim
 *   Wiederkommen beginnt ein neuer (`ON_START`).
 * - **Eingaben.** Jeder Tipp wird im [PointerEventPass.Initial] mitgelesen, also **bevor** ein
 *   Knopf ihn verbraucht – sonst zählte nur, was zufällig ins Leere ging. Verbraucht wird dabei
 *   nichts; die Bildschirme merken von der Messung nichts.
 * - **Wessen Zeit.** [profil] sagt, wem sie gehört. `null` heißt „niemandem": kein aktives
 *   Profil, oder der Elternbereich ist offen. Beides wird verworfen, ein Wechsel schneidet den
 *   laufenden Abschnitt durch (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`).
 *
 * [speichere] bekommt jeden Abschnitt, sobald er fertig ist – vorher gibt es nichts zu schreiben,
 * denn ein Abschnitt entsteht erst mit seinem Ende. Die Übergabe ist bewusst **nicht** suspend:
 * Der letzte Abschnitt wird genau dann fertig, wenn die App verschwindet, und darf nicht an
 * einem Scope hängen, den dieser Moment gerade abräumt (siehe
 * `io.github.glaside.zahlenschatzjagd.data.BildschirmzeitRepository`).
 *
 * [jetzt] ist die Uhr; sie ist ein Parameter, damit die Messung ohne Wartezeit prüfbar bleibt.
 *
 * **Was nicht abgedeckt ist:** Wird die App im Vordergrund abgeschossen, gibt es kein `ON_STOP`
 * und der laufende Abschnitt ist verloren. Ein Zwischenspeichern per Zeitgeber wäre der Preis
 * dafür; für einen Fall, den man an einer Hand abzählt, ist er zu hoch.
 */
@Composable
fun Modifier.bildschirmzeitMessen(
    profil: String?,
    speichere: (Zeitabschnitt) -> Unit,
    jetzt: () -> Long = System::currentTimeMillis,
): Modifier {
    val uhr = remember { BildschirmzeitUhr() }
    val aktuellSpeichere by rememberUpdatedState(speichere)
    val aktuellJetzt by rememberUpdatedState(jetzt)

    // Ein fertiger Abschnitt geht sofort weg; `null` heißt „nichts wurde fertig".
    fun festhalten(abschnitt: Zeitabschnitt?) {
        if (abschnitt != null) aktuellSpeichere(abschnitt)
    }

    LaunchedEffect(profil) { festhalten(uhr.beobachte(profil, aktuellJetzt())) }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        festhalten(uhr.beobachte(profil, aktuellJetzt()))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        festhalten(uhr.anhalten(aktuellJetzt()))
    }

    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                festhalten(uhr.tipp(aktuellJetzt()))
            }
        }
    }
}
