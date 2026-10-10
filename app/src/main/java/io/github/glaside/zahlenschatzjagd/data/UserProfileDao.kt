package io.github.glaside.zahlenschatzjagd.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    /** Alle Profile, älteste zuerst – stabile Reihenfolge für die Profil-Liste. */
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    fun getAllProfiles(): Flow<List<UserProfile>>

    /** Einmaliger Schnappschuss statt [Flow] – für Sicherung und Einspielen im Elternbereich. */
    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC")
    suspend fun getAllProfilesOnce(): List<UserProfile>

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getProfileCount(): Int

    @Query("SELECT * FROM user_profiles WHERE name = :name")
    suspend fun getProfile(name: String): UserProfile?

    /** Anlegen; [OnConflictStrategy.ABORT] erzwingt eindeutige Namen (PK). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: UserProfile)

    /** Mehrere Profile auf einmal anlegen – für das Einspielen einer Sicherung. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(profiles: List<UserProfile>)

    @Query("UPDATE user_profiles SET avatarId = :avatarId WHERE name = :name")
    suspend fun updateAvatar(name: String, avatarId: String)

    /** Schreibt die Freigabe eines Profils – gespeichert wird das Abgewählte, siehe [UserProfile]. */
    @Query(
        "UPDATE user_profiles SET gesperrteKennungen = :gesperrteKennungen, " +
            "gesperrteZahlenraeume = :gesperrteZahlenraeume WHERE name = :name"
    )
    suspend fun updateFreigabe(name: String, gesperrteKennungen: String, gesperrteZahlenraeume: String)

    // --- Interne Bausteine der kaskadierenden Transaktionen (nicht direkt aufrufen) ---

    @Query("UPDATE user_profiles SET name = :newName WHERE name = :oldName")
    suspend fun renameProfileRow(oldName: String, newName: String)

    @Query("UPDATE task_history SET userName = :newName WHERE userName = :oldName")
    suspend fun renameHistoryRows(oldName: String, newName: String)

    @Query("UPDATE zeitabschnitte SET profil = :newName WHERE profil = :oldName")
    suspend fun renameZeitabschnittRows(oldName: String, newName: String)

    @Query("DELETE FROM user_profiles WHERE name = :name")
    suspend fun deleteProfileRow(name: String)

    @Query("DELETE FROM task_history WHERE userName = :name")
    suspend fun deleteHistoryRows(name: String)

    @Query("DELETE FROM zeitabschnitte WHERE profil = :name")
    suspend fun deleteZeitabschnittRows(name: String)

    /**
     * Benennt ein Profil um und zieht die zugehörige [TaskHistory] und die gemessene
     * Bildschirmzeit ([Zeitabschnitt]) mit – alles in einer Transaktion, damit Fortschritt und
     * Profil niemals auseinanderlaufen.
     */
    @Transaction
    suspend fun rename(oldName: String, newName: String) {
        renameProfileRow(oldName, newName)
        renameHistoryRows(oldName, newName)
        renameZeitabschnittRows(oldName, newName)
    }

    /**
     * Löscht ein Profil samt seiner gesamten [TaskHistory] (Fortschritt) und seiner
     * Bildschirmzeit in einer Transaktion.
     */
    @Transaction
    suspend fun delete(name: String) {
        deleteProfileRow(name)
        deleteHistoryRows(name)
        deleteZeitabschnittRows(name)
    }
}
