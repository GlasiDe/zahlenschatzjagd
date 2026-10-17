package io.github.glaside.zahlenschatzjagd.ui.selection

import io.github.glaside.zahlenschatzjagd.EINMALEINS_REIHEN
import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.ZahlenmauerDifficulty
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.einmaleinsKennung

/** Fachliche Kennung eines Spiels auf der Rechnen-Insel. */
enum class RechenspielId { GRUNDRECHENARTEN, VERDOPPELN, ZAHLENMAUER, EINMALEINS }

/**
 * Beschreibung eines Rechen-Spiels für die Spielauswahl – bewusst **reine Daten** ohne
 * Compose/Android (analog zu [io.github.glaside.zahlenschatzjagd.ui.selection.Zahlenspiel]), damit die
 * Zusammenstellung unit-testbar bleibt. [spoken] ist der Text, den Ella vorliest.
 *
 * [kennungen] sind die Fortschrittskennungen, die über dieses Spiel erreichbar sind – die
 * Verbindung zur Freigabe (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`): Ist
 * keine davon erlaubt, verschwindet die Kachel. `UebungsformKennungenTest` hält die Zuordnung
 * über alle Übungsformen hinweg vollständig.
 */
data class Rechenspiel(
    val id: RechenspielId,
    val title: String,
    val spoken: String,
    val kennungen: List<String>,
)

/**
 * Die Spiele der Rechnen-Insel in Anzeige-Reihenfolge. **Zentrale Stelle** für Erweiterungen:
 * ein neues Rechenspiel kommt hier als zusätzlicher [Rechenspiel]-Eintrag hinzu (plus
 * Grafik-Zuordnung im `RechnenMenuScreen`), ohne dass die Szene selbst angefasst werden muss.
 */
val RECHNEN_GAMES: List<Rechenspiel> = listOf(
    Rechenspiel(
        id = RechenspielId.GRUNDRECHENARTEN,
        title = "Grundrechenarten",
        spoken = "Grundrechenarten",
        kennungen = RECHENARTEN.map { it.kennung },
    ),
    Rechenspiel(
        id = RechenspielId.VERDOPPELN,
        title = "Verdoppeln",
        spoken = "Verdoppeln",
        kennungen = listOf("VERDOPPELN_LEICHT", "VERDOPPELN_SCHWER"),
    ),
    Rechenspiel(
        id = RechenspielId.ZAHLENMAUER,
        title = "Zahlenmauer",
        spoken = "Zahlenmauer",
        kennungen = ZahlenmauerDifficulty.entries.map { it.kennung },
    ),
    Rechenspiel(
        id = RechenspielId.EINMALEINS,
        title = "Einmaleins",
        spoken = "Einmaleins",
        // Je Reihe, darin je Stufe – dieselbe Reihenfolge wie im Abzeichen-Katalog.
        kennungen = EINMALEINS_REIHEN.flatMap { reihe ->
            EinmaleinsSchwierigkeitsstufe.entries.map { einmaleinsKennung(reihe, it) }
        },
    ),
)

/** Die Spiele, die dieses Profil auf der Rechnen-Insel sieht – siehe `sichtbareInseln`. */
fun sichtbareRechenspiele(freigabe: Freigabe): List<Rechenspiel> =
    RECHNEN_GAMES.filter { freigabe.erlaubtEineVon(it.kennungen) }
