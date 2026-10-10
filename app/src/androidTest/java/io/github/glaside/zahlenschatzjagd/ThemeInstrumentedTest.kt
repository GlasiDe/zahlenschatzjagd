package io.github.glaside.zahlenschatzjagd

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guard: Das App-Theme muss die warmen Pergament-Töne liefern. Schützt davor, dass das
 * globale ColorScheme versehentlich wieder auf das alte flache Blau/Weiß zurückfällt und
 * damit alle Screens aus dem Piraten-Look reißt.
 */
@RunWith(AndroidJUnit4::class)
class ThemeInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theme_nutztPergamentFarben() {
        var background: Color? = null
        var onBackground: Color? = null
        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                background = MaterialTheme.colorScheme.background
                onBackground = MaterialTheme.colorScheme.onBackground
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(MapParchment, background)
            assertEquals(MapInk, onBackground)
        }
    }
}
