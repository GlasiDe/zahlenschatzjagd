package io.github.glaside.zahlenschatzjagd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Die reine Aufgaben-Erzeugung der Übungsform **Nachbarzahlen** (Vorbild: [ZahlenmauerLogicTest]).
 * Geprüft wird das äußere Verhalten – welche Aufgaben herauskommen –, nicht, wie sie intern
 * zusammengesetzt werden.
 */
class NachbarzahlenLogicTest {

    /** Der kleine Zahlenraum – für alles, was in beiden gleich ist und nur einen Beleg braucht. */
    private val zahlenraum20 = 20

    /** Beide wählbaren Zahlenräume – die Ränder müssen in **jedem** stimmen, nicht nur im kleinen. */
    private val zahlenraeume = listOf(20, 100)

    /** Wie viele verschiedene Aufgaben die leichte Stufe im Zahlenraum 20 kennt: die Mitten 1..19. */
    private val vorratsGroesse = 19

    /** Die schwere Stufe zieht über Reihe **und** Position – dieselben 19 Reihen mal drei Positionen. */
    private val vorratsGroesseSchwer = vorratsGroesse * Nachbarposition.entries.size

    @Test
    fun alleDreiZahlenLiegenImGewaehltenZahlenraum() {
        zahlenraeume.forEach { gewaehlt ->
            NachbarzahlenSchwierigkeitsstufe.entries.forEach { schwierigkeitsstufe ->
                moeglicheAufgaben(schwierigkeitsstufe, gewaehlt).forEach { aufgabe ->
                    assertTrue(
                        "Reihe ${aufgabe.links}|${aufgabe.mitte}|${aufgabe.rechts} verlässt den Zahlenraum $gewaehlt",
                        aufgabe.links >= 0 && aufgabe.rechts <= gewaehlt,
                    )
                }
            }
        }
    }

    @Test
    fun dieMittlereZahlLiegtAmRandBeiEinsUndBeiZahlenraumMinusEins() {
        zahlenraeume.forEach { gewaehlt ->
            val mitten = moeglicheAufgaben(NachbarzahlenSchwierigkeitsstufe.LEICHT, gewaehlt).map { it.mitte }

            assertEquals(1, mitten.min())
            assertEquals(gewaehlt - 1, mitten.max())
        }
    }

    @Test
    fun dieDreiZahlenSindAufeinanderfolgend() {
        moeglicheAufgaben(NachbarzahlenSchwierigkeitsstufe.LEICHT, zahlenraum20).forEach { aufgabe ->
            assertEquals(aufgabe.mitte - 1, aufgabe.links)
            assertEquals(aufgabe.mitte + 1, aufgabe.rechts)
        }
    }

    @Test
    fun inDerLeichtenStufeIstImmerDieMittlereZahlVorgegeben() {
        moeglicheAufgaben(NachbarzahlenSchwierigkeitsstufe.LEICHT, zahlenraum20).forEach { aufgabe ->
            assertEquals(Nachbarposition.MITTE, aufgabe.vorgegeben)
        }
    }

    @Test
    fun inDerSchwerenStufeKommenAlleDreiPositionenVor() {
        val positionen = moeglicheAufgaben(NachbarzahlenSchwierigkeitsstufe.SCHWER, zahlenraum20)
            .map { it.vorgegeben }

        assertEquals(Nachbarposition.entries.toSet(), positionen.toSet())
    }

    /**
     * Über eine ganze Runde hinweg sieht ein Kind alle drei Formen – nicht nur die gewohnte Mitte.
     * Zugesichert wird das, nicht dem Zufall überlassen: Rein zufällig gezogen fehlte in einer
     * Runde von fünf Aufgaben (dem Minimum des Reglers) in gut einem Drittel der Fälle eine
     * Position. Deshalb über viele Startwerte geprüft und schon nach den ersten **drei** Aufgaben.
     */
    @Test
    fun jedeRundeDerSchwerenStufeZeigtAlleDreiPositionen() {
        (1..50).forEach { startwert ->
            val vorrat = Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.SCHWER, zahlenraum20, Random(startwert))

            val ersteDrei = List(Nachbarposition.entries.size) { vorrat.ziehe() }

            assertEquals(
                "Startwert $startwert lässt eine Position aus",
                Nachbarposition.entries.toSet(),
                ersteDrei.map { it.vorgegeben }.toSet(),
            )
        }
    }

    /** Welche Position zuerst drankommt, bleibt trotz der Schichtung dem Zufall überlassen. */
    @Test
    fun dieSchichtungLegtDieReihenfolgeDerPositionenNichtFest() {
        val ersten = (1..50).map { startwert ->
            Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.SCHWER, zahlenraum20, Random(startwert)).ziehe()
        }

        assertEquals(Nachbarposition.entries.toSet(), ersten.map { it.vorgegeben }.toSet())
    }

    /**
     * Die Ränder je vorgegebener Position – sie folgen alle aus der einen Regel „alle drei Zahlen
     * liegen im Zahlenraum": vorgegebene linke Zahl `0..N-2`, mittlere `1..N-1`, rechte `2..N`.
     */
    @Test
    fun dieRaenderStimmenJeVorgegebenerPositionInBeidenZahlenraeumen() {
        zahlenraeume.forEach { gewaehlt ->
            val nachPosition = moeglicheAufgaben(NachbarzahlenSchwierigkeitsstufe.SCHWER, gewaehlt)
                .groupBy({ it.vorgegeben }) { it.zahlAn(it.vorgegeben) }

            assertEquals(0..(gewaehlt - 2), nachPosition.getValue(Nachbarposition.LINKS).spanne())
            assertEquals(1..(gewaehlt - 1), nachPosition.getValue(Nachbarposition.MITTE).spanne())
            assertEquals(2..gewaehlt, nachPosition.getValue(Nachbarposition.RECHTS).spanne())
        }
    }

    /**
     * Die beiden Reihen, um derentwillen der Zahlenraum 100 überhaupt zur Wahl steht: der
     * Zehnerübergang `39 | 40 | 41` und der obere Rand `98 | 99 | 100`. Die 100 selbst kann nicht
     * die Mitte sein – die 101 läge außerhalb.
     */
    @Test
    fun derZahlenraum100BringtZehneruebergangUndOberenRand() {
        val leicht = moeglicheAufgaben(NachbarzahlenSchwierigkeitsstufe.LEICHT, 100)

        assertTrue("39 | 40 | 41 fehlt", Nachbarzahlenaufgabe(40, Nachbarposition.MITTE) in leicht)
        assertTrue("98 | 99 | 100 fehlt", Nachbarzahlenaufgabe(99, Nachbarposition.MITTE) in leicht)
        assertTrue("Die 100 darf nicht die Mitte sein", Nachbarzahlenaufgabe(100, Nachbarposition.MITTE) !in leicht)
    }

    /** Im großen Zahlenraum sind es die Mitten 1..99 – auch hier ohne Auslassung und ohne Vorzug. */
    @Test
    fun einVollerVorratImZahlenraum100EnthaeltJedeReiheGenauEinmal() {
        val vorrat = Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.LEICHT, 100, Random(1))

        val gezogen = List(99) { vorrat.ziehe() }

        assertEquals((1..99).toList(), gezogen.map { it.mitte }.sorted())
    }

    /**
     * Dieselbe Reihe von links und von rechts sind zwei **verschiedene** Aufgaben: Die Ziehung ohne
     * Zurücklegen geht über die Kombination aus Reihe und vorgegebener Position.
     */
    @Test
    fun einVollerVorratDerSchwerenStufeEnthaeltJedeReiheEinmalJePosition() {
        val vorrat = Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.SCHWER, zahlenraum20, Random(1))

        val gezogen = List(vorratsGroesseSchwer) { vorrat.ziehe() }

        assertEquals(vorratsGroesseSchwer, gezogen.toSet().size)
        assertEquals(vorratsGroesse, gezogen.count { it.vorgegeben == Nachbarposition.LINKS })
    }

    @Test
    fun genauEinePositionIstVorgegeben_dieBeidenAnderenSindLuecken() {
        moeglicheAufgaben(NachbarzahlenSchwierigkeitsstufe.LEICHT, zahlenraum20).forEach { aufgabe ->
            assertEquals(2, aufgabe.luecken.size)
            assertTrue(aufgabe.vorgegeben !in aufgabe.luecken)
        }
    }

    /**
     * Deckt drei Zusagen des Tickets in einem Zug ab: Innerhalb einer Runde wiederholt sich keine
     * Reihe, solange der Vorrat reicht; es gibt **keine** 0/1-Reduktion (die Mitte 1, also die
     * Reihe `0 1 2`, ist dabei); und Zehnerübergänge werden **nicht** bevorzugt – über einen
     * vollen Vorrat kommt jede Reihe genau gleich oft vor.
     */
    @Test
    fun einVollerVorratEnthaeltJedeReiheGenauEinmal() {
        val vorrat = Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.LEICHT, zahlenraum20, Random(1))

        val gezogen = List(vorratsGroesse) { vorrat.ziehe() }

        assertEquals((1..vorratsGroesse).toList(), gezogen.map { it.mitte }.sorted())
    }

    @Test
    fun istDerVorratLeer_beginntErVonVorn() {
        val vorrat = Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.LEICHT, zahlenraum20, Random(2))
        val ersteRunde = List(vorratsGroesse) { vorrat.ziehe() }

        val zweiteRunde = List(vorratsGroesse) { vorrat.ziehe() }

        assertEquals(ersteRunde.toSet(), zweiteRunde.toSet())
    }

    @Test
    fun zweiStartwerteZiehenNichtDieselbeErsteReihe() {
        val ersteMitEins = Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.LEICHT, zahlenraum20, Random(1)).ziehe()
        val ersteMitZwei = Nachbarzahlenvorrat(NachbarzahlenSchwierigkeitsstufe.LEICHT, zahlenraum20, Random(2)).ziehe()

        assertTrue("Der Zufallsgenerator sollte die Reihenfolge bestimmen", ersteMitEins != ersteMitZwei)
    }

    /** Die Form, in der die Reihe in der Historie steht: die Lücken als Platzhalter, in Leserichtung. */
    @Test
    fun derReihenTextZeigtDieLueckenAlsPlatzhalter() {
        val aufgabe = Nachbarzahlenaufgabe(mitte = 7, vorgegeben = Nachbarposition.MITTE)

        assertEquals("_ 7 _", aufgabe.reihenText())
    }

    @Test
    fun derLoesungsTextNenntBeideGesuchtenZahlenInLeserichtung() {
        val aufgabe = Nachbarzahlenaufgabe(mitte = 7, vorgegeben = Nachbarposition.MITTE)

        assertEquals("6, 8", aufgabe.loesungsText())
    }

    /** Die Form der schweren Stufe im Übungsstand: Die vorgegebene Zahl steht dort, wo sie stand. */
    @Test
    fun beiVorgegebenerAeussererZahlStehtDiePositionInReiheUndLoesung() {
        val vonLinks = Nachbarzahlenaufgabe(mitte = 10, vorgegeben = Nachbarposition.LINKS)
        val vonRechts = Nachbarzahlenaufgabe(mitte = 19, vorgegeben = Nachbarposition.RECHTS)

        assertEquals("9 _ _", vonLinks.reihenText())
        assertEquals("10, 11", vonLinks.loesungsText())
        assertEquals("_ _ 20", vonRechts.reihenText())
        assertEquals("18, 19", vonRechts.loesungsText())
    }

    /**
     * Die Fortschrittskennung ist **persistiert** und darf sich nie ändern
     * (`docs/adr/0005-persistierte-namen-entkoppelt.md`).
     */
    @Test
    fun dieKennungDerLeichtenStufeIstEingefroren() {
        assertEquals("NACHBARZAHLEN_LEICHT", NachbarzahlenSchwierigkeitsstufe.LEICHT.kennung)
    }

    @Test
    fun dieKennungDerSchwerenStufeIstEingefroren() {
        assertEquals("NACHBARZAHLEN_SCHWER", NachbarzahlenSchwierigkeitsstufe.SCHWER.kennung)
    }
}

/** Die Spanne der vorgegebenen Zahlen einer Position – kürzer als zwei Zusicherungen je Rand. */
private fun List<Int>.spanne(): IntRange = min()..max()
