package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import io.github.glaside.zahlenschatzjagd.EINMALEINS_ZAHLENRAUM
import io.github.glaside.zahlenschatzjagd.EinmaleinsSettings
import io.github.glaside.zahlenschatzjagd.Malaufgabe
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.merkeFuerAktivesProfil
import io.github.glaside.zahlenschatzjagd.einmaleinsKennung
import io.github.glaside.zahlenschatzjagd.newlyReachedTiers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Was beide Einmaleins-Stufen gleich festhalten: jede Aufgabe als eine Zeile der Historie, unter
 * der Kennung **ihrer Reihe** – eine Runde mit der 3er- und der 7er-Reihe füllt also zwei Truhen –,
 * den Rundenabschluss und die in der Runde neu erreichten Abzeichen.
 *
 * Abzeichen race-frei wie im `GameViewModel`: Der Stand zu Rundenbeginn wird einmal geladen, die
 * richtigen Antworten zählen synchron mit; am Ende ergibt beides den neuen Stand, ohne die Datenbank
 * erneut zu befragen.
 *
 * Ohne Repository (reine Logik-Tests) wird nichts festgehalten, und die Runde endet ohne Abzeichen.
 */
internal class Einmaleinsfortschritt(
    private val settings: EinmaleinsSettings,
    private val taskRepository: TaskRepository?,
    private val userPreferencesRepository: UserPreferencesRepository?,
    private val rundenRepository: RundenRepository?,
    private val scope: CoroutineScope,
) {
    private val kennungen = settings.reihen.map { einmaleinsKennung(it, settings.schwierigkeitsstufe) }
    private val stand = mutableMapOf<String, Int>()
    private val richtigeDieserRunde = mutableMapOf<String, Int>()
    private val standJob: Job? = taskRepository?.let { repo ->
        scope.launch {
            val profil = aktivesProfil()
            for (kennung in kennungen) stand[kennung] = repo.summeErtrag(profil, kennung, EINMALEINS_ZAHLENRAUM)
        }
    }

    /** Eine beantwortete Aufgabe: zählen und als eine Zeile in die Historie schreiben. */
    fun halteFest(aufgabe: Malaufgabe, eingabe: String, richtig: Boolean) {
        val kennung = einmaleinsKennung(aufgabe.reihe, settings.schwierigkeitsstufe)
        if (richtig) richtigeDieserRunde[kennung] = (richtigeDieserRunde[kennung] ?: 0) + 1
        val repo = taskRepository ?: return
        scope.launch {
            repo.record(
                TaskHistory(
                    userName = aktivesProfil(),
                    taskCategory = kennung,
                    numberRange = EINMALEINS_ZAHLENRAUM,
                    questionText = aufgabe.text(),
                    expectedAnswer = aufgabe.ergebnis.toString(),
                    userAnswer = eingabe,
                    isCorrect = richtig,
                )
            )
        }
    }

    /**
     * Die Runde ist durchgezogen: den Rundenabschluss setzen und [beendet] mit den neu erreichten
     * Abzeichen aufrufen, sobald der Stand zu Rundenbeginn geladen ist. Wer abbricht, kommt hier nie
     * an (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`).
     */
    fun beende(beendet: (List<UnlockedBadge>) -> Unit) {
        rundenRepository?.let { repo ->
            scope.launch { repo.merkeFuerAktivesProfil(userPreferencesRepository) }
        }
        val job = standJob
        if (job == null) {
            beendet(emptyList())
            return
        }
        scope.launch {
            job.join()
            beendet(neueAbzeichen())
        }
    }

    private fun neueAbzeichen(): List<UnlockedBadge> =
        kennungen.flatMap { kennung ->
            val vorher = stand[kennung] ?: 0
            val dazu = richtigeDieserRunde[kennung] ?: 0
            newlyReachedTiers(vorher, vorher + dazu).map { UnlockedBadge(kennung, EINMALEINS_ZAHLENRAUM, it) }
        }

    private suspend fun aktivesProfil(): String =
        userPreferencesRepository?.activeUser?.first() ?: UserPreferencesRepository.DEFAULT_USER
}

/** Wie viele Ziffern eine Antwort höchstens hat – das größte Ergebnis ist `10 · 10 = 100`. */
internal const val MAX_ZIFFERN: Int = 3
