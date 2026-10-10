package io.github.glaside.zahlenschatzjagd.ui.selection

import io.github.glaside.zahlenschatzjagd.categoryForType
import io.github.glaside.zahlenschatzjagd.data.Freigabe

/** Fachliche Kennung eines Spiels auf der Zahlenspiele-Insel. */
enum class ZahlenspielId { VERLIEBTE, VERGLEICHEN, NACHBARZAHLEN }

/**
 * Beschreibung eines Zahlenspiels für die Spielauswahl – bewusst **reine Daten** ohne
 * Compose/Android (analog zu [io.github.glaside.zahlenschatzjagd.ui.home.Island]), damit die
 * Zusammenstellung unit-testbar bleibt. [spoken] ist der Text, den Ella vorliest.
 *
 * [kennungen] sind die Fortschrittskennungen, die über dieses Spiel erreichbar sind – die
 * Verbindung zur Freigabe (siehe [Rechenspiel]).
 */
data class Zahlenspiel(
    val id: ZahlenspielId,
    val title: String,
    val spoken: String,
    val kennungen: List<String>,
)

/**
 * Die Spiele der Zahlenspiele-Insel in Anzeige-Reihenfolge. **Zentrale Stelle** für
 * Erweiterungen: Ein neues Zahlenspiel kommt hier als zusätzlicher [Zahlenspiel]-Eintrag hinzu
 * (plus Grafik-Zuordnung im `ZahlenspieleMenuScreen`), ohne dass die Szene selbst angefasst
 * werden muss.
 */
val ZAHLENSPIELE_GAMES: List<Zahlenspiel> = listOf(
    Zahlenspiel(
        id = ZahlenspielId.VERLIEBTE,
        title = "Verliebte Zahlen",
        spoken = "Verliebte Zahlen",
        kennungen = listOf("VERLIEBTE_ZAHLEN", "VERLIEBTE_ZAHLEN_OHNE_HILFE"),
    ),
    Zahlenspiel(
        id = ZahlenspielId.VERGLEICHEN,
        title = "Zahlen vergleichen",
        spoken = "Zahlen vergleichen",
        kennungen = listOf("GROESSER_KLEINER"),
    ),
    // Hinten angehängt: Die Reihenfolge ist reine Anzeigesache, und so bleiben die bestehenden
    // Kacheln an ihrem gewohnten Platz.
    Zahlenspiel(
        id = ZahlenspielId.NACHBARZAHLEN,
        title = "Nachbarzahlen",
        spoken = "Nachbarzahlen",
        kennungen = listOf("NACHBARZAHLEN_LEICHT", "NACHBARZAHLEN_SCHWER"),
    ),
)

/** Die Spiele, die dieses Profil auf der Zahlenspiele-Insel sieht – siehe `sichtbareInseln`. */
fun sichtbareZahlenspiele(freigabe: Freigabe): List<Zahlenspiel> =
    ZAHLENSPIELE_GAMES.filter { freigabe.erlaubtEineVon(it.kennungen) }

/**
 * Die Hilfe-Stufen von Verliebte Zahlen, die dieses Profil wählen kann – `true` heißt „Mit Hilfe"
 * und steht vorn, weil es die Voreinstellung ist. Sind beide freigegeben, zeigt der
 * Auswahl-Bildschirm einen Schalter; ist es nur eine, übt das Kind ohne Schalter in dieser
 * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
 */
fun verliebteZahlenHilfen(freigabe: Freigabe): List<Boolean> =
    listOf(true, false).filter { mitHilfe ->
        freigabe.erlaubt(categoryForType("Verliebte", showCoins = mitHilfe))
    }
