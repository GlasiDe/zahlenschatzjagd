package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.data.PIN_LENGTH
import io.github.glaside.zahlenschatzjagd.data.ParentAccess

/**
 * Das PIN-Tor vor dem Elternbereich – es tritt an die Stelle der Elternfrage ([AdultGate]),
 * sobald eine PIN gesetzt ist.
 *
 * **Fehlversuche bleiben unbegrenzt und ohne Wartezeit** (siehe
 * `docs/adr/0016-pin-optional-neben-der-elternfrage.md`): Eine harte Sperre nach N Versuchen
 * wäre eine Selbstsperre und würde den Rückweg entwerten, den dieser Bildschirm gerade erst
 * anbietet.
 *
 * Der Rückweg führt über die selbst formulierte Sicherheitsfrage. Die vergessene PIN wird dabei
 * **nie angezeigt** – sie liegt nur als Prüfsumme vor; nach richtiger Antwort vergibt man eine
 * neue ([onResetPassed]).
 */
@Composable
fun ParentPinGate(
    access: ParentAccess,
    onPass: () -> Unit,
    onResetPassed: () -> Unit,
    onCancel: () -> Unit,
) {
    var askSecurityQuestion by remember { mutableStateOf(false) }

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
        Spacer(Modifier.height(16.dp))
        if (askSecurityQuestion) {
            SecurityQuestionStep(
                access = access,
                onPassed = onResetPassed,
                onBack = { askSecurityQuestion = false },
            )
        } else {
            PinStep(
                access = access,
                onPass = onPass,
                onForgotten = { askSecurityQuestion = true },
                onCancel = onCancel,
            )
        }
    }
}

/** Schritt 1: die PIN eingeben. */
@Composable
private fun PinStep(
    access: ParentAccess,
    onPass: () -> Unit,
    onForgotten: () -> Unit,
    onCancel: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Text(text = "Bitte gib die PIN ein:", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = input,
        onValueChange = {
            input = it.filter(Char::isDigit).take(PIN_LENGTH)
            showError = false
        },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        isError = showError,
        modifier = Modifier.testTag("parent_pin_input")
    )
    if (showError) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Das stimmt nicht ganz.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("parent_pin_error")
        )
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { if (access.acceptsPin(input)) onPass() else showError = true },
        modifier = Modifier.testTag("parent_pin_submit")
    ) {
        Text("Weiter")
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onForgotten, modifier = Modifier.testTag("parent_pin_forgot")) {
        Text("PIN vergessen?")
    }
    TextButton(onClick = onCancel, modifier = Modifier.testTag("parent_pin_cancel")) {
        Text("Zurück")
    }
}

/** Schritt 2: der Rückweg über die selbst formulierte Sicherheitsfrage. */
@Composable
private fun SecurityQuestionStep(
    access: ParentAccess,
    onPassed: () -> Unit,
    onBack: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Text(
        text = access.question,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_security_question")
    )
    Spacer(Modifier.height(16.dp))
    // Verborgen wie die PIN selbst: Wer die Antwort mitliest, kommt an ihr vorbei in den
    // Elternbereich – und genau hier steht sie vor den Augen dessen, der sie erraten will.
    OutlinedTextField(
        value = input,
        onValueChange = {
            input = it
            showError = false
        },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        isError = showError,
        modifier = Modifier.testTag("parent_security_answer_input")
    )
    if (showError) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Das stimmt nicht ganz.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("parent_security_answer_error")
        )
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { if (access.acceptsAnswer(input)) onPassed() else showError = true },
        modifier = Modifier.testTag("parent_security_answer_submit")
    ) {
        Text("Weiter")
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Stimmt die Antwort, vergibst du gleich eine neue PIN. Die alte lässt sich " +
            "nicht anzeigen.",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onBack, modifier = Modifier.testTag("parent_security_back")) {
        Text("Zurück zur PIN")
    }
}
