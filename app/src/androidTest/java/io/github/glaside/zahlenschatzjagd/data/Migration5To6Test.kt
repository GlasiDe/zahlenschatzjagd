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
 * Prüft die echte Migration 5 → 6 auf einer manuell aufgebauten v5-Datenbank (wie
 * [Migration4To5Test], also ohne exportiertes Schema).
 *
 * Zwei Kernaussagen: Die Tabelle der **abgeschlossenen Runden** kommt leer dazu – rückwirkend lässt
 * sich nicht sagen, welche Runde durchgezogen wurde –, und alles Vorhandene übersteht das Update
 * unangetastet (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`,
 * `docs/adr/0004-echte-datenbank-migration-statt-zuruecksetzen.md`).
 */
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    private fun openV5Database(context: Context, name: String? = null): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(5) {
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
                db.execSQL(
                    "CREATE TABLE zeitabschnitte (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "profil TEXT NOT NULL, beginn INTEGER NOT NULL, ende INTEGER NOT NULL)"
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
    fun migrate5To6_legtDieRundenLeerAn_undLaesstAllesAndereUnangetastet() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openV5Database(context)
        db.execSQL(
            "INSERT INTO user_profiles (name, avatarId, createdAt, gesperrteKennungen, " +
                "gesperrteZahlenraeume) VALUES ('Emma', 'avatar_pirat_1', 111, 'MINUS', '100')"
        )
        db.execSQL(
            "INSERT INTO task_history (userName, taskCategory, numberRange, questionText, " +
                "expectedAnswer, userAnswer, isCorrect, timestamp) " +
                "VALUES ('Emma', 'PLUS', 10, '1 + 1 = _', '2', '2', 1, 4711)"
        )
        db.execSQL("INSERT INTO zeitabschnitte (profil, beginn, ende) VALUES ('Emma', 10, 70)")

        AppDatabase.MIGRATION_5_6.migrate(db)

        // Die neue Tabelle existiert und ist leer – gezählt wird ab dem Update.
        db.query("SELECT COUNT(*) FROM rundenabschluesse").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // Sie nimmt ohne Weiteres eine Runde auf (Spalten und Typen stimmen).
        db.execSQL("INSERT INTO rundenabschluesse (profil, zeitpunkt) VALUES ('Emma', 4712)")
        db.query("SELECT id, profil, zeitpunkt FROM rundenabschluesse").use { cursor ->
            cursor.moveToFirst()
            assertTrue("Die id muss automatisch vergeben werden", cursor.getInt(0) > 0)
            assertEquals("Emma", cursor.getString(1))
            assertEquals(4712L, cursor.getLong(2))
        }
        // Profil, Fortschritt und Bildschirmzeit bleiben, wie sie waren.
        db.query("SELECT gesperrteKennungen FROM user_profiles WHERE name = 'Emma'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("MINUS", cursor.getString(0))
        }
        db.query("SELECT timestamp FROM task_history WHERE userName = 'Emma'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(4711L, cursor.getLong(0))
        }
        db.query("SELECT beginn FROM zeitabschnitte WHERE profil = 'Emma'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(10L, cursor.getLong(0))
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
    fun nachDemUpdate_oeffnetRoomDieDatenbank_undDerKalenderKannZaehlen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_5_6_test.db"
        context.deleteDatabase(name)
        try {
            openV5Database(context, name).apply {
                execSQL(
                    "INSERT INTO user_profiles (name, avatarId, createdAt, gesperrteKennungen, " +
                        "gesperrteZahlenraeume) VALUES ('Emma', 'avatar_pirat_1', 111, '', '')"
                )
                close()
            }

            // Alle Migrationen, nicht nur 5 → 6: Die v5-Datenbank muss bis zur **heutigen**
            // Version hochgezogen werden, sonst bleibt sie auf halbem Weg stehen.
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .mitAllenMigrationen()
                // Kein destruktiver Fallback: Er würde die Tabellen neu anlegen und den
                // eigentlichen Fehler – eine unpassende Migration – gerade verdecken.
                .build()
            try {
                db.rundenabschlussDao().insert(Rundenabschluss(profil = "Emma", zeitpunkt = 4712))

                assertEquals(listOf("Emma"), db.userProfileDao().getAllProfilesOnce().map { it.name })
                assertEquals(4712L, db.rundenabschlussDao().getForProfil("Emma").single().zeitpunkt)
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
