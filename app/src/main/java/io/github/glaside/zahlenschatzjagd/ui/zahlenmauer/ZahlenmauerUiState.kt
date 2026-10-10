package io.github.glaside.zahlenschatzjagd.ui.zahlenmauer

import io.github.glaside.zahlenschatzjagd.GameSound
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.Zahlenmauer
import io.github.glaside.zahlenschatzjagd.ZahlenmauerPos

/**
 * Eingabezustand einer einzelnen Lücke. Getrennt von [io.github.glaside.zahlenschatzjagd.ZahlenmauerCell]
 * (die nur die Lösung + isGiven kennt), damit die UI nie direkt an die Lösung herankommt, bevor
 * geprüft wurde. [isCorrect] bleibt `null`, bis mit dem ✓ des Ziffernblocks geprüft wurde
 * (siehe [ZahlenmauerUiState.checked]) – danach sind alle Lücken einer Mauer gleichzeitig
 * ausgewertet (kein Nachbessern, One-Shot-Feedback wie bei den übrigen Spielen).
 */
data class ZahlenmauerEntryState(val input: String = "", val isCorrect: Boolean? = null)

/**
 * Ein einziger, konsistenter Snapshot des Zahlenmauer-Bildschirms. Analog zu
 * [io.github.glaside.zahlenschatzjagd.ui.game.GameUiState], aber pro "Aufgabe" (= Mauer) mehrere
 * gleichzeitige Lücken statt einer einzelnen Antwort.
 */
data class ZahlenmauerUiState(
    val wall: Zahlenmauer? = null,
    val entries: Map<ZahlenmauerPos, ZahlenmauerEntryState> = emptyMap(),
    val selectedPos: ZahlenmauerPos? = null,
    val checked: Boolean = false, // true = mit ✓ geprüft, Ergebnisse sichtbar, keine Eingabe mehr möglich
    val wallNumber: Int = 1,
    val totalWalls: Int = 0,
    // Zählt pro RUNDE ganze Mauern, nicht einzelne Zellen: eine Mauer ist nur "richtig", wenn
    // ALLE ihre Lücken korrekt waren (siehe ZahlenmauerViewModel.onCheck) - eine einzelne
    // falsche Zelle lässt die ganze Mauer als nicht bestanden zählen, analog zu einer
    // "Aufgabe" bei den übrigen Spielen. Daraus werden die Sterne; die Truhe rechnet dagegen
    // mit dem gestaffelten Ertrag (docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md).
    val correctWalls: Int = 0,
    val totalWallsChecked: Int = 0,
    val isFinished: Boolean = false,
    val newlyUnlockedBadges: List<UnlockedBadge> = emptyList(),
    /**
     * Der **Ton** nach dem Prüfen – einer für die ganze Mauer, nicht einer je Lücke: „richtig" nur,
     * wenn alle Lücken stimmen, sonst „falsch", auch bei einer teilweise richtigen Mauer
     * (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`). Abgespielt wird er in der
     * Oberfläche, damit das ViewModel ohne Context auskommt; sie quittiert mit
     * [ZahlenmauerViewModel.tonGespielt], danach steht er nicht mehr aus – auch nicht nach dem
     * Drehen (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
     */
    val ausstehenderTon: GameSound? = null,
)
