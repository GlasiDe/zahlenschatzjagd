package io.github.glaside.zahlenschatzjagd.data

/**
 * In-Memory-Attrappe des [TaskHistoryDao]. Damit sind das Aufzeichnen einer Aufgabe und der
 * Übungsstand ohne Gerät prüfbar (siehe `docs/agents/testing.md`).
 *
 * Die Abfragen antworten wie die echten – inklusive Reihenfolge: `aufgabenSeit` liefert die
 * jüngste zuerst, worauf sich das Fenster des Übungsstands verlässt.
 */
class FakeTaskHistoryDao(vorhanden: List<TaskHistory> = emptyList()) : TaskHistoryDao {

    val eingefuegt = vorhanden.toMutableList()

    override suspend fun insertTask(task: TaskHistory) {
        eingefuegt.add(task)
    }

    override suspend fun insertAll(tasks: List<TaskHistory>) {
        eingefuegt.addAll(tasks)
    }

    override suspend fun getAllTasksForUser(name: String): List<TaskHistory> =
        eingefuegt.filter { it.userName == name }

    override suspend fun getWrongTasks(name: String, category: String): List<TaskHistory> =
        eingefuegt.filter { it.userName == name && it.taskCategory == category && !it.isCorrect }

    override suspend fun aufgabenSeit(profil: String, seit: Long): List<TaskHistory> =
        eingefuegt
            .filter { it.userName == profil && it.timestamp >= seit }
            .sortedByDescending { it.timestamp }

    override suspend fun summeErtrag(name: String, category: String, numberRange: Int): Int =
        eingefuegt
            .filter { it.userName == name && it.taskCategory == category && it.numberRange == numberRange }
            .sumOf { it.ertrag }

    override suspend fun getAllTasks(): List<TaskHistory> = eingefuegt.toList()
}
