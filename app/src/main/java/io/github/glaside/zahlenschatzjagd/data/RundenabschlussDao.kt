package io.github.glaside.zahlenschatzjagd.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Zugriff auf die abgeschlossenen Runden (siehe [Rundenabschluss]). */
@Dao
interface RundenabschlussDao {

    /** Eine gerade beendete Runde festhalten. */
    @Insert
    suspend fun insert(abschluss: Rundenabschluss)

    /** Mehrere auf einmal – für das Einspielen einer Sicherung. */
    @Insert
    suspend fun insertAll(abschluesse: List<Rundenabschluss>)

    /** Die Runden eines Profils, jüngste zuerst – für den Übungskalender. */
    @Query("SELECT * FROM rundenabschluesse WHERE profil = :profil ORDER BY zeitpunkt DESC")
    suspend fun getForProfil(profil: String): List<Rundenabschluss>

    /** Alles, über alle Profile – für die Sicherung im Elternbereich. */
    @Query("SELECT * FROM rundenabschluesse")
    suspend fun getAll(): List<Rundenabschluss>
}
