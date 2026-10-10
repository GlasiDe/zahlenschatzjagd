package io.github.glaside.zahlenschatzjagd.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Zugriff auf die gemessene **Bildschirmzeit** (siehe [Zeitabschnitt]). */
@Dao
interface ZeitabschnittDao {

    /** Einen fertigen Abschnitt festhalten. */
    @Insert
    suspend fun insert(abschnitt: Zeitabschnitt)

    /** Mehrere auf einmal – für das Einspielen einer Sicherung. */
    @Insert
    suspend fun insertAll(abschnitte: List<Zeitabschnitt>)

    /** Die Bildschirmzeit eines Profils, jüngste zuerst – für den Übungskalender. */
    @Query("SELECT * FROM zeitabschnitte WHERE profil = :profil ORDER BY beginn DESC")
    suspend fun getForProfil(profil: String): List<Zeitabschnitt>

    /** Alles, über alle Profile – für die Sicherung im Elternbereich. */
    @Query("SELECT * FROM zeitabschnitte")
    suspend fun getAll(): List<Zeitabschnitt>
}
