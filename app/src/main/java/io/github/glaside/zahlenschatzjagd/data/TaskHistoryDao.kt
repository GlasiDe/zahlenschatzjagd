package io.github.glaside.zahlenschatzjagd.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TaskHistoryDao {

    // 1. Eine neu gerechnete Aufgabe speichern
    @Insert
    suspend fun insertTask(task: TaskHistory)

    // 2. Die gesamte Historie eines Kindes abrufen (für den Statistik-Bildschirm)
    // ORDER BY timestamp DESC bedeutet: Die neuesten Aufgaben zuerst!
    @Query("SELECT * FROM task_history WHERE userName = :name ORDER BY timestamp DESC")
    suspend fun getAllTasksForUser(name: String): List<TaskHistory>

    // 3. Die "schlaue" Abfrage für unser Random-System:
    // Hole alle falschen Aufgaben (isCorrect = 0) eines Kindes in einer bestimmten Kategorie
    @Query("SELECT * FROM task_history WHERE userName = :name AND taskCategory = :category AND isCorrect = 0")
    suspend fun getWrongTasks(name: String, category: String): List<TaskHistory>

    /**
     * Für die Abzeichen: der summierte **Ertrag** eines Kindes in einer Kombination aus
     * Fortschrittskennung und Zahlenraum. Meist ist das die Zahl richtig gelöster Aufgaben; eine
     * Zahlenmauer trägt gestaffelt bei (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
     */
    @Query(
        "SELECT COALESCE(SUM(ertrag), 0) FROM task_history " +
            "WHERE userName = :name AND taskCategory = :category AND numberRange = :numberRange"
    )
    suspend fun summeErtrag(name: String, category: String, numberRange: Int): Int

    /**
     * Für den **Übungsstand** im Elternbereich: die Aufgaben eines Profils ab einem Zeitpunkt,
     * die jüngste zuerst.
     *
     * Nicht `getAllTasksForUser`, obwohl das dieselbe Antwort enthielte: Der Übungsstand schaut
     * höchstens 30 Tage zurück, und die Historie eines fleißigen Kindes wächst unbegrenzt weiter.
     * Was die Datenbank aussieben kann, soll sie aussieben.
     */
    @Query("SELECT * FROM task_history WHERE userName = :profil AND timestamp >= :seit ORDER BY timestamp DESC")
    suspend fun aufgabenSeit(profil: String, seit: Long): List<TaskHistory>

    // 5. Für die Sicherung im Elternbereich: die komplette Historie aller Kinder.
    @Query("SELECT * FROM task_history")
    suspend fun getAllTasks(): List<TaskHistory>

    // 6. Für das Einspielen einer Sicherung: viele Aufgaben auf einmal.
    @Insert
    suspend fun insertAll(tasks: List<TaskHistory>)
}
