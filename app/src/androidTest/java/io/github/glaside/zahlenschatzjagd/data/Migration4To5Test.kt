package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft die echte Migration 4 → 5 auf einer manuell aufgebauten v4-Datenbank (wie
 * [Migration3To4Test], also ohne exportiertes Schema).
 *
 * Zwei Kernaussagen: Die Tabelle für die **Bildschirmzeit** kommt leer dazu – rückwirkende Zahlen
 * gibt es bewusst nicht –, und vorhandener Fortschritt übersteht das Update unangetastet
 * (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`,
 * `docs/adr/0004-echte-datenbank-migration-statt-zuruecksetzen.md`).
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    private fun openV4Database(context: Context, name: String? = null): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(4) {
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
                        "createdAt INTEGER NOT NULL, " +
                        "gesperrteKennungen TEXT NOT NULL DEFAULT '', " +
                        "gesperrteZahlenraeume TEXT NOT NULL DEFAULT '')"
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name) // null = In-Memory
            .callback(callback)
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun migrate4To5_legtDieBildschirmzeitLeerAn_undLaesstFortschrittUnangetastet() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openV4Database(context)
        db.execSQL(
            "INSERT INTO user_profiles (name, avatarId, createdAt, gesperrteKennungen, " +
                "gesperrteZahlenraeume) VALUES ('Emma', 'avatar_pirat_1', 111, 'MINUS', '100')"
        )
        db.execSQL(
            "INSERT INTO task_history (userName, taskCategory, numberRange, questionText, " +
                "expectedAnswer, userAnswer, isCorrect, timestamp) " +
                "VALUES ('Emma', 'PLUS', 10, '1 + 1 = _', '2', '2', 1, 4711)"
        )

        AppDatabase.MIGRATION_4_5.migrate(db)

        // Die neue Tabelle existiert und ist leer – die Messung beginnt mit dem Update.
        db.query("SELECT COUNT(*) FROM zeitabschnitte").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // Sie nimmt ohne Weiteres einen Abschnitt auf (Spalten und Typen stimmen).
        db.execSQL("INSERT INTO zeitabschnitte (profil, beginn, ende) VALUES ('Emma', 10, 70)")
        db.query("SELECT id, profil, beginn, ende FROM zeitabschnitte").use { cursor ->
            cursor.moveToFirst()
            assertTrue("Die id muss automatisch vergeben werden", cursor.getInt(0) > 0)
            assertEquals("Emma", cursor.getString(1))
            assertEquals(10L, cursor.getLong(2))
            assertEquals(70L, cursor.getLong(3))
        }
        // Profil und Fortschritt bleiben, wie sie waren.
        db.query("SELECT gesperrteKennungen FROM user_profiles WHERE name = 'Emma'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("MINUS", cursor.getString(0))
        }
        db.query("SELECT timestamp FROM task_history WHERE userName = 'Emma'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(4711L, cursor.getLong(0))
        }
        db.close()
    }

    /**
     * Derselbe Weg, aber über **Room** und eine echte Datei – also genau das, was beim Update auf
     * einem Gerät mit installierter App passiert. Erst hier prüft Room das Schema gegen die Entity: Wäre in der
     * `CREATE TABLE`-Anweisung der Migration eine Spalte anders geschrieben oder anders getypt,
     * stürzte die App bei jedem Start ab. Der direkte Migrations-Test oben merkt das nicht.
     */
    @Test
    fun nachDemUpdate_oeffnetRoomDieDatenbank_undDieMessungKannLosgehen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_4_5_test.db"
        context.deleteDatabase(name)
        try {
            openV4Database(context, name).apply {
                execSQL(
                    "INSERT INTO user_profiles (name, avatarId, createdAt, gesperrteKennungen, " +
                        "gesperrteZahlenraeume) VALUES ('Emma', 'avatar_pirat_1', 111, '', '')"
                )
                close()
            }

            // Alle Migrationen, nicht nur 4 → 5: Die v4-Datenbank muss bis zur **heutigen**
            // Version hochgezogen werden, sonst bleibt sie auf halbem Weg stehen.
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .mitAllenMigrationen()
                // Kein destruktiver Fallback: Er würde die Tabellen neu anlegen und den
                // eigentlichen Fehler – eine unpassende Migration – gerade verdecken.
                .build()
            try {
                db.zeitabschnittDao().insert(Zeitabschnitt(profil = "Emma", beginn = 10, ende = 70))

                assertEquals(listOf("Emma"), db.userProfileDao().getAllProfilesOnce().map { it.name })
                assertEquals(60L, db.zeitabschnittDao().getForProfil("Emma").single().dauerMillis)
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
