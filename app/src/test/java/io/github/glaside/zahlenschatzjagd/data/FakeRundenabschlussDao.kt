package io.github.glaside.zahlenschatzjagd.data

/**
 * In-Memory-Attrappe des [RundenabschlussDao]. Damit sind das Festhalten einer beendeten Runde
 * und der Übungskalender ohne Gerät prüfbar (siehe `docs/agents/testing.md`).
 */
class FakeRundenabschlussDao(vorhanden: List<Rundenabschluss> = emptyList()) : RundenabschlussDao {

    val eingefuegt = vorhanden.toMutableList()

    override suspend fun insert(abschluss: Rundenabschluss) {
        eingefuegt.add(abschluss)
    }

    override suspend fun insertAll(abschluesse: List<Rundenabschluss>) {
        eingefuegt.addAll(abschluesse)
    }

    override suspend fun getForProfil(profil: String): List<Rundenabschluss> =
        eingefuegt.filter { it.profil == profil }.sortedByDescending { it.zeitpunkt }

    override suspend fun getAll(): List<Rundenabschluss> = eingefuegt.toList()
}
