package io.github.glaside.zahlenschatzjagd

import kotlin.random.Random

/** Position einer Zelle in der Pyramide: [row] = Etage von unten (0 = Basis), [col] = Spalte (0-basiert, links). */
data class ZahlenmauerPos(val row: Int, val col: Int)

/**
 * Eine Pyramiden-Zelle. [solution] ist IMMER gesetzt (auch bei Lücken) – einzige Quelle der
 * Wahrheit für ViewModel/UI, damit eine Lücke nie „erraten" werden muss. [isGiven] steuert nur
 * die Anzeige (vorausgefüllt/nicht editierbar vs. vom Kind zu befüllende Lücke).
 */
data class ZahlenmauerCell(val pos: ZahlenmauerPos, val solution: Int, val isGiven: Boolean)

/**
 * Die vier Schwierigkeitsstufen (siehe LEHRPLAN.md, Abschnitt „Zahlenmauer"): feste Kombination
 * aus Etagenzahl, Zahlenraum (einer der etablierten Werte 10/20/50/100) und Aufgabenform.
 * [isUltra] = Umkehraufgabe (fehlende Basiszellen), sonst reine Aufbauaufgabe.
 */
enum class ZahlenmauerDifficulty(
    val floors: Int,
    val range: Int,
    val isUltra: Boolean,
    private val mindestRichtigFuerTeilertrag: Int,
) {
    LEICHT(floors = 3, range = 20, isUltra = false, mindestRichtigFuerTeilertrag = 2),
    MITTEL(floors = 4, range = 50, isUltra = false, mindestRichtigFuerTeilertrag = 4),
    SCHWER(floors = 5, range = 100, isUltra = false, mindestRichtigFuerTeilertrag = 6),
    ULTRA(floors = 5, range = 100, isUltra = true, mindestRichtigFuerTeilertrag = 6);

    /**
     * Der **Ertrag** einer geprüften Mauer dieser Stufe – wie viel sie zu ihrer Truhe beiträgt:
     * 2, wenn alle [luecken] richtig sind, 1 ab der Mindestzahl richtiger Lücken, sonst 0.
     * Bewusst flach und für alle Stufen gleich hoch, siehe
     * `docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`.
     */
    fun ertrag(richtige: Int, luecken: Int): Int = when {
        richtige >= luecken -> ERTRAG_GANZ_RICHTIG
        richtige >= mindestRichtigFuerTeilertrag -> ERTRAG_TEILWEISE_RICHTIG
        else -> 0
    }

    /**
     * Die Fortschrittskennung dieser Stufe – jede Stufe sammelt eigene Abzeichen und wird
     * eigens freigegeben. **Persistiert** (`TaskHistory.taskCategory`): Der Enum-Name ist damit
     * eingefroren, siehe `docs/adr/0005-persistierte-namen-entkoppelt.md`.
     */
    val kennung: String get() = "ZAHLENMAUER_$name"
}

/** Ertrag einer ganz richtigen Mauer – siehe [ZahlenmauerDifficulty.ertrag]. */
const val ERTRAG_GANZ_RICHTIG = 2
private const val ERTRAG_TEILWEISE_RICHTIG = 1

/** Eine vollständige Mauer: alle Zellen aller Etagen (row 0 = Basis, höchste row = Spitze). */
data class Zahlenmauer(val difficulty: ZahlenmauerDifficulty, val cells: List<ZahlenmauerCell>) {
    fun cellAt(row: Int, col: Int): ZahlenmauerCell = cells.first { it.pos.row == row && it.pos.col == col }
    val gaps: List<ZahlenmauerCell> get() = cells.filterNot { it.isGiven }
}

private const val ULTRA_MAX_ATTEMPTS = 50
private val ULTRA_FALLBACK_HIDDEN = setOf(0, 2)

/** Erzeugt eine Mauer der gewünschten [difficulty]. [random] injizierbar für deterministische Tests. */
fun createZahlenmauer(difficulty: ZahlenmauerDifficulty, random: Random = Random.Default): Zahlenmauer =
    if (difficulty.isUltra) {
        createZahlenmauerUltra(difficulty, random)
    } else {
        buildWallFromBase(difficulty, createAufbauBase(difficulty.floors, difficulty.range, random))
    }

/** Pascal-Zeile [n] (0-basiert): `[C(n,0), ..., C(n,n)]` – Gewichte, mit denen jede Basiszelle
 * in die Spitze eingeht. */
private fun binomialRow(n: Int): IntArray {
    val row = IntArray(n + 1)
    row[0] = 1
    for (i in 1..n) {
        row[i] = row[i - 1] * (n - i + 1) / i
    }
    return row
}

/** Rechnet aus einer Basiszeile [base] alle darüberliegenden Etagen hoch; Basis wird als isGiven markiert. */
private fun buildWallFromBase(difficulty: ZahlenmauerDifficulty, base: IntArray): Zahlenmauer {
    val rows = mutableListOf(base.toList())
    var current: List<Int> = base.toList()
    while (current.size > 1) {
        current = (0 until current.size - 1).map { current[it] + current[it + 1] }
        rows.add(current)
    }
    val cells = rows.flatMapIndexed { row, values ->
        values.mapIndexed { col, value -> ZahlenmauerCell(ZahlenmauerPos(row, col), value, isGiven = row == 0) }
    }
    return Zahlenmauer(difficulty, cells)
}

/**
 * Erzeugt die Basis-Werte einer Aufbau-Mauer, sodass die Spitze garantiert im Zahlenraum
 * bleibt: bei `floors` Basiszellen ist die Spitze `Σ gewicht_i · x_i` (Pascal-Gewichte); blindes
 * Ziehen jeder Basiszelle aus `0..range` würde bei mehr Etagen fast immer überschreiten. Statt
 * Rejection-Sampling (das hier eine inakzeptabel niedrige Trefferquote hätte, siehe
 * LEHRPLAN.md): (1) jede Basiszelle zunächst auf eine sichere Obergrenze `range / Σgewichte`
 * begrenzt ziehen (mit Trivial-Reduktion, sofern die Obergrenze groß genug ist, um die
 * Verteilung nicht zu stark zu verzerren), (2) ein zufällig bestimmter TEIL des verbleibenden
 * Budgets (0 bis zum vollen Rest) randomisiert auf einzelne Basiszellen verteilen. Terminiert
 * immer (kein Reject nötig).
 *
 * Wichtig: Schritt (2) verteilt bewusst NICHT immer das komplette verbleibende Budget - sonst
 * würde die Spitze (da mindestens die beiden äußeren Basiszellen stets Gewicht 1 haben und so
 * jeden verbleibenden Rest aufnehmen können) praktisch immer exakt den Zahlenraum erreichen,
 * statt über viele Mauern hinweg zu variieren.
 *
 * Die Verteilung erfolgt rundenweise (jede Zelle höchstens ein Mal je Runde, Reihenfolge je
 * Runde neu gemischt) statt rein zufällig gezogen – sonst könnten billige Zellen (kleines
 * Gewicht, z. B. die äußeren Basiszellen) das gesamte Budget an sich reißen, während andere
 * Zellen unverändert bleiben. So verteilt sich das Budget gleichmäßiger über alle Zellen.
 */
private fun createAufbauBase(floors: Int, range: Int, random: Random): IntArray {
    val weights = binomialRow(floors - 1)
    val totalWeight = weights.sum()
    val safeCap = range / totalWeight
    val allowTrivialReduction = safeCap >= 4

    val base = IntArray(floors) { drawBaseCell(safeCap, allowTrivialReduction, random) }

    val maxRemainingBudget = range - base.indices.sumOf { weights[it] * base[it] }
    var budgetToDistribute = if (maxRemainingBudget > 0) random.nextInt(maxRemainingBudget + 1) else 0
    while (budgetToDistribute > 0) {
        var madeProgress = false
        for (i in base.indices.shuffled(random)) {
            if (budgetToDistribute <= 0) break
            if (weights[i] <= budgetToDistribute) {
                base[i] += 1
                budgetToDistribute -= weights[i]
                madeProgress = true
            }
        }
        if (!madeProgress) break
    }
    return base
}

/** Eine einzelne Basiszelle, mit derselben 0/1-Trivial-Reduktion (≈90 %/≈70 %) wie in GameLogic.kt. */
private fun drawBaseCell(safeCap: Int, allowTrivialReduction: Boolean, random: Random): Int {
    var value: Int
    do {
        value = (0..safeCap).random(random)
        var suppress = false
        if (allowTrivialReduction) {
            if (value == 0 && random.nextDouble() < 0.90) {
                suppress = true
            } else if (value == 1 && random.nextDouble() < 0.70) suppress = true
        }
    } while (suppress)
    return value
}

/**
 * Ultra: vollständige Mauer wie Schwer erzeugen, dann zwei nicht benachbarte Basiszellen
 * verstecken und die jeweils stützende Elternzelle zusätzlich aufdecken (siehe
 * [applyUltraMask]), sodass jede versteckte Basiszahl durch eine einzelne Subtraktion
 * bestimmbar ist. Wird mit [isSolvableByPropagation] verifiziert; bei Mindestabstand 2 ist das
 * bereits durch Konstruktion garantiert, die Prüfung dient als Absicherung. Nach
 * [ULTRA_MAX_ATTEMPTS] erfolglosen Versuchen greift ein deterministischer Fallback.
 */
private fun createZahlenmauerUltra(difficulty: ZahlenmauerDifficulty, random: Random): Zahlenmauer {
    repeat(ULTRA_MAX_ATTEMPTS) {
        val base = createAufbauBase(difficulty.floors, difficulty.range, random)
        val fullWall = buildWallFromBase(difficulty, base)
        val hiddenIndices = chooseHiddenBaseIndices(difficulty.floors, random)
        val wall = applyUltraMask(fullWall, hiddenIndices)
        val given = wall.cells.filter { it.isGiven }.map { it.pos }.toSet()
        if (isSolvableByPropagation(wall, given)) return wall
    }
    val base = createAufbauBase(difficulty.floors, difficulty.range, random)
    val fullWall = buildWallFromBase(difficulty, base)
    return applyUltraMask(fullWall, ULTRA_FALLBACK_HIDDEN)
}

/** Wählt zwei Basis-Spaltenindizes mit Mindestabstand 2 (keine gemeinsame Elternzelle mit 2 Unbekannten). */
private fun chooseHiddenBaseIndices(floors: Int, random: Random): Set<Int> {
    val pairs = mutableListOf<Pair<Int, Int>>()
    for (i in 0 until floors) {
        for (j in i + 1 until floors) {
            if (j - i >= 2) pairs.add(i to j)
        }
    }
    val (a, b) = pairs.random(random)
    return setOf(a, b)
}

/** Versteckt [hiddenBaseIndices] in der Basis und deckt je Index dessen stützende Elternzelle
 * (Etage 1, deren *anderes* Kind eine nicht versteckte Nachbar-Basiszelle ist) zusätzlich auf. */
private fun applyUltraMask(wall: Zahlenmauer, hiddenBaseIndices: Set<Int>): Zahlenmauer {
    fun supportParentPosition(hiddenBaseIndex: Int): ZahlenmauerPos =
        if (hiddenBaseIndex > 0) ZahlenmauerPos(1, hiddenBaseIndex - 1) else ZahlenmauerPos(1, hiddenBaseIndex)

    val supportPositions = hiddenBaseIndices.map(::supportParentPosition).toSet()
    val cells = wall.cells.map { cell ->
        val isGiven = when {
            cell.pos.row == 0 -> cell.pos.col !in hiddenBaseIndices
            cell.pos in supportPositions -> true
            else -> false
        }
        cell.copy(isGiven = isGiven)
    }
    return wall.copy(cells = cells)
}

/**
 * Prüft, ob [wall] ausgehend von den als bekannt angenommenen Positionen [given] durch
 * Fixpunkt-Verkettung vollständig bestimmbar ist: solange sich noch etwas ableiten lässt, gilt
 * für jedes Eltern-Kinder-Trio (Elternzelle + ihre zwei Kinder) – sind genau 2 der 3 Positionen
 * bekannt, ist die dritte ableitbar (Addition, wenn die Kinder bekannt sind; Subtraktion, wenn
 * Elternzelle + ein Kind bekannt sind). Deckt damit sowohl Aufbau- als auch Umkehraufgaben ab,
 * ohne je ein Gleichungssystem lösen zu müssen.
 */
fun isSolvableByPropagation(wall: Zahlenmauer, given: Set<ZahlenmauerPos>): Boolean {
    val floors = wall.difficulty.floors
    val trios = (1 until floors).flatMap { row ->
        (0 until floors - row).map { col ->
            listOf(ZahlenmauerPos(row, col), ZahlenmauerPos(row - 1, col), ZahlenmauerPos(row - 1, col + 1))
        }
    }

    val known = given.toMutableSet()
    var progress = true
    while (progress) {
        progress = false
        for (trio in trios) {
            if (trio.count { it in known } != 2) continue
            known.add(trio.first { it !in known })
            progress = true
        }
    }
    return wall.cells.all { it.pos in known }
}

/**
 * Formatiert die lokale Gleichung um [cell] mit [blank] an der Lückenstelle, analog zu
 * [Task.equation]. Aufbau-Lücke (row > 0): `"linkesKind + rechtesKind = _"`. Ultra-Basislücke
 * (row == 0): `"_ + bekannterNachbar = bekannteElternzelle"`, je nachdem auf welcher Seite die
 * Stützzelle liegt (siehe [applyUltraMask]).
 */
fun Zahlenmauer.equationFor(cell: ZahlenmauerCell, blank: String): String {
    val pos = cell.pos
    if (pos.row > 0) {
        val left = cellAt(pos.row - 1, pos.col)
        val right = cellAt(pos.row - 1, pos.col + 1)
        return "${left.solution} + ${right.solution} = $blank"
    }
    val viaLeftParent = pos.col > 0 && cellAt(1, pos.col - 1).isGiven
    return if (viaLeftParent) {
        val parent = cellAt(1, pos.col - 1)
        val sibling = cellAt(0, pos.col - 1)
        "$blank + ${sibling.solution} = ${parent.solution}"
    } else {
        val parent = cellAt(1, pos.col)
        val sibling = cellAt(0, pos.col + 1)
        "$blank + ${sibling.solution} = ${parent.solution}"
    }
}
