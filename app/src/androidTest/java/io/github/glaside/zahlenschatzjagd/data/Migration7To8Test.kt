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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft die echte Migration 7 → 8 auf einer manuell aufgebauten v7-Datenbank (wie
 * [Migration6To7Test], also ohne exportiertes Schema).
 *
 * Kernaussage: Wer Verliebte Zahlen vor dem Update abgewählt hatte, findet die Übungsform auch
 * danach nicht wieder – die neue Stufe „Ohne Hilfe" wird mitgesperrt
 * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`). Die Erwartungen stehen als
 * gespeicherte Zeichenketten da, nicht als Aufruf von `encodeSperre` – sonst prüfte der Test die
 * Regel gegen sich selbst.
 */
@RunWith(AndroidJUnit4::class)
class Migration7To8Test {

    private fun openV7Database(context: Context, name: String? = null): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(7) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE task_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "userName TEXT NOT NULL, taskCategory TEXT NOT NULL, " +
                        "numberRange INTEGER NOT NULL, questionText TEXT NOT NULL, " +
                        "expectedAnswer TEXT NOT NULL, userAnswer TEXT NOT NULL, " +
                        "isCorrect INTEGER NOT NULL, timestamp INTEGER NOT NULL, " +
                        "ertrag INTEGER NOT NULL DEFAULT 0)"
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
                db.execSQL(
                    "CREATE TABLE rundenabschluesse (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "profil TEXT NOT NULL, zeitpunkt INTEGER NOT NULL)"
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

    /** Ein Profil mit den gespeicherten Sperren [gesperrt] – so, wie `encodeSperre` sie schreibt. */
    private fun SupportSQLiteDatabase.profil(name: String, gesperrt: String) {
        execSQL(
            "INSERT INTO user_profiles (name, avatarId, createdAt, gesperrteKennungen, gesperrteZahlenraeume) " +
                "VALUES ('$name', 'avatar_pirat_1', 1, '$gesperrt', '20')"
        )
    }

    private fun SupportSQLiteDatabase.sperren(): List<String> =
        query("SELECT gesperrteKennungen FROM user_profiles ORDER BY name").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }

    @Test
    fun migrate7To8_sperrtOhneHilfeNurDortMit_woVerliebteZahlenAbgewaehltWar() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openV7Database(context)
        db.profil("A_allein", "VERLIEBTE_ZAHLEN")
        db.profil("B_mittendrin", "PLUS,VERLIEBTE_ZAHLEN,MAL")
        db.profil("C_nichts", "")
        db.profil("D_anderes", "PLUS,ZAHLENMAUER_ULTRA")
        // Die alte Kennung ist ein Präfix der neuen – sie darf hier nicht als gesperrt gelten.
        db.profil("E_nurOhneHilfe", "PLUS,VERLIEBTE_ZAHLEN_OHNE_HILFE")

        AppDatabase.MIGRATION_7_8.migrate(db)

        assertEquals(
            listOf(
                "VERLIEBTE_ZAHLEN,VERLIEBTE_ZAHLEN_OHNE_HILFE",
                "PLUS,VERLIEBTE_ZAHLEN,MAL,VERLIEBTE_ZAHLEN_OHNE_HILFE",
                "",
                "PLUS,ZAHLENMAUER_ULTRA",
                "PLUS,VERLIEBTE_ZAHLEN_OHNE_HILFE",
            ),
            db.sperren(),
        )
        // Die Zahlenräume bleiben unangetastet.
        db.query("SELECT DISTINCT gesperrteZahlenraeume FROM user_profiles").use { cursor ->
            cursor.moveToFirst()
            assertEquals("20", cursor.getString(0))
        }
        db.close()
    }

    /** Ein zweiter Lauf darf nichts doppelt eintragen – etwa nach einem abgebrochenen Update. */
    @Test
    fun migrate7To8_traegtOhneHilfeNichtDoppeltEin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openV7Database(context)
        db.profil("Emma", "VERLIEBTE_ZAHLEN,VERLIEBTE_ZAHLEN_OHNE_HILFE")

        AppDatabase.MIGRATION_7_8.migrate(db)

        assertEquals(listOf("VERLIEBTE_ZAHLEN,VERLIEBTE_ZAHLEN_OHNE_HILFE"), db.sperren())
        db.close()
    }

    /**
     * Derselbe Weg über **Room** und eine echte Datei. Danach ist Verliebte Zahlen für das Kind
     * ganz weg – keine der beiden Stufen ist freigegeben –, der Rest der Freigabe bleibt.
     */
    @Test
    fun nachDemUpdate_bleibtVerliebteZahlenFuerDasKindAbgewaehlt() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_7_8_test.db"
        context.deleteDatabase(name)
        try {
            openV7Database(context, name).apply {
                profil("Emma", "VERLIEBTE_ZAHLEN")
                close()
            }

            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .mitAllenMigrationen()
                // Kein destruktiver Fallback: Er würde den eigentlichen Fehler verdecken.
                .build()
            try {
                val freigabe = db.userProfileDao().getProfile("Emma")!!.freigabe
                assertFalse(freigabe.erlaubtEineVon(listOf("VERLIEBTE_ZAHLEN", "VERLIEBTE_ZAHLEN_OHNE_HILFE")))
                assertTrue(freigabe.erlaubt("PLUS", 10))
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
