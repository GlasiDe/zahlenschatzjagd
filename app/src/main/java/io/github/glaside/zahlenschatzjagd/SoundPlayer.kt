package io.github.glaside.zahlenschatzjagd

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import io.github.glaside.zahlenschatzjagd.data.TonUndVorlesenRepository

/**
 * Die in der App abspielbaren Sounds.
 *
 * Die zugehörigen Dateien in `res/raw` sind bewusst auf eine einheitliche Lautheit
 * (~ -14 LUFS, EBU R128) gebracht, damit alle Sounds gleich laut wahrgenommen
 * werden. Deshalb ist hier keine Lautstärke-Korrektur pro Sound nötig. Der Fehlerton liegt
 * mit rund -16 LUFS knapp darunter: Er ist ein Trommelschlag, dessen Spitze lauter nicht
 * zulässt. Herkunft, Lizenz und Bearbeitung stehen in `ASSETS.md`, Abschnitt „6. Töne".
 */
enum class GameSound(@get:RawRes val resId: Int) {
    CORRECT(R.raw.correct),
    WRONG(R.raw.wrong),
    FINISH(R.raw.finish),
}

/**
 * Zentrale Stelle zum Abspielen kurzer Feedback-Sounds, damit kein Screen ein eigenes
 * MediaPlayer-Handling braucht.
 *
 * Hier liegt auch der **Schalter für den Ton** aus dem Elternbereich. Bewusst im Player und
 * nicht an den Aufrufstellen: Sonst müsste jeder Screen, der einen Sound spielt, den Schalter
 * selbst lesen – und der erste, der es vergisst, macht ihn wirkungslos. Der Schalter gilt für
 * dieses Gerät (`docs/adr/0024-ton-und-vorlesen-am-geraet.md`); wer ihn setzt, steht im
 * `ZahlenschatzjagdNavHost`.
 */
object SoundPlayer {

    /**
     * Bis zur ersten Antwort des DataStore gilt die Voreinstellung „an". Anders als bei der
     * Vorlesefunktion ist das gefahrlos: Der erste Ton kommt frühestens nach einer gelösten
     * Aufgabe, also lange nach dieser Antwort.
     */
    @Volatile
    private var tonAn: Boolean = TonUndVorlesenRepository.VOREINSTELLUNG

    /** Schaltet die Töne an oder aus. */
    fun setzeTonAn(an: Boolean) {
        tonAn = an
    }

    /**
     * Sieht jeden Aufruf von [play], bevor der Schalter greift – nur für Tests, die zählen, wie oft
     * ein Ton angefordert wurde (`EinmaligeAusgabenInstrumentedTest`).
     */
    @Volatile
    internal var beobachter: ((GameSound) -> Unit)? = null

    /**
     * Spielt [sound] – sofern der Ton an ist.
     *
     * **Nie aus einem nackten `LaunchedEffect` rufen:** Der läuft nach dem Drehen des Geräts neu an
     * und spielt denselben Ton noch einmal. Was ein Bildschirm von sich aus spielt, geht über
     * `ui/EinmaligeAusgaben.kt` (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`);
     * `EinmaligeAusgabenWaechterTest` prüft das. Aus einem `onClick` darf direkt gespielt werden.
     */
    fun play(context: Context, sound: GameSound) {
        beobachter?.invoke(sound)
        val resId = zuSpielendeRessource(sound) ?: return
        // create() kann bei Problemen null liefern – dann still nichts abspielen statt crashen.
        val mp = MediaPlayer.create(context, resId) ?: return
        mp.setOnCompletionListener { it.release() } // Gibt den Speicher nach dem Abspielen frei.
        mp.start()
    }

    /**
     * Was [play] gleich abspielt – `null`, wenn der Ton ausgeschaltet ist. Ausgelagert, damit der
     * Schalter ohne Gerät prüfbar ist, und bewusst dieselbe Stelle, durch die [play] geht: Ein
     * zweiter Guard könnte davon abweichen.
     */
    internal fun zuSpielendeRessource(sound: GameSound): Int? = sound.resId.takeIf { tonAn }
}
