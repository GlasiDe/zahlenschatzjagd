package io.github.glaside.zahlenschatzjagd.data

import java.security.MessageDigest
import kotlin.random.Random

/**
 * Der Zugang zum Elternbereich, soweit er sich ohne Android entscheiden lässt: Prüfsummen,
 * Normalisierung der Antwort und die Frage, welches Tor gilt.
 *
 * Zwei Tore stehen nebeneinander (siehe `docs/adr/0016-pin-optional-neben-der-elternfrage.md`):
 * Die **Elternfrage** gilt immer, solange keine PIN gesetzt ist. Erst hinter ihr lässt sich
 * eine PIN vergeben – ohne diese Reihenfolge könnte ein Kind auf einem frisch aktualisierten
 * Gerät selbst eine PIN setzen und die Erwachsenen aussperren.
 *
 * Frei von Compose und Android, damit die Prüflogik ohne Gerät testbar ist – und deshalb in
 * `data/` statt bei den Bildschirmen: [ParentAccessRepository] muss diese Typen kennen, und die
 * Datenschicht darf nicht in die Oberfläche hineingreifen (`docs/agents/testing.md`, „Der
 * Schnitt bestimmt, wo Code liegt").
 */

/** Die Länge der PIN. Kürzer wäre zu leicht, länger auf dem Handy lästig. */
const val PIN_LENGTH = 4

/**
 * Der gesetzte Zugang: PIN und Antwort auf die Sicherheitsfrage liegen **nur als Prüfsumme**
 * auf dem Gerät. Die PIN wird deshalb nie angezeigt; wer sie vergisst, vergibt nach richtiger
 * Antwort eine neue.
 *
 * „Keine PIN gesetzt" ist kein eigener Zustand dieser Klasse, sondern schlicht `null`.
 */
data class ParentAccess(
    val salt: String,
    val pinChecksum: String,
    /** Die frei formulierte Sicherheitsfrage – als Einzige im Klartext, sie wird ja gezeigt. */
    val question: String,
    val answerChecksum: String,
) {
    /** Prüft die eingegebene PIN gegen die gespeicherte Prüfsumme. */
    fun acceptsPin(pin: String): Boolean = checksum(pin, salt) == pinChecksum

    /**
     * Prüft die Antwort auf die Sicherheitsfrage. Wird vor dem Vergleich normalisiert, sonst
     * scheitert die Rettung an „Meier" gegen „meier" – im ungünstigsten Moment.
     */
    fun acceptsAnswer(rawAnswer: String): Boolean =
        checksum(normalizeAnswer(rawAnswer), salt) == answerChecksum
}

/** Welches Tor vor dem Elternbereich steht. */
enum class ParentGate { ELTERNFRAGE, PIN }

/** Keine PIN gesetzt → Elternfrage. PIN gesetzt → PIN. Die Elternfrage entfällt dann. */
fun gateFor(access: ParentAccess?): ParentGate =
    if (access == null) ParentGate.ELTERNFRAGE else ParentGate.PIN

/**
 * Bringt eine frei formulierte Antwort auf eine vergleichbare Form: außen abschneiden, innere
 * Leerzeichenfolgen auf eines eindampfen, kleinschreiben. [lowercase] ohne Locale ist bewusst
 * gewählt – es arbeitet locale-unabhängig, sonst würde dieselbe Antwort auf einem türkisch
 * eingestellten Gerät anders normalisiert als auf einem deutschen.
 */
fun normalizeAnswer(rawAnswer: String): String =
    rawAnswer.trim().replace(WHITESPACE, " ").lowercase()

/**
 * SHA-256 über Salt und Wert, hexadezimal. Bewusst kein PBKDF2 mit vielen Runden: Die Gefahr
 * hier ist ein Kind mit dem Familien-Handy, nicht jemand, der die DataStore-Datei offline
 * durchprobiert. Das Salt sorgt dafür, dass zwei Geräte mit derselben PIN nicht dieselbe
 * Prüfsumme tragen.
 */
fun checksum(value: String, salt: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest((salt + value).toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

/**
 * Ein frisches Salt für einen neu vergebenen Zugang. [random] ist injizierbar, damit die
 * Erzeugung deterministisch getestet werden kann – wie bei `createAdultChallenge`.
 */
fun newSalt(random: Random = Random.Default): String =
    (1..SALT_BYTES).joinToString("") { "%02x".format(random.nextInt(0, BYTE_VALUES)) }

/** Genau [PIN_LENGTH] Ziffern, sonst nichts. */
fun isValidPin(pin: String): Boolean =
    pin.length == PIN_LENGTH && pin.all(Char::isDigit)

/** Was aus einer ausgefüllten PIN-Vergabe folgt. */
sealed interface PinSetupAction {
    /** PIN, Sicherheitsfrage und Antwort werden gemeinsam neu gesetzt. */
    data class SetAll(val pin: String, val question: String, val answer: String) : PinSetupAction

    /**
     * Nur die PIN wird ausgetauscht; Sicherheitsfrage und Antwort bleiben, wie sie sind. Das
     * ist der übliche Fall beim Ändern: Wer seine PIN wechselt, will deshalb nicht auch noch
     * seine Frage neu erfinden.
     */
    data class SetPinOnly(val pin: String) : PinSetupAction

    /** Es fehlt noch etwas – [reason] ist der anzeigbare Satz dazu. */
    data class Rejected(val reason: String) : PinSetupAction
}

/**
 * Entscheidet, was eine ausgefüllte Vergabe bewirkt. Bewusst außerhalb des Bildschirms, damit
 * die Regeln ohne Gerät prüfbar sind.
 *
 * [existingQuestion] ist die bereits gesetzte Sicherheitsfrage – `null` beim ersten Einrichten.
 * Bleibt sie unangetastet und das Antwortfeld leer, bleibt auch die gespeicherte Antwort
 * bestehen; sie liegt ohnehin nur als Prüfsumme vor und ließe sich gar nicht vorbelegen.
 */
fun pinSetupAction(
    existingQuestion: String?,
    pin: String,
    pinRepeat: String,
    question: String,
    answer: String,
): PinSetupAction = when {
    !isValidPin(pin) ->
        PinSetupAction.Rejected("Die PIN muss aus genau $PIN_LENGTH Ziffern bestehen.")

    pin != pinRepeat ->
        PinSetupAction.Rejected("Die beiden PINs sind nicht gleich.")

    existingQuestion != null && question.trim() == existingQuestion && answer.isBlank() ->
        PinSetupAction.SetPinOnly(pin)

    question.isBlank() ->
        PinSetupAction.Rejected("Bitte formuliere eine Frage.")

    normalizeAnswer(answer).isEmpty() ->
        PinSetupAction.Rejected("Bitte gib die Antwort auf deine Frage ein.")

    else -> PinSetupAction.SetAll(pin, question, answer)
}

private const val SALT_BYTES = 16

private const val BYTE_VALUES = 256

private val WHITESPACE = Regex("""\s+""")
