package io.github.glaside.zahlenschatzjagd.ui.util

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sichert die Größenberechnung für Elemente ab, deren Breite/Höhe je nach Android-Anzeigegröße
 * (verfügbarer Platz) schwankt - siehe `docs/agents/testing.md` für den Hintergrund: bei
 * größerer Anzeigegröße schrumpft die verfügbare dp-Fläche, feste Größen liefen vorher aus dem
 * Bildschirm oder wurden abgeschnitten.
 */
class AdaptiveSizingTest {

    @Test
    fun adaptiveElementSize_liegtZwischenMinUndIdeal_beiNormalemPlatz() {
        // 3 Elemente, 8dp Abstand dazwischen, 300dp verfügbar -> je Element (300-16)/3 = 94.67dp,
        // liegt zwischen 48dp (min) und 100dp (ideal).
        val size = adaptiveElementSize(available = 300.dp, count = 3, ideal = 100.dp, min = 48.dp, spacing = 8.dp)
        assertEquals(94.67f, size.value, 0.1f)
    }

    @Test
    fun adaptiveElementSize_wirdAufIdealGedeckelt_beiSehrVielPlatz() {
        val size = adaptiveElementSize(available = 1000.dp, count = 3, ideal = 100.dp, min = 48.dp, spacing = 8.dp)
        assertEquals(100.dp, size)
    }

    @Test
    fun adaptiveElementSize_faelltNieUnterDieMindestgroesse() {
        // 3 Elemente auf nur 100dp verfügbarer Breite passen bei weitem nicht in 100.dp Idealgröße.
        val size = adaptiveElementSize(available = 100.dp, count = 3, ideal = 100.dp, min = 48.dp, spacing = 8.dp)
        assertEquals(48.dp, size)
    }

    @Test
    fun adaptiveElementSize_liefertIdealBeiNullElementen() {
        val size = adaptiveElementSize(available = 100.dp, count = 0, ideal = 80.dp, min = 40.dp)
        assertEquals(80.dp, size)
    }

    @Test
    fun fitsWithoutScrollFallback_true_wennMindestgroesseNochPasst() {
        assertTrue(fitsWithoutScrollFallback(available = 300.dp, count = 3, min = 48.dp, spacing = 8.dp))
    }

    @Test
    fun fitsWithoutScrollFallback_false_wennSelbstMindestgroesseNichtMehrPasst() {
        // 6 Spalten (z.B. Zahlenmauer "Ultra") à mindestens 40dp + 6dp Abstand passen nicht in 200dp.
        assertFalse(fitsWithoutScrollFallback(available = 200.dp, count = 6, min = 40.dp, spacing = 6.dp))
    }

    @Test
    fun densityScale_ist1_wennGenugPlatzVorhanden() {
        assertEquals(1f, densityScale(available = 700.dp, natural = 600.dp), 0.001f)
    }

    @Test
    fun densityScale_schrumpftProportional_beiWenigerPlatz() {
        assertEquals(0.75f, densityScale(available = 450.dp, natural = 600.dp, minScale = 0.5f), 0.001f)
    }

    @Test
    fun densityScale_wirdNieKleinerAlsMinScale() {
        assertEquals(0.5f, densityScale(available = 100.dp, natural = 600.dp, minScale = 0.5f), 0.001f)
    }
}
