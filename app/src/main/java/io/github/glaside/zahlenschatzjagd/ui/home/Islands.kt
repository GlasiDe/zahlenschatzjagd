package io.github.glaside.zahlenschatzjagd.ui.home

import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.ui.selection.RECHNEN_GAMES
import io.github.glaside.zahlenschatzjagd.ui.selection.ZAHLENSPIELE_GAMES

/** Fachliche Kennung einer Insel auf der Startkarte (Schatzkarte). */
enum class IslandId { RECHNEN, ZAHLENSPIELE, ABZEICHEN, ELTERN }

/**
 * Beschreibung einer Insel auf der Schatzkarte – bewusst **reine Daten** ohne
 * Compose/Android, damit die Zusammenstellung unit-testbar bleibt.
 *
 * - [symbol] ist das große, leseunabhängige Emoji/Zeichen der Insel (für Nicht-Leser).
 * - [spoken] ist der Text, den das Maskottchen Ella vorliest (Vorlesefunktion).
 * - [enabled] = false zeigt eine Insel als „kommt bald" (gedämpft, nicht antippbar);
 *   künftige ROADMAP-Übungsformen (Uhrzeit, Geld, Mathehaus …) werden schlicht als
 *   weitere Einträge in [HOME_ISLANDS] ergänzt.
 * - [kennungen] sind die Fortschrittskennungen, die hinter dieser Insel überhaupt erreichbar
 *   sind; eine **leere** Liste heißt „hängt an keiner Freigabe" (Abzeichen, Für Eltern).
 *   Getrennt von [enabled]: „kommt bald" und „ist für dich nicht freigegeben" dürfen nicht
 *   gleich aussehen (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`).
 */
data class Island(
    val id: IslandId,
    val title: String,
    val symbol: String,
    val spoken: String,
    val enabled: Boolean = true,
    val kennungen: List<String> = emptyList(),
)

/**
 * Die Inseln der Startkarte in Anzeige-Reihenfolge. **Zentrale Stelle** für Erweiterungen:
 * Eine neue Übungswelt kommt hier als zusätzlicher [Island]-Eintrag hinzu, ohne dass das
 * Karten-Layout selbst angefasst werden muss.
 */
val HOME_ISLANDS: List<Island> = listOf(
    // Die Kennungen einer Übungs-Insel sind die ihrer Spiele – abgeleitet statt abgeschrieben,
    // sonst müsste ein neues Spiel an zwei Stellen eingetragen werden.
    Island(
        id = IslandId.RECHNEN,
        title = "Rechnen",
        symbol = "➕➖",
        spoken = "Rechnen üben",
        kennungen = RECHNEN_GAMES.flatMap { it.kennungen },
    ),
    Island(
        id = IslandId.ZAHLENSPIELE,
        title = "Zahlenspiele",
        symbol = "🪙",
        spoken = "Zahlenspiele",
        kennungen = ZAHLENSPIELE_GAMES.flatMap { it.kennungen },
    ),
    Island(IslandId.ABZEICHEN, "Abzeichen", "🏅", "Deine Abzeichen"),
    Island(IslandId.ELTERN, "Für Eltern", "⚓", "Bereich für die Eltern"),
)

/**
 * Die Inseln, die dieses Profil sehen darf. **Nicht Freigegebenes ist unsichtbar, nicht
 * gedämpft**: Was keine erlaubte Option mehr hat, verschwindet ganz – erst die Kachel im
 * Spielmenü, dann die Insel. Abzeichen und Für Eltern bleiben immer stehen; Erreichtes wird nie
 * weggenommen, und die Erwachsenen müssen zurückkönnen.
 */
fun sichtbareInseln(freigabe: Freigabe): List<Island> =
    HOME_ISLANDS.filter { it.kennungen.isEmpty() || freigabe.erlaubtEineVon(it.kennungen) }
