package io.github.glaside.zahlenschatzjagd.ui.schutzhalt

import io.github.glaside.zahlenschatzjagd.data.Datenbankstart
import io.github.glaside.zahlenschatzjagd.data.SCHEMA_VERSION_UNBEKANNT
import io.github.glaside.zahlenschatzjagd.data.Schutzhaltgrund

/**
 * Was im **Schutzhalt** auf dem Bildschirm steht. Compose-frei und deshalb ohne Gerät prüfbar –
 * derselbe Schnitt wie bei [io.github.glaside.zahlenschatzjagd.ui.sicherung.BackupMessage].
 *
 * Zwei Adressaten auf einem Bildschirm: [gesprochen] gehört dem Kind, das die App geöffnet hat
 * und nicht liest; alles andere gehört dem Erwachsenen, den es holt.
 */
data class SchutzhaltMeldung(
    val gesprochen: String,
    val ueberschrift: String,
    val erklaerung: String,
    val handlung: String,
    val zahlen: String,
    val ursache: String?,
)

/** Der Satz für das Kind – bei jeder Ursache derselbe, weil ihm der Unterschied nichts sagt. */
private const val FUER_DAS_KIND = "Hier ist gerade etwas kaputt. Hol bitte einen Erwachsenen."

/**
 * Baut die Meldung zum vorliegenden Schutzhalt. Die Überschrift sagt zuerst das Wichtigste:
 * Es ist nichts verloren. Wer diesen Bildschirm sieht, hat sonst als Erstes Angst um den
 * Fortschritt seines Kindes.
 */
fun schutzhaltMeldung(
    schutzhalt: Datenbankstart.Schutzhalt,
    appVersion: String,
): SchutzhaltMeldung = SchutzhaltMeldung(
    gesprochen = FUER_DAS_KIND,
    ueberschrift = "Der Fortschritt ist sicher",
    erklaerung = erklaerungZu(schutzhalt.grund),
    handlung = handlungZu(schutzhalt.grund),
    zahlen = zahlenZu(schutzhalt.gefunden, schutzhalt.erwartet, appVersion),
    ursache = ursacheZu(schutzhalt),
)

/**
 * Die technische Meldung – **nur** bei [Schutzhaltgrund.NICHT_LESBAR].
 *
 * Sie kommt aus Room und ist englisch, also für Eltern nicht lesbar. In den beiden anderen
 * Fällen sagt sie zudem nichts, was die drei Zahlen nicht schon sagen: „A migration from 1 to 6"
 * ist wörtlich „Gefunden: 1 · Erwartet: 6". Bleibt der eine Fall, in dem sie der einzige Hinweis
 * ist, was überhaupt kaputt ist – beschädigte Datei, voller Speicher. Dort ist ein
 * abfotografierter englischer Satz mehr wert als gar keiner.
 */
private fun ursacheZu(schutzhalt: Datenbankstart.Schutzhalt): String? =
    schutzhalt.ursache.takeIf { schutzhalt.grund == Schutzhaltgrund.NICHT_LESBAR }

private fun erklaerungZu(grund: Schutzhaltgrund): String = when (grund) {
    Schutzhaltgrund.WEG_FEHLT ->
        "Diese Version der App kann die gespeicherten Übungsdaten noch nicht lesen. Sie hat " +
            "deshalb nichts verändert – aller Fortschritt liegt unberührt auf dem Gerät."

    Schutzhaltgrund.DATEN_NEUER ->
        "Auf diesem Gerät lief schon einmal eine neuere Version der Zahlenschatzjagd. Die " +
            "gespeicherten Übungsdaten sind neuer als diese App, deshalb hat sie nichts verändert."

    Schutzhaltgrund.NICHT_LESBAR ->
        "Die gespeicherten Übungsdaten lassen sich nicht öffnen. Die App hat nichts verändert " +
            "und sie unberührt liegen lassen."
}

/**
 * Wo ein Erwachsener den Schutzhalt meldet: eine Adresse, die jede Familie erreicht, auch ohne
 * den Entwickler zu kennen. Die Zahlen gehen per Bildschirmfoto mit.
 */
private const val MELDESTELLE = "github.com/GlasiDe/zahlenschatzjagd/issues"

private const val BITTE_UM_MELDUNG =
    "Bitte melde das unter $MELDESTELLE und schick die Zahlen unten mit – am einfachsten als " +
        "Bildschirmfoto dieser Seite."

private fun handlungZu(grund: Schutzhaltgrund): String = when (grund) {
    Schutzhaltgrund.WEG_FEHLT ->
        "$BITTE_UM_MELDUNG Mit der nächsten Version der App ist der Fortschritt wieder da."

    Schutzhaltgrund.DATEN_NEUER ->
        "Installiere wieder die neuere Version der App – danach ist alles wie vorher."

    Schutzhaltgrund.NICHT_LESBAR ->
        "$BITTE_UM_MELDUNG Bis dahin bitte die App nicht löschen: Beim Löschen gehen die Daten " +
            "wirklich verloren."
}

/** Die drei Zahlen, mit denen sich der Fall nachbauen lässt. */
private fun zahlenZu(gefunden: Int, erwartet: Int, appVersion: String): String {
    val gefundenText =
        if (gefunden == SCHEMA_VERSION_UNBEKANNT) "unbekannt" else gefunden.toString()
    return "Gefunden: $gefundenText  ·  Erwartet: $erwartet  ·  App: $appVersion"
}
