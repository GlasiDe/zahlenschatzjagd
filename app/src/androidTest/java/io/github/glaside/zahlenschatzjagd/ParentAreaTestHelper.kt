package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking

/**
 * Gemeinsame Schritte der Tests rund um den Elternbereich – der Weg dorthin führt bei allen
 * durch dasselbe Tor.
 */

/**
 * Entfernt eine gesetzte PIN aus dem echten DataStore der App. Muss **vor** dem ersten
 * Compose-Frame laufen, sonst entscheidet die Navigation noch mit dem alten Zustand, welches Tor
 * sie zeigt – siehe `docs/agents/testing.md`.
 *
 * Ohne dieses Aufräumen würde eine PIN aus einem Testlauf in den nächsten (und in andere
 * Testklassen) hineinreichen.
 */
fun clearParentPin() {
    val app = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()
    runBlocking { app.container.parentAccessRepository.clearPin() }
}

/** Vergibt eine PIN im echten DataStore der App – wie [clearParentPin] vor dem ersten Frame. */
fun setzeParentPin(pin: String, frage: String, antwort: String) {
    val app = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()
    runBlocking {
        app.container.parentAccessRepository.setPin(pin = pin, question = frage, answer = antwort)
    }
}

/**
 * Setzt den Schalter „Profilwechsel ohne PIN erlauben" im echten DataStore der App. Wie
 * [clearParentPin] vor dem ersten Compose-Frame aufzurufen – und nach dem Test wieder auf die
 * Voreinstellung zu bringen, sonst reicht er in andere Testklassen hinein.
 */
fun setzeProfilwechselOhnePin(erlaubt: Boolean) {
    val app = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()
    runBlocking { app.container.parentAccessRepository.setzeProfilwechselOhnePin(erlaubt) }
}

/**
 * Öffnet den Elternbereich über die Elternfrage: Eltern-Insel antippen, die angezeigte
 * Multiplikation auslesen und lösen. Setzt voraus, dass keine PIN gesetzt ist
 * ([clearParentPin]).
 */
fun ComposeTestRule.oeffneElternbereichUeberDieElternfrage() {
    onNodeWithTag("island_eltern").performClick()
    // Das Tor entscheidet erst nach der ersten Antwort des DataStore, ob es die Elternfrage
    // oder die PIN zeigt – vorher ist der Bildschirm leer.
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithTag("adult_gate_question").fetchSemanticsNodes().isNotEmpty()
    }
    loeseDieElternfrage()
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithTag("parent_entry_sicherung").fetchSemanticsNodes().isNotEmpty()
    }
}

/** Die angezeigte Multiplikation auslesen und beantworten. Setzt voraus, dass sie schon steht. */
fun ComposeTestRule.loeseDieElternfrage() {
    val node = onNodeWithTag("adult_gate_question").fetchSemanticsNode()
    val text = node.config[SemanticsProperties.Text].joinToString(separator = "") { it.text }
    val match = Regex("""(\d+)\s*[×x*]\s*(\d+)""").find(text)
        ?: error("Unerwartetes Format der Elternfrage: $text")
    val (a, b) = match.destructured
    onNodeWithTag("adult_gate_input").performTextInput((a.toInt() * b.toInt()).toString())
    onNodeWithTag("adult_gate_submit").performClick()
}

/** Aus der Übersicht des Elternbereichs den Eintrag „Sicherung" öffnen. */
fun ComposeTestRule.oeffneSicherung() {
    onNodeWithTag("parent_entry_sicherung").performScrollTo().performClick()
}

/**
 * Setzt die beiden Schalter für Ton und Vorlesefunktion im echten DataStore der App. Wie
 * [clearParentPin] vor dem ersten Compose-Frame aufzurufen – und nach dem Test wieder auf die
 * Voreinstellung zu bringen, sonst reichen sie in andere Testklassen hinein.
 */
fun setzeTonUndVorlesen(tonAn: Boolean, vorlesenAn: Boolean) {
    val app = ApplicationProvider.getApplicationContext<ZahlenschatzjagdApp>()
    runBlocking {
        app.container.tonUndVorlesenRepository.setzeTonAn(tonAn)
        app.container.tonUndVorlesenRepository.setzeVorlesenAn(vorlesenAn)
    }
}

/** Aus der Übersicht den Eintrag „Ton & Vorlesen" öffnen. */
fun ComposeTestRule.oeffneTonUndVorlesen() {
    onNodeWithTag("parent_entry_ton").performScrollTo().performClick()
    warteAuf("ton_switch")
}

/** Vom Bildschirm „Ton & Vorlesen" zurück in die Übersicht. */
fun ComposeTestRule.verlasseTonUndVorlesen() {
    onNodeWithTag("ton_back_button").performScrollTo().performClick()
    warteAuf("parent_entry_ton")
}

/** Aus der Übersicht den Eintrag „Zugang" öffnen – dort liegen PIN und Profilwechsel-Schalter. */
fun ComposeTestRule.oeffneZugang() {
    onNodeWithTag("parent_entry_zugang").performScrollTo().performClick()
    warteAuf("parent_pin_manage_button")
}

/** Vom Zugang zurück in die Übersicht. */
fun ComposeTestRule.verlasseDenZugang() {
    onNodeWithTag("zugang_back_button").performScrollTo().performClick()
    warteAuf("parent_entry_zugang")
}

/** Aus der Übersicht zurück auf die Startkarte. */
fun ComposeTestRule.verlasseDenElternbereich() {
    onNodeWithTag("parent_back_button").performScrollTo().performClick()
    warteAuf("island_eltern")
}

/**
 * Warten, bis eine Marke auftaucht. Fast jeder Schritt rund um den Elternbereich braucht das:
 * Erst die Antwort des DataStore entscheidet, was gezeichnet wird.
 */
fun ComposeTestRule.warteAuf(testTag: String) {
    warteBis { onAllNodesWithTag(testTag).fetchSemanticsNodes().isNotEmpty() }
}

fun ComposeTestRule.warteBis(bedingung: ComposeTestRule.() -> Boolean) {
    waitUntil(timeoutMillis = 10_000) { bedingung() }
}
