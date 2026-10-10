package io.github.glaside.zahlenschatzjagd.data

import kotlinx.coroutines.flow.first

/**
 * Schreibt und liest die abgeschlossenen Runden (siehe [Rundenabschluss]).
 *
 * Anders als beim [BildschirmzeitRepository] darf das Schreiben hier `suspend` sein: Eine Runde
 * endet mitten in der laufenden App – die Auswertung mit ihren Sternen steht danach noch auf dem
 * Bildschirm –, nicht in dem Moment, in dem die Oberfläche verschwindet.
 */
class RundenRepository(private val dao: RundenabschlussDao) {

    /** Hält eine gerade beendete Runde fest. */
    suspend fun merke(profil: String, zeitpunkt: Long = System.currentTimeMillis()) =
        dao.insert(Rundenabschluss(profil = profil, zeitpunkt = zeitpunkt))

    /** Die Runden eines Profils, jüngste zuerst. */
    suspend fun fuerProfil(profil: String): List<Rundenabschluss> = dao.getForProfil(profil)
}

/**
 * Hält eine beendete Runde für das gerade aktive Profil fest.
 *
 * Als Erweiterung und nicht als Methode: Genau diesen Handgriff brauchen beide Spiel-ViewModels am
 * Ende ihrer Runde, und keines von beiden soll dafür eine eigene Funktion tragen. Ohne
 * [prefs] – in reinen Logik-Tests – zählt die Runde wie die Historie auf
 * [UserPreferencesRepository.DEFAULT_USER].
 */
suspend fun RundenRepository.merkeFuerAktivesProfil(prefs: UserPreferencesRepository?) {
    merke(prefs?.activeUser?.first() ?: UserPreferencesRepository.DEFAULT_USER)
}
