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
 * Prüft die echte Migration 2 → 3 direkt auf einer manuell aufgebauten v2-Datenbank (ohne
 * Room-Öffnung, daher ohne exportiertes Schema). Kernaussage: bestehende [TaskHistory]
 * bleibt erhalten und für jeden vorhandenen `userName` entsteht ein Profil – der Fortschritt
 * geht beim Update also nicht verloren.
 */
@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    private fun openV2Database(context: Context): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE task_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "userName TEXT NOT NULL, taskCategory TEXT NOT NULL, " +
                        "numberRange INTEGER NOT NULL, questionText TEXT NOT NULL, " +
                        "expectedAnswer TEXT NOT NULL, userAnswer TEXT NOT NULL, " +
                        "isCorrect INTEGER NOT NULL, timestamp INTEGER NOT NULL)"
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

    private fun insertHistory(db: SupportSQLiteDatabase, user: String) {
        db.execSQL(
            "INSERT INTO task_history " +
                "(userName, taskCategory, numberRange, questionText, expectedAnswer, userAnswer, isCorrect, timestamp) " +
                "VALUES ('$user', 'PLUS', 10, '1 + 1 = _', '2', '2', 1, 111)"
        )
    }

    private fun SupportSQLiteDatabase.queryInt(sql: String): Int =
        query(sql).use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun migrate2To3_seedsProfilesFromHistory_andPreservesHistory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openV2Database(context)
        insertHistory(db, "default")
        insertHistory(db, "Emma")
        insertHistory(db, "Emma")

        AppDatabase.MIGRATION_2_3.migrate(db)

        // Historie bleibt vollständig erhalten.
        assertEquals(3, db.queryInt("SELECT COUNT(*) FROM task_history"))

        // Für jeden distinct userName gibt es genau ein Profil.
        assertEquals(2, db.queryInt("SELECT COUNT(*) FROM user_profiles"))
        val names = mutableListOf<String>()
        db.query("SELECT name FROM user_profiles ORDER BY name").use { c ->
            while (c.moveToNext()) names.add(c.getString(0))
        }
        assertEquals(listOf("Emma", "default"), names)

        // Seed-Avatar wie in der Migration festgelegt.
        db.query("SELECT avatarId FROM user_profiles WHERE name = 'Emma'").use {
            it.moveToFirst()
            assertEquals("avatar_pirat_1", it.getString(0))
        }
        db.close()
    }
}
