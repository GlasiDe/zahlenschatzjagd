package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgesTest {

    /**
     * Die **Fortschrittskennungen** stehen als Klartext in `task_history.taskCategory` – in der
     * Datenbank auf dem Gerät jedes Kindes und in jeder Sicherungsdatei. Die Abzeichen werden
     * über sie summiert (`summeErtrag`), deshalb setzt schon ein einziger geänderter Wert die
     * betroffene Schatztruhe still auf leer zurück.
     *
     * Diese Liste ist bewusst von Hand gepflegt und **nicht** aus [BADGE_CATEGORIES] abgeleitet:
     * Nur so wird eine Umbenennung rot statt unbemerkt. Eine **neue** Kategorie darf hier
     * ergänzt werden; eine bestehende zu ändern kostet Kinder ihren Fortschritt.
     */
    @Test
    fun fortschrittskennungen_sindEingefroren() {
        val eingefroren = setOf(
            "PLUS", "MINUS", "MAL", "GETEILT",
            "VERLIEBTE_ZAHLEN", "VERLIEBTE_ZAHLEN_OHNE_HILFE",
            "GROESSER_KLEINER",
            "VERDOPPELN_LEICHT", "VERDOPPELN_SCHWER",
            "ZAHLENMAUER_LEICHT", "ZAHLENMAUER_MITTEL",
            "ZAHLENMAUER_SCHWER", "ZAHLENMAUER_ULTRA",
            "NACHBARZAHLEN_LEICHT", "NACHBARZAHLEN_SCHWER",
            "EINMALEINS_2_AUFSAGEN", "EINMALEINS_2_DURCHEINANDER",
            "EINMALEINS_3_AUFSAGEN", "EINMALEINS_3_DURCHEINANDER",
            "EINMALEINS_4_AUFSAGEN", "EINMALEINS_4_DURCHEINANDER",
            "EINMALEINS_5_AUFSAGEN", "EINMALEINS_5_DURCHEINANDER",
            "EINMALEINS_6_AUFSAGEN", "EINMALEINS_6_DURCHEINANDER",
            "EINMALEINS_7_AUFSAGEN", "EINMALEINS_7_DURCHEINANDER",
            "EINMALEINS_8_AUFSAGEN", "EINMALEINS_8_DURCHEINANDER",
            "EINMALEINS_9_AUFSAGEN", "EINMALEINS_9_DURCHEINANDER",
            "EINMALEINS_10_AUFSAGEN", "EINMALEINS_10_DURCHEINANDER",
        )

        val aktuell = BADGE_CATEGORIES.map { it.key }.toSet()

        val verschwunden = eingefroren - aktuell
        assertEquals(
            "Fortschrittskennung(en) verschwunden – gespeicherter Fortschritt wird dadurch " +
                "nicht mehr gezählt: $verschwunden",
            emptySet<String>(),
            verschwunden,
        )
    }

    /**
     * Das Einmaleins rechnet in allen 18 Kennungen im festen Zahlenraum 100 – auch die 2er-Reihe.
     * Der Wert steht in der Historie und ist damit so eingefroren wie die Kennung (ADR-0034).
     */
    @Test
    fun einmaleins_hatAchtzehnKennungenImFestenZahlenraum100_ineinemReiter() {
        val einmaleins = BADGE_CATEGORIES.filter { it.key.startsWith("EINMALEINS_") }

        assertEquals(18, einmaleins.size)
        einmaleins.forEach { assertEquals(it.key, listOf(100), it.ranges) }
        assertEquals(
            einmaleins.map { it.key },
            BADGE_TABS.single { it.key == EINMALEINS_TAB }.categoryKeys,
        )
        val siebenerDurcheinander = einmaleins.single { it.key == "EINMALEINS_7_DURCHEINANDER" }
        assertEquals("7er-Reihe · Durcheinander", siebenerDurcheinander.chestLabel)
    }

    /** Jede Kennung darf nur einmal vorkommen, sonst zählen zwei Truhen dieselben Aufgaben. */
    @Test
    fun fortschrittskennungen_sindEindeutig() {
        val keys = BADGE_CATEGORIES.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
    }

    /**
     * `UnlockedBadge.encode` trennt die Felder mit "-". Enthielte eine Kennung selbst einen
     * Bindestrich, zerfiele das Navigations-Argument in zu viele Segmente und das frisch
     * gewonnene Abzeichen würde beim Dekodieren verworfen (siehe `BADGE_FIELD_SEP`).
     */
    @Test
    fun fortschrittskennungen_enthaltenKeinTrennzeichen() {
        BADGE_CATEGORIES.forEach { kategorie ->
            assertEquals(
                "Kennung \"${kategorie.key}\" enthält das Feldtrennzeichen \"-\"",
                -1,
                kategorie.key.indexOf('-'),
            )
        }
    }

    @Test
    fun highestTier_belowFirstThreshold_isNull() {
        assertNull(highestTier(0))
        assertNull(highestTier(19))
    }

    @Test
    fun highestTier_atAndBetweenThresholds_returnsCorrectTier() {
        assertEquals(BadgeTier.BRONZE, highestTier(20))
        assertEquals(BadgeTier.BRONZE, highestTier(49))
        assertEquals(BadgeTier.SILBER, highestTier(50))
        assertEquals(BadgeTier.GOLD, highestTier(100))
        assertEquals(BadgeTier.SAPHIR, highestTier(150))
        assertEquals(BadgeTier.DIAMANT, highestTier(200))
        assertEquals(BadgeTier.PLATIN, highestTier(500))
        assertEquals(BadgeTier.PLATIN, highestTier(9999))
    }

    @Test
    fun tiersUpToNext_growsWithProgress() {
        assertEquals(listOf(BadgeTier.BRONZE), tiersUpToNext(0))
        assertEquals(listOf(BadgeTier.BRONZE), tiersUpToNext(19))
        assertEquals(listOf(BadgeTier.BRONZE, BadgeTier.SILBER), tiersUpToNext(20))
        assertEquals(listOf(BadgeTier.BRONZE, BadgeTier.SILBER), tiersUpToNext(49))
        assertEquals(
            listOf(BadgeTier.BRONZE, BadgeTier.SILBER, BadgeTier.GOLD),
            tiersUpToNext(50)
        )
        assertEquals(BadgeTier.entries.toList(), tiersUpToNext(499))
        assertEquals(BadgeTier.entries.toList(), tiersUpToNext(500))
        assertEquals(BadgeTier.entries.toList(), tiersUpToNext(9999))
    }

    @Test
    fun newlyReachedTiers_noProgress_isEmpty() {
        assertEquals(emptyList<BadgeTier>(), newlyReachedTiers(20, 20))
        assertEquals(emptyList<BadgeTier>(), newlyReachedTiers(30, 25)) // previous > new
    }

    @Test
    fun newlyReachedTiers_crossingSingleThreshold_returnsThatTier() {
        assertEquals(listOf(BadgeTier.BRONZE), newlyReachedTiers(19, 20))
        assertEquals(listOf(BadgeTier.SILBER), newlyReachedTiers(49, 55))
    }

    @Test
    fun newlyReachedTiers_crossingMultipleThresholds_returnsAllInOrder() {
        assertEquals(
            listOf(BadgeTier.BRONZE, BadgeTier.SILBER),
            newlyReachedTiers(19, 60)
        )
    }

    @Test
    fun encodeAndDecode_roundTrips() {
        val badges = listOf(
            UnlockedBadge("PLUS", 20, BadgeTier.BRONZE),
            UnlockedBadge("GETEILT", 100, BadgeTier.GOLD),
        )
        val encoded = badges.joinToString(",") { it.encode() }
        assertEquals(badges, decodeUnlockedBadges(encoded))
    }

    @Test
    fun decode_placeholderOrGarbage_isEmpty() {
        assertEquals(emptyList<UnlockedBadge>(), decodeUnlockedBadges("none"))
        assertEquals(emptyList<UnlockedBadge>(), decodeUnlockedBadges("kaputt_x"))
    }

    @Test
    fun encodeAndDecode_verliebteZahlen_roundTrips() {
        // Regression: Der Key VERLIEBTE_ZAHLEN enthält selbst einen Unterstrich. Mit dem
        // früheren "_"-Trenner ergab split() vier Segmente und das Abzeichen wurde beim
        // Dekodieren verworfen – die Ordensverleihung erschien dort nie.
        val badge = UnlockedBadge("VERLIEBTE_ZAHLEN", 10, BadgeTier.BRONZE)
        assertEquals(listOf(badge), decodeUnlockedBadges(badge.encode()))
    }

    @Test
    fun encodeAndDecode_mixedWithUnderscoreKey_roundTrips() {
        val badges = listOf(
            UnlockedBadge("PLUS", 20, BadgeTier.BRONZE),
            UnlockedBadge("VERLIEBTE_ZAHLEN", 10, BadgeTier.SILBER),
        )
        val encoded = badges.joinToString(",") { it.encode() }
        assertEquals(badges, decodeUnlockedBadges(encoded))
    }

    @Test
    fun badgeCategories_ranges_areAppropriate() {
        val verliebte = BADGE_CATEGORIES.first { it.key == "VERLIEBTE_ZAHLEN" }
        assertEquals(listOf(10), verliebte.ranges)
        val plus = BADGE_CATEGORIES.first { it.key == "PLUS" }
        assertEquals(BADGE_RANGES, plus.ranges)
    }

    /**
     * „Mit Hilfe" und „Ohne Hilfe" teilen sich den Reiter Verliebte Zahlen und unterscheiden sich
     * dort allein in der Beschriftung – dasselbe Wort wie auf dem Auswahl-Bildschirm und in der
     * Freigabe (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
     */
    @Test
    fun verliebteZahlen_mitUndOhneHilfe_teilenSichEinenReiter() {
        val reiter = BADGE_TABS.single { it.key == "VERLIEBTE_ZAHLEN" }
        assertEquals(listOf("VERLIEBTE_ZAHLEN", "VERLIEBTE_ZAHLEN_OHNE_HILFE"), reiter.categoryKeys)

        val truhen = reiter.categoryKeys.map { key ->
            val kategorie = BADGE_CATEGORIES.single { it.key == key }
            truhenBeschriftung(kategorie, kategorie.ranges.single())
        }
        assertEquals(listOf("Mit Hilfe", "Ohne Hilfe"), truhen)
    }

    /**
     * Die Mindestanzahl, ab der der **Übungsstand** eine Trefferquote zeigt, steht **je Kennung**
     * im Katalog statt als ein Wert für alles: Die Zahlenmauer schreibt eine Zeile je **Mauer**
     * (3–10 je Runde), die Grundrechenarten eine je **Aufgabe** (5–25 je Runde). Dieselbe Schwelle
     * hieße für beide etwas anderes – bei der Zahlenmauer stünde nach zwei Runden noch immer „zu
     * wenig geübt".
     */
    @Test
    fun mindestAufgaben_stehenJeKennung_undSindBeiDerZahlenmauerNiedriger() {
        val plus = BADGE_CATEGORIES.first { it.key == "PLUS" }
        assertEquals(10, plus.mindestAufgaben)

        BADGE_CATEGORIES.filter { it.key.startsWith("ZAHLENMAUER") }.forEach { kategorie ->
            assertEquals(
                "Die Zahlenmauer zählt je Mauer, nicht je Feld: ${kategorie.key}",
                5,
                kategorie.mindestAufgaben,
            )
        }
    }

    /** Ohne positive Schwelle stünde bei null geübten Aufgaben eine Trefferquote von 0 %. */
    @Test
    fun jedeKennungHatEinePositiveMindestanzahl() {
        BADGE_CATEGORIES.forEach { kategorie ->
            assertEquals(
                "Kennung ${kategorie.key} ohne Mindestanzahl",
                true,
                kategorie.mindestAufgaben > 0,
            )
        }
    }

    @Test
    fun badgeTabs_coverEveryCategoryExactlyOnce() {
        // Jede Kategorie muss in genau einem Reiter landen, sonst verschwindet ihre Truhe
        // (fehlt) oder taucht doppelt auf (verwirrend) in der Abzeichen-Übersicht.
        val allTabKeys = BADGE_TABS.flatMap { it.categoryKeys }
        assertEquals(BADGE_CATEGORIES.map { it.key }.sorted(), allTabKeys.sorted())
        assertEquals(allTabKeys.size, allTabKeys.toSet().size)
    }

    @Test
    fun badgeTabs_verdoppeln_teiltSichEinenReiterMitLeichtUndSchwerAlsGetrennteTruhen() {
        val verdoppelnTab = BADGE_TABS.first { it.key == "VERDOPPELN" }
        assertEquals(listOf("VERDOPPELN_LEICHT", "VERDOPPELN_SCHWER"), verdoppelnTab.categoryKeys)

        val leicht = BADGE_CATEGORIES.first { it.key == "VERDOPPELN_LEICHT" }
        val schwer = BADGE_CATEGORIES.first { it.key == "VERDOPPELN_SCHWER" }
        assertEquals("Leicht", leicht.chestLabel)
        assertEquals("Schwer", schwer.chestLabel)
    }

    /**
     * Nachbarzahlen ist der Fall, für den die Beschriftung **beides** tragen muss: Im gemeinsamen
     * Reiter stehen vier Truhen, die sich in der Stufe **und** im Zahlenraum unterscheiden.
     */
    @Test
    fun truhenBeschriftung_nachbarzahlen_nenntStufeUndZahlenraum() {
        val leicht = BADGE_CATEGORIES.first { it.key == "NACHBARZAHLEN_LEICHT" }
        val schwer = BADGE_CATEGORIES.first { it.key == "NACHBARZAHLEN_SCHWER" }

        assertEquals("Leicht bis 20", truhenBeschriftung(leicht, 20))
        assertEquals("Leicht bis 100", truhenBeschriftung(leicht, 100))
        assertEquals("Schwer bis 20", truhenBeschriftung(schwer, 20))
        assertEquals("Schwer bis 100", truhenBeschriftung(schwer, 100))
    }

    /** Eine Kategorie mit nur einem Zahlenraum bleibt bei ihrer Stufe – „Leicht bis 10" sagte nichts. */
    @Test
    fun truhenBeschriftung_beiEinemZahlenraum_bleibtEsBeiDerStufe() {
        val verdoppeln = BADGE_CATEGORIES.first { it.key == "VERDOPPELN_LEICHT" }

        assertEquals("Leicht", truhenBeschriftung(verdoppeln, 10))
    }

    @Test
    fun truhenBeschriftung_ohneStufe_nenntDenZahlenraum() {
        val plus = BADGE_CATEGORIES.first { it.key == "PLUS" }

        assertEquals("Zahlenraum bis 50", truhenBeschriftung(plus, 50))
    }

    /**
     * Der eigentliche Zweck der Beschriftung: In **einem** Reiter der Abzeichen-Übersicht darf keine
     * Truhe wie eine andere heißen – sonst weiß ein Kind nicht, welche es gerade füllt.
     */
    @Test
    fun truhenBeschriftungen_sindInnerhalbEinesReitersEindeutig() {
        BADGE_TABS.forEach { tab ->
            val beschriftungen = tab.categoryKeys
                .mapNotNull { key -> BADGE_CATEGORIES.firstOrNull { it.key == key } }
                .flatMap { kategorie -> kategorie.ranges.map { truhenBeschriftung(kategorie, it) } }

            assertEquals(
                "Der Reiter ${tab.key} hat doppelte Truhen-Beschriftungen: $beschriftungen",
                beschriftungen.size,
                beschriftungen.toSet().size,
            )
        }
    }
}
