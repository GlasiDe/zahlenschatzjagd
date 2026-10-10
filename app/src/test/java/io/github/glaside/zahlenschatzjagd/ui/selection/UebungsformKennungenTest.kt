package io.github.glaside.zahlenschatzjagd.ui.selection

import io.github.glaside.zahlenschatzjagd.categoryForType
import io.github.glaside.zahlenschatzjagd.data.ALLE_KENNUNGEN
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Hält die Zuordnung Übungsform → Fortschrittskennungen vollständig: Jede Kennung aus
 * `BADGE_CATEGORIES` muss über genau eine Übungsform erreichbar sein.
 *
 * Ohne diesen Test wäre eine neue Kennung zwar freigegeben (siehe
 * `docs/adr/0018-freigabe-als-sperre-gespeichert.md`), aber über kein Spiel erreichbar – und im
 * Elternbereich stünde ein Schalter, der nichts bewirkt.
 */
class UebungsformKennungenTest {

    private val alleZugeordneten =
        (RECHNEN_GAMES.flatMap { it.kennungen } + ZAHLENSPIELE_GAMES.flatMap { it.kennungen })

    @Test
    fun jedeKennungGehoertZuGenauEinerUebungsform() {
        assertEquals(
            "Jede Fortschrittskennung braucht genau eine Übungsform",
            ALLE_KENNUNGEN.sorted(),
            alleZugeordneten.sorted(),
        )
    }

    @Test
    fun keineUebungsformIstOhneKennung() {
        (RECHNEN_GAMES.map { it.id.name to it.kennungen } + ZAHLENSPIELE_GAMES.map { it.id.name to it.kennungen })
            .forEach { (name, kennungen) ->
                assertEquals("Übungsform $name ohne Kennung", true, kennungen.isNotEmpty())
            }
    }

    /** Die Kennung einer Rechenart muss die sein, unter der ihre Aufgaben auch gezählt werden. */
    @Test
    fun jedeRechenartTraegtDieKennungIhrerAufgaben() {
        RECHENARTEN.forEach { rechenart ->
            assertEquals(rechenart.kennung, categoryForType(rechenart.type))
        }
    }

    @Test
    fun eineKachelVerschwindetErstWennKeineIhrerKennungenErlaubtIst() {
        val nurVerdoppelnLeicht = Freigabe(kennungen = setOf("VERDOPPELN_LEICHT"), zahlenraeume = setOf(10))

        assertEquals(
            listOf(RechenspielId.VERDOPPELN),
            sichtbareRechenspiele(nurVerdoppelnLeicht).map { it.id },
        )
        assertEquals(emptyList<ZahlenspielId>(), sichtbareZahlenspiele(nurVerdoppelnLeicht).map { it.id })
    }

    @Test
    fun ohneFreigabeBleibtKeineKachelUebrig() {
        val nichts = Freigabe(kennungen = emptySet(), zahlenraeume = emptySet())

        assertEquals(emptyList<Rechenspiel>(), sichtbareRechenspiele(nichts))
        assertEquals(emptyList<Zahlenspiel>(), sichtbareZahlenspiele(nichts))
    }

    @Test
    fun ohneEinschraenkungBleibenAlleKachelnStehen() {
        assertEquals(RECHNEN_GAMES, sichtbareRechenspiele(Freigabe.ALLES))
        assertEquals(ZAHLENSPIELE_GAMES, sichtbareZahlenspiele(Freigabe.ALLES))
    }
}
