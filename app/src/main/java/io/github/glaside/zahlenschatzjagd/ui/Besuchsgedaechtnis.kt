package io.github.glaside.zahlenschatzjagd.ui

/**
 * Unterscheidet einen **Besuch** eines Bildschirms vom bloßen Neuaufbau nach dem Drehen
 * (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`). Für den Bildschirm sehen beide
 * gleich aus: Er wird neu gezeichnet, sein gespeicherter Zustand ist noch da.
 *
 * Die Regel: Ein Besuch beginnt, wenn zuvor ein **anderer** Eintrag des Verlaufs sichtbar war. Die
 * Navigation meldet jeden sichtbar werdenden Eintrag über [wirdSichtbar]; nach dem Drehen meldet
 * sie denselben noch einmal, und das ändert nichts. Die Begrüßung fragt mit [begruessungFaellig]
 * und erfährt so genau einmal je Besuch ein „ja".
 *
 * Frei von Compose und Android, damit die Regel ohne Gerät prüfbar ist; über das Drehen trägt
 * es der `ZahlenschatzjagdNavHost` mit `rememberSaveable`.
 */
class Besuchsgedaechtnis(
    sichtbar: String? = null,
    begruesst: Boolean = false,
) {
    /** Die Kennung des zuletzt sichtbaren Eintrags im Verlauf. */
    var sichtbar: String? = sichtbar
        private set

    /** Ob der laufende Besuch von [sichtbar] schon begrüßt wurde. */
    var begruesst: Boolean = begruesst
        private set

    fun wirdSichtbar(eintrag: String) {
        if (eintrag == sichtbar) return
        sichtbar = eintrag
        begruesst = false
    }

    /** `true` genau einmal je Besuch von [eintrag] – und nur, solange er der sichtbare ist. */
    fun begruessungFaellig(eintrag: String): Boolean {
        if (eintrag != sichtbar || begruesst) return false
        begruesst = true
        return true
    }
}
