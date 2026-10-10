package io.github.glaside.zahlenschatzjagd.ui.home

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sichert die Zuordnung Insel → Grafik/Position ([ISLAND_VISUALS]) ab: jede Insel der
 * Startkarte muss eine gültige Grafik und eine Position innerhalb der Kartenfläche haben,
 * damit auf der Schatzkarte keine Insel „unsichtbar" oder außerhalb landet.
 */
class HomeVisualsTest {

    @Test
    fun jedeInselHatEineVisualDefinition() {
        HOME_ISLANDS.forEach { island ->
            assertNotNull("Visual fehlt für ${island.id}", ISLAND_VISUALS[island.id])
        }
    }

    @Test
    fun jedeVisualHatGueltigeGrafikUndPosition() {
        ISLAND_VISUALS.forEach { (id, visual) ->
            assertNotEquals("Drawable-Id 0 (fehlend) für $id", 0, visual.image)
            assertTrue("x außerhalb [0,1] für $id", visual.x in 0f..1f)
            assertTrue("y außerhalb [0,1] für $id", visual.y in 0f..1f)
        }
    }

    /**
     * Unten zeigt die Kartengrafik ein Schiff und ein Seeungeheuer, die frei bleiben sollen.
     * Die Insel-Mitten liegen deshalb in der oberen Hälfte; Grafik und Titel-Pille reichen von dort
     * noch ein Stück nach unten.
     */
    @Test
    fun inselnLassenDieUntereHaelfteFuerSchiffUndSeeungeheuerFrei() {
        ISLAND_VISUALS.forEach { (id, visual) ->
            assertTrue("$id liegt mit y=${visual.y} zu tief", visual.y <= 0.5f)
        }
    }
}
