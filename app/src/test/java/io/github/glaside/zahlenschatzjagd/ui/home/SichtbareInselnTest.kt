package io.github.glaside.zahlenschatzjagd.ui.home

import io.github.glaside.zahlenschatzjagd.data.ALLE_KENNUNGEN
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Prüft, was von der Startkarte übrig bleibt: **Nicht Freigegebenes ist unsichtbar**, und was
 * keine erlaubte Option mehr hat, verschwindet eine Ebene höher – erst die Kachel, dann die
 * Insel (`docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`).
 */
class SichtbareInselnTest {

    @Test
    fun ohneEinschraenkungSindAlleInselnDa() {
        assertEquals(HOME_ISLANDS, sichtbareInseln(Freigabe.ALLES))
    }

    @Test
    fun eineInselOhneErlaubteUebungsformVerschwindet() {
        // Nur noch „Verliebte Zahlen" (Zahlenspiele-Insel) – die Rechnen-Insel hat nichts mehr.
        val nurVerliebte = Freigabe(kennungen = setOf("VERLIEBTE_ZAHLEN"), zahlenraeume = setOf(10))

        assertEquals(
            listOf(IslandId.ZAHLENSPIELE, IslandId.ABZEICHEN, IslandId.ELTERN),
            sichtbareInseln(nurVerliebte).map { it.id },
        )
    }

    @Test
    fun einGesperrterZahlenraumKannEineInselLeeren() {
        // Alle Kennungen freigegeben, aber nur die 100: Die Zahlenspiele-Insel führt dann
        // nur noch zu „Zahlen vergleichen" – „Verliebte Zahlen" gibt es nur bis 10.
        val nurHundert = Freigabe(kennungen = ALLE_KENNUNGEN, zahlenraeume = setOf(100))

        assertEquals(
            listOf(IslandId.RECHNEN, IslandId.ZAHLENSPIELE, IslandId.ABZEICHEN, IslandId.ELTERN),
            sichtbareInseln(nurHundert).map { it.id },
        )
    }

    @Test
    fun ohneJedeFreigabeBleibenNurAbzeichenUndFuerEltern() {
        val nichts = Freigabe(kennungen = emptySet(), zahlenraeume = emptySet())

        assertEquals(
            listOf(IslandId.ABZEICHEN, IslandId.ELTERN),
            sichtbareInseln(nichts).map { it.id },
        )
    }

    @Test
    fun abzeichenUndElternHaengenAnKeinerKennung() {
        listOf(IslandId.ABZEICHEN, IslandId.ELTERN).forEach { id ->
            assertEquals(
                "Insel $id darf nicht von der Freigabe abhängen",
                emptyList<String>(),
                HOME_ISLANDS.first { it.id == id }.kennungen,
            )
        }
    }
}
