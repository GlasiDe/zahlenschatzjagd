package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.glaside.zahlenschatzjagd.data.PIN_LENGTH
import io.github.glaside.zahlenschatzjagd.data.PinSetupAction
import io.github.glaside.zahlenschatzjagd.data.pinSetupAction

/**
 * Das Vergeben einer neuen PIN samt Sicherheitsfrage.
 *
 * Erreichbar nur von innen – aus dem Elternbereich (also hinter Elternfrage oder alter PIN)
 * oder nach richtiger Antwort auf die Sicherheitsfrage. Diese Reihenfolge ist der Punkt:
 * Ohne sie könnte ein Kind auf einem frisch aktualisierten Gerät selbst eine PIN setzen und die
 * Erwachsenen aussperren (`docs/adr/0016-pin-optional-neben-der-elternfrage.md`).
 *
 * Frage und Antwort sind **frei formuliert**. Feste Fragenlisten sind im Haushalt wertlos:
 * Haustier, Geburtsort und Lieblingsessen kennt das Kind besser als die Eltern. Deshalb steht
 * der Hinweis, etwas zu wählen, das das eigene Kind nicht weiß, sichtbar auf dem Bildschirm.
 */
@Composable
fun ParentPinSetupScreen(
    existingQuestion: String?,
    onSave: (pin: String, question: String, answer: String) -> Unit,
    onChangePinOnly: (pin: String) -> Unit,
    onBack: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var pinRepeat by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(existingQuestion.orEmpty()) }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ParentPage(
        title = if (existingQuestion == null) "PIN einrichten" else "PIN ändern",
        onBack = onBack,
        backTestTag = "parent_pin_setup_back_button",
    ) {
        Text(
            text = "Mit einer PIN ist der Elternbereich auch dann noch geschützt, wenn dein " +
                "Kind die Rechenaufgabe lösen kann.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        PinFields(
            pin = pin,
            pinRepeat = pinRepeat,
            onPinChange = {
                pin = it
                error = null
            },
            onPinRepeatChange = {
                pinRepeat = it
                error = null
            },
        )

        Spacer(Modifier.height(24.dp))
        SecurityQuestionFields(
            existingQuestion = existingQuestion,
            question = question,
            answer = answer,
            onQuestionChange = {
                question = it
                error = null
            },
            onAnswerChange = {
                answer = it
                error = null
            },
        )

        ErrorText(error)

        Spacer(Modifier.height(20.dp))
        SaveButton {
            when (val action = pinSetupAction(existingQuestion, pin, pinRepeat, question, answer)) {
                is PinSetupAction.SetAll -> onSave(action.pin, action.question, action.answer)
                is PinSetupAction.SetPinOnly -> onChangePinOnly(action.pin)
                is PinSetupAction.Rejected -> error = action.reason
            }
        }
    }
}

/** Was einer Vergabe noch im Weg steht – nur sichtbar, wenn es etwas zu sagen gibt. */
@Composable
private fun ErrorText(error: String?) {
    error?.let {
        Spacer(Modifier.height(12.dp))
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("parent_pin_setup_error")
        )
    }
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_pin_save_button")
    ) {
        Text("PIN speichern")
    }
}

/** Die neue PIN, zweimal einzugeben. */
@Composable
private fun PinFields(
    pin: String,
    pinRepeat: String,
    onPinChange: (String) -> Unit,
    onPinRepeatChange: (String) -> Unit,
) {
    PinField(
        value = pin,
        label = "Neue PIN ($PIN_LENGTH Ziffern)",
        testTag = "parent_pin_new_input",
        onValueChange = onPinChange,
    )
    Spacer(Modifier.height(8.dp))
    PinField(
        value = pinRepeat,
        label = "PIN wiederholen",
        testTag = "parent_pin_repeat_input",
        onValueChange = onPinRepeatChange,
    )
}

/**
 * Der Rückweg für den Fall, dass die PIN vergessen wird – frei formuliert.
 *
 * Beim Ändern einer vorhandenen PIN ist das Ganze **freiwillig**: Wer Frage und Antwortfeld so
 * lässt, wie sie sind, behält den bisherigen Rückweg. Die Antwort ließe sich ohnehin nicht
 * vorbelegen, sie liegt nur als Prüfsumme vor.
 */
@Composable
private fun SecurityQuestionFields(
    existingQuestion: String?,
    question: String,
    answer: String,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
) {
    // Die Antwort ist so schutzwürdig wie die PIN selbst – wer sie mitliest, kommt an ihr
    // vorbei in den Elternbereich. Ein Tippfehler wäre hier aber besonders teuer, weil er den
    // Rückweg still zerstört: deshalb zusätzlich der Schalter zum Aufdecken.
    var answerVisible by remember { mutableStateOf(false) }
    val changing = existingQuestion != null

    Text(text = "Falls du die PIN vergisst", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    // Der Hinweis, etwas zu wählen, das das eigene Kind nicht weiß, steht in **beiden** Fällen:
    // ADR-0016 verlangt ihn, und gebraucht wird er gerade beim Ändern – dorthin führt auch der
    // Weg nach dem Rücksetzen, wo eine neue Frage formuliert wird.
    Text(
        text = if (changing) {
            "Frage und Antwort bleiben, solange du sie nicht anfasst. Formulierst du eine neue " +
                "Frage, wähle wieder etwas, das dein Kind nicht weiß."
        } else {
            "Die PIN lässt sich später nicht anzeigen. Mit dieser Frage kommst du " +
                "trotzdem wieder herein – wähle etwas, das dein Kind nicht weiß."
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("parent_pin_setup_hint")
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = question,
        onValueChange = onQuestionChange,
        label = { Text("Deine Frage") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_pin_question_input")
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = answer,
        onValueChange = onAnswerChange,
        // Beim Ändern sagt die Beschriftung genau, was der Code tut: Was hier steht, ersetzt die
        // gespeicherte Antwort – auch bei unveränderter Frage. „Nur bei neuer Frage" hatte das
        // Gegenteil behauptet und hätte einen versehentlichen Tastendruck zum stillen Verlust
        // des Rückwegs gemacht.
        label = {
            Text(if (changing) "Neue Antwort (leer lassen, dann bleibt sie)" else "Deine Antwort")
        },
        singleLine = true,
        visualTransformation = if (answerVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_pin_answer_input")
    )
    TextButton(
        onClick = { answerVisible = !answerVisible },
        modifier = Modifier.testTag("parent_pin_answer_visibility")
    ) {
        Text(if (answerVisible) "Antwort verbergen" else "Antwort anzeigen")
    }
    Text(
        text = "Groß- und Kleinschreibung spielt später keine Rolle.",
        style = MaterialTheme.typography.bodySmall
    )
}

/** Zahlenfeld für eine PIN – maskiert, auf Ziffern und [PIN_LENGTH] Stellen begrenzt. */
@Composable
private fun PinField(
    value: String,
    label: String,
    testTag: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(PIN_LENGTH)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
