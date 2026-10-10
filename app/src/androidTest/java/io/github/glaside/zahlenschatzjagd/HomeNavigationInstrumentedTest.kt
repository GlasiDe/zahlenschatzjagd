package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deckt die neue Einstiegs-Navigation ab: die Startkarte zeigt die Inseln und führt in die
 * jeweiligen Bereiche.
 *
 * Die Eltern-Insel prüft `ParentPinInstrumentedTest`, nicht diese Klasse: Welches Tor dort
 * steht, hängt davon ab, ob auf dem Gerät eine PIN gesetzt ist. Das lässt sich nur aufräumen,
 * bevor die Activity ihren ersten Frame zeichnet – siehe `docs/agents/testing.md`.
 */
@RunWith(AndroidJUnit4::class)
class HomeNavigationInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun home_zeigtDieInseln() {
        composeTestRule.onNodeWithTag("island_rechnen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("island_zahlenspiele").assertIsDisplayed()
        composeTestRule.onNodeWithTag("island_abzeichen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("island_eltern").assertIsDisplayed()
    }

    /** Der neue Name steht als Titel auf der Karte, die Sprechblase darunter ist entfallen. */
    @Test
    fun home_zeigtDenTitelOhneSprechblase() {
        composeTestRule.onNodeWithText("Zahlenschatzjagd 🏴‍☠️").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tippe auf eine Insel!").assertDoesNotExist()
    }

    @Test
    fun rechnenInsel_oeffnetSpielauswahl() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        // Die Insel führt jetzt in die Spielauswahl-Szene mit den beiden Spiel-Objekten.
        composeTestRule.onNodeWithTag("game_grundrechenarten").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_verdoppeln").assertIsDisplayed()
    }

    @Test
    fun grundrechenarten_oeffnetAuswahlbildschirm() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_grundrechenarten").performClick()
        composeTestRule.onNodeWithTag("start_button").assertIsDisplayed()
    }

    @Test
    fun verdoppeln_oeffnetAuswahlbildschirm() {
        composeTestRule.onNodeWithTag("island_rechnen").performClick()
        composeTestRule.onNodeWithTag("game_verdoppeln").performClick()
        composeTestRule.onNodeWithTag("start_button").assertIsDisplayed()
    }

    @Test
    fun zahlenspieleInsel_oeffnetSpielauswahl() {
        composeTestRule.onNodeWithTag("island_zahlenspiele").performClick()
        // Die Insel führt jetzt in die Spielauswahl-Szene mit den beiden Spiel-Objekten.
        composeTestRule.onNodeWithTag("game_verliebte").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_vergleich").assertIsDisplayed()
    }

    @Test
    fun abzeichenInsel_oeffnetAbzeichen() {
        composeTestRule.onNodeWithTag("island_abzeichen").performClick()
        composeTestRule.onNodeWithTag("badges_back_button").assertIsDisplayed()
    }

    @Test
    fun abzeichenInsel_verdoppelnReiterZeigtLeichteUndSchwereTruheGetrennt() {
        composeTestRule.onNodeWithTag("island_abzeichen").performClick()
        // Der Reiter liegt im ScrollableTabRow zunächst außerhalb des sichtbaren Bereichs
        // und muss erst ins Bild gescrollt werden, sonst landet der Klick daneben.
        composeTestRule.onNodeWithTag("badge_tab_VERDOPPELN").performScrollTo().performClick()

        // Regression: leichter und schwerer Modus teilen sich einen Reiter ("Verdoppeln"),
        // zeigen darin aber zwei getrennte Truhen statt zwei separater Reiter.
        composeTestRule.onNodeWithText("Leicht").assertIsDisplayed()
        composeTestRule.onNodeWithText("Schwer").assertIsDisplayed()
    }

}
