package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskHistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TaskHistoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskHistoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entry(
        user: String,
        category: String,
        correct: Boolean,
        numberRange: Int = 10,
        timestamp: Long = System.currentTimeMillis(),
    ) = TaskHistory(
        userName = user,
        taskCategory = category,
        numberRange = numberRange,
        questionText = "1 + 1 = _",
        expectedAnswer = "2",
        userAnswer = if (correct) "2" else "3",
        isCorrect = correct,
        timestamp = timestamp,
    )

    @Test
    fun insertAndGetAllTasksForUser_returnsOnlyThatUsersTasks() = runBlocking {
        dao.insertTask(entry("ella", "+", true))
        dao.insertTask(entry("ella", "-", false))
        dao.insertTask(entry("max", "+", true))

        val ellas = dao.getAllTasksForUser("ella")

        assertEquals(2, ellas.size)
        assertTrue(ellas.all { it.userName == "ella" })
    }

    @Test
    fun getWrongTasks_returnsOnlyWrongTasksOfCategory() = runBlocking {
        dao.insertTask(entry("ella", "+", true))
        dao.insertTask(entry("ella", "+", false))
        dao.insertTask(entry("ella", "-", false))

        val wrongPlus = dao.getWrongTasks("ella", "+")

        assertEquals(1, wrongPlus.size)
        assertEquals("+", wrongPlus.first().taskCategory)
        assertTrue(wrongPlus.none { it.isCorrect })
    }

    /**
     * Die Grundlage des **Übungsstands**. Auf die Reihenfolge kommt es an: Das Fenster nimmt „die
     * letzten 50 je Fortschrittskennung", und das ist nur eine eindeutige Menge, solange die
     * jüngste Aufgabe zuerst kommt.
     */
    @Test
    fun aufgabenSeit_liefertNurDiesesProfilAbDemZeitpunkt_juengsteZuerst() = runBlocking {
        val jetzt = 1_800_000_000_000L
        val tag = 24L * 60 * 60 * 1000
        dao.insertTask(entry("ella", "PLUS", true, timestamp = jetzt))
        dao.insertTask(entry("ella", "MINUS", true, timestamp = jetzt - 2 * tag))
        dao.insertTask(entry("ella", "MAL", true, timestamp = jetzt - 40 * tag)) // zu alt
        dao.insertTask(entry("max", "PLUS", true, timestamp = jetzt))            // anderes Profil

        val fenster = dao.aufgabenSeit("ella", jetzt - 30 * tag)

        assertEquals(listOf("PLUS", "MINUS"), fenster.map { it.taskCategory })
    }

    @Test
    fun summeErtrag_filtertNachProfilKennungUndZahlenraum() = runBlocking {
        dao.insertTask(entry("ella", "PLUS", true, numberRange = 10))
        dao.insertTask(entry("ella", "PLUS", true, numberRange = 10))
        dao.insertTask(entry("ella", "PLUS", false, numberRange = 10)) // falsch → zählt nicht
        dao.insertTask(entry("ella", "PLUS", true, numberRange = 20))  // anderer Zahlenraum
        dao.insertTask(entry("ella", "MINUS", true, numberRange = 10)) // andere Kategorie
        dao.insertTask(entry("max", "PLUS", true, numberRange = 10))   // anderer Nutzer

        assertEquals(2, dao.summeErtrag("ella", "PLUS", 10))
        assertEquals(1, dao.summeErtrag("ella", "PLUS", 20))
        assertEquals(0, dao.summeErtrag("ella", "GETEILT", 10))
    }

    /** Eine ganz richtige Mauer bringt 2, eine teilweise richtige 1 (ADR-0027). */
    @Test
    fun summeErtrag_addiertDenGestaffeltenErtragDerZahlenmauer() = runBlocking {
        dao.insertTask(entry("ella", "ZAHLENMAUER_MITTEL", true, numberRange = 50).copy(ertrag = 2))
        dao.insertTask(entry("ella", "ZAHLENMAUER_MITTEL", false, numberRange = 50).copy(ertrag = 1))
        dao.insertTask(entry("ella", "ZAHLENMAUER_MITTEL", false, numberRange = 50).copy(ertrag = 0))

        assertEquals(3, dao.summeErtrag("ella", "ZAHLENMAUER_MITTEL", 50))
    }
}
