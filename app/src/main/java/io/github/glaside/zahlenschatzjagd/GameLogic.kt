package io.github.glaside.zahlenschatzjagd

import kotlin.random.Random

data class Task(
    val num1: Int,
    val num2: Int,
    val operation: String, // Anzeige-Symbol: "+", "-", "·", ":"
    val correctAnswer: Int,
    val category: String, // stabiler Key für die Historie/Abzeichen: "PLUS", "MINUS", ...
    // Nur für „Verliebte Zahlen" relevant: steht die Lücke vor num1 (statt danach)?
    val firstIsBlank: Boolean = false,
    // Nur gesetzt, wenn die Aufgabe als Auswahl (Multiple-Choice) statt Zifferneingabe
    // beantwortet wird: enthält genau 3 Werte (inkl. correctAnswer) in zufälliger Reihenfolge.
    val answerOptions: List<Int>? = null
)

/**
 * Der anzuzeigende Gleichungs-Text mit [blank] an der gesuchten Stelle. **Einzige Quelle der
 * Wahrheit** für die Aufgabendarstellung, damit GameScreen (Eingabe) und GameViewModel
 * (Historie mit "_") nicht auseinanderlaufen.
 *
 * Bei „verliebten Zahlen" steht die Lücke **zwischen** den Zahlen (`6 + _ = 10` oder
 * `_ + 6 = 10`, je nach [Task.firstIsBlank]; Zielzahl = [num2]), bei allen anderen Typen am
 * Ende (`3 · 4 = _`).
 */
fun Task.equation(blank: String): String = when (category) {
    "VERLIEBTE_ZAHLEN", "VERLIEBTE_ZAHLEN_OHNE_HILFE" ->
        if (firstIsBlank) "$blank + $num1 = $num2" else "$num1 + $blank = $num2"
    // Beim Zahlvergleich steht die Lücke **zwischen** den Zahlen (`3 _ 7`); gesucht ist das
    // Relationszeichen, nicht eine Zahl am Ende. Bewusst mit extra Leerraum (statt einem
    // einzelnen Leerzeichen), damit die Zahlen auf der Tafel nicht gedrungen wirken.
    "GROESSER_KLEINER" -> "$num1   $blank   $num2"
    // Bewusst ohne Rechenzeichen (kein "+"/"·"): Verdoppeln soll als eigenständiges Konzept
    // erkannt werden, nicht als Addition/Multiplikation getarnt (siehe LEHRPLAN.md). Nur für
    // die Historie relevant – GameScreen zeigt „Verdoppeln" stattdessen als eigene, noch
    // schlankere Kachel (Überschrift + Zahl, ganz ohne „=") an.
    "VERDOPPELN_LEICHT", "VERDOPPELN_SCHWER" -> "$num1 verdoppeln = $blank"
    else -> "$num1 $operation $num2 = $blank"
}

/**
 * Die erwartete Antwort als String – für die Historie (`TaskHistory.expectedAnswer`).
 * Bei den Rechenarten die Ergebniszahl, beim Zahlvergleich das korrekte Relationszeichen
 * (das dort in [Task.operation] steht, da die Antwort keine Zahl ist).
 */
fun Task.expectedAnswer(): String = when (category) {
    "GROESSER_KLEINER" -> operation
    else -> correctAnswer.toString()
}

/**
 * Bildet den Auswahl-String einer Rechenart (aus [GameSettings.types]) auf den stabilen
 * Kategorie-/Abzeichen-Key ab. Zentral, damit `createTask` und die Abzeichen-Zuordnung im
 * ViewModel dieselbe Zuordnung nutzen (früher lag hier ein `uppercase()`, das
 * "Verliebte" → "VERLIEBTE" ≠ "VERLIEBTE_ZAHLEN" falsch abbildete).
 *
 * [multipleChoice] wird nur für „Verdoppeln" ausgewertet: leichter (Auswahl-Buttons) und
 * schwerer Modus (Zifferneingabe) sammeln **getrennte** Abzeichen, da sie unterschiedlich
 * schwer sind – anders als bei den übrigen Typen gibt es dafür keine Zahlenraum-Unterteilung
 * (Verdoppeln hat ohnehin nur den festen Zahlenraum 0..10, siehe `LEHRPLAN.md`).
 *
 * [showCoins] wird nur für „Verliebte" ausgewertet: „Mit Hilfe" und „Ohne Hilfe" sind zwei
 * Schwierigkeitsstufen mit eigener Kennung. Die alte Kennung `VERLIEBTE_ZAHLEN` meint „mit
 * Hilfe", damit der bisherige Fortschritt dort weiterzählt
 * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
 */
fun categoryForType(
    type: String,
    multipleChoice: Boolean = false,
    showCoins: Boolean = false,
): String = when (type) {
    "Plus" -> "PLUS"
    "Minus" -> "MINUS"
    "Mal" -> "MAL"
    "Geteilt" -> "GETEILT"
    "Verliebte" -> if (showCoins) "VERLIEBTE_ZAHLEN" else "VERLIEBTE_ZAHLEN_OHNE_HILFE"
    "Vergleichen" -> "GROESSER_KLEINER"
    "Verdoppeln" -> if (multipleChoice) "VERDOPPELN_LEICHT" else "VERDOPPELN_SCHWER"
    else -> type.uppercase()
}

enum class PerformanceTier { PERFECT, GREAT, GOOD, KEEP_GOING }

// Reine, testbare Version der bisherigen generateTask()-Logik aus GameScreen.
// Verhalten unverändert, nur mit injizierbarem Random statt IntRange.random().
fun createTask(
    types: List<String>,
    range: Int,
    random: Random = Random.Default,
    multipleChoice: Boolean = false,
    showCoins: Boolean = false,
): Task {
    val randomOp = types.random(random)
    val num1: Int
    val num2: Int
    val operation: String
    val category: String
    val correctAnswer: Int
    var firstIsBlank = false
    var answerOptions: List<Int>? = null

    when (randomOp) {
        "Plus" -> {
            operation = "+"
            category = "PLUS"
            var a: Int
            var b: Int

            do {
                a = (0..range).random(random)
                b = (0..range).random(random)

                val sumTooLarge = a + b > range

                val hasZero = a == 0 || b == 0
                val hasOne = a == 1 || b == 1

                var suppress = false
                if (hasZero && random.nextDouble() < 0.90) {
                    suppress = true
                } else if (hasOne && random.nextDouble() < 0.70) suppress = true
            } while (sumTooLarge || suppress)

            num1 = a
            num2 = b
            correctAnswer = num1 + num2
        }
        "Minus" -> {
            operation = "-"
            category = "MINUS"
            var a: Int
            var b: Int

            do {
                a = (0..range).random(random)
                b = (0..range).random(random)

                val negativeResult = a < b
                val difference = a - b

                val hasZero = b == 0 || difference == 0
                val hasOne = b == 1 || difference == 1

                var suppress = false
                if (hasZero && random.nextDouble() < 0.90) {
                    suppress = true
                } else if (hasOne && random.nextDouble() < 0.70) suppress = true
            } while (negativeResult || suppress)

            num1 = a
            num2 = b
            correctAnswer = num1 - num2
        }
        "Mal" -> {
            operation = "·"
            category = "MAL"
            // Faktoren skalieren mit dem Zahlenraum, gedeckelt auf kleines Einmaleins (max. 10).
            // sqrt(range) sorgt für sanfte Progression: kleiner Zahlenraum -> kleinere Faktoren.
            val maxFactor = minOf(10, kotlin.math.sqrt(range.toDouble()).toInt())
            var a: Int
            var b: Int

            do {
                a = (0..maxFactor).random(random)
                b = (0..maxFactor).random(random)

                val hasZero = a == 0 || b == 0
                val hasOne = a == 1 || b == 1

                var suppress = false
                if (hasZero && random.nextDouble() < 0.90) {
                    suppress = true
                } else if (hasOne && random.nextDouble() < 0.70) suppress = true
            } while (suppress)

            num1 = a
            num2 = b
            correctAnswer = num1 * num2
        }
        "Geteilt" -> {
            operation = ":"
            category = "GETEILT"
            // Divisor bleibt auf einstellige Zahlen begrenzt (Lehrplan NRW), niemals 0!
            // Divisor und Quotient teilen sich dieselbe sqrt(range)-Deckelung wie beim
            // Malnehmen; der Dividend (b*q) bleibt dadurch im Zahlenraum (bis 100).
            val maxFactor = minOf(10, kotlin.math.sqrt(range.toDouble()).toInt())
            var b: Int
            var q: Int

            do {
                b = (1..maxOf(1, maxFactor)).random(random)
                q = (0..maxFactor).random(random)

                val hasZero = q == 0
                val hasOne = b == 1 || q == 1

                var suppress = false
                if (hasZero && random.nextDouble() < 0.90) {
                    suppress = true
                } else if (hasOne && random.nextDouble() < 0.70) suppress = true
            } while (suppress)

            num2 = b
            num1 = b * q
            correctAnswer = q
        }
        "Verliebte" -> {
            // „Verliebte Zahlen": Zerlegung der Zielzahl (= range, aktuell 10) in num1 + Partner.
            // Angezeigt als „num1 + _ = range" (siehe Task.equation); num2 hält die Zielzahl.
            operation = "+"
            category = categoryForType("Verliebte", showCoins = showCoins)
            var a: Int

            do {
                a = (0..range).random(random)

                // Abweichend von den übrigen Typen: NUR der 0/Zielzahl-Fall (0+10 / 10+0) ist
                // trivial und wird reduziert. Zerlegungen mit 1 (1+9, 9+1) sind hier bewusst
                // KEINE Trivialaufgaben, sondern erwünschter Lernstoff (siehe LEHRPLAN.md).
                val hasZero = a == 0 || a == range

                val suppress = hasZero && random.nextDouble() < 0.90
            } while (suppress)

            num1 = a
            num2 = range
            correctAnswer = range - a
            firstIsBlank = random.nextBoolean()
        }
        "Vergleichen" -> {
            // Zahlvergleich: zwei Zahlen aus 0..range, alle drei Relationszeichen „<", „>" und
            // „=" sind möglich (siehe LEHRPLAN.md). Die richtige Antwort ist das Vergleichszeichen
            // selbst und wird in [operation] gehalten; [correctAnswer] bleibt ungenutzt (die
            // Antwort ist keine Zahl). Anders als bei den Rechenarten werden 0/1 hier NICHT
            // reduziert – „0 < 5", „1 > 0" usw. sind sinnvolle Vergleiche (siehe LEHRPLAN.md).
            category = "GROESSER_KLEINER"
            val a = (0..range).random(random)
            val b = (0..range).random(random)

            operation = when {
                a > b -> ">"
                a < b -> "<"
                else -> "="
            }
            num1 = a
            num2 = b
            correctAnswer = 0
        }
        "Verdoppeln" -> {
            // Verdoppeln: Ausgangszahl 0..10 (Kernaufgaben-Basis, siehe LEHRPLAN.md), gesucht
            // ist das Doppelte. Als Addition (num1+num1) modelliert, damit expectedAnswer()/
            // Historie ohne Sonderfall funktionieren; die Anzeige (Task.equation) blendet das
            // Rechenzeichen bewusst aus.
            operation = "+"
            category = categoryForType("Verdoppeln", multipleChoice)
            val a = (0..10).random(random)
            num1 = a
            num2 = a
            correctAnswer = a * 2
            if (multipleChoice) {
                answerOptions = generateVerdoppelnDistractors(a, correctAnswer, random)
            }
        }
        else -> throw IllegalArgumentException("Unbekannte Rechenart: $randomOp")
    }

    return Task(num1, num2, operation, correctAnswer, category, firstIsBlank, answerOptions)
}

/**
 * Erzeugt die 3 Auswahl-Optionen (inkl. [correct]) für den leichten Verdoppeln-Modus. Die zwei
 * falschen Optionen werden aus zwei gemischten Fehler-Strategien gezogen ("off-by-one" am
 * Ergebnis *und* Verdopplung der Nachbarzahl), damit Kinder kein festes Auswahlmuster statt der
 * Verdopplung selbst lernen (siehe LEHRPLAN.md, Abschnitt "Verdoppeln"). Reihenfolge inkl. der
 * Position von [correct] wird zusätzlich gemischt.
 */
private fun generateVerdoppelnDistractors(num1: Int, correct: Int, random: Random): List<Int> {
    val candidates = setOf(
        correct - 1,
        correct + 1,
        2 * (num1 - 1),
        2 * (num1 + 1)
    ).filter { it >= 0 && it != correct }

    val chosen = candidates.shuffled(random).take(2)
    return (listOf(correct) + chosen).shuffled(random)
}

fun isAnswerCorrect(userInput: String, correctAnswer: Int): Boolean {
    return userInput.toIntOrNull() == correctAnswer
}

fun performanceTier(correct: Int, total: Int): PerformanceTier {
    val percentage = if (total > 0) correct.toFloat() / total.toFloat() else 0f
    return when {
        percentage >= 1f -> PerformanceTier.PERFECT
        percentage >= 0.8f -> PerformanceTier.GREAT
        percentage >= 0.5f -> PerformanceTier.GOOD
        else -> PerformanceTier.KEEP_GOING
    }
}

/**
 * Anzahl verdienter Sterne (0–3) einer Runde, abgeleitet aus der Trefferquote.
 * Dient als *einzige* Rundenbewertung im Auswertungs-Bildschirm; die Schwellen sind hier
 * zentral anpassbar.
 */
fun earnedStars(correct: Int, total: Int): Int {
    val percentage = if (total > 0) correct.toFloat() / total.toFloat() else 0f
    return when {
        percentage >= 1f -> 3 // alles richtig
        percentage >= 0.75f -> 2
        percentage >= 0.5f -> 1
        else -> 0
    }
}
