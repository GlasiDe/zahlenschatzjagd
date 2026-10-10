package io.github.glaside.zahlenschatzjagd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.data.Rundenabschluss
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.UserProfile
import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungskalenderScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungskalenderUiState
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungsstandScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungsstandUiState
import io.github.glaside.zahlenschatzjagd.ui.parent.baueUebungskalender
import io.github.glaside.zahlenschatzjagd.ui.parent.baueUebungsstand
import io.github.glaside.zahlenschatzjagd.ui.selection.SelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.stats.StatsScreen
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Guard gegen Layout-Fehler bei großer Android-Anzeigegröße: Stellt man auf einem
 * 1080x2400-Gerät die Anzeigegröße auf „groß" (Dichte 540 statt 420), schrumpft die
 * verfügbare Fläche auf rund 320 x 640 dp. Zwei Bildschirme waren dadurch für Kinder
 * nicht mehr bedienbar – der Start-Button des Auswahlbildschirms rutschte aus dem Bild,
 * und auf dem Auswertungsbildschirm wurden „Gleich nochmal!" und „Andere Aufgaben" auf
 * Höhe 0 zusammengedrückt, sobald die Karte mit neuen Abzeichen erschien.
 *
 * Die Tests rendern die Screens bewusst in einen künstlich kleinen Container, damit solche
 * Fehler geräteunabhängig auffallen und nicht nur auf einem entsprechend konfigurierten
 * Emulator.
 */
@RunWith(AndroidJUnit4::class)
class SmallScreenLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun auswahlbildschirm_startButtonIstAufKleinerFlaecheSichtbar() {
        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 600.dp)) {
                    SelectionScreen(freigabe = Freigabe.ALLES, onStartGame = {})
                }
            }
        }

        composeTestRule.onNodeWithTag("start_button").assertIsDisplayed()
    }

    @Test
    fun auswertung_aktionsButtonsBleibenAufKleinerFlaecheBedienbar() {
        // Bewusst mehrere frisch freigeschaltete Abzeichen: die Karte wird dadurch besonders
        // hoch – genau der Fall, in dem die Buttons vorher verschwanden.
        val badges = listOf(
            UnlockedBadge("PLUS", 10, BadgeTier.BRONZE),
            UnlockedBadge("MINUS", 20, BadgeTier.SILBER),
            UnlockedBadge("MAL", 100, BadgeTier.GOLD),
        )

        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 600.dp)) {
                    StatsScreen(
                        correct = 10,
                        total = 10,
                        unlockedBadges = badges,
                        onPlayAgain = {},
                        onBackToSelection = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("play_again_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    /**
     * Sieben Spalten sind das Dichteste, was diese App quer hinstellt: Auf 320 dp bleiben je Tag
     * rund 41 dp. Der Test füllt jeden Tag der laufenden Woche – jede Zelle trägt dann eine
     * Tageszahl und jede Zeile eine Dauer – und prüft, dass keine Spalte aus dem Bild fällt.
     */
    @Test
    fun uebungskalender_zeigtAufKleinerFlaecheAlleSiebenSpalten() {
        val zone = ZoneId.of("Europe/Berlin")
        val montag = LocalDate.of(2026, 8, 10)
        val tage = (0..6).map { montag.plusDays(it.toLong()) }
        val runden = tage.map { tag ->
            Rundenabschluss(profil = "Mira", zeitpunkt = tag.atTime(17, 45).atZone(zone).toEpochMs())
        }
        val zeiten = tage.map { tag ->
            Zeitabschnitt(
                profil = "Mira",
                beginn = tag.atTime(17, 0).atZone(zone).toEpochMs(),
                ende = tag.atTime(17, 45).atZone(zone).toEpochMs(),
            )
        }
        val state = UebungskalenderUiState(
            profile = listOf(
                UserProfile("Mira", "avatar_pirat_8", createdAt = 1),
                UserProfile("Ben", "avatar_pirat_2", createdAt = 2),
            ),
            gewaehltesProfil = "Mira",
            // Als „heute" der Sonntag: Dann ist die ganze Woche gefüllt und keine Zelle blass.
            wochen = baueUebungskalender(runden, zeiten, tage.last(), zone),
            geschwisterHinweis = true,
            geladen = true,
        )

        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 600.dp)) {
                    UebungskalenderScreen(state = state, onWaehleProfil = {}, onBack = {})
                }
            }
        }

        for (tag in tage) {
            composeTestRule.onNodeWithTag("kalender_tag_$tag").performScrollTo().assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("kalender_woche_$montag").performScrollTo().assertIsDisplayed()
    }

    /**
     * Die tiefste Zeile des Übungsstands ist die engste: zwei Stufen Einrückung, links die
     * Beschriftung und rechts der längste Befund („zu wenig geübt"). Auf 320 dp ist das die Stelle,
     * an der die linke Spalte auf Breite null gedrückt würde – derselbe Fehler wie damals bei den
     * Auswertungs-Buttons, nur quer statt hoch.
     */
    @Test
    fun uebungsstand_bleibtAufKleinerFlaecheBisInDieDritteEbeneLesbar() {
        // Plus: 12 Aufgaben bis 10 (über der Schwelle) und 3 bis 20 (darunter) – so trägt die
        // dritte Ebene beide Fälle, die Prozentzahl und den langen Text.
        val historie = List(12) { aufgabe("PLUS", zahlenraum = 10, vorMinuten = it.toLong()) } +
            List(3) { aufgabe("PLUS", zahlenraum = 20, vorMinuten = 12L + it) }
        val state = UebungsstandUiState(
            profile = listOf(UserProfile("Mira", "avatar_pirat_8", createdAt = 1)),
            gewaehltesProfil = "Mira",
            zeilen = baueUebungsstand(historie, JETZT),
            geladen = true,
        )

        composeTestRule.setContent {
            ZahlenschatzjagdTheme {
                Box(modifier = Modifier.size(width = 320.dp, height = 600.dp)) {
                    UebungsstandScreen(state = state, onWaehleProfil = {}, onBack = {})
                }
            }
        }

        composeTestRule.onNodeWithTag("uebungsstand_zeile_GRUNDRECHENARTEN")
            .performScrollTo().performClick()
        composeTestRule.onNodeWithTag("uebungsstand_zeile_PLUS")
            .performScrollTo().performClick()

        for (zahlenraum in listOf(10, 20, 50, 100)) {
            composeTestRule.onNodeWithTag("uebungsstand_zeile_PLUS_$zahlenraum")
                .performScrollTo().assertIsDisplayed()
        }
    }
}

/** Ein fester Zeitpunkt: Mittwoch, der 12. August 2026, 12 Uhr. */
private val JETZT: Long = LocalDate.of(2026, 8, 12).atTime(12, 0)
    .atZone(ZoneId.of("Europe/Berlin")).toEpochMs()

private fun aufgabe(kennung: String, zahlenraum: Int, vorMinuten: Long) = TaskHistory(
    userName = "Mira",
    taskCategory = kennung,
    numberRange = zahlenraum,
    questionText = "1 + 1 = _",
    expectedAnswer = "2",
    userAnswer = "2",
    isCorrect = true,
    timestamp = JETZT - vorMinuten * 60_000L,
)

/** Kurzform für „diese Zeit in Millisekunden seit 1970". */
private fun ZonedDateTime.toEpochMs(): Long = toInstant().toEpochMilli()
