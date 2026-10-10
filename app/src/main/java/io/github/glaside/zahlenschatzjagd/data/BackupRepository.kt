package io.github.glaside.zahlenschatzjagd.data

import androidx.room.withTransaction

/**
 * Sicherung erstellen und einspielen – die Sicht, die das UI braucht. Als Schnittstelle
 * formuliert, damit der Elternbereich ohne Datenbank (und damit ohne Gerät) testbar bleibt.
 */
interface BackupStore {
    suspend fun createBackup(appVersionName: String): BackupFile
    suspend fun restore(file: BackupFile): MergePlan
}

/**
 * Room-Umsetzung von [BackupStore].
 *
 * Das Einspielen läuft vollständig in **einer** Transaktion: Entweder sind Profile *und*
 * Fortschritt drin, oder es ändert sich gar nichts – ein Abbruch mittendrin kann die Datenbank
 * also nicht in einen halben Zustand bringen.
 */
class BackupRepository(private val database: AppDatabase) : BackupStore {

    private val profileDao = database.userProfileDao()
    private val taskDao = database.taskHistoryDao()
    private val zeitDao = database.zeitabschnittDao()
    private val rundenDao = database.rundenabschlussDao()

    /** Der komplette aktuelle Stand als Sicherungs-Datenstruktur. */
    override suspend fun createBackup(appVersionName: String): BackupFile = BackupFile(
        exportedAt = System.currentTimeMillis(),
        appVersionName = appVersionName,
        profiles = profileDao.getAllProfilesOnce().map { it.toBackup() },
        history = taskDao.getAllTasks().map { it.toBackup() },
        bildschirmzeit = zeitDao.getAll().map { it.toBackup() },
        runden = rundenDao.getAll().map { it.toBackup() },
    )

    /**
     * Spielt eine Sicherung ein und führt sie mit dem vorhandenen Bestand zusammen. Gibt zurück,
     * was tatsächlich ergänzt bzw. als bereits vorhanden übersprungen wurde.
     */
    override suspend fun restore(file: BackupFile): MergePlan = database.withTransaction {
        val existing = BackupSnapshot(
            profiles = profileDao.getAllProfilesOnce().map { it.toBackup() },
            history = taskDao.getAllTasks().map { it.toBackup() },
            bildschirmzeit = zeitDao.getAll().map { it.toBackup() },
            runden = rundenDao.getAll().map { it.toBackup() },
        )
        val plan = mergeBackup(existing, file)
        if (plan.newProfiles.isNotEmpty()) {
            profileDao.insertAll(plan.newProfiles.map { it.toEntity() })
        }
        if (plan.newTasks.isNotEmpty()) {
            taskDao.insertAll(plan.newTasks.map { it.toEntity() })
        }
        if (plan.newZeitabschnitte.isNotEmpty()) {
            zeitDao.insertAll(plan.newZeitabschnitte.map { it.toEntity() })
        }
        if (plan.newRunden.isNotEmpty()) {
            rundenDao.insertAll(plan.newRunden.map { it.toEntity() })
        }
        plan
    }
}
