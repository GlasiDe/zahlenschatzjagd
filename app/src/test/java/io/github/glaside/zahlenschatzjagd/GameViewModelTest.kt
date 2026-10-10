package io.github.glaside.zahlenschatzjagd

import io.github.glaside.zahlenschatzjagd.ui.game.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameViewModelTest {

    private fun viewModel(count: Int = 3): GameViewModel =
        // Nur "Plus", damit die erwartete Lösung direkt aus der Aufgabe folgt.
        GameViewModel(
            settings = GameSettings(types = listOf("Plus"), range = 10, count = count),
            random = Random(1234),
        )

    /** Tippt eine Zahl Ziffer für Ziffer ins ViewModel. */
    private fun GameViewModel.typeNumber(value: Int) {
        value.toString().forEach { onDigit(it.toString()) }
    }

    @Test
    fun initialState_hasFirstTaskAndNoFeedback() {
        val vm = viewModel(count = 5)
        val state = vm.uiState.value

        assertNotNull("Die erste Aufgabe wird sofort erzeugt", state.currentTask)
        assertEquals(1, state.taskNumber)
        assertEquals(5, state.totalTasks)
        assertEquals(0, state.correctAnswers)
        assertEquals("", state.userInput)
        assertNull(state.feedback)
        assertFalse(state.isFinished)
    }

    @Test
    fun onDigit_appendsUpToThreeDigits_onClearResets() {
        val vm = viewModel()
        vm.onDigit("1")
        vm.onDigit("2")
        vm.onDigit("3")
        vm.onDigit("4") // wird ignoriert (max. 3 Ziffern)
        assertEquals("123", vm.uiState.value.userInput)

        vm.onClear()
        assertEquals("", vm.uiState.value.userInput)
    }

    @Test
    fun onCheck_withCorrectAnswer_setsPositiveFeedbackAndCountsUp() {
        val vm = viewModel()
        val expected = vm.uiState.value.currentTask!!.correctAnswer

        vm.typeNumber(expected)
        vm.onCheck()

        val state = vm.uiState.value
        val feedback = state.feedback
        assertNotNull(feedback)
        assertTrue(feedback!!.isCorrect)
        // Feedback.correctAnswer ist seit dem Zahlvergleich ein String (Zahl bzw. Zeichen).
        assertEquals(expected.toString(), feedback.correctAnswer)
        assertEquals(1, state.correctAnswers)
    }

    @Test
    fun onCheck_withWrongAnswer_setsNegativeFeedbackAndDoesNotCount() {
        val vm = viewModel()
        val expected = vm.uiState.value.currentTask!!.correctAnswer

        vm.typeNumber(expected + 1) // garantiert falsch
        vm.onCheck()

        val state = vm.uiState.value
        val feedback = state.feedback
        assertNotNull(feedback)
        assertFalse(feedback!!.isCorrect)
        assertEquals(0, state.correctAnswers)
    }

    @Test
    fun onCheck_withNonNumericEmptyInput_isIgnored() {
        val vm = viewModel()
        vm.onCheck() // leere Eingabe
        assertNull(vm.uiState.value.feedback)
    }

    @Test
    fun onNext_advancesToNextTaskAndClearsFeedback() {
        val vm = viewModel(count = 3)
        val first = vm.uiState.value.currentTask
        vm.typeNumber(first!!.correctAnswer)
        vm.onCheck()

        vm.onNext()

        val state = vm.uiState.value
        assertEquals(2, state.taskNumber)
        assertNull(state.feedback)
        assertEquals("", state.userInput)
        assertFalse(state.isFinished)
    }

    @Test
    fun initialState_uebernimmtShowCoinsAusDenSettings() {
        val mitMuenzen = GameViewModel(
            settings = GameSettings(types = listOf("Verliebte"), range = 10, count = 3, showCoins = true),
            random = Random(1234),
        )
        assertTrue(mitMuenzen.uiState.value.showCoins)

        // Default (klassische Rechenart) bleibt ohne Münzen.
        assertFalse(viewModel().uiState.value.showCoins)
    }

    @Test
    fun verliebteZahlen_erzeugenPartnerAufgabeZurZehn() {
        val vm = GameViewModel(
            settings = GameSettings(types = listOf("Verliebte"), range = 10, count = 3, showCoins = true),
            random = Random(1234),
        )
        val task = vm.uiState.value.currentTask!!
        assertEquals("VERLIEBTE_ZAHLEN", task.category)
        assertEquals(10, task.num2)
        assertEquals(10 - task.num1, task.correctAnswer)
    }

    private fun vergleichenViewModel() = GameViewModel(
        settings = GameSettings(types = listOf("Vergleichen"), range = 20, count = 3),
        random = Random(1234),
    )

    private fun verdoppelnLeichtViewModel() = GameViewModel(
        settings = GameSettings(types = listOf("Verdoppeln"), range = 10, count = 3, multipleChoice = true),
        random = Random(1234),
    )

    /**
     * Ein Tippen auf `<`, `=` oder `>` wählt nur aus: Das Zeichen steht in der Lücke, ausgewertet
     * wird erst mit ✓ – wie bei jeder Zifferneingabe.
     */
    @Test
    fun onCompare_waehltNurAus_ohneAuszuwerten() {
        val vm = vergleichenViewModel()
        val task = vm.uiState.value.currentTask!!
        assertEquals("GROESSER_KLEINER", task.category)

        vm.onCompare(task.operation)

        val state = vm.uiState.value
        assertEquals(task.operation, state.userInput)
        assertNull(state.feedback)
        assertEquals(0, state.correctAnswers)
    }

    @Test
    fun onCompare_eineNeueAuswahlErsetztDieVorige() {
        val vm = vergleichenViewModel()

        vm.onCompare("<")
        vm.onCompare(">")

        assertEquals(">", vm.uiState.value.userInput)
    }

    @Test
    fun onCompare_dannOnCheck_mitRichtigemZeichen_setztPositivesFeedbackUndZaehltHoch() {
        val vm = vergleichenViewModel()
        val task = vm.uiState.value.currentTask!!
        val falsch = if (task.operation == "<") ">" else "<"

        vm.onCompare(falsch) // umentschieden – zählt nicht
        vm.onCompare(task.operation) // das korrekte Vergleichszeichen steht in operation
        vm.onCheck()

        val state = vm.uiState.value
        assertTrue(state.feedback!!.isCorrect)
        assertEquals(1, state.correctAnswers)
        assertEquals(task.operation, state.userInput)
    }

    @Test
    fun onCompare_dannOnCheck_mitFalschemZeichen_zaehltNichtUndZeigtRichtigeUngleichung() {
        val vm = vergleichenViewModel()
        val task = vm.uiState.value.currentTask!!
        val falsch = if (task.operation == "<") ">" else "<"

        vm.onCompare(falsch)
        vm.onCheck()

        val state = vm.uiState.value
        val feedback = state.feedback
        assertNotNull(feedback)
        assertFalse(feedback!!.isCorrect)
        assertEquals(0, state.correctAnswers)
        // Feedback zeigt die vollständige richtige Ungleichung (z. B. "3 < 7").
        assertEquals(task.equation(task.operation), feedback.correctAnswer)
    }

    @Test
    fun onCheck_beimVergleichOhneAuswahl_bewirktNichts() {
        val vm = vergleichenViewModel()

        vm.onCheck()

        assertNull(vm.uiState.value.feedback)
    }

    /** Wie beim Vergleich: Antippen wählt nur aus, die Zahl steht in der Aufgabe. */
    @Test
    fun onSelectOption_waehltNurAus_ohneAuszuwerten() {
        val vm = verdoppelnLeichtViewModel()
        val task = vm.uiState.value.currentTask!!
        assertEquals("VERDOPPELN_LEICHT", task.category)
        assertNotNull("Im leichten Modus sollten Auswahl-Optionen gesetzt sein", task.answerOptions)

        vm.onSelectOption(task.correctAnswer)

        val state = vm.uiState.value
        assertEquals(task.correctAnswer.toString(), state.userInput)
        assertNull(state.feedback)
    }

    @Test
    fun onSelectOption_dannOnCheck_mitRichtigemWert_setztPositivesFeedbackUndZaehltHoch() {
        val vm = verdoppelnLeichtViewModel()
        val task = vm.uiState.value.currentTask!!
        val falsch = task.answerOptions!!.first { it != task.correctAnswer }

        vm.onSelectOption(falsch) // umentschieden – zählt nicht
        vm.onSelectOption(task.correctAnswer)
        vm.onCheck()

        val state = vm.uiState.value
        assertTrue(state.feedback!!.isCorrect)
        assertEquals(1, state.correctAnswers)
    }

    @Test
    fun onSelectOption_dannOnCheck_mitFalschemWert_zaehltNichtHoch() {
        val vm = verdoppelnLeichtViewModel()
        val task = vm.uiState.value.currentTask!!
        val falsch = task.answerOptions!!.first { it != task.correctAnswer }

        vm.onSelectOption(falsch)
        vm.onCheck()

        val state = vm.uiState.value
        assertFalse(state.feedback!!.isCorrect)
        assertEquals(0, state.correctAnswers)
    }

    @Test
    fun onCheck_beiVerdoppelnLeichtOhneAuswahl_bewirktNichts() {
        val vm = verdoppelnLeichtViewModel()

        vm.onCheck()

        assertNull(vm.uiState.value.feedback)
    }

    /** Nach der Rückmeldung lässt sich die abgegebene Antwort nicht mehr umwählen. */
    @Test
    fun nachDemAbgeben_aendertEineAuswahlNichtsMehr() {
        val vm = vergleichenViewModel()
        val task = vm.uiState.value.currentTask!!
        vm.onCompare(task.operation)
        vm.onCheck()

        vm.onCompare(if (task.operation == "<") ">" else "<")

        assertEquals(task.operation, vm.uiState.value.userInput)
    }

    @Test
    fun verdoppeln_feedbackZeigtAequivalenzZuAdditionUndMultiplikation() {
        val vm = GameViewModel(
            settings = GameSettings(types = listOf("Verdoppeln"), range = 10, count = 3),
            random = Random(1234),
        )
        val task = vm.uiState.value.currentTask!!

        vm.typeNumber(task.correctAnswer)
        vm.onCheck()

        // „2 · n" und nicht „n · 2": zweimal die Zahl – so lesen es Grundschulkinder.
        val n = task.num1
        assertEquals(
            "$n + $n = ${2 * n}   2 · $n = ${2 * n}",
            vm.uiState.value.feedback!!.extraExplanation,
        )
    }

    @Test
    fun andereAufgabentypen_habenKeineAequivalenzErklaerung() {
        val vm = viewModel()
        val task = vm.uiState.value.currentTask!!

        vm.typeNumber(task.correctAnswer)
        vm.onCheck()

        assertNull(vm.uiState.value.feedback!!.extraExplanation)
    }

    @Test
    fun answeringAllTasksCorrectly_finishesWithFullScore() {
        val count = 4
        val vm = viewModel(count = count)

        repeat(count) {
            val task = vm.uiState.value.currentTask!!
            vm.typeNumber(task.correctAnswer)
            vm.onCheck()
            vm.onNext()
        }

        val state = vm.uiState.value
        assertTrue(state.isFinished)
        assertEquals(count, state.correctAnswers)
    }

    /**
     * Der **Ton** einer Aufgabe steht aus, bis die Oberfläche ihn quittiert – danach nicht mehr,
     * auch wenn der Bildschirm neu aufgebaut wird
     * (`docs/adr/0030-einmalige-ausgaben-ueberstehen-das-drehen.md`).
     */
    @Test
    fun nachDemPruefenStehtDerTonAus_bisErQuittiertIst() {
        val vm = viewModel()
        assertNull("Vor dem Prüfen steht kein Ton aus", vm.uiState.value.ausstehenderTon)

        vm.typeNumber(vm.uiState.value.currentTask!!.correctAnswer)
        vm.onCheck()
        assertEquals(GameSound.CORRECT, vm.uiState.value.ausstehenderTon)

        vm.tonGespielt()
        assertNull(vm.uiState.value.ausstehenderTon)
        assertNotNull("Die Auswertung bleibt sichtbar", vm.uiState.value.feedback)
    }

    /** Ein nicht gespielter Ton gehört zur vorigen Aufgabe und kommt nicht bei der nächsten. */
    @Test
    fun jedeGepruefteAufgabeBringtIhrenEigenenTon() {
        val vm = viewModel()
        vm.typeNumber(vm.uiState.value.currentTask!!.correctAnswer)
        vm.onCheck()
        vm.onNext()
        assertNull(vm.uiState.value.ausstehenderTon)

        vm.typeNumber(vm.uiState.value.currentTask!!.correctAnswer + 1)
        vm.onCheck()

        assertEquals(GameSound.WRONG, vm.uiState.value.ausstehenderTon)
    }
}
