package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserProfileDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UserProfileDao
    private lateinit var taskDao: TaskHistoryDao
    private lateinit var zeitDao: ZeitabschnittDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.userProfileDao()
        taskDao = db.taskHistoryDao()
        zeitDao = db.zeitabschnittDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun history(user: String) = TaskHistory(
        userName = user,
        taskCategory = "PLUS",
        numberRange = 10,
        questionText = "1 + 1 = _",
        expectedAnswer = "2",
        userAnswer = "2",
        isCorrect = true,
    )

    @Test
    fun insertAndGetAll_returnsProfilesInCreationOrder() = runBlocking {
        dao.insert(UserProfile("Emma", "avatar_pirat_1", createdAt = 1))
        dao.insert(UserProfile("Max", "avatar_pirat_2", createdAt = 2))

        val all = dao.getAllProfiles().first()

        assertEquals(listOf("Emma", "Max"), all.map { it.name })
        assertEquals(2, dao.getProfileCount())
    }

    @Test
    fun insertDuplicateName_throws() {
        runBlocking { dao.insert(UserProfile("Emma", "avatar_pirat_1")) }
        assertThrows(Exception::class.java) {
            runBlocking { dao.insert(UserProfile("Emma", "avatar_pirat_2")) }
        }
    }

    @Test
    fun rename_movesProfileAndItsHistory() = runBlocking {
        dao.insert(UserProfile("Emma", "avatar_pirat_1"))
        taskDao.insertTask(history("Emma"))
        taskDao.insertTask(history("Emma"))
        zeitDao.insert(Zeitabschnitt(profil = "Emma", beginn = 1, ende = 2))

        dao.rename("Emma", "Lina")

        assertEquals(listOf("Lina"), dao.getAllProfiles().first().map { it.name })
        assertNull(dao.getProfile("Emma"))
        assertEquals(2, taskDao.getAllTasksForUser("Lina").size)
        assertTrue(taskDao.getAllTasksForUser("Emma").isEmpty())
        // Auch die Bildschirmzeit hängt am Profil und muss mitwandern.
        assertEquals(1, zeitDao.getForProfil("Lina").size)
        assertTrue(zeitDao.getForProfil("Emma").isEmpty())
    }

    @Test
    fun delete_removesProfileAndItsHistory() = runBlocking {
        dao.insert(UserProfile("Emma", "avatar_pirat_1"))
        taskDao.insertTask(history("Emma"))
        zeitDao.insert(Zeitabschnitt(profil = "Emma", beginn = 1, ende = 2))

        dao.delete("Emma")

        assertTrue(dao.getAllProfiles().first().isEmpty())
        assertTrue(taskDao.getAllTasksForUser("Emma").isEmpty())
        assertTrue(zeitDao.getForProfil("Emma").isEmpty())
    }

    @Test
    fun rename_undDelete_lassenFremdeBildschirmzeitInRuhe() = runBlocking {
        dao.insert(UserProfile("Emma", "avatar_pirat_1"))
        dao.insert(UserProfile("Max", "avatar_pirat_2"))
        zeitDao.insert(Zeitabschnitt(profil = "Max", beginn = 1, ende = 2))

        dao.rename("Emma", "Lina")
        dao.delete("Lina")

        assertEquals(1, zeitDao.getForProfil("Max").size)
    }

    @Test
    fun updateAvatar_changesOnlyAvatar() = runBlocking {
        dao.insert(UserProfile("Emma", "avatar_pirat_1"))

        dao.updateAvatar("Emma", "avatar_pirat_7")

        assertEquals("avatar_pirat_7", dao.getProfile("Emma")?.avatarId)
    }
}
