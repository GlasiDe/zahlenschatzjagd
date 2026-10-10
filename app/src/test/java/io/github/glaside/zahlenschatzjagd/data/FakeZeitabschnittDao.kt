package io.github.glaside.zahlenschatzjagd.data

/**
 * In-Memory-Attrappe des [ZeitabschnittDao]. Damit ist die Auswertung der Bildschirmzeit ohne
 * Gerät prüfbar (siehe `docs/agents/testing.md`).
 */
class FakeZeitabschnittDao(vorhanden: List<Zeitabschnitt> = emptyList()) : ZeitabschnittDao {

    val eingefuegt = vorhanden.toMutableList()

    override suspend fun insert(abschnitt: Zeitabschnitt) {
        eingefuegt.add(abschnitt)
    }

    override suspend fun insertAll(abschnitte: List<Zeitabschnitt>) {
        eingefuegt.addAll(abschnitte)
    }

    override suspend fun getForProfil(profil: String): List<Zeitabschnitt> =
        eingefuegt.filter { it.profil == profil }.sortedByDescending { it.beginn }

    override suspend fun getAll(): List<Zeitabschnitt> = eingefuegt.toList()
}
