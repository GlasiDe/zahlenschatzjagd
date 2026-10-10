package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ZahlenmauerLogicTest {

    private val iterations = 500

    @Test
    fun createZahlenmauer_everyNonBaseCell_isSumOfItsTwoChildren() {
        val random = Random(1)
        ZahlenmauerDifficulty.entries.forEach { difficulty ->
            repeat(iterations) {
                val wall = createZahlenmauer(difficulty, random)
                wall.cells.filter { it.pos.row > 0 }.forEach { cell ->
                    val left = wall.cellAt(cell.pos.row - 1, cell.pos.col)
                    val right = wall.cellAt(cell.pos.row - 1, cell.pos.col + 1)
                    assertEquals(
                        "Zelle ${cell.pos} sollte Summe ihrer Kinder sein",
                        left.solution + right.solution,
                        cell.solution
                    )
                }
            }
        }
    }

    @Test
    fun createZahlenmauer_apexNeverExceedsRangeAndCellsAreNonNegative() {
        val random = Random(2)
        ZahlenmauerDifficulty.entries.forEach { difficulty ->
            repeat(iterations) {
                val wall = createZahlenmauer(difficulty, random)
                val apex = wall.cells.first { it.pos.row == difficulty.floors - 1 }
                assertTrue(
                    "Spitze sollte im Zahlenraum bleiben: ${apex.solution} (${difficulty.name})",
                    apex.solution <= difficulty.range
                )
                assertTrue(
                    "Keine negativen Werte erlaubt (${difficulty.name})",
                    wall.cells.all { it.solution >= 0 }
                )
            }
        }
    }

    @Test
    fun createZahlenmauer_apexVariesAndDoesNotAlwaysHitTheMaximumRange() {
        // Regression: createAufbauBase() hat früher IMMER das komplette verbleibende Budget
        // verteilt, wodurch die Spitze bei jeder Mauer exakt den Zahlenraum erreichte (nie
        // weniger) - das machte Aufgaben eintönig und wurde von einem Elternteil beim
        // tatsächlichen Spielen bemerkt. Jetzt wird nur ein zufälliger Teil des Budgets verteilt.
        val random = Random(11)
        listOf(ZahlenmauerDifficulty.LEICHT, ZahlenmauerDifficulty.MITTEL, ZahlenmauerDifficulty.SCHWER)
            .forEach { difficulty ->
                val apexValues = (1..iterations).map {
                    val wall = createZahlenmauer(difficulty, random)
                    wall.cells.first { it.pos.row == difficulty.floors - 1 }.solution
                }
                val hitMax = apexValues.count { it == difficulty.range }
                assertTrue(
                    "Spitze sollte über viele Mauern hinweg variieren, nicht immer $iterations " +
                        "Mal exakt ${difficulty.range} treffen (${difficulty.name}), war: $hitMax/$iterations",
                    hitMax < iterations
                )
                assertTrue(
                    "Spitze sollte in mehreren unterschiedlichen Werten vorkommen (${difficulty.name})",
                    apexValues.distinct().size > 1
                )
            }
    }

    @Test
    fun createZahlenmauer_aufbau_baseIsFullyGivenAndRestAreGaps() {
        val random = Random(4)
        listOf(ZahlenmauerDifficulty.LEICHT, ZahlenmauerDifficulty.MITTEL, ZahlenmauerDifficulty.SCHWER)
            .forEach { difficulty ->
                repeat(iterations) {
                    val wall = createZahlenmauer(difficulty, random)
                    wall.cells.forEach { cell ->
                        assertEquals(
                            "Zelle ${cell.pos} (${difficulty.name}): isGiven sollte genau der Basis entsprechen",
                            cell.pos.row == 0,
                            cell.isGiven
                        )
                    }
                }
            }
    }

    @Test
    fun createZahlenmauer_allDifficulties_reduceTrivialZeroAndOneBaseCells() {
        // Mit den aktuellen Zahlenräumen (20/50/100/100) liegt safeCap bei allen vier Stufen
        // bei mindestens 5 (>= 4) -> Trivial-Reduktion ist bei jeder Stufe aktiv, nicht mehr
        // nur bei Ultra (siehe LEHRPLAN.md, Abschnitt "Zahlenmauer").
        val random = Random(42)
        val total = 1000
        ZahlenmauerDifficulty.entries.forEach { difficulty ->
            var withZero = 0
            var withOne = 0
            var baseCellCount = 0
            repeat(total) {
                val wall = createZahlenmauer(difficulty, random)
                wall.cells.filter { it.pos.row == 0 }.forEach { cell ->
                    baseCellCount++
                    if (cell.solution == 0) withZero++
                    if (cell.solution == 1) withOne++
                }
            }
            assertTrue(
                "Nullen sollten reduziert sein (${difficulty.name}), waren: $withZero/$baseCellCount",
                withZero < baseCellCount * 0.20
            )
            assertTrue(
                "Einsen sollten reduziert sein (${difficulty.name}), waren: $withOne/$baseCellCount",
                withOne < baseCellCount * 0.30
            )
        }
    }

    @Test
    fun createZahlenmauer_ultra_hidesExactlyTwoBaseCells() {
        val random = Random(8)
        repeat(iterations) {
            val wall = createZahlenmauer(ZahlenmauerDifficulty.ULTRA, random)
            val hiddenBase = wall.cells.filter { it.pos.row == 0 && !it.isGiven }
            assertEquals(2, hiddenBase.size)
        }
    }

    @Test
    fun createZahlenmauer_ultra_hiddenBaseCellsAreNeverAdjacent() {
        val random = Random(9)
        repeat(iterations) {
            val wall = createZahlenmauer(ZahlenmauerDifficulty.ULTRA, random)
            val cols = wall.cells.filter { it.pos.row == 0 && !it.isGiven }.map { it.pos.col }.sorted()
            assertTrue("Abstand sollte >= 2 sein, war: $cols", cols[1] - cols[0] >= 2)
        }
    }

    @Test
    fun createZahlenmauer_ultra_isAlwaysSolvableByPropagation() {
        val random = Random(10)
        repeat(iterations) {
            val wall = createZahlenmauer(ZahlenmauerDifficulty.ULTRA, random)
            val given = wall.cells.filter { it.isGiven }.map { it.pos }.toSet()
            assertTrue(isSolvableByPropagation(wall, given))
        }
    }

    @Test
    fun isSolvableByPropagation_returnsTrue_forCompleteAufbauWall() {
        val random = Random(3)
        listOf(ZahlenmauerDifficulty.LEICHT, ZahlenmauerDifficulty.MITTEL, ZahlenmauerDifficulty.SCHWER)
            .forEach { difficulty ->
                repeat(50) {
                    val wall = createZahlenmauer(difficulty, random)
                    val given = wall.cells.filter { it.pos.row == 0 }.map { it.pos }.toSet()
                    assertTrue(isSolvableByPropagation(wall, given))
                }
            }
    }

    @Test
    fun isSolvableByPropagation_returnsFalse_whenTwoAdjacentBaseCellsHiddenWithoutSupport() {
        // Manuell konstruierte 5-Etagen-Mauer (Basis 1,2,3,4,5). Nur die drei rechten
        // Basiszellen (Spalte 2,3,4) sind bekannt, Spalte 0 und 1 (benachbart!) fehlen - und
        // KEINE Etage-1-Stützzelle wurde zusätzlich aufgedeckt. Da Spalte 0 und 1 sich nur die
        // gemeinsame Elternzelle (1,0) mit zwei Unbekannten teilen, ist das Paar nicht durch
        // Einzel-Subtraktion auflösbar - das muss als unlösbar erkannt werden. (Spalte 2-4
        // erlauben zwar, (1,2) und (1,3) sowie (2,2) vorwärts abzuleiten, das erreicht aber nie
        // Spalte 0/1.)
        val base = intArrayOf(1, 2, 3, 4, 5)
        val row1 = intArrayOf(3, 5, 7, 9)
        val row2 = intArrayOf(8, 12, 16)
        val row3 = intArrayOf(20, 28)
        val apex = intArrayOf(48)
        val cells = buildList {
            base.forEachIndexed { col, v -> add(ZahlenmauerCell(ZahlenmauerPos(0, col), v, isGiven = true)) }
            row1.forEachIndexed { col, v -> add(ZahlenmauerCell(ZahlenmauerPos(1, col), v, isGiven = true)) }
            row2.forEachIndexed { col, v -> add(ZahlenmauerCell(ZahlenmauerPos(2, col), v, isGiven = true)) }
            row3.forEachIndexed { col, v -> add(ZahlenmauerCell(ZahlenmauerPos(3, col), v, isGiven = true)) }
            apex.forEachIndexed { col, v -> add(ZahlenmauerCell(ZahlenmauerPos(4, col), v, isGiven = true)) }
        }
        val wall = Zahlenmauer(ZahlenmauerDifficulty.SCHWER, cells)
        val given = setOf(ZahlenmauerPos(0, 2), ZahlenmauerPos(0, 3), ZahlenmauerPos(0, 4))

        assertFalse(isSolvableByPropagation(wall, given))
    }

    @Test
    fun zahlenmauerDifficulties_useOnlyEstablishedNumberRanges() {
        val allowed = setOf(10, 20, 50, 100)
        ZahlenmauerDifficulty.entries.forEach {
            assertTrue("Unbekannter Zahlenraum: ${it.range}", it.range in allowed)
        }
    }

    @Test
    fun equationFor_aufbauGap_zeigtLueckeAmEnde() {
        val random = Random(5)
        repeat(iterations) {
            val wall = createZahlenmauer(ZahlenmauerDifficulty.LEICHT, random)
            val gap = wall.gaps.first { it.pos.row > 0 }
            val left = wall.cellAt(gap.pos.row - 1, gap.pos.col)
            val right = wall.cellAt(gap.pos.row - 1, gap.pos.col + 1)
            assertEquals("${left.solution} + ${right.solution} = _", wall.equationFor(gap, "_"))
        }
    }

    @Test
    fun equationFor_ultraBasisLuecke_zeigtLueckeAmAnfangUndStimmtArithmetisch() {
        val random = Random(6)
        repeat(iterations) {
            val wall = createZahlenmauer(ZahlenmauerDifficulty.ULTRA, random)
            val baseGap = wall.gaps.first { it.pos.row == 0 }
            val text = wall.equationFor(baseGap, "_")
            assertTrue("Sollte mit der Lücke beginnen: $text", text.startsWith("_ + "))

            val match = Regex("""_ \+ (\d+) = (\d+)""").matchEntire(text)
            assertNotNull("Unerwartetes Format: $text", match)
            val (siblingText, parentText) = match!!.destructured
            assertEquals(baseGap.solution, parentText.toInt() - siblingText.toInt())
        }
    }

    /**
     * Der **Ertrag** einer Mauer laut `docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`:
     * ganz richtig 2, ab einer Mindestzahl richtiger Lücken 1, sonst 0 – mit den Grenzen der
     * Tabelle im ADR als Erwartung.
     */
    @Test
    fun ertrag_folgtDerTabelleAusAdr0027_einschliesslichDerGrenzen() {
        // Stufe, Lücken, richtige Lücken → Ertrag
        val faelle = listOf(
            Triple(ZahlenmauerDifficulty.LEICHT, 3, 1) to 0,
            Triple(ZahlenmauerDifficulty.LEICHT, 3, 2) to 1,
            Triple(ZahlenmauerDifficulty.LEICHT, 3, 3) to 2,
            Triple(ZahlenmauerDifficulty.MITTEL, 6, 3) to 0,
            Triple(ZahlenmauerDifficulty.MITTEL, 6, 4) to 1,
            Triple(ZahlenmauerDifficulty.MITTEL, 6, 6) to 2,
            Triple(ZahlenmauerDifficulty.SCHWER, 10, 5) to 0,
            Triple(ZahlenmauerDifficulty.SCHWER, 10, 6) to 1,
            Triple(ZahlenmauerDifficulty.SCHWER, 10, 10) to 2,
            Triple(ZahlenmauerDifficulty.ULTRA, 10, 5) to 0,
            Triple(ZahlenmauerDifficulty.ULTRA, 10, 6) to 1,
            Triple(ZahlenmauerDifficulty.ULTRA, 10, 10) to 2,
            Triple(ZahlenmauerDifficulty.LEICHT, 3, 0) to 0,
        )
        faelle.forEach { (fall, erwartet) ->
            val (stufe, luecken, richtige) = fall
            assertEquals("$stufe, $richtige von $luecken richtig", erwartet, stufe.ertrag(richtige, luecken))
        }
    }
}
