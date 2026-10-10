package io.github.glaside.zahlenschatzjagd.ui.selection

import io.github.glaside.zahlenschatzjagd.data.ALLE_KENNUNGEN
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die reine Zusammenstellung der Zahlenspiele-Spielauswahl ([ZAHLENSPIELE_GAMES]). Das
 * Rendering (Insel-Szene) hängt an Compose und wird hier nicht getestet.
 */
class ZahlenspieleGamesTest {

    @Test
    fun enthaeltDieSpieleInReihenfolge() {
        assertEquals(
            listOf(ZahlenspielId.VERLIEBTE, ZahlenspielId.VERGLEICHEN, ZahlenspielId.NACHBARZAHLEN),
            ZAHLENSPIELE_GAMES.map { it.id }
        )
    }

    @Test
    fun jedesSpielHatTitelUndVorlesetext() {
        ZAHLENSPIELE_GAMES.forEach { game ->
            assertTrue("Titel fehlt bei ${game.id}", game.title.isNotBlank())
            assertTrue("Vorlesetext fehlt bei ${game.id}", game.spoken.isNotBlank())
        }
    }

    @Test
    fun spielKennungenSindEindeutig() {
        val ids = ZAHLENSPIELE_GAMES.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    /**
     * Welche Hilfe-Stufen von Verliebte Zahlen das Kind wählen kann. Sind beide freigegeben, zeigt
     * der Auswahl-Bildschirm einen Schalter mit „Mit Hilfe" als Voreinstellung; ist es nur eine,
     * entfällt der Schalter (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
     */
    @Test
    fun verliebteZahlen_beideStufenFreigegeben_mitHilfeZuerst() {
        assertEquals(listOf(true, false), verliebteZahlenHilfen(Freigabe.ALLES))
    }

    @Test
    fun verliebteZahlen_nurMitHilfeFreigegeben() {
        val freigabe = Freigabe.ALLES.copy(kennungen = ALLE_KENNUNGEN - "VERLIEBTE_ZAHLEN_OHNE_HILFE")

        assertEquals(listOf(true), verliebteZahlenHilfen(freigabe))
    }

    @Test
    fun verliebteZahlen_nurOhneHilfeFreigegeben() {
        val freigabe = Freigabe.ALLES.copy(kennungen = ALLE_KENNUNGEN - "VERLIEBTE_ZAHLEN")

        assertEquals(listOf(false), verliebteZahlenHilfen(freigabe))
        assertTrue(ZahlenspielId.VERLIEBTE in sichtbareZahlenspiele(freigabe).map { it.id })
    }

    @Test
    fun verliebteZahlen_beideStufenAbgewaehlt_dieKachelVerschwindet() {
        val freigabe = Freigabe.ALLES.copy(
            kennungen = ALLE_KENNUNGEN - "VERLIEBTE_ZAHLEN" - "VERLIEBTE_ZAHLEN_OHNE_HILFE",
        )

        assertEquals(emptyList<Boolean>(), verliebteZahlenHilfen(freigabe))
        assertFalse(ZahlenspielId.VERLIEBTE in sichtbareZahlenspiele(freigabe).map { it.id })
    }
}
