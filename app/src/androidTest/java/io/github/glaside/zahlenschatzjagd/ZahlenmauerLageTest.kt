package io.github.glaside.zahlenschatzjagd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme
import io.github.glaside.zahlenschatzjagd.ui.zahlenmauer.ZahlenmauerPyramid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Die Zahlenmauer steht waagerecht mittig. Sie saß schon einmal oben links in ihrem Rahmen,
 * weil die umgebende `BoxWithConstraints` keine Ausrichtung trug.
 *
 * Die Pyramide wird in einen Rahmen fester Breite gesetzt, wie in `SmallScreenLayoutTest`: So ist
 * auch der Scroll-Rückfall erreichbar, den die Ultra-Stufe erst unterhalb von 224 dp Breite
 * braucht – der Emulator lässt bei Anzeigegröße „groß" 320 dp, abzüglich Rand also 288 dp.
 */
@RunWith(AndroidJUnit4::class)
class ZahlenmauerLageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun leichteMauer_stehtMittig() {
        zeigeMauer(ZahlenmauerDifficulty.LEICHT, rahmenBreite = 320.dp)
        pruefeSpitzeMittig(ZahlenmauerDifficulty.LEICHT)
    }

    @Test
    fun mittlereMauer_stehtMittig() {
        zeigeMauer(ZahlenmauerDifficulty.MITTEL, rahmenBreite = 320.dp)
        pruefeSpitzeMittig(ZahlenmauerDifficulty.MITTEL)
    }

    @Test
    fun schwereMauer_stehtMittig() {
        zeigeMauer(ZahlenmauerDifficulty.SCHWER, rahmenBreite = 320.dp)
        pruefeSpitzeMittig(ZahlenmauerDifficulty.SCHWER)
    }

    @Test
    fun ultraMauer_stehtMittig() {
        zeigeMauer(ZahlenmauerDifficulty.ULTRA, rahmenBreite = 320.dp)
        pruefeSpitzeMittig(ZahlenmauerDifficulty.ULTRA)
    }

    /** Fünf Zellen à 40 dp plus Abstände brauchen 224 dp – hier greift der Scroll-Rückfall. */
    @Test
    fun ultraMauer_stehtMitScrollRueckfallMittig() {
        val stufe = ZahlenmauerDifficulty.ULTRA
        zeigeMauer(stufe, rahmenBreite = 200.dp)
        val letzteBasiszelle = "zahlenmauer_cell_0_${stufe.floors - 1}"

        // Voraussetzung: Der Rückfall greift wirklich, die Basis ragt über den Rahmen hinaus.
        val rahmen = composeTestRule.onNodeWithTag(RAHMEN_TAG).fetchSemanticsNode()
        val basisEnde = composeTestRule.onNodeWithTag(letzteBasiszelle).fetchSemanticsNode()
        assertTrue(
            "Die Basis passt in den Rahmen, der Scroll-Rückfall wird gar nicht geprüft",
            basisEnde.positionInRoot.x + basisEnde.size.width > rahmen.positionInRoot.x + rahmen.size.width,
        )

        pruefeSpitzeMittig(stufe)
        composeTestRule.onNodeWithTag(letzteBasiszelle).performScrollTo().assertIsDisplayed()
    }

    private fun zeigeMauer(stufe: ZahlenmauerDifficulty, rahmenBreite: Dp) {
        val mauer = createZahlenmauer(stufe, Random(1))
        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                Box(modifier = Modifier.width(rahmenBreite).testTag(RAHMEN_TAG)) {
                    ZahlenmauerPyramid(
                        wall = mauer,
                        entries = emptyMap(),
                        selectedPos = null,
                        checked = false,
                        onSelectCell = {},
                    )
                }
            }
        }
    }

    private fun pruefeSpitzeMittig(stufe: ZahlenmauerDifficulty) {
        val rahmen = composeTestRule.onNodeWithTag(RAHMEN_TAG).getBoundsInRoot()
        val spitze = composeTestRule.onNodeWithTag("zahlenmauer_cell_${stufe.floors - 1}_0").getBoundsInRoot()
        val rahmenMitte = (rahmen.left + rahmen.right).value / 2
        val spitzeMitte = (spitze.left + spitze.right).value / 2
        assertEquals("Mitte der Spitze (dp)", rahmenMitte, spitzeMitte, TOLERANZ_DP)
    }

    private companion object {
        const val RAHMEN_TAG = "rahmen"

        /** Rundung beim Umrechnen in Pixel. */
        const val TOLERANZ_DP = 1f
    }
}
