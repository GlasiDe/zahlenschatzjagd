package io.github.glaside.zahlenschatzjagd

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

/**
 * Frische Installationen haben kein Profil; die App leitet dann auf den Profilbildschirm um, wo
 * zwei Wege weiterführen – anlegen oder eine Sicherung einspielen (siehe `ZahlenschatzjagdNavHost`
 * und `docs/adr/0023-erststart-bietet-die-sicherung-an.md`). Andere Instrumented Tests erwarten
 * aber direkt die Startkarte, unabhängig vom Profil-Zustand des jeweiligen Testgeräts. Diese
 * Hilfsfunktion geht einmalig den Weg über das Anlegen, falls die App tatsächlich dort
 * gelandet ist, und ist andernfalls ein No-Op.
 *
 * Direkt nach dem Activity-Start ist weder der Profilbildschirm noch die Startkarte sicher
 * sichtbar: Der `NavHost` wartet erst auf die erste (asynchrone) Room-Antwort, bevor er
 * entscheidet, ob umgeleitet wird. Ein einmaliger synchroner Check direkt nach dem Start
 * wäre daher ein Race – `waitUntil` sorgt dafür, dass wirklich einer der beiden
 * Endzustände erreicht ist, bevor entschieden wird.
 */
fun ComposeTestRule.ensureActiveProfileExists(name: String = "Testpirat") {
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithTag("profile_add_button").fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag("home_profile_badge").fetchSemanticsNodes().isNotEmpty()
    }
    // Trägt die App eine markierte Version, die auf dem Testgerät noch nicht gezeigt war, liegt
    // der Änderungshinweis über allem. Einmal weggetippt, ist er für alle folgenden Tests erledigt.
    waitForIdle()
    if (onAllNodesWithTag("aenderungshinweis_verstanden").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("aenderungshinweis_verstanden").performClick()
    }
    if (onAllNodesWithTag("profile_add_button").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("profile_add_button").performClick()
        onNodeWithTag("profile_name_input").performTextInput(name)
        onNodeWithTag("profile_create_confirm").performClick()
        waitUntil(timeoutMillis = 10_000) {
            onAllNodesWithTag("first_profile_hint_confirm").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("first_profile_hint_confirm").performClick()
    }
}
