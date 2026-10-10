package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft die echte Migration 3 → 4 auf einer manuell aufgebauten v3-Datenbank (wie
 * [Migration2To3Test], also ohne exportiertes Schema). Kernaussage: Die Freigabe kommt leer
 * dazu – bestehende Kinder dürfen nach dem Update unverändert alles üben, statt vor lauter
 * verschwundenen Inseln zu stehen.
 */
@RunWith(AndroidJUnit4::class)
class Migration3To4Test {

    private fun openV3Database(context: Context): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(3) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE task_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "userName TEXT NOT NULL, taskCategory TEXT NOT NULL, " +
                        "numberRange INTEGER NOT NULL, questionText TEXT NOT NULL, " +
                        "expectedAnswer TEXT NOT NULL, userAnswer TEXT NOT NULL, " +
                        "isCorrect INTEGER NOT NULL, timestamp INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE user_profiles (" +
                        "name TEXT NOT NULL PRIMARY KEY, " +
                        "avatarId TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // In-Memory
            .callback(callback)
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun migrate3To4_legtDieFreigabeLeerAn_undBestehendeKinderDuerfenAlles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openV3Database(context)
        db.execSQL("INSERT INTO user_profiles (name, avatarId, createdAt) VALUES ('Emma', 'avatar_pirat_1', 111)")

        AppDatabase.MIGRATION_3_4.migrate(db)

        db.query(
            "SELECT avatarId, createdAt, gesperrteKennungen, gesperrteZahlenraeume " +
                "FROM user_profiles WHERE name = 'Emma'"
        ).use { cursor ->
            cursor.moveToFirst()
            // Das Profil selbst bleibt unangetastet …
            assertEquals("avatar_pirat_1", cursor.getString(0))
            assertEquals(111, cursor.getInt(1))
            // … und nichts ist abgewählt.
            val profil = UserProfile(
                name = "Emma",
                avatarId = cursor.getString(0),
                createdAt = cursor.getLong(1),
                gesperrteKennungen = cursor.getString(2),
                gesperrteZahlenraeume = cursor.getString(3),
            )
            assertEquals(Freigabe.ALLES, profil.freigabe)
        }
        db.close()
    }
}
