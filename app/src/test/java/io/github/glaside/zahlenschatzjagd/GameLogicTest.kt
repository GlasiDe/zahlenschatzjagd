package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    private val iterations = 500

    @Test
    fun createTask_plus_staysWithinRangeAndSumsCorrectly() {
        val random = Random(1)
        repeat(iterations) {
            val task = createTask(listOf("Plus"), range = 10, random = random)
            assertEquals("+", task.operation)
            assertTrue("num1 sollte im Zahlenraum liegen: ${task.num1}", task.num1 in 0..10)
            assertTrue("num2 sollte nicht negativ sein: ${task.num2}", task.num2 >= 0)
            assertEquals(task.num1 + task.num2, task.correctAnswer)
            assertTrue("Summe sollte im Zahlenraum bleiben: ${task.correctAnswer}", task.correctAnswer <= 10)
        }
    }

    @Test
    fun createTask_minus_neverNegativeAndSubtractsCorrectly() {
        val random = Random(2)
        repeat(iterations) {
            val task = createTask(listOf("Minus"), range = 10, random = random)
            assertEquals("-", task.operation)
            assertTrue("num2 darf num1 nicht übersteigen", task.num2 <= task.num1)
            assertEquals(task.num1 - task.num2, task.correctAnswer)
            assertTrue("Ergebnis darf nicht negativ sein: ${task.correctAnswer}", task.correctAnswer >= 0)
        }
    }

    @Test
    fun createTask_mal_staysWithinRangeAndMultipliesCorrectly() {
        val random = Random(3)
        repeat(iterations) {
            val task = createTask(listOf("Mal"), range = 10, random = random)
            assertEquals("·", task.operation)
            // Faktoren sind auf sqrt(range) gedeckelt (kleines Einmaleins). Bei range = 10
            // ergibt das 0..3; die 0 ist möglich, da Nullen nur reduziert, nicht verboten sind.
            assertTrue("Erster Faktor sollte im gedeckelten Bereich liegen: ${task.num1}", task.num1 in 0..3)
            assertTrue("Zweiter Faktor sollte im gedeckelten Bereich liegen: ${task.num2}", task.num2 in 0..3)
            assertEquals(task.num1 * task.num2, task.correctAnswer)
            assertTrue("Produkt sollte im Zahlenraum bleiben: ${task.correctAnswer}", task.correctAnswer <= 10)
        }
    }

    @Test
    fun createTask_geteilt_dividesEvenlyAndQuotientIsCorrect() {
        val random = Random(4)
        repeat(iterations) {
            val task = createTask(listOf("Geteilt"), range = 10, random = random)
            assertEquals(":", task.operation)
            // Divisor bleibt einstellig (Lehrplan NRW) und zusätzlich auf sqrt(range) gedeckelt:
            // bei range = 10 also 1..3, niemals 0.
            assertTrue("Teiler sollte im gedeckelten Bereich liegen: ${task.num2}", task.num2 in 1..3)
            assertEquals(0, task.num1 % task.num2)
            assertEquals(task.num1 / task.num2, task.correctAnswer)
        }
    }

    @Test
    fun createTask_plus_reducesTrivialZeroAndOneOperands() {
        // Sichert die Kern-Absicht der Generierungs-Überarbeitung ab: Aufgaben mit 0 oder 1
        // als Operand sollen deutlich seltener sein als bei ungefiltertem Uniform-Sampling.
        // Bei range = 10 lägen ungefiltert ~19 % der Werte auf 0 bzw. 1 pro Operand; mit
        // beiden Operanden entsprechend deutlich höher. Wir prüfen nur eine grobe Obergrenze,
        // ohne konkrete PRNG-Werte zu verdrahten.
        val random = Random(42)
        val total = 1000
        var withZero = 0
        var withOne = 0
        repeat(total) {
            val task = createTask(listOf("Plus"), range = 10, random = random)
            if (task.num1 == 0 || task.num2 == 0) withZero++
            if (task.num1 == 1 || task.num2 == 1) withOne++
        }
        assertTrue("Nullen sollten stark reduziert sein, waren: $withZero/$total", withZero < total * 0.20)
        assertTrue("Einsen sollten reduziert sein, waren: $withOne/$total", withOne < total * 0.30)
    }

    @Test
    fun createTask_verliebte_zerlegtDieZielzahlKorrekt() {
        val random = Random(7)
        repeat(iterations) {
            val task = createTask(listOf("Verliebte"), range = 10, random = random, showCoins = true)
            assertEquals("+", task.operation)
            assertEquals("VERLIEBTE_ZAHLEN", task.category)
            // num1 = bekannter Summand, num2 = Zielzahl (10), correctAnswer = Partner.
            assertTrue("num1 sollte in 0..10 liegen: ${task.num1}", task.num1 in 0..10)
            assertEquals("num2 muss die Zielzahl 10 sein", 10, task.num2)
            assertEquals("Partner = 10 - num1", 10 - task.num1, task.correctAnswer)
        }
    }

    @Test
    fun createTask_verliebte_reduziertNur0Und10AberNicht1() {
        // Abweichende Trivial-Regel: 0/10 wird reduziert, 1/9 (num1 == 1 oder Partner == 1)
        // bleibt bewusst erhalten (echter Lernstoff, siehe LEHRPLAN.md).
        val random = Random(99)
        val total = 2000
        var withZeroOrTen = 0
        var withOne = 0
        repeat(total) {
            val task = createTask(listOf("Verliebte"), range = 10, random = random)
            if (task.num1 == 0 || task.num1 == 10) withZeroOrTen++
            if (task.num1 == 1 || task.num1 == 9) withOne++
        }
        assertTrue("0/10 sollten stark reduziert sein, waren: $withZeroOrTen/$total", withZeroOrTen < total * 0.06)
        assertTrue("1/9 sollten NICHT unterdrückt werden, waren: $withOne/$total", withOne > total * 0.10)
    }

    @Test
    fun createTask_vergleich_zeichenPasstZurGroesseOderGleichheit() {
        val random = Random(11)
        var sawEqual = false
        repeat(iterations) {
            val task = createTask(listOf("Vergleichen"), range = 20, random = random)
            assertEquals("GROESSER_KLEINER", task.category)
            assertTrue("num1 sollte im Zahlenraum liegen: ${task.num1}", task.num1 in 0..20)
            assertTrue("num2 sollte im Zahlenraum liegen: ${task.num2}", task.num2 in 0..20)
            val erwartet = when {
                task.num1 > task.num2 -> ">"
                task.num1 < task.num2 -> "<"
                else -> "="
            }
            assertEquals("Das Vergleichszeichen muss zur Größe passen", erwartet, task.operation)
            if (task.num1 == task.num2) sawEqual = true
        }
        assertTrue("Erwarte mindestens einen Gleichheitsfall in $iterations Durchläufen", sawEqual)
    }

    @Test
    fun createTask_verdoppeln_verdoppeltDieZahlKorrekt() {
        val random = Random(21)
        repeat(iterations) {
            // Ohne multipleChoice -> schwerer Modus -> eigene Abzeichen-Kategorie.
            val task = createTask(listOf("Verdoppeln"), range = 10, random = random)
            assertEquals("VERDOPPELN_SCHWER", task.category)
            assertTrue("num1 sollte in 0..10 liegen: ${task.num1}", task.num1 in 0..10)
            assertEquals("num2 sollte identisch zu num1 sein (n+n)", task.num1, task.num2)
            assertEquals(task.num1 * 2, task.correctAnswer)
            assertTrue("Ergebnis sollte in 0..20 liegen: ${task.correctAnswer}", task.correctAnswer in 0..20)
        }
    }

    @Test
    fun createTask_verdoppeln_mitMultipleChoice_hatEigeneAbzeichenKategorie() {
        val random = Random(25)
        val task = createTask(listOf("Verdoppeln"), range = 10, random = random, multipleChoice = true)
        assertEquals("VERDOPPELN_LEICHT", task.category)
    }

    @Test
    fun createTask_verdoppeln_ohneMultipleChoice_hatKeineAnswerOptions() {
        val random = Random(22)
        val task = createTask(listOf("Verdoppeln"), range = 10, random = random)
        assertEquals(null, task.answerOptions)
    }

    @Test
    fun createTask_verdoppeln_mitMultipleChoice_liefertDreiEindeutigeOptionenMitKorrekterAntwort() {
        val random = Random(23)
        repeat(iterations) {
            val task = createTask(listOf("Verdoppeln"), range = 10, random = random, multipleChoice = true)
            val options = task.answerOptions
            assertNotNull("answerOptions sollte gesetzt sein", options)
            assertEquals(3, options!!.size)
            assertEquals("Optionen sollten eindeutig sein", 3, options.distinct().size)
            assertTrue("Optionen sollten die korrekte Antwort enthalten", task.correctAnswer in options)
            assertTrue("Optionen sollten nicht negativ sein", options.all { it >= 0 })
        }
    }

    @Test
    fun createTask_verdoppeln_mitMultipleChoice_positionDerKorrektenAntwortVariiert() {
        // Absicherung gegen ein festes Auswahlmuster (z. B. "immer die mittlere Zahl"): über
        // viele Runden muss die korrekte Antwort an allen drei Positionen vorkommen.
        val random = Random(24)
        val positionCounts = IntArray(3)
        repeat(iterations) {
            val task = createTask(listOf("Verdoppeln"), range = 10, random = random, multipleChoice = true)
            val index = task.answerOptions!!.indexOf(task.correctAnswer)
            positionCounts[index]++
        }
        positionCounts.forEachIndexed { index, count ->
            assertTrue(
                "Position $index sollte mehrfach vorkommen, war: $count/$iterations",
                count > iterations * 0.15
            )
        }
    }

    @Test
    fun equation_verdoppeln_zeigtKeinRechenzeichen() {
        // Nur fuer die Historie relevant (GameScreen zeigt Verdoppeln als eigene Kachel ohne
        // "="); gilt fuer beide Schwierigkeitsgrade.
        listOf("VERDOPPELN_LEICHT", "VERDOPPELN_SCHWER").forEach { category ->
            val task = Task(num1 = 5, num2 = 5, operation = "+", correctAnswer = 10, category = category)
            val text = task.equation("_")
            assertEquals("5 verdoppeln = _", text)
            assertFalse("Kein '+' im Aufgabentext", text.contains("+"))
            assertFalse("Kein '·' im Aufgabentext", text.contains("·"))
        }
    }

    @Test
    fun equation_vergleich_zeigtLueckeInDerMitte() {
        val task = Task(num1 = 3, num2 = 7, operation = "<", correctAnswer = 0, category = "GROESSER_KLEINER")
        assertEquals("3   ?   7", task.equation("?"))
        assertEquals("3   <   7", task.equation("<"))
    }

    @Test
    fun equation_vergleich_mitGleichheitszeichenBeiGleichenZahlen() {
        val task = Task(num1 = 5, num2 = 5, operation = "=", correctAnswer = 0, category = "GROESSER_KLEINER")
        assertEquals("5   =   5", task.equation("="))
    }

    @Test
    fun expectedAnswer_vergleichIstDasZeichen_sonstDieZahl() {
        val vergleich = Task(num1 = 8, num2 = 5, operation = ">", correctAnswer = 0, category = "GROESSER_KLEINER")
        assertEquals(">", vergleich.expectedAnswer())

        val plus = Task(num1 = 3, num2 = 4, operation = "+", correctAnswer = 7, category = "PLUS")
        assertEquals("7", plus.expectedAnswer())
    }

    @Test
    fun equation_verliebte_zeigtLueckeInDerMitte() {
        val task = Task(num1 = 6, num2 = 10, operation = "+", correctAnswer = 4, category = "VERLIEBTE_ZAHLEN")
        assertEquals("6 + _ = 10", task.equation("_"))
        assertEquals("6 + 4 = 10", task.equation("4"))
    }

    @Test
    fun equation_verliebte_zeigtLueckeAmAnfangWennFirstIsBlank() {
        val task = Task(
            num1 = 6,
            num2 = 10,
            operation = "+",
            correctAnswer = 4,
            category = "VERLIEBTE_ZAHLEN",
            firstIsBlank = true
        )
        assertEquals("_ + 6 = 10", task.equation("_"))
        assertEquals("4 + 6 = 10", task.equation("4"))
    }

    /**
     * „Mit Hilfe" und „Ohne Hilfe" sind zwei Schwierigkeitsstufen mit eigener Kennung
     * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`). Die alte Kennung meint
     * „mit Hilfe", damit der bisherige Fortschritt dort weiterzählt.
     */
    @Test
    fun createTask_verliebte_ohneHilfe_laeuftUnterEigenerKennung() {
        val random = Random(7)
        repeat(iterations) {
            val task = createTask(listOf("Verliebte"), range = 10, random = random, showCoins = false)
            assertEquals("VERLIEBTE_ZAHLEN_OHNE_HILFE", task.category)
            assertEquals("Partner = 10 - num1", 10 - task.num1, task.correctAnswer)
        }
    }

    @Test
    fun equation_verliebteOhneHilfe_zeigtLueckeWieMitHilfe() {
        val mitte = Task(
            num1 = 6,
            num2 = 10,
            operation = "+",
            correctAnswer = 4,
            category = "VERLIEBTE_ZAHLEN_OHNE_HILFE",
        )
        val vorne = mitte.copy(firstIsBlank = true)

        assertEquals("6 + _ = 10", mitte.equation("_"))
        assertEquals("_ + 6 = 10", vorne.equation("_"))
    }

    @Test
    fun createTask_verliebte_variiertLueckenpositionUeberVieleRunden() {
        // Über viele Runden müssen beide Lückenpositionen vorkommen (kein fest verdrahtetes
        // "immer die zweite Zahl fehlt" mehr).
        val random = Random(42)
        var firstBlankCount = 0
        var secondBlankCount = 0
        repeat(iterations) {
            val task = createTask(listOf("Verliebte"), range = 10, random = random)
            if (task.firstIsBlank) firstBlankCount++ else secondBlankCount++
        }
        assertTrue(
            "Auch die erste Zahl sollte mal fehlen: $firstBlankCount/$iterations",
            firstBlankCount > iterations * 0.3
        )
        assertTrue(
            "Auch die zweite Zahl sollte mal fehlen: $secondBlankCount/$iterations",
            secondBlankCount > iterations * 0.3
        )
    }

    @Test
    fun equation_klassisch_zeigtLueckeAmEnde() {
        val task = Task(num1 = 3, num2 = 4, operation = "·", correctAnswer = 12, category = "MAL")
        assertEquals("3 · 4 = _", task.equation("_"))
    }

    @Test
    fun categoryForType_bildetAlleTypenAufStabileKeysAb() {
        assertEquals("PLUS", categoryForType("Plus"))
        assertEquals("MINUS", categoryForType("Minus"))
        assertEquals("MAL", categoryForType("Mal"))
        assertEquals("GETEILT", categoryForType("Geteilt"))
        // Verliebte Zahlen: je Hilfe-Stufe eine eigene Kategorie; die alte meint „mit Hilfe".
        assertEquals("VERLIEBTE_ZAHLEN", categoryForType("Verliebte", showCoins = true))
        assertEquals("VERLIEBTE_ZAHLEN_OHNE_HILFE", categoryForType("Verliebte", showCoins = false))
        assertEquals("GROESSER_KLEINER", categoryForType("Vergleichen"))
        // Verdoppeln hat je Schwierigkeitsgrad eine eigene Kategorie (getrennte Abzeichen).
        assertEquals("VERDOPPELN_SCHWER", categoryForType("Verdoppeln"))
        assertEquals("VERDOPPELN_SCHWER", categoryForType("Verdoppeln", multipleChoice = false))
        assertEquals("VERDOPPELN_LEICHT", categoryForType("Verdoppeln", multipleChoice = true))
    }

    @Test
    fun createTask_usesOnlyRequestedOperationTypes() {
        val random = Random(5)
        repeat(iterations) {
            val task = createTask(listOf("Plus", "Minus"), range = 20, random = random)
            assertTrue(
                "Nur Plus oder Minus erwartet, war: ${task.operation}",
                task.operation == "+" || task.operation == "-"
            )
        }
    }

    @Test(expected = NoSuchElementException::class)
    fun createTask_emptyTypesList_throws() {
        createTask(emptyList(), range = 10, random = Random(6))
    }

    @Test
    fun isAnswerCorrect_matchingNumber_returnsTrue() {
        assertTrue(isAnswerCorrect("7", 7))
    }

    @Test
    fun isAnswerCorrect_wrongNumber_returnsFalse() {
        assertFalse(isAnswerCorrect("3", 7))
    }

    @Test
    fun isAnswerCorrect_nonNumericInput_returnsFalse() {
        assertFalse(isAnswerCorrect("", 7))
        assertFalse(isAnswerCorrect("abc", 7))
    }

    @Test
    fun performanceTier_allCorrect_isPerfect() {
        assertEquals(PerformanceTier.PERFECT, performanceTier(10, 10))
    }

    @Test
    fun performanceTier_eightyPercent_isGreat() {
        assertEquals(PerformanceTier.GREAT, performanceTier(8, 10))
    }

    @Test
    fun performanceTier_fiftyPercent_isGood() {
        assertEquals(PerformanceTier.GOOD, performanceTier(5, 10))
    }

    @Test
    fun performanceTier_belowFifty_isKeepGoing() {
        assertEquals(PerformanceTier.KEEP_GOING, performanceTier(4, 10))
    }

    @Test
    fun performanceTier_noTasks_isKeepGoing() {
        assertEquals(PerformanceTier.KEEP_GOING, performanceTier(0, 0))
    }

    @Test
    fun earnedStars_allCorrect_isThree() {
        assertEquals(3, earnedStars(10, 10))
    }

    @Test
    fun earnedStars_seventyFivePercent_isTwo() {
        assertEquals(2, earnedStars(3, 4))
        assertEquals(2, earnedStars(8, 10))
    }

    @Test
    fun earnedStars_fiftyPercent_isOne() {
        assertEquals(1, earnedStars(5, 10))
    }

    @Test
    fun earnedStars_belowFifty_isZero() {
        assertEquals(0, earnedStars(4, 10))
        assertEquals(0, earnedStars(0, 10))
    }

    @Test
    fun earnedStars_noTasks_isZero() {
        assertEquals(0, earnedStars(0, 0))
    }
}
