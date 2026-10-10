package io.github.glaside.zahlenschatzjagd.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die reine Zusammenstellung der Startkarten-Inseln ([HOME_ISLANDS]). Das Rendering
 * hängt an Compose und wird hier nicht getestet.
 */
class IslandsTest {

    @Test
    fun enthaeltDieVierErwartetenInselnInReihenfolge() {
        assertEquals(
            listOf(IslandId.RECHNEN, IslandId.ZAHLENSPIELE, IslandId.ABZEICHEN, IslandId.ELTERN),
            HOME_ISLANDS.map { it.id }
        )
    }

    @Test
    fun jedeInselHatTitelSymbolUndVorlesetext() {
        HOME_ISLANDS.forEach { island ->
            assertTrue("Titel fehlt bei ${island.id}", island.title.isNotBlank())
            assertTrue("Symbol fehlt bei ${island.id}", island.symbol.isNotBlank())
            assertTrue("Vorlesetext fehlt bei ${island.id}", island.spoken.isNotBlank())
        }
    }

    @Test
    fun aktuelleInselnSindAlleFreigeschaltet() {
        assertTrue(HOME_ISLANDS.all { it.enabled })
    }
}
