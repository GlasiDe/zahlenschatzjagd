package io.github.glaside.zahlenschatzjagd.ui

import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt

/**
 * Misst die **Bildschirmzeit** und schneidet sie in [Zeitabschnitt]e.
 *
 * Bewusst **ohne Android und ohne eigene Uhr**: Jeder Zeitpunkt kommt als Parameter herein. Damit
 * lässt sich jeder Fall – auch die halbe Stunde auf dem Sofa – ohne Gerät und ohne Wartezeit
 * prüfen (`docs/agents/testing.md`).
 *
 * Sie liegt neben der Oberfläche und nicht in `data/`, obwohl sie Compose-frei ist: Gebraucht
 * wird sie allein von [bildschirmzeitMessen]; die Datenschicht kennt nur den fertigen
 * [Zeitabschnitt]. Derselbe Schnitt wie bei `BackupMessage.kt` (`docs/agents/testing.md`).
 *
 * Die Uhr ist eine kleine Zustandsmaschine mit drei Meldungen:
 * - [beobachte] – wessen Zeit gerade zählt. `null` heißt „gar keine": kein aktives Profil, oder
 *   der Elternbereich ist offen. Ein Wechsel schneidet den laufenden Abschnitt durch.
 * - [tipp] – das Kind hat etwas angetippt.
 * - [anhalten] – die App ist nicht mehr im Vordergrund.
 *
 * Jede Meldung gibt den Abschnitt zurück, der dadurch **fertig** geworden ist – oder `null`.
 * Aufrufer schreiben genau das weg; ein Abschnitt entsteht also erst, wenn er zu Ende ist, und
 * bleibt danach unverändert (siehe [Zeitabschnitt]).
 *
 * Das Ende ist nie später als der letzte Tipp plus [PAUSE_MILLIS]. Ohne diese Deckelung würde die
 * App das aufgeklappt liegende Tablet messen, während das Kind Kakao holt
 * (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`).
 */
class BildschirmzeitUhr(private val pauseNachMillis: Long = PAUSE_MILLIS) {

    private var laufend: Laufend? = null

    /** Der noch offene Abschnitt. [letzteAktivitaet] ist der letzte Tipp – oder der [beginn]. */
    private data class Laufend(val profil: String, val beginn: Long, val letzteAktivitaet: Long)

    /**
     * Meldet, wessen Bildschirmzeit ab [jetzt] zählt (`null`: keine). Ein Wechsel beendet den
     * laufenden Abschnitt und beginnt gegebenenfalls einen neuen; dieselbe Angabe noch einmal
     * ändert nichts.
     */
    fun beobachte(profil: String?, jetzt: Long): Zeitabschnitt? {
        if (laufend?.profil == profil) return null
        val fertig = schliesse(jetzt)
        laufend = profil?.let { Laufend(profil = it, beginn = jetzt, letzteAktivitaet = jetzt) }
        return fertig
    }

    /**
     * Das Kind hat etwas angetippt. Ist die Pause seit dem letzten Tipp voll, war die App
     * zwischendurch unbenutzt: Der alte Abschnitt endet dann **rückwirkend**, und mit diesem Tipp
     * beginnt ein neuer.
     */
    fun tipp(jetzt: Long): Zeitabschnitt? {
        val aktuell = laufend ?: return null
        // War die Pause voll, ist der alte Abschnitt rückwirkend zu Ende und dieser Tipp beginnt
        // einen neuen. Sonst hält er den laufenden nur am Leben.
        val pauseVorbei = jetzt - aktuell.letzteAktivitaet >= pauseNachMillis
        laufend = aktuell.copy(
            beginn = if (pauseVorbei) jetzt else aktuell.beginn,
            letzteAktivitaet = jetzt,
        )
        return if (pauseVorbei) aktuell.bis(aktuell.letzteAktivitaet + pauseNachMillis) else null
    }

    /** Die App ist nicht mehr im Vordergrund; ein laufender Abschnitt wird fertig. */
    fun anhalten(jetzt: Long): Zeitabschnitt? = schliesse(jetzt)

    private fun schliesse(jetzt: Long): Zeitabschnitt? {
        val aktuell = laufend ?: return null
        laufend = null
        return aktuell.bis(minOf(jetzt, aktuell.letzteAktivitaet + pauseNachMillis))
    }

    /**
     * Der fertige Abschnitt – oder `null`, wenn er keine Dauer hat. Das deckt auch eine
     * rückwärts gestellte Uhr ab: Lieber nichts messen als ein negatives Ende speichern.
     */
    private fun Laufend.bis(ende: Long): Zeitabschnitt? =
        if (ende > beginn) Zeitabschnitt(profil = profil, beginn = beginn, ende = ende) else null

    companion object {
        /** Nach so langer Zeit ohne Eingabe gilt die App als nicht mehr benutzt: zwei Minuten. */
        const val PAUSE_MILLIS: Long = 2 * 60 * 1000
    }
}
