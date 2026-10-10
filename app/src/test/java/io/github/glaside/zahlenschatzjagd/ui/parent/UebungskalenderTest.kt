package io.github.glaside.zahlenschatzjagd.ui.parent

import io.github.glaside.zahlenschatzjagd.data.Rundenabschluss
import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Die Rechnung hinter dem Übungskalender: Welcher Tag gilt als geübt, und wie viel
 * Bildschirmzeit fällt auf ihn und auf seine Woche.
 *
 * Feste Zeitzone und fester „heute"-Tag – sonst wäre der Test von der Uhr des Rechners abhängig.
 */
class UebungskalenderTest {

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    /** Montag, der 10. August 2026 – der „heute" aller Tests hier. */
    private val heute: LocalDate = LocalDate.of(2026, 8, 10)

    private fun zeitpunkt(datum: String, uhrzeit: String): Long =
        LocalDateTime.parse("${datum}T$uhrzeit").atZone(zone).toInstant().toEpochMilli()

    private fun runde(datum: String, uhrzeit: String) =
        Rundenabschluss(profil = "Emma", zeitpunkt = zeitpunkt(datum, uhrzeit))

    private fun abschnitt(datum: String, von: String, bis: String) = Zeitabschnitt(
        profil = "Emma",
        beginn = zeitpunkt(datum, von),
        ende = zeitpunkt(datum, bis),
    )

    private fun kalender(
        runden: List<Rundenabschluss> = emptyList(),
        zeitabschnitte: List<Zeitabschnitt> = emptyList(),
        wochen: Int = 6,
    ) = baueUebungskalender(runden, zeitabschnitte, heute, zone, wochen)

    private fun List<Kalenderwoche>.tag(datum: LocalDate): Kalendertag =
        flatMap { it.tage }.single { it.datum == datum }

    // --- Tagesbewertung: geübt oder nicht ------------------------------------------------------

    @Test
    fun tagMitAbgeschlossenerRunde_giltAlsGeuebt() {
        val wochen = kalender(runden = listOf(runde("2026-08-06", "17:30")))

        assertTrue(wochen.tag(LocalDate.of(2026, 8, 6)).geuebt)
    }

    /**
     * Der Kern des Vorhabens: Bildschirmzeit allein färbt den Tag **nicht**. Sonst zählte schon
     * als geübt, wer die App öffnet und einmal danebentippt.
     */
    @Test
    fun tagNurMitBildschirmzeit_giltNichtAlsGeuebt() {
        val wochen = kalender(zeitabschnitte = listOf(abschnitt("2026-08-06", "17:30", "17:45")))

        val tag = wochen.tag(LocalDate.of(2026, 8, 6))
        assertFalse(tag.geuebt)
        assertEquals(15 * 60_000L, tag.bildschirmzeitMillis)
    }

    @Test
    fun mehrereRundenAnEinemTag_werdenGezaehlt() {
        val wochen = kalender(
            runden = listOf(
                runde("2026-08-06", "09:00"),
                runde("2026-08-06", "17:30"),
                runde("2026-08-07", "17:30"),
            )
        )

        assertEquals(2, wochen.tag(LocalDate.of(2026, 8, 6)).runden)
        assertEquals(1, wochen.tag(LocalDate.of(2026, 8, 7)).runden)
    }

    // --- Tagesgrenzen -------------------------------------------------------------------------

    @Test
    fun rundeKurzVorMitternacht_zaehltZumAngefangenenTag() {
        val wochen = kalender(runden = listOf(runde("2026-08-06", "23:59:59")))

        assertTrue(wochen.tag(LocalDate.of(2026, 8, 6)).geuebt)
        assertFalse(wochen.tag(LocalDate.of(2026, 8, 7)).geuebt)
    }

    @Test
    fun rundeKurzNachMitternacht_zaehltZumNeuenTag() {
        val wochen = kalender(runden = listOf(runde("2026-08-07", "00:00")))

        assertFalse(wochen.tag(LocalDate.of(2026, 8, 6)).geuebt)
        assertTrue(wochen.tag(LocalDate.of(2026, 8, 7)).geuebt)
    }

    // --- Summierung je Tag und Woche ------------------------------------------------------------

    @Test
    fun bildschirmzeitEinesTages_wirdAufsummiert() {
        val wochen = kalender(
            zeitabschnitte = listOf(
                abschnitt("2026-08-06", "09:00", "09:20"),
                abschnitt("2026-08-06", "17:00", "17:05"),
            )
        )

        assertEquals(25 * 60_000L, wochen.tag(LocalDate.of(2026, 8, 6)).bildschirmzeitMillis)
    }

    /**
     * Ein Abschnitt über Mitternacht gehört **beiden** Tagen – jedem der Teil, der auf ihn fällt.
     * Sonst schriebe ein Tag die Minuten des anderen auf sein Konto.
     */
    @Test
    fun abschnittUeberMitternacht_wirdAnDerTagesgrenzeGeteilt() {
        val wochen = kalender(
            zeitabschnitte = listOf(
                Zeitabschnitt(
                    profil = "Emma",
                    beginn = zeitpunkt("2026-08-06", "23:50"),
                    ende = zeitpunkt("2026-08-07", "00:10"),
                )
            )
        )

        assertEquals(10 * 60_000L, wochen.tag(LocalDate.of(2026, 8, 6)).bildschirmzeitMillis)
        assertEquals(10 * 60_000L, wochen.tag(LocalDate.of(2026, 8, 7)).bildschirmzeitMillis)
    }

    @Test
    fun woche_summiertDieBildschirmzeitUndZaehltDieGeuebtenTage() {
        val wochen = kalender(
            runden = listOf(runde("2026-08-04", "17:00"), runde("2026-08-06", "17:00")),
            zeitabschnitte = listOf(
                abschnitt("2026-08-04", "17:00", "17:30"),
                abschnitt("2026-08-06", "17:00", "17:15"),
            ),
            wochen = 2,
        )

        // 3.–9. August 2026 ist die vorletzte der beiden Wochen.
        val woche = wochen.single { it.beginn == LocalDate.of(2026, 8, 3) }
        assertEquals(2, woche.geuebteTage)
        assertEquals(45 * 60_000L, woche.bildschirmzeitMillis)
    }

    // --- Zuschnitt des Zeitraums ---------------------------------------------------------------

    @Test
    fun kalender_hatSiebenTageJeWocheUndBeginntMontags() {
        val wochen = kalender(wochen = 6)

        assertEquals(6, wochen.size)
        for (woche in wochen) {
            assertEquals(7, woche.tage.size)
            assertEquals(DayOfWeek.MONDAY, woche.beginn.dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, woche.ende.dayOfWeek)
        }
    }

    @Test
    fun kalender_endetInDerWocheVonHeuteUndIstAufsteigendSortiert() {
        val wochen = kalender(wochen = 3)

        assertEquals(LocalDate.of(2026, 7, 27), wochen.first().beginn)
        assertEquals(LocalDate.of(2026, 8, 10), wochen.last().beginn)
        assertTrue(wochen.last().tage.any { it.datum == heute })
    }

    @Test
    fun tageNachHeute_sindAlsZukunftMarkiert() {
        val wochen = kalender(wochen = 1)

        assertFalse(wochen.tag(heute).zukunft)
        assertTrue(wochen.tag(heute.plusDays(1)).zukunft)
    }

    @Test
    fun datenAusserhalbDesZeitraums_bleibenUnberuecksichtigt() {
        val wochen = kalender(
            runden = listOf(runde("2026-07-01", "17:00")),
            zeitabschnitte = listOf(abschnitt("2026-07-01", "17:00", "18:00")),
            wochen = 2,
        )

        assertTrue(wochen.all { it.geuebteTage == 0 })
        assertEquals(0L, wochen.sumOf { it.bildschirmzeitMillis })
    }

    // --- Beschriftungen -------------------------------------------------------------------------

    @Test
    fun wocheInEinemMonat_nenntDenMonatNurEinmal() {
        val woche = kalender(wochen = 1).single()

        assertEquals("10.–16. Aug.", wochenBeschriftung(woche))
    }

    @Test
    fun wocheUeberDenMonatswechsel_nenntBeideMonate() {
        val woche = kalender(wochen = 3).first()

        assertEquals("27. Juli–2. Aug.", wochenBeschriftung(woche))
    }

    @Test
    fun tag_wirdMitWochentagUndDatumBeschriftet() {
        assertEquals("Mo, 10. Aug.", tagesBeschriftung(heute))
        assertEquals("So, 16. Aug.", tagesBeschriftung(LocalDate.of(2026, 8, 16)))
    }

    @Test
    fun rundenanzahl_bleibtInEinzahlUndMehrzahlDeutsch() {
        assertEquals("1 Runde", rundenAnzahl(1))
        assertEquals("3 Runden", rundenAnzahl(3))
    }

    /**
     * Nur der gefärbte Tag enthält das Wort *geübt* – daran hängt der instrumentierte Test, und
     * nur so lässt sich das Punktemuster überhaupt vorlesen.
     */
    @Test
    fun tagesbeschreibung_sagtInWortenWasDieFarbeZeigt() {
        val wochen = kalender(
            runden = listOf(runde("2026-08-10", "17:30")),
            zeitabschnitte = listOf(abschnitt("2026-08-10", "17:00", "17:12")),
            wochen = 1,
        )

        assertEquals(
            "Mo, 10. Aug.: geübt, 1 Runde, 12 Min",
            tagBeschreibung(wochen.tag(heute)),
        )
        assertEquals(
            "Di, 11. Aug.: noch offen",
            tagBeschreibung(wochen.tag(heute.plusDays(1))),
        )
        assertEquals(
            "So, 9. Aug.: keine abgeschlossene Runde",
            tagBeschreibung(kalender(wochen = 2).tag(LocalDate.of(2026, 8, 9))),
        )
    }

    /**
     * Der Tag, den das Muster allein nicht zeigen kann: Die App war offen, geübt wurde nicht.
     * Seine Minuten müssen trotzdem irgendwo stehen.
     */
    @Test
    fun tagOhneRundeAberMitZeit_nenntSeineDauerTrotzdem() {
        val wochen = kalender(
            zeitabschnitte = listOf(abschnitt("2026-08-04", "17:00", "17:08")),
            wochen = 2,
        )

        assertEquals(
            "Di, 4. Aug.: keine abgeschlossene Runde, 8 Min",
            tagBeschreibung(wochen.tag(LocalDate.of(2026, 8, 4))),
        )
    }

    // --- Darstellung einer Dauer ---------------------------------------------------------------

    @Test
    fun dauer_wirdFuerErwachseneLesbarGeschrieben() {
        assertEquals("–", formatiereDauer(0))
        assertEquals("unter 1 Min", formatiereDauer(30_000))
        assertEquals("12 Min", formatiereDauer(12 * 60_000L))
        assertEquals("1 Std", formatiereDauer(60 * 60_000L))
        assertEquals("2 Std 5 Min", formatiereDauer(125 * 60_000L))
    }
}
