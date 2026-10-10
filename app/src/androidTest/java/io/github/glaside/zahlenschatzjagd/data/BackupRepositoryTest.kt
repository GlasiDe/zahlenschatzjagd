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

/**
 * Prüft Sicherung und Wiederherstellung gegen eine echte Room-Datenbank – also genau den Weg,
 * den der Umzug auf ein neues Gerät nimmt.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BackupRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BackupRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun task(user: String, timestamp: Long, correct: Boolean = true) = TaskHistory(
        userName = user,
        taskCategory = "VERLIEBTE_ZAHLEN",
        numberRange = 10,
        questionText = "3 + _ = 10",
        expectedAnswer = "7",
        userAnswer = if (correct) "7" else "6",
        isCorrect = correct,
        timestamp = timestamp,
    )

    private suspend fun fuelleDatenbank() {
        db.userProfileDao().insert(UserProfile("Mira", "avatar_pirat_8", createdAt = 100))
        db.userProfileDao().insert(UserProfile("Chris", "avatar_pirat_5", createdAt = 200))
        db.taskHistoryDao().insertAll(
            listOf(task("Mira", 1), task("Mira", 2, correct = false), task("Chris", 3))
        )
        db.zeitabschnittDao().insertAll(
            listOf(
                Zeitabschnitt(profil = "Mira", beginn = 1_000, ende = 61_000),
                Zeitabschnitt(profil = "Chris", beginn = 2_000, ende = 32_000),
            )
        )
        db.rundenabschlussDao().insertAll(
            listOf(
                Rundenabschluss(profil = "Mira", zeitpunkt = 61_000),
                Rundenabschluss(profil = "Chris", zeitpunkt = 32_000),
            )
        )
    }

    @Test
    fun sicherungEnthaeltProfileUndKompletteHistorie() = runBlocking {
        fuelleDatenbank()

        val backup = repository.createBackup("1.2")

        assertEquals(listOf("Mira", "Chris"), backup.profiles.map { it.name })
        assertEquals(3, backup.history.size)
        assertEquals(2, backup.bildschirmzeit.size)
        assertEquals(2, backup.runden.size)
        assertEquals("1.2", backup.appVersionName)
        assertEquals(BackupFile.CURRENT_FORMAT_VERSION, backup.formatVersion)
    }

    @Test
    fun sicherungLaesstSichAufLeeremGeraetVollstaendigWiederherstellen() = runBlocking {
        fuelleDatenbank()
        val backup = repository.createBackup("1.2")

        // Zweite, leere Datenbank spielt das neue Handy.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val neuesGeraet = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val plan = BackupRepository(neuesGeraet).restore(backup)

            assertEquals(2, plan.newProfiles.size)
            assertEquals(3, plan.newTasks.size)
            assertEquals(2, plan.newZeitabschnitte.size)
            assertEquals(2, plan.newRunden.size)
            assertEquals(
                backup.profiles,
                neuesGeraet.userProfileDao().getAllProfilesOnce().map { it.toBackup() }
            )
            assertEquals(
                backup.history.toSet(),
                neuesGeraet.taskHistoryDao().getAllTasks().map { it.toBackup() }.toSet()
            )
            // Die Bildschirmzeit steht nach dem Gerätewechsel nicht wieder auf null.
            assertEquals(
                backup.bildschirmzeit.toSet(),
                neuesGeraet.zeitabschnittDao().getAll().map { it.toBackup() }.toSet()
            )
            // Und das Punktemuster des Übungskalenders ebenso nicht wieder leer.
            assertEquals(
                backup.runden.toSet(),
                neuesGeraet.rundenabschlussDao().getAll().map { it.toBackup() }.toSet()
            )
            // Die Statistik-Abfragen müssen auf den eingespielten Daten weiterhin stimmen.
            assertEquals(1, neuesGeraet.taskHistoryDao().summeErtrag("Mira", "VERLIEBTE_ZAHLEN", 10))
            assertEquals(1, neuesGeraet.taskHistoryDao().getWrongTasks("Mira", "VERLIEBTE_ZAHLEN").size)
        } finally {
            neuesGeraet.close()
        }
    }

    @Test
    fun zweitesEinspielen_erzeugtKeineDubletten() = runBlocking {
        fuelleDatenbank()
        val backup = repository.createBackup("1.2")

        repository.restore(backup)
        val zweiterPlan = repository.restore(backup)

        assertTrue(zweiterPlan.newProfiles.isEmpty())
        assertTrue(zweiterPlan.newTasks.isEmpty())
        assertTrue(zweiterPlan.newZeitabschnitte.isEmpty())
        assertTrue(zweiterPlan.newRunden.isEmpty())
        assertEquals(3, db.taskHistoryDao().getAllTasks().size)
        assertEquals(2, db.userProfileDao().getProfileCount())
        assertEquals(2, db.zeitabschnittDao().getAll().size)
        assertEquals(2, db.rundenabschlussDao().getAll().size)
    }

    @Test
    fun vorhandenerFortschrittBleibtErhalten_undWirdErgaenzt() = runBlocking {
        // Auf dem Gerät ist schon gespielt worden ...
        db.userProfileDao().insert(UserProfile("Mira", "avatar_pirat_1", createdAt = 100))
        db.taskHistoryDao().insertAll(listOf(task("Mira", 500)))

        // ... und es kommt eine ältere Sicherung mit anderen Aufgaben dazu.
        val plan = repository.restore(
            BackupFile(
                exportedAt = 0,
                profiles = listOf(BackupProfile("Mira", "avatar_pirat_8", createdAt = 1)),
                history = listOf(task("Mira", 1).toBackup(), task("Mira", 2).toBackup()),
            )
        )

        assertEquals(2, plan.newTasks.size)
        assertEquals(1, plan.skippedProfiles)
        assertEquals(3, db.taskHistoryDao().getAllTasks().size)
        // Der aktuelle Avatar des Kindes darf nicht durch den alten ersetzt werden.
        assertEquals("avatar_pirat_1", db.userProfileDao().getProfile("Mira")?.avatarId)
    }

    @Test
    fun eingespielteAufgabenBekommenEigeneIds() = runBlocking {
        db.taskHistoryDao().insertAll(listOf(task("Mira", 500)))

        repository.restore(
            BackupFile(exportedAt = 0, history = listOf(task("Mira", 1).toBackup()))
        )

        val ids = db.taskHistoryDao().getAllTasks().map { it.id }
        assertEquals("IDs müssen eindeutig bleiben", ids.size, ids.toSet().size)
    }

    @Test
    fun leereDatenbank_ergibtLeereSicherung() = runBlocking {
        val backup = repository.createBackup("1.2")

        assertTrue(backup.profiles.isEmpty())
        assertTrue(backup.history.isEmpty())
        assertTrue(backup.bildschirmzeit.isEmpty())
        assertTrue(backup.runden.isEmpty())
    }

    /**
     * Eine Sicherung aus der Zeit vor Bildschirmzeit und Übungskalender – sie darf weder die
     * gemessene Zeit noch die gezählten Runden auf dem Gerät anrühren.
     */
    @Test
    fun alteSicherungOhneZeitachse_laesstZeitUndRundenStehen() = runBlocking {
        db.zeitabschnittDao().insert(Zeitabschnitt(profil = "Mira", beginn = 1_000, ende = 61_000))
        db.rundenabschlussDao().insert(Rundenabschluss(profil = "Mira", zeitpunkt = 61_000))

        val plan = repository.restore(
            BackupFile(
                exportedAt = 0,
                profiles = listOf(BackupProfile("Mira", "avatar_pirat_8", createdAt = 1)),
            )
        )

        assertTrue(plan.newZeitabschnitte.isEmpty())
        assertTrue(plan.newRunden.isEmpty())
        assertEquals(1, db.zeitabschnittDao().getAll().size)
        assertEquals(1, db.rundenabschlussDao().getAll().size)
    }

    /** Die Runden eines Kindes gehören ihm allein – der Kalender fragt genau danach. */
    @Test
    fun rundenWerdenJeProfilAbgefragt() = runBlocking {
        fuelleDatenbank()

        val miras = db.rundenabschlussDao().getForProfil("Mira")

        assertEquals(listOf(61_000L), miras.map { it.zeitpunkt })
        assertTrue(db.rundenabschlussDao().getForProfil("Niemand").isEmpty())
    }
}
