package io.github.glaside.zahlenschatzjagd

/**
 * Reine, testbare Logik des Abzeichen-Systems. Abzeichen werden nicht separat gespeichert,
 * sondern aus der Aufgaben-Historie (summierter **Ertrag** je Kategorie × Zahlenraum – meist die
 * Anzahl richtig gelöster Aufgaben) abgeleitet.
 */

/** Eine Abzeichen-Stufe mit dem summierten Ertrag (meist: richtige Aufgaben), ab dem sie verdient ist. */
enum class BadgeTier(val threshold: Int) {
    BRONZE(20),
    SILBER(50),
    GOLD(100),
    SAPHIR(150),
    DIAMANT(200),
    PLATIN(500),
}

/** Zahlenräume, für die Abzeichen getrennt gesammelt werden. */
val BADGE_RANGES = listOf(10, 20, 50, 100)

/**
 * Eine Aufgaben-Kategorie, für die Abzeichen (Schatztruhen) gesammelt werden. [ranges] sind
 * die für diese Kategorie sinnvollen Zahlenräume – je eine Truhe pro Zahlenraum. Standard
 * sind alle [BADGE_RANGES]; Kategorien mit fixem Zahlenraum (z. B. „Verliebte Zahlen") geben
 * hier nur ihren einen Zahlenraum an, damit keine dauerhaft leeren Truhen entstehen.
 *
 * [chestLabel] überschreibt die Standard-Truhenbeschriftung ("Zahlenraum bis X"), wenn sich
 * mehrere Kategorien einen Reiter teilen (siehe [BADGE_TABS]) und ihre Truhen dort nicht durch
 * den Zahlenraum, sondern anders unterschieden werden müssen (z. B. Verdoppeln: leicht/schwer).
 * Es benennt die **Kategorie**, nicht die einzelne Truhe – hat die Kategorie mehrere [ranges],
 * setzt [truhenBeschriftung] den Zahlenraum an.
 *
 * [mindestAufgaben] ist die Schwelle des **Übungsstands**: So viele Aufgaben müssen im Fenster
 * liegen, bevor eine Trefferquote überhaupt behauptet wird. Sie steht hier und nicht als ein Wert
 * für alles, weil eine „Aufgabe" je Kategorie verschieden viel ist – siehe
 * [MINDEST_AUFGABEN_ZAHLENMAUER].
 */
data class BadgeCategory(
    val key: String,
    val label: String,
    val symbol: String,
    val ranges: List<Int> = BADGE_RANGES,
    val chestLabel: String? = null,
    val mindestAufgaben: Int = MINDEST_AUFGABEN_STANDARD,
)

/**
 * Ab so vielen Aufgaben zeigt der Übungsstand eine Trefferquote. Zwei Runden zu je fünf Aufgaben
 * sind die kleinste Grundlage, auf der eine Prozentzahl mehr sagt als der Zufall.
 */
const val MINDEST_AUFGABEN_STANDARD: Int = 10

/**
 * Dieselbe Schwelle für die Zahlenmauer. Niedriger, weil dort **eine ganze Mauer** eine Zeile der
 * Historie ist (`ZahlenmauerViewModel.recordWall`): 3–10 je Runde statt der 5–25 Aufgaben einer
 * Runde Grundrechenarten. Mit der Standard-Schwelle stünde nach zwei durchgezogenen Runden noch
 * immer „zu wenig geübt". Der gestaffelte Ertrag einer Mauer ändert daran nichts – er wirkt nur
 * auf die Truhen, nicht auf den Übungsstand (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
 */
const val MINDEST_AUFGABEN_ZAHLENMAUER: Int = 5

/**
 * Katalog der aktuell abzeichen-fähigen Kategorien. Der [key] ist der stabile Wert, der in
 * der Historie (`TaskHistory.taskCategory`) gespeichert wird; künftige Kategorien werden hier
 * ergänzt. Jeder Eintrag ist zugleich ein Tab in der Abzeichen-Übersicht.
 */
val BADGE_CATEGORIES = listOf(
    BadgeCategory("PLUS", "Plus", "+"),
    BadgeCategory("MINUS", "Minus", "-"),
    BadgeCategory("MAL", "Mal", "·"),
    BadgeCategory("GETEILT", "Geteilt", ":"),
    // Verliebte Zahlen: „Mit Hilfe" (Münzen zeigen, was fehlt) und „Ohne Hilfe" sind zwei
    // Schwierigkeitsstufen mit getrennten Truhen. Die alte Kennung meint „mit Hilfe" und behält
    // damit den gesamten bisherigen Fortschritt (docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md).
    BadgeCategory(
        key = "VERLIEBTE_ZAHLEN",
        label = "Verliebte Zahlen (mit Hilfe)",
        symbol = "💕",
        ranges = listOf(10),
        chestLabel = "Mit Hilfe",
    ),
    BadgeCategory(
        key = "VERLIEBTE_ZAHLEN_OHNE_HILFE",
        label = "Verliebte Zahlen (ohne Hilfe)",
        symbol = "💕",
        ranges = listOf(10),
        chestLabel = "Ohne Hilfe",
    ),
    BadgeCategory("GROESSER_KLEINER", "Zahlen vergleichen", "❮❯"),
    // Verdoppeln: leichter (Auswahl) und schwerer Modus (Zifferneingabe) sammeln getrennte
    // Abzeichen, da unterschiedlich schwer; keine Zahlenraum-Unterteilung nötig (fix 0..10).
    // Beide teilen sich in der Abzeichen-Übersicht einen Reiter (siehe BADGE_TABS), daher
    // chestLabel statt "Zahlenraum bis 10" (sonst wären die beiden Truhen dort ununterscheidbar).
    BadgeCategory("VERDOPPELN_LEICHT", "Verdoppeln (leicht)", "✖️2", ranges = listOf(10), chestLabel = "Leicht"),
    BadgeCategory("VERDOPPELN_SCHWER", "Verdoppeln (schwer)", "✍️2", ranges = listOf(10), chestLabel = "Schwer"),
    // Zahlenmauer: eine Truhe je Schwierigkeitsstufe (nicht je Zahlenraum) - jede Stufe hat
    // ohnehin einen eigenen, festen Zahlenraum (siehe ZahlenmauerDifficulty), aber chestLabel
    // macht die Truhen-Beschriftung robuster/lesbarer als "Zahlenraum bis X" (analog Verdoppeln).
    BadgeCategory(
        key = "ZAHLENMAUER_LEICHT",
        label = "Zahlenmauer (leicht)",
        symbol = "🧱",
        ranges = listOf(20),
        chestLabel = "Leicht",
        mindestAufgaben = MINDEST_AUFGABEN_ZAHLENMAUER,
    ),
    BadgeCategory(
        key = "ZAHLENMAUER_MITTEL",
        label = "Zahlenmauer (mittel)",
        symbol = "🧱",
        ranges = listOf(50),
        chestLabel = "Mittel",
        mindestAufgaben = MINDEST_AUFGABEN_ZAHLENMAUER,
    ),
    BadgeCategory(
        key = "ZAHLENMAUER_SCHWER",
        label = "Zahlenmauer (schwer)",
        symbol = "🧱",
        ranges = listOf(100),
        chestLabel = "Schwer",
        mindestAufgaben = MINDEST_AUFGABEN_ZAHLENMAUER,
    ),
    BadgeCategory(
        key = "ZAHLENMAUER_ULTRA",
        label = "Zahlenmauer (Ultra)",
        symbol = "🧱⚡",
        ranges = listOf(100),
        chestLabel = "Ultra",
        mindestAufgaben = MINDEST_AUFGABEN_ZAHLENMAUER,
    ),
    // Nachbarzahlen: Die vier Truhen unterscheiden sich sowohl in der Schwierigkeitsstufe als auch
    // im Zahlenraum (leicht/schwer × 20/100), deshalb tragen sie beides in der Beschriftung –
    // „Zahlenraum bis 20" ließe „Leicht" und „Schwer" im gemeinsamen Reiter ununterscheidbar.
    // Zusammengesetzt wird sie in [truhenBeschriftung]; hier steht nur die Stufe, damit Freigabe
    // und Übungsstand sie ohne den Zahlenraum wiederverwenden können.
    // Eine Runde entspricht hier der üblichen Aufgabenzahl, also bleibt es bei der
    // Standard-Schwelle des Übungsstands.
    BadgeCategory(
        key = "NACHBARZAHLEN_LEICHT",
        label = "Nachbarzahlen (leicht)",
        symbol = "←→",
        ranges = listOf(20, 100),
        chestLabel = "Leicht",
    ),
    BadgeCategory(
        key = "NACHBARZAHLEN_SCHWER",
        label = "Nachbarzahlen (schwer)",
        symbol = "←→",
        ranges = listOf(20, 100),
        chestLabel = "Schwer",
    ),
) + EINMALEINS_KATEGORIEN

/** Das Symbol des Einmaleins: wie die Rechenart Mal mit dem Malpunkt, aber als ganze Aufgabe. */
private const val EINMALEINS_SYMBOL = "1·1"

/**
 * Einmaleins: eine Fortschrittskennung je Reihe und Stufe, 18 insgesamt, alle im festen Zahlenraum 100
 * (`docs/adr/0034-einmaleins-mit-kennungen-je-reihe-und-stufe.md`). Geordnet nach Reihe, darin
 * nach Stufe – so steht im Übungsstand die 7er-Reihe aufgesagt direkt über der durcheinander.
 * Abzeichen und Freigabe zeigen sie stattdessen als Raster Reihe × Stufe.
 */
private val EINMALEINS_KATEGORIEN: List<BadgeCategory>
    get() = EINMALEINS_REIHEN.flatMap { reihe ->
        EinmaleinsSchwierigkeitsstufe.entries.map { stufe ->
            BadgeCategory(
                key = einmaleinsKennung(reihe, stufe),
                label = "Einmaleins (${reihenName(reihe)}, ${stufe.beschriftung})",
                symbol = EINMALEINS_SYMBOL,
                ranges = listOf(EINMALEINS_ZAHLENRAUM),
                chestLabel = "${reihenName(reihe)} · ${stufe.beschriftung}",
            )
        }
    }

/**
 * Ein Reiter der Abzeichen-Übersicht (`BadgeScreen`): fasst eine oder mehrere [BADGE_CATEGORIES]
 * zusammen, die sich denselben Reiter teilen. Für die meisten Kategorien 1:1 zur jeweiligen
 * [BadgeCategory]; Verdoppeln fasst leichten und schweren Modus in einem gemeinsamen Reiter
 * „Verdoppeln" zusammen, der dann zwei Truhen zeigt (leicht/schwer statt Zahlenraum).
 */
data class BadgeTab(
    val key: String,
    val label: String,
    val symbol: String,
    val categoryKeys: List<String>,
)

val BADGE_TABS = listOf(
    BadgeTab("PLUS", "Plus", "+", listOf("PLUS")),
    BadgeTab("MINUS", "Minus", "-", listOf("MINUS")),
    BadgeTab("MAL", "Mal", "·", listOf("MAL")),
    BadgeTab("GETEILT", "Geteilt", ":", listOf("GETEILT")),
    BadgeTab(
        "VERLIEBTE_ZAHLEN",
        "Verliebte Zahlen",
        "💕",
        listOf("VERLIEBTE_ZAHLEN", "VERLIEBTE_ZAHLEN_OHNE_HILFE"),
    ),
    BadgeTab("GROESSER_KLEINER", "Zahlen vergleichen", "❮❯", listOf("GROESSER_KLEINER")),
    BadgeTab("VERDOPPELN", "Verdoppeln", "✖️2", listOf("VERDOPPELN_LEICHT", "VERDOPPELN_SCHWER")),
    BadgeTab(
        "ZAHLENMAUER",
        "Zahlenmauer",
        "🧱",
        listOf("ZAHLENMAUER_LEICHT", "ZAHLENMAUER_MITTEL", "ZAHLENMAUER_SCHWER", "ZAHLENMAUER_ULTRA")
    ),
    BadgeTab("NACHBARZAHLEN", "Nachbarzahlen", "←→", listOf("NACHBARZAHLEN_LEICHT", "NACHBARZAHLEN_SCHWER")),
    BadgeTab(EINMALEINS_TAB, "Einmaleins", EINMALEINS_SYMBOL, EINMALEINS_KATEGORIEN.map { it.key }),
)

/** Der Reiter, dessen Truhen und Kästchen als Raster Reihe × Stufe stehen statt als Liste. */
const val EINMALEINS_TAB: String = "EINMALEINS"

/**
 * Was an **einer** Schatztruhe steht. Sie muss innerhalb ihres Reiters eindeutig sein – und worin
 * sich die Truhen dort unterscheiden, hängt an der Kategorie:
 *
 * - ohne [BadgeCategory.chestLabel] am Zahlenraum: „Zahlenraum bis 50" (Plus, Minus, …),
 * - mit [BadgeCategory.chestLabel] und einem einzigen Zahlenraum an der Stufe: „Leicht"
 *   (Verdoppeln, Zahlenmauer),
 * - mit beidem an beidem: „Leicht bis 20" (Nachbarzahlen). Ohne den angesetzten Zahlenraum trügen
 *   dort zwei Truhen desselben Reiters dieselbe Beschriftung.
 *
 * Reine Logik statt einer Zeile in der Oberfläche, damit die Eindeutigkeit ohne Gerät prüfbar ist.
 */
fun truhenBeschriftung(kategorie: BadgeCategory, zahlenraum: Int): String {
    val stufe = kategorie.chestLabel ?: return "Zahlenraum bis $zahlenraum"
    return if (kategorie.ranges.size > 1) "$stufe bis $zahlenraum" else stufe
}

/** Höchste bereits erreichte Stufe für den gegebenen Zähler, oder null wenn noch keine. */
fun highestTier(count: Int): BadgeTier? =
    BadgeTier.entries.lastOrNull { count >= it.threshold }

/**
 * Die in der Übersicht anzuzeigenden Stufen: alle bereits erreichten **plus** die nächste
 * noch offene (als „Sammelalbum", das mit dem Fortschritt wächst). Bei `count = 0` ist das
 * nur die erste Stufe; sind alle erreicht, sind es alle. Nie leer.
 */
fun tiersUpToNext(count: Int): List<BadgeTier> {
    val next = BadgeTier.entries.firstOrNull { count < it.threshold }
    return if (next == null) {
        BadgeTier.entries.toList()
    } else {
        BadgeTier.entries.filter { it.threshold <= next.threshold }
    }
}

/**
 * Die Stufen, deren Schwelle durch den Zuwachs von [previousCount] auf [newCount] **neu**
 * überschritten wurde (also im Bereich `(previousCount, newCount]` liegt). Leer, wenn kein
 * Zuwachs oder keine Schwelle getroffen wurde.
 */
fun newlyReachedTiers(previousCount: Int, newCount: Int): List<BadgeTier> =
    BadgeTier.entries.filter { it.threshold in (previousCount + 1)..newCount }

/** Ein konkret verdientes Abzeichen (Kombination aus Kategorie, Zahlenraum und Stufe). */
data class UnlockedBadge(val categoryKey: String, val numberRange: Int, val tier: BadgeTier)

/**
 * Trennzeichen der drei Felder in [UnlockedBadge.encode]. Bewusst „-" (Bindestrich) statt
 * „_": Kategorie-Keys wie `VERLIEBTE_ZAHLEN` enthalten selbst Unterstriche, wodurch ein
 * `split("_")` das Abzeichen in zu viele Segmente zerlegt und beim Dekodieren verworfen
 * hätte. „-" kommt in keinem Key/Stufennamen vor und ist routensicher.
 */
private const val BADGE_FIELD_SEP = "-"

/** Kompakte Kodierung für die Übergabe als Navigations-Argument, z. B. "PLUS-20-BRONZE". */
fun UnlockedBadge.encode(): String =
    listOf(categoryKey, numberRange.toString(), tier.name).joinToString(BADGE_FIELD_SEP)

/**
 * Dekodiert eine komma-getrennte Liste kodierter Abzeichen. Unbekannte/leere Einträge
 * (z. B. der Platzhalter "none") werden übersprungen.
 */
fun decodeUnlockedBadges(encoded: String): List<UnlockedBadge> =
    encoded.split(",").mapNotNull { part ->
        val segments = part.split(BADGE_FIELD_SEP)
        if (segments.size != 3) return@mapNotNull null
        val (key, rangeText, tierName) = segments
        val range = rangeText.toIntOrNull() ?: return@mapNotNull null
        val tier = BadgeTier.entries.firstOrNull { it.name == tierName } ?: return@mapNotNull null
        UnlockedBadge(key, range, tier)
    }

// Die konkrete, je Stufe gemalte Truhen-Grafik (leer → Bronze/Silber/Gold → Saphir →
// Diamant → Krone) liegt als gemalte Bildserie in der UI-Schicht (`ui/badges/BadgeVisuals.kt`,
// `chestStageDrawable`); siehe `ASSETS.md`.
