package io.github.glaside.zahlenschatzjagd.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Begrüßung, die Ella beim Öffnen der Startkarte vorliest. */
const val HOME_WELCOME_SPOKEN = "Willkommen! Tippe auf eine Insel."

/**
 * Prozessweiter Vorleser für das Maskottchen Ella (Vorlesefunktion für
 * Nicht-Leser). Bewusst ein **Singleton** analog zu `SoundPlayer` und **nicht** an die
 * Lebensdauer eines Screens gebunden:
 *
 * - **Kein `shutdown()` beim Verlassen eines Screens.** Würde das `TextToSpeech` im
 *   `onDispose` der Startseite freigegeben, schnitte die Navigation die gerade gestartete
 *   Ansage mitten im Wort ab, denn sie entfernt die Startseite sofort. Als
 *   prozessweites Objekt läuft die Ansage im TTS-Engine-Dienst weiter, auch wenn der
 *   Screen schon gewechselt hat.
 * - **Pending-Text gegen die Init-Race:** Die TTS-Initialisierung ist asynchron. Ein
 *   `speak()` direkt beim ersten Anzeigen (z. B. die Begrüßung) käme sonst, bevor die
 *   Engine bereit ist, und würde still verworfen. Stattdessen wird der Text gemerkt und
 *   nach abgeschlossener Initialisierung nachgeholt.
 * - **Nicht stumm nur wegen fehlender deutscher Stimme:** Manche Geräte haben keine
 *   deutschen Sprachdaten. Es genügt eine erfolgreich initialisierte Engine – fehlt Deutsch,
 *   wird mit der Default-Stimme gesprochen (eine Stimme ist besser als gar keine).
 *
 * **Graceful degradation:** Liefert die Engine gar keinen Erfolg (keine TTS-Engine
 * installiert), bleibt der Vorleser einfach still – kein Crash, keine für ein Kind
 * unverständliche Fehlermeldung.
 *
 * Hier liegt auch der **Schalter für die Vorlesefunktion** aus dem Elternbereich – im Vorleser
 * und nicht an den Aufrufstellen, damit ihn keiner der Bildschirme vergessen kann. Er gilt für
 * dieses Gerät (`docs/adr/0024-ton-und-vorlesen-am-geraet.md`); gesetzt wird er im
 * `ZahlenschatzjagdNavHost`.
 */
object Narrator {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    /**
     * Der Schalter aus dem Elternbereich – `null`, solange der DataStore noch nicht geantwortet
     * hat. Der dritte Zustand ist nötig, weil die Begrüßung der Startkarte im ersten Frame
     * losgeschickt wird und damit vor jeder Antwort: Mit „an" als Annahme spräche ein
     * ausgeschalteter Vorleser bei jedem App-Start genau einmal. Unbekannt heißt deshalb
     * vormerken, nicht sprechen – dieselbe Behandlung wie bei einer noch startenden Engine.
     */
    private var vorlesenAn: Boolean? = null

    /**
     * Sieht jeden Aufruf von [speak], bevor der Schalter greift – nur für Tests, die zählen, wie
     * oft vorgelesen werden sollte (`EinmaligeAusgabenInstrumentedTest`).
     */
    @Volatile
    internal var beobachter: ((String) -> Unit)? = null

    /**
     * Liest [text] vor. Ein laufender Text wird abgebrochen (QUEUE_FLUSH).
     *
     * **Nie aus einem nackten `LaunchedEffect` rufen:** Der läuft nach dem Drehen des Geräts neu an
     * und liest denselben Satz noch einmal. Was ein Bildschirm von sich aus vorliest, geht über
     * `EinmaligeAusgaben.kt` (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`);
     * `EinmaligeAusgabenWaechterTest` prüft das. Aus einem `onClick` darf direkt vorgelesen werden.
     */
    fun speak(context: Context, text: String) {
        beobachter?.invoke(text)
        when (naechsteAktion(text)) {
            SpeakAction.DROP -> return
            SpeakAction.SPEAK_NOW -> tts?.let { utter(it, text) }
            SpeakAction.QUEUE_PENDING -> pending = text
            SpeakAction.START_INIT_AND_QUEUE -> {
                pending = text
                ensureInit(context.applicationContext)
            }
        }
    }

    /**
     * Schaltet die Vorlesefunktion an oder aus. Beim Ausschalten verstummt auch ein gerade
     * laufender Satz – wer den Schalter umlegt, will Ruhe, nicht das Ende des angefangenen
     * Textes – und ein vorgemerkter wird verworfen statt später nachgeholt.
     */
    fun setzeVorlesenAn(an: Boolean) {
        vorlesenAn = an
        if (an) {
            holeVorgemerktesNach()
        } else {
            pending = null
            tts?.stop()
        }
    }

    /** Was ein `speak(text)` im aktuellen Zustand auslösen würde. */
    internal fun naechsteAktion(text: String): SpeakAction =
        decideSpeak(text, tts != null, ready, vorlesenAn)

    private fun ensureInit(appContext: Context) {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            val engine = tts
            if (engine != null && shouldMarkReady(status)) {
                // Deutsch versuchen; ist es nicht verfügbar, mit der Default-Stimme
                // weitersprechen statt zu verstummen.
                engine.setLanguage(Locale.GERMAN)
                ready = true
                holeVorgemerktesNach()
            }
        }
    }

    /**
     * Spricht den vorgemerkten Text, sobald **beides** stimmt: Die Engine ist bereit und der
     * Schalter sagt „an". Beide Bedingungen können in beliebiger Reihenfolge eintreten, deshalb
     * fragen beide Auslöser hier dasselbe ab.
     */
    private fun holeVorgemerktesNach() {
        val engine = tts ?: return
        if (!ready || vorlesenAn != true) return
        pending?.let { text ->
            pending = null
            utter(engine, text)
        }
    }

    private fun utter(engine: TextToSpeech, text: String) {
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }
}

/** Was mit einem [speak]-Wunsch im aktuellen Zustand geschehen soll. */
internal enum class SpeakAction { SPEAK_NOW, QUEUE_PENDING, START_INIT_AND_QUEUE, DROP }

/**
 * Reine, framework-freie Entscheidung, was ein `speak(text)` auslösen soll – ausgelagert,
 * damit die Race-Behandlung unit-testbar ist:
 * - leerer Text → nichts tun,
 * - Vorlesefunktion ausgeschaltet → nichts tun,
 * - Engine noch nicht erzeugt → Initialisierung starten und Text vormerken,
 * - Engine bereit und Schalter „an" → sofort sprechen,
 * - sonst → Text vormerken (wird nachgeholt, sobald beides zutrifft).
 *
 * [vorlesenAn] ist `null`, solange der Schalter noch nicht geladen ist; dann wird vorgemerkt.
 */
internal fun decideSpeak(
    text: String,
    ttsCreated: Boolean,
    ready: Boolean,
    vorlesenAn: Boolean?,
): SpeakAction =
    when {
        text.isBlank() -> SpeakAction.DROP
        vorlesenAn == false -> SpeakAction.DROP
        !ttsCreated -> SpeakAction.START_INIT_AND_QUEUE
        ready && vorlesenAn == true -> SpeakAction.SPEAK_NOW
        else -> SpeakAction.QUEUE_PENDING
    }

/**
 * Ob die Engine nach [onInit][TextToSpeech.OnInitListener] als sprechbereit gilt. Bewusst
 * **unabhängig von der Sprachverfügbarkeit**: Ein erfolgreicher Init reicht; fehlt die
 * deutsche Stimme, wird mit der Default-Stimme gesprochen statt komplett zu verstummen.
 */
internal fun shouldMarkReady(status: Int): Boolean = status == TextToSpeech.SUCCESS
