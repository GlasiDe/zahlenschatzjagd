package io.github.glaside.zahlenschatzjagd.ui.selection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die reine Zusammenstellung der Rechnen-Spielauswahl ([RECHNEN_GAMES]). Das Rendering
 * (Insel-Szene) hängt an Compose und wird hier nicht getestet.
 */
class RechenspieleGamesTest {

    @Test
    fun enthaeltAlleSpieleInReihenfolge() {
        assertEquals(
            listOf(
                RechenspielId.GRUNDRECHENARTEN,
                RechenspielId.VERDOPPELN,
                RechenspielId.ZAHLENMAUER,
                RechenspielId.EINMALEINS,
            ),
            RECHNEN_GAMES.map { it.id }
        )
    }

    @Test
    fun jedesSpielHatTitelUndVorlesetext() {
        RECHNEN_GAMES.forEach { game ->
            assertTrue("Titel fehlt bei ${game.id}", game.title.isNotBlank())
            assertTrue("Vorlesetext fehlt bei ${game.id}", game.spoken.isNotBlank())
        }
    }

    @Test
    fun spielKennungenSindEindeutig() {
        val ids = RECHNEN_GAMES.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }
}
