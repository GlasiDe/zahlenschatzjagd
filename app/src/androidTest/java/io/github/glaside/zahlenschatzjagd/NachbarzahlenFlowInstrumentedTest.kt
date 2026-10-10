package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deckt den Weg der Übungsform **Nachbarzahlen** ab: Startkarte → Zahlenspiele-Insel → Kachel →
 * Auswahl → ✓ → Auswertung (Vorbild: `VergleichenFlowInstrumentedTest`), in der leichten wie
 * in der schweren Stufe und in beiden Zahlenräumen. Zusätzlich abgesichert: Der ✓ wertet erst,
 * wenn beide Lücken gefüllt sind, und das Abbrechen (✕) führt zur **Nachbarzahlen**-Auswahl
 * zurück und nicht zu einer der Nachbar-Übungsformen.
 */
@RunWith(AndroidJUnit4::class)
class NachbarzahlenFlowInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /** Die Voreinstellung des Aufgabenzahl-Reglers – so lang ist die Runde im Test. */
    private val aufgabenzahl = 10

    @Before
    fun setUp() {
        composeTestRule.ensureActiveProfileExists()
    }

    @Test
    fun ganzeRunde_laesstSichDurchspielen() {
        starteNachbarzahlen()
        spieleRundeDurch()

        // Nach der letzten Aufgabe steht der Auswertungs-Bildschirm mit „Andere Aufgaben".
        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    /**
     * Abgegeben wird mit dem ✓ des gemeinsamen Ziffernblocks. Er ist nie gesperrt, wertet
     * aber erst, wenn beide Lücken gefüllt sind – vorher bleibt die Aufgabe offen.
     */
    @Test
    fun ueberDenLueckenStehenPfeile_undDerHakenWertetErstMitBeidenLuecken() {
        starteNachbarzahlen()

        composeTestRule.onNodeWithTag("nachbarzahlen_pfeil_LINKS").assertTextEquals("←")
        composeTestRule.onNodeWithTag("nachbarzahlen_pfeil_RECHTS").assertTextEquals("→")
        composeTestRule.onAllNodesWithText("Prüfen").assertCountEquals(0)

        val mitte = vorgegebeneZahl()
        fuelleLuecke("nachbarzahlen_feld_LINKS", mitte - 1)
        composeTestRule.onNodeWithTag("digit_check").performClick()
        // Das Ergebnis erscheint nach jedem Werten – die Lösung darunter nur bei einer falschen Reihe.
        composeTestRule.onAllNodesWithTag("nachbarzahlen_result_text").assertCountEquals(0)

        fuelleLuecke("nachbarzahlen_feld_RECHTS", mitte + 1)
        composeTestRule.onNodeWithTag("digit_check").performClick()
        composeTestRule.onNodeWithTag("nachbarzahlen_result_text").assertIsDisplayed()
    }

    @Test
    fun nachDemPruefenStehenLoesungUndWeiterDa() {
        starteNachbarzahlen()

        val mitte = vorgegebeneZahl()
        fuelleLuecke("nachbarzahlen_feld_LINKS", mitte + 5) // bewusst falsch
        fuelleLuecke("nachbarzahlen_feld_RECHTS", mitte + 1)
        composeTestRule.onNodeWithTag("digit_check").performClick()

        composeTestRule.onNodeWithTag("nachbarzahlen_loesung_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nachbarzahlen_next_button").assertIsDisplayed()
        // Die eigene – falsche – Eingabe bleibt sichtbar, nicht die Lösung.
        composeTestRule.onNodeWithTag("nachbarzahlen_zahl_LINKS").assertTextEquals("${mitte + 5}")
    }

    /**
     * Dieselbe Runde in der **schweren** Stufe: Hier steht die vorgegebene Zahl mal links, mal in
     * der Mitte, mal rechts – der Test liest sie deshalb vom Bildschirm ab, statt sie in der Mitte
     * zu erwarten. Dass jede Runde alle drei Formen zeigt, ist zugesichert und nicht dem Zufall
     * überlassen (siehe `Nachbarzahlenvorrat`), darf hier also ohne festen Startwert stehen.
     */
    @Test
    fun schwereStufe_laesstSichVonJederVorgegebenenPositionAusDurchspielen() {
        starteNachbarzahlen(schwer = true)

        val vorgegebenePositionen = (1..aufgabenzahl).map {
            val position = loeseAktuelleAufgabe()
            composeTestRule.onNodeWithTag("nachbarzahlen_next_button").performClick()
            position
        }

        assertEquals(setOf("LINKS", "MITTE", "RECHTS"), vorgegebenePositionen.toSet())
        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    /**
     * Dieselbe Runde im **Zahlenraum 100**. Geprüft wird dabei das, was der große Zahlenraum an der
     * Oberfläche verlangt: In eine Lücke gehen **drei** Ziffern hinein (bis 20 wäre nach zweien
     * Schluss), und sie lassen sich wieder löschen.
     */
    @Test
    fun zahlenraum100_nimmtDreistelligeEingabenAnUndLaesstSichDurchspielen() {
        starteNachbarzahlen(zahlenraum = 100)

        fuelleLuecke("nachbarzahlen_feld_LINKS", 100)
        assertEquals("100", feldText("LINKS"))

        composeTestRule.onNodeWithTag("digit_clear").performClick()
        assertEquals("?", feldText("LINKS"))

        spieleRundeDurch()

        composeTestRule.onNodeWithTag("back_to_selection_button").assertIsDisplayed()
    }

    @Test
    fun dieAuswahlNenntDieGewaehlteSchwierigkeitsstufe() {
        composeTestRule.onNodeWithTag("island_zahlenspiele").performClick()
        composeTestRule.onNodeWithTag("game_nachbarzahlen").performClick()

        composeTestRule.onNodeWithTag("nachbarzahlen_stufe_text").assertTextContains("Leicht", substring = true)

        composeTestRule.onNodeWithTag("toggle_difficulty").performScrollTo().performClick()

        composeTestRule.onNodeWithTag("nachbarzahlen_stufe_text").assertTextContains("Schwer", substring = true)
    }

    @Test
    fun spielAbbrechen_kehrtZurNachbarzahlenAuswahlZurueck() {
        starteNachbarzahlen()

        composeTestRule.onNodeWithTag("nachbarzahlen_cancel_button").performClick()

        // Der Zurück-Knopf der Nachbarzahlen-Auswahl ist deren eindeutiges Merkmal.
        composeTestRule.onNodeWithTag("nachbarzahlen_back_button").assertIsDisplayed()
    }

    /**
     * Öffnet die Zahlenspiele-Insel, wählt „Nachbarzahlen" und startet eine Runde. Stufe und
     * [zahlenraum] sind frei kombinierbar; gescrollt wird vor jedem Tippen, weil der
     * Auswahl-Bildschirm bei großer Anzeigegröße länger ist als der Schirm.
     */
    private fun starteNachbarzahlen(schwer: Boolean = false, zahlenraum: Int = 20) {
        composeTestRule.onNodeWithTag("island_zahlenspiele").performClick()
        composeTestRule.onNodeWithTag("game_nachbarzahlen").performClick()
        if (schwer) composeTestRule.onNodeWithTag("toggle_difficulty").performScrollTo().performClick()
        composeTestRule.onNodeWithText("0-$zahlenraum").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("start_button").performScrollTo().performClick()
    }

    /**
     * Löst die Aufgabe, die gerade dasteht – gleichgültig, welche Zahl vorgegeben ist. Die
     * vorgegebene Stelle ist die einzige, in der keine leere Lücke („?") steht; die beiden anderen
     * ergeben sich aus ihrem Abstand zu ihr. Zurück kommt die Stelle, an der die vorgegebene Zahl
     * stand.
     */
    private fun loeseAktuelleAufgabe(): String {
        val positionen = listOf("LINKS", "MITTE", "RECHTS")
        // Erst alle drei lesen, dann tippen: Die erste Eingabe ersetzt sonst das „?" der anderen.
        val texte = positionen.map { feldText(it) }
        val vorgegeben = texte.indexOfFirst { it != "?" }
        assertEquals("Genau eine Zahl ist vorgegeben, in: $texte", 2, texte.count { it == "?" })

        val vorgegebeneZahl = texte[vorgegeben].toInt()
        positionen.forEachIndexed { stelle, position ->
            if (stelle == vorgegeben) return@forEachIndexed
            fuelleLuecke("nachbarzahlen_feld_$position", vorgegebeneZahl + (stelle - vorgegeben))
        }
        composeTestRule.onNodeWithTag("digit_check").performClick()
        return positionen[vorgegeben]
    }

    /**
     * Was in einem der drei Felder steht – die vorgegebene Zahl, eine Eingabe oder „?". Am
     * **unzusammengefassten** Baum abgelesen: Eine offene Lücke ist antippbar, und Compose zieht
     * deren Beschriftung dann in den Knoten des Rahmens hinein.
     */
    private fun feldText(position: String): String {
        val node = composeTestRule
            .onNodeWithTag("nachbarzahlen_zahl_$position", useUnmergedTree = true)
            .fetchSemanticsNode()
        return node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }.trim()
    }

    /** Löst alle Aufgaben der Runde richtig, bis die Auswertung steht. */
    private fun spieleRundeDurch() {
        repeat(aufgabenzahl) {
            val mitte = vorgegebeneZahl()
            fuelleLuecke("nachbarzahlen_feld_LINKS", mitte - 1)
            fuelleLuecke("nachbarzahlen_feld_RECHTS", mitte + 1)
            composeTestRule.onNodeWithTag("digit_check").performClick()
            composeTestRule.onNodeWithTag("nachbarzahlen_next_button").performClick()
        }
    }

    /** Die vorgegebene Zahl der aktuellen Aufgabe – in der leichten Stufe immer die mittlere. */
    private fun vorgegebeneZahl(): Int = feldText("MITTE").toInt()

    /** Tippt eine Lücke an und gibt [wert] über den Ziffernblock ein. */
    private fun fuelleLuecke(feldTag: String, wert: Int) {
        composeTestRule.onNodeWithTag(feldTag).performClick()
        composeTestRule.onNodeWithTag("digit_clear").performClick()
        wert.toString().forEach { ziffer ->
            composeTestRule.onNodeWithTag("digit_$ziffer").performClick()
        }
    }
}
