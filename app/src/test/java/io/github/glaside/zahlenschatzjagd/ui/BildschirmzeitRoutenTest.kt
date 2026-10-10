package io.github.glaside.zahlenschatzjagd.ui

import io.github.glaside.zahlenschatzjagd.ui.parent.ParentEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft, auf welchen Routen **Bildschirmzeit** gezählt wird.
 *
 * Die Routen kommen aus [Routes] selbst, nicht abgeschrieben: Wer eine neue Ecke des
 * Elternbereichs anlegt und sie hier vergisst, bekommt sonst still deren Zeit dem Kind
 * angerechnet (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`).
 */
class BildschirmzeitRoutenTest {

    @Test
    fun aufDenKinderBildschirmen_wirdGemessen() {
        val kinderRouten = listOf(
            Routes.HOME,
            Routes.SELECTION,
            Routes.RECHNEN_MENU,
            Routes.VERDOPPELN_SELECTION,
            Routes.ZAHLENMAUER_SELECTION,
            Routes.ZAHLENMAUER_GAME,
            Routes.ZAHLENMAUER_STATS,
            Routes.ZAHLENSPIELE_MENU,
            Routes.ZAHLENSPIELE_SELECTION,
            Routes.VERGLEICHEN_SELECTION,
            Routes.GAME,
            Routes.STATS,
            Routes.BADGES,
            Routes.PROFILE_PATTERN,
            Routes.profile(verwaltung = true),
        )

        kinderRouten.forEach { route ->
            assertTrue("Auf „$route\" muss die Bildschirmzeit zählen", zaehltAlsBildschirmzeit(route))
        }
    }

    @Test
    fun imElternbereich_wirdNichtGemessen() {
        val elternRouten = listOf(
            Routes.PARENT,
            Routes.PARENT_PIN_SETUP,
            Routes.PARENT_GATE_PATTERN,
            Routes.parentGate(Torziel.ELTERNBEREICH),
            Routes.parentGate(Torziel.PROFILVERWALTUNG),
        ) + ParentEntry.entries.map { Routes.parentEntry(it) }

        elternRouten.forEach { route ->
            assertFalse("Zeit auf „$route\" gehört keinem Kind", zaehltAlsBildschirmzeit(route))
        }
    }

    @Test
    fun ohneRoute_wirdNichtGemessen() {
        // Vor dem ersten Ziel ist noch nichts zu sehen – also gibt es auch nichts zu messen.
        assertFalse(zaehltAlsBildschirmzeit(null))
    }
}
