package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Eine kleine Sicherungsaufgabe vor dem Elternbereich („Adult-Gate"). Bewusst eine
 * Multiplikation, die für Grundschulkinder der Zielgruppe (noch) zu schwer ist – so bleibt
 * der Elternbereich vor versehentlichem oder kindlichem Zugriff geschützt. Dieses Muster ist
 * in Kinder-Apps üblich und faktisch von Google Play Families gefordert.
 */
data class AdultChallenge(val a: Int, val b: Int) {
    /** Die anzuzeigende Frage, z. B. „7 × 8 = ?". */
    val question: String get() = "$a × $b = ?"

    /** Prüft, ob [answer] die richtige Lösung ist. */
    fun accepts(answer: Int): Boolean = answer == a * b
}

/**
 * Erzeugt eine Challenge mit zwei einstelligen Faktoren im „schwierigen" Bereich (6–9),
 * damit das Ergebnis zweistellig und für die Zielgruppe unbequem ist. [random] ist
 * injizierbar, damit die Erzeugung deterministisch getestet werden kann.
 */
fun createAdultChallenge(random: Random = Random.Default): AdultChallenge =
    AdultChallenge(a = random.nextInt(6, 10), b = random.nextInt(6, 10))

/**
 * Der Gate-Bildschirm: zeigt die [AdultChallenge] und ein Zahlenfeld. Bei richtiger Antwort
 * [onPass], sonst wird die Eingabe geleert und ein Hinweis gezeigt. [onCancel] führt zurück.
 */
@Composable
fun AdultGate(
    onPass: () -> Unit,
    onCancel: () -> Unit,
) {
    // Eine feste Challenge pro Aufruf (nicht bei jedem Tastendruck neu).
    val challenge = remember { createAdultChallenge() }
    var input by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nur für Erwachsene ⚓",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Bitte löse zur Bestätigung:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = challenge.question,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag("adult_gate_question")
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it.filter(Char::isDigit)
                showError = false
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            isError = showError,
            modifier = Modifier.testTag("adult_gate_input")
        )
        if (showError) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Das stimmt nicht ganz.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val answer = input.toIntOrNull()
                if (answer != null && challenge.accepts(answer)) onPass() else showError = true
            },
            modifier = Modifier.testTag("adult_gate_submit")
        ) {
            Text("Weiter")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onCancel, modifier = Modifier.testTag("adult_gate_cancel")) {
            Text("Zurück")
        }
    }
}
