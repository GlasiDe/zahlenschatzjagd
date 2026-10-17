package io.github.glaside.zahlenschatzjagd.ui.parent

import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.ui.selection.UEBUNGSFORMEN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Der **Übungsstand**: das Fenster, die Schwelle je Fortschrittskennung und die Gliederung.
 *
 * Die Uhr steht fest – sonst hinge das 30-Tage-Fenster an der Uhr des Rechners.
 */
class UebungsstandTest {

    /** Mittwoch, der 12. August 2026, 12 Uhr. */
    private val jetzt: Long = LocalDateTime.parse("2026-08-12T12:00")
        .atZone(ZoneId.of("Europe/Berlin")).toInstant().toEpochMilli()

    private val tag = 24L * 60 * 60 * 1000
    private val minute = 60L * 1000

    /**
     * Eine Aufgabe in der Historie. [vorMinuten] staffelt die Zeitstempel, damit „die letzten 50"
     * eine eindeutige Menge ist und nicht von der Reihenfolge gleicher Stempel abhängt.
     */
    private fun aufgabe(
        kennung: String,
        richtig: Boolean = true,
        zahlenraum: Int = 10,
        vorTagen: Long = 0,
        vorMinuten: Long = 0,
    ) = TaskHistory(
        userName = "Mira",
        taskCategory = kennung,
        numberRange = zahlenraum,
        questionText = "1 + 1 = _",
        expectedAnswer = "2",
        userAnswer = if (richtig) "2" else "3",
        isCorrect = richtig,
        timestamp = jetzt - vorTagen * tag - vorMinuten * minute,
    )

    /** [anzahl] Aufgaben derselben Kennung, die jüngste zuerst. */
    private fun aufgaben(
        kennung: String,
        anzahl: Int,
        richtig: Boolean = true,
        zahlenraum: Int = 10,
        vorTagen: Long = 0,
    ) = List(anzahl) { index ->
        aufgabe(kennung, richtig, zahlenraum, vorTagen, vorMinuten = index.toLong())
    }

    /** Die Zeile mit diesem Schlüssel – gleich auf welcher der drei Ebenen sie liegt. */
    private fun List<Uebungsstandzeile>.zeile(schluessel: String): Uebungsstandzeile =
        alleZeilen().firstOrNull { it.schluessel == schluessel }
            ?: error("Keine Zeile \"$schluessel\" in ${alleZeilen().map { it.schluessel }}")

    private fun List<Uebungsstandzeile>.alleZeilen(): List<Uebungsstandzeile> =
        flatMap { listOf(it) + it.unterzeilen.alleZeilen() }

    // --- Das Fenster ---------------------------------------------------------------------

    @Test
    fun eineKennungBringtHoechstens50AufgabenEin() {
        // 60 Aufgaben, die 10 ältesten falsch: Fielen sie mit ins Fenster, stünde die Quote unter
        // 100 %.
        val historie = aufgaben("PLUS", anzahl = 50) +
            List(10) { aufgabe("PLUS", richtig = false, vorMinuten = 50L + it) }

        val plus = baueUebungsstand(historie, jetzt).zeile("PLUS")

        assertEquals(50, plus.aufgaben)
        assertEquals(50, plus.richtig)
    }

    @Test
    fun wasAelterAls30TageIstZaehltNichtMehrMit() {
        val historie = aufgaben("PLUS", anzahl = 20) +
            aufgaben("PLUS", anzahl = 20, richtig = false, vorTagen = 31)

        val plus = baueUebungsstand(historie, jetzt).zeile("PLUS")

        assertEquals(20, plus.aufgaben)
        assertEquals(100, plus.trefferquote)
    }

    @Test
    fun genauAmRandDes30TageFenstersZaehltNoch() {
        val historie = listOf(aufgabe("PLUS", vorTagen = 30))

        assertEquals(1, baueUebungsstand(historie, jetzt).zeile("PLUS").aufgaben)
    }

    /**
     * Die 50 gelten **je Kennung**, nicht je Übungsform: Sonst verdrängte fleißiges Plus-Üben das
     * Minus aus dem Fenster, und über Minus stünde nichts mehr da.
     */
    @Test
    fun dasFensterGiltJeKennung_nichtJeUebungsform() {
        val historie = aufgaben("PLUS", anzahl = 50) + aufgaben("MINUS", anzahl = 50)

        val zeilen = baueUebungsstand(historie, jetzt)

        assertEquals(100, zeilen.zeile("GRUNDRECHENARTEN").aufgaben)
        assertEquals(50, zeilen.zeile("PLUS").aufgaben)
        assertEquals(50, zeilen.zeile("MINUS").aufgaben)
    }

    // --- Die Schwelle --------------------------------------------------------------------

    @Test
    fun unterDerSchwelleStehtKeineProzentzahl() {
        val plus = baueUebungsstand(aufgaben("PLUS", anzahl = 9), jetzt).zeile("PLUS")

        assertTrue(plus.geuebt)
        assertNull(plus.trefferquote)
    }

    @Test
    fun abDerSchwelleStehtEineProzentzahl() {
        val plus = baueUebungsstand(aufgaben("PLUS", anzahl = 10), jetzt).zeile("PLUS")

        assertEquals(100, plus.trefferquote)
    }

    /** Eine ganze Mauer ist eine Zeile der Historie – deshalb reichen dort fünf. */
    @Test
    fun dieZahlenmauerErreichtIhreSchwelleFrueher() {
        val historie = aufgaben("ZAHLENMAUER_LEICHT", anzahl = 5, zahlenraum = 20)

        val mauer = baueUebungsstand(historie, jetzt).zeile("ZAHLENMAUER_LEICHT")

        assertEquals(5, mauer.mindestAufgaben)
        assertEquals(100, mauer.trefferquote)
    }

    /**
     * Der Ertrag wirkt allein auf die Truhen: Eine teilweise richtige Mauer (Ertrag 1) zählt im
     * Übungsstand weiter als nicht gelöst (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
     */
    @Test
    fun eineTeilweiseRichtigeMauerZaehltImUebungsstandAlsNichtGeloest() {
        val ganz = aufgaben("ZAHLENMAUER_MITTEL", anzahl = 5, zahlenraum = 50)
        val teilweise = aufgaben("ZAHLENMAUER_MITTEL", anzahl = 5, richtig = false, zahlenraum = 50, vorTagen = 1)
            .map { it.copy(ertrag = 1) }

        val mauer = baueUebungsstand(ganz + teilweise, jetzt).zeile("ZAHLENMAUER_MITTEL")

        assertEquals(50, mauer.trefferquote)
    }

    /** „Noch nie angefasst" und „zu dünn für eine Aussage" sind zwei verschiedene Befunde. */
    @Test
    fun nochNichtGeuebtIstNichtDasselbeWieZuWenigGeuebt() {
        val zeilen = baueUebungsstand(aufgaben("PLUS", anzahl = 3), jetzt)

        assertTrue(zeilen.zeile("PLUS").geuebt)
        assertFalse(zeilen.zeile("MINUS").geuebt)
        assertEquals("zu wenig geübt", quotenText(zeilen.zeile("PLUS")))
        assertEquals("noch nicht geübt", quotenText(zeilen.zeile("MINUS")))
    }

    @Test
    fun dieTrefferquoteRundetKaufmaennisch() {
        // 7 von 12 = 58,33 % → 58; 8 von 12 = 66,67 % → 67
        val siebenVonZwoelf = aufgaben("PLUS", anzahl = 7) +
            List(5) { aufgabe("PLUS", richtig = false, vorMinuten = 7L + it) }
        val achtVonZwoelf = aufgaben("PLUS", anzahl = 8) +
            List(4) { aufgabe("PLUS", richtig = false, vorMinuten = 8L + it) }
        // Und der Fall, an dem sich „kaufmännisch" überhaupt erst zeigt: 9 von 24 = genau 37,5 %.
        // Abschneiden ergäbe hier 37.
        val neunVonVierundzwanzig = aufgaben("PLUS", anzahl = 9) +
            List(15) { aufgabe("PLUS", richtig = false, vorMinuten = 9L + it) }

        assertEquals(58, baueUebungsstand(siebenVonZwoelf, jetzt).zeile("PLUS").trefferquote)
        assertEquals(67, baueUebungsstand(achtVonZwoelf, jetzt).zeile("PLUS").trefferquote)
        assertEquals(38, baueUebungsstand(neunVonVierundzwanzig, jetzt).zeile("PLUS").trefferquote)
    }

    // --- Die Zeile rechnet nur mit dem, was darunter eine Prozentzahl trägt ---------------

    /**
     * Am Gerät aufgefallen: Grundrechenarten stand auf 95 %, aufgeklappt trug nur Minus eine Zahl –
     * 96 % –, der Rest „zu wenig geübt". Die obere Zahl lag damit **außerhalb** des Bereichs aller
     * sichtbaren Zahlen, weil die zu dünnen Übungen mitgerechnet wurden. Eine Zeile, die man nicht
     * nachvollziehen kann, ist schlechter als eine, die aufgeht.
     */
    @Test
    fun eineUebungUnterDerSchwelleVerschiebtDieZeileDarueberNicht() {
        // Minus: 48 von 50 richtig = 96 %. Plus: 3 Aufgaben, alle falsch – zu dünn für eine Aussage.
        val historie = aufgaben("MINUS", anzahl = 48) +
            List(2) { aufgabe("MINUS", richtig = false, vorMinuten = 48L + it) } +
            aufgaben("PLUS", anzahl = 3, richtig = false)

        val zeilen = baueUebungsstand(historie, jetzt)

        assertNull("Plus ist zu dünn", zeilen.zeile("PLUS").trefferquote)
        assertEquals(96, zeilen.zeile("MINUS").trefferquote)
        assertEquals(96, zeilen.zeile("GRUNDRECHENARTEN").trefferquote)
    }

    /** Dieselbe Regel eine Ebene tiefer: der Zahlenraum unter der Schwelle zieht die Kennung nicht. */
    @Test
    fun einZahlenraumUnterDerSchwelleVerschiebtSeineKennungNicht() {
        val historie = aufgaben("PLUS", anzahl = 20, zahlenraum = 10) +
            List(4) { aufgabe("PLUS", richtig = false, zahlenraum = 20, vorMinuten = 20L + it) }

        val plus = baueUebungsstand(historie, jetzt).zeile("PLUS")

        assertNull(plus.unterzeilen.single { it.beschriftung == "bis 20" }.trefferquote)
        assertEquals(100, plus.trefferquote)
    }

    /**
     * Trägt keine einzige Übung darunter eine Zahl, trägt die Übungsform auch keine – selbst wenn in
     * Summe genug zusammenkäme. Sonst stünde oben eine Prozentzahl, für die darunter jede Grundlage
     * ausdrücklich bestritten wird.
     */
    @Test
    fun ohneEineEinzigeTragfaehigeUebungTraegtAuchDieZeileDarueberKeineZahl() {
        val historie = aufgaben("PLUS", anzahl = 6) + aufgaben("MINUS", anzahl = 6) +
            aufgaben("MAL", anzahl = 6) + aufgaben("GETEILT", anzahl = 6)

        val grundrechenarten = baueUebungsstand(historie, jetzt).zeile("GRUNDRECHENARTEN")

        assertNull(grundrechenarten.trefferquote)
        assertEquals("zu wenig geübt", quotenText(grundrechenarten))
    }

    /**
     * Die **Anzahl** bleibt davon unberührt: „Was geübt wurde" ist die andere Frage des Bildschirms
     * und zählt alles, auch was für eine Quote zu dünn ist.
     */
    @Test
    fun dieAnzahlZaehltAuchDieAufgabenDieKeineQuoteTragen() {
        val historie = aufgaben("MINUS", anzahl = 50) + aufgaben("PLUS", anzahl = 3)

        val grundrechenarten = baueUebungsstand(historie, jetzt).zeile("GRUNDRECHENARTEN")

        assertEquals(53, grundrechenarten.aufgaben)
        assertEquals(100, grundrechenarten.trefferquote)
    }

    // --- Die Gliederung ------------------------------------------------------------------

    @Test
    fun dieObersteEbeneSindDieUebungsformen() {
        val zeilen = baueUebungsstand(emptyList(), jetzt)

        assertEquals(UEBUNGSFORMEN.map { it.name }, zeilen.map { it.beschriftung })
    }

    /** Drei Ebenen: Übungsform → Rechenart → Zahlenraum. */
    @Test
    fun grundrechenartenKlappenAufDieRechenartenUndDieseAufDieZahlenraeume() {
        val zeilen = baueUebungsstand(emptyList(), jetzt)

        val grundrechenarten = zeilen.zeile("GRUNDRECHENARTEN")
        assertEquals(
            listOf("Plus", "Minus", "Mal", "Geteilt"),
            grundrechenarten.unterzeilen.map { it.beschriftung },
        )
        assertEquals(
            listOf("bis 10", "bis 20", "bis 50", "bis 100"),
            zeilen.zeile("PLUS").unterzeilen.map { it.beschriftung },
        )
    }

    /**
     * Aufgeklappt wird nur, wo es darunter mehr als eins gibt. Jede Stufe der Zahlenmauer hat
     * genau einen festen Zahlenraum – eine dritte Ebene wiederholte dort nur die Zeile darüber.
     */
    @Test
    fun dieStufenDerZahlenmauerHabenKeineZahlenraumEbene() {
        val zeilen = baueUebungsstand(emptyList(), jetzt)

        assertEquals(
            listOf("Leicht", "Mittel", "Schwer", "Ultra"),
            zeilen.zeile("ZAHLENMAUER").unterzeilen.map { it.beschriftung },
        )
        assertEquals(emptyList<Uebungsstandzeile>(), zeilen.zeile("ZAHLENMAUER_LEICHT").unterzeilen)
    }

    /**
     * Nachbarzahlen nutzt alle drei Ebenen: erst die beiden Stufen – sie sind **verschiedene**
     * Leistungen und dürfen nicht in einer Zahl verschwimmen –, darunter je die beiden Zahlenräume.
     */
    @Test
    fun nachbarzahlenWeisenLeichtUndSchwerGetrenntAus_jeMitBeidenZahlenraeumen() {
        val zeilen = baueUebungsstand(emptyList(), jetzt)

        assertEquals(
            listOf("Leicht", "Schwer"),
            zeilen.zeile("NACHBARZAHLEN").unterzeilen.map { it.beschriftung },
        )
        assertEquals(
            listOf("bis 20", "bis 100"),
            zeilen.zeile("NACHBARZAHLEN_SCHWER").unterzeilen.map { it.beschriftung },
        )
    }

    /**
     * Das Einmaleins klappt auf eine Zeile je Kennung auf – Reihe und Stufe –, ohne Zahlenraum-Ebene,
     * weil alle 18 im festen Zahlenraum 100 rechnen. So sieht ein Erwachsener, welche Reihe hakt.
     */
    @Test
    fun einmaleinsZeigtEineZeileJeReiheUndStufe() {
        val zeilen = baueUebungsstand(aufgaben("EINMALEINS_7_DURCHEINANDER", 10, zahlenraum = 100), jetzt)

        val einmaleins = zeilen.zeile("EINMALEINS")
        assertEquals(18, einmaleins.unterzeilen.size)
        assertEquals("2er-Reihe · Aufsagen", einmaleins.unterzeilen.first().beschriftung)
        assertEquals("10er-Reihe · Durcheinander", einmaleins.unterzeilen.last().beschriftung)
        assertEquals("100 %", quotenText(zeilen.zeile("EINMALEINS_7_DURCHEINANDER")))
        assertEquals("noch nicht geübt", quotenText(zeilen.zeile("EINMALEINS_7_AUFSAGEN")))
        assertEquals(emptyList<Uebungsstandzeile>(), zeilen.zeile("EINMALEINS_7_AUFSAGEN").unterzeilen)
    }

    /** Die leichte Stufe schiebt die schwere nicht – jede Stufe rechnet in ihrem eigenen Fenster. */
    @Test
    fun dieLeichteStufeDerNachbarzahlenRechnetNichtInDieSchwere() {
        val historie = aufgaben("NACHBARZAHLEN_LEICHT", anzahl = 20, zahlenraum = 20) +
            List(20) { aufgabe("NACHBARZAHLEN_SCHWER", richtig = false, zahlenraum = 100, vorMinuten = 20L + it) }

        val zeilen = baueUebungsstand(historie, jetzt)

        assertEquals(100, zeilen.zeile("NACHBARZAHLEN_LEICHT").trefferquote)
        assertEquals(0, zeilen.zeile("NACHBARZAHLEN_SCHWER").trefferquote)
        assertEquals(20, zeilen.zeile("NACHBARZAHLEN_SCHWER_100").aufgaben)
        assertEquals(0, zeilen.zeile("NACHBARZAHLEN_SCHWER_20").aufgaben)
    }

    /** Eine einzige Kennung: Die Übungsform klappt direkt auf ihre Zahlenräume auf. */
    @Test
    fun zahlenVergleichenKlapptDirektAufDieZahlenraeume() {
        val zeilen = baueUebungsstand(emptyList(), jetzt)

        assertEquals(
            listOf("bis 10", "bis 20", "bis 50", "bis 100"),
            zeilen.zeile("VERGLEICHEN").unterzeilen.map { it.beschriftung },
        )
    }

    /**
     * „Mit Hilfe" und „Ohne Hilfe" sind zwei Kennungen mit je einem festen Zahlenraum: aufgeklappt
     * wird bis zur Stufe, darunter nichts. So sehen Eltern, ob ihr Kind es schon ohne Münzen kann
     * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`).
     */
    @Test
    fun verliebteZahlenZeigenMitUndOhneHilfeGetrennt() {
        val historie = aufgaben("VERLIEBTE_ZAHLEN", anzahl = 12) +
            List(10) { aufgabe("VERLIEBTE_ZAHLEN_OHNE_HILFE", richtig = it < 5, vorMinuten = 12L + it) }

        val stufen = baueUebungsstand(historie, jetzt).zeile("VERLIEBTE").unterzeilen

        assertEquals(listOf("Mit Hilfe", "Ohne Hilfe"), stufen.map { it.beschriftung })
        assertEquals(listOf(100, 50), stufen.map { it.trefferquote })
        assertEquals(emptyList<Uebungsstandzeile>(), stufen.flatMap { it.unterzeilen })
    }

    @Test
    fun dieZahlenraumZeilenTeilenDasFensterIhrerKennungAuf() {
        val historie = aufgaben("PLUS", anzahl = 12, zahlenraum = 10) +
            List(8) { aufgabe("PLUS", richtig = false, zahlenraum = 20, vorMinuten = 12L + it) }

        val plus = baueUebungsstand(historie, jetzt).zeile("PLUS")

        assertEquals(20, plus.aufgaben)
        assertEquals(12, plus.unterzeilen.single { it.beschriftung == "bis 10" }.aufgaben)
        assertEquals(8, plus.unterzeilen.single { it.beschriftung == "bis 20" }.aufgaben)
        assertEquals(0, plus.unterzeilen.single { it.beschriftung == "bis 100" }.aufgaben)
    }

    /**
     * Eine Kennung, die es nicht mehr gibt, macht den Übungsstand nicht unlesbar – genau wie bei
     * der Freigabe (`freigabeAusSperren`).
     */
    @Test
    fun eineUnbekannteKennungWirdStillUebergangen() {
        val historie = aufgaben("PLUS", anzahl = 10) + aufgaben("ENTFALLENE_UEBUNG", anzahl = 10)

        val zeilen = baueUebungsstand(historie, jetzt)

        assertEquals(10, zeilen.zeile("GRUNDRECHENARTEN").aufgaben)
        assertTrue(zeilen.alleZeilen().none { it.schluessel == "ENTFALLENE_UEBUNG" })
    }

    @Test
    fun jederSchluesselKommtNurEinmalVor() {
        val schluessel = baueUebungsstand(emptyList(), jetzt).alleZeilen().map { it.schluessel }

        assertEquals(schluessel.size, schluessel.toSet().size)
    }

    // --- Die Texte -----------------------------------------------------------------------

    /** Das Ticket verlangt, dass die Schwelle **auf dem Bildschirm** steht – und beide Werte. */
    @Test
    fun derSchwellenhinweisNenntBeideSchwellen() {
        val hinweis = schwellenHinweis(baueUebungsstand(emptyList(), jetzt))

        assertTrue(hinweis, hinweis.contains("10"))
        assertTrue(hinweis, hinweis.contains("Zahlenmauer"))
        assertTrue(hinweis, hinweis.contains("5"))
    }

    @Test
    fun dieAnzahlBleibtBeiEinerAufgabeDeutsch() {
        assertEquals("1 Aufgabe", aufgabenText(1))
        assertEquals("12 Aufgaben", aufgabenText(12))
    }
}
