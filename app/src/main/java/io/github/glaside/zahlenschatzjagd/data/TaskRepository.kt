package io.github.glaside.zahlenschatzjagd.data

/**
 * Dünner Wrapper um [TaskHistoryDao]. Kapselt den Datenbankzugriff, damit ViewModels
 * nicht direkt gegen Room arbeiten. Die Abfrage-Methoden werden für kommende Features
 * (Statistik, gezieltes Üben mit falschen Aufgaben) bereits durchgereicht.
 */
class TaskRepository(private val dao: TaskHistoryDao) {

    suspend fun record(task: TaskHistory) = dao.insertTask(task)

    suspend fun getAllTasksForUser(name: String): List<TaskHistory> =
        dao.getAllTasksForUser(name)

    suspend fun getWrongTasks(name: String, category: String): List<TaskHistory> =
        dao.getWrongTasks(name, category)

    /** Die Aufgaben eines Profils ab einem Zeitpunkt – Grundlage des Übungsstands. */
    suspend fun aufgabenSeit(profil: String, seit: Long): List<TaskHistory> =
        dao.aufgabenSeit(profil, seit)

    /** Summierter Ertrag je (Fortschrittskennung × Zahlenraum) – Basis der Abzeichen. */
    suspend fun summeErtrag(name: String, category: String, numberRange: Int): Int =
        dao.summeErtrag(name, category, numberRange)
}
