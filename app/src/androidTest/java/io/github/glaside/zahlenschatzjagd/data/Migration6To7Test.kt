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
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft die echte Migration 6 → 7 auf einer manuell aufgebauten v6-Datenbank (wie
 * [Migration5To6Test], also ohne exportiertes Schema).
 *
 * Kernaussage: Jede vorhandene Aufgabe bekommt ihren **Ertrag**, und zwar **rückwirkend nur nach
 * oben** – eine ganz richtige Zahlenmauer 2, eine falsche 0, jede andere Aufgabe 1 bzw. 0
 * (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`). Die Erwartungen stehen hier als Zahlen,
 * nicht als Aufruf der Regel aus dem Code – sonst prüfte der Test die Regel gegen sich selbst.
 */
@RunWith(AndroidJUnit4::class)
class Migration6To7Test {

    private fun openV6Database(context: Context, name: String? = null): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(6) {
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

    /** Eine Aufgabe der Fortschrittskennung [kennung] im Zahlenraum [zahlenraum]. */
    private fun SupportSQLiteDatabase.aufgabe(kennung: String, zahlenraum: Int, richtig: Boolean, timestamp: Long) {
        execSQL(
            "INSERT INTO task_history (userName, taskCategory, numberRange, questionText, " +
                "expectedAnswer, userAnswer, isCorrect, timestamp) " +
                "VALUES ('Emma', '$kennung', $zahlenraum, 'Aufgabe', '1', '1', ${if (richtig) 1 else 0}, $timestamp)"
        )
    }

    @Test
    fun migrate6To7_gibtJederAufgabeIhrenErtrag_rueckwirkendNurNachOben() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = openV6Database(context)
        db.aufgabe("ZAHLENMAUER_LEICHT", 20, richtig = true, timestamp = 1)
        db.aufgabe("ZAHLENMAUER_MITTEL", 50, richtig = true, timestamp = 2)
        db.aufgabe("ZAHLENMAUER_SCHWER", 100, richtig = true, timestamp = 3)
        db.aufgabe("ZAHLENMAUER_ULTRA", 100, richtig = true, timestamp = 4)
        db.aufgabe("ZAHLENMAUER_ULTRA", 100, richtig = false, timestamp = 5)
        db.aufgabe("PLUS", 10, richtig = true, timestamp = 6)
        db.aufgabe("PLUS", 10, richtig = false, timestamp = 7)
        db.aufgabe("VERDOPPELN_LEICHT", 10, richtig = true, timestamp = 8)

        AppDatabase.MIGRATION_6_7.migrate(db)

        val ertraege = db.query("SELECT ertrag FROM task_history ORDER BY timestamp").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getInt(0)) }
        }
        assertEquals(listOf(2, 2, 2, 2, 0, 1, 0, 1), ertraege)
        db.close()
    }

    /**
     * Derselbe Weg über **Room** und eine echte Datei – erst hier prüft Room das Schema gegen die
     * Entity (Spaltenname, Typ, Vorgabewert). Und die Truhe rechnet danach mit dem Ertrag: zwei
     * ganz richtige Mauern sind 4, nicht 2.
     */
    @Test
    fun nachDemUpdate_oeffnetRoomDieDatenbank_undDieTruheSummiertDenErtrag() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_6_7_test.db"
        context.deleteDatabase(name)
        try {
            openV6Database(context, name).apply {
                aufgabe("ZAHLENMAUER_LEICHT", 20, richtig = true, timestamp = 1)
                aufgabe("ZAHLENMAUER_LEICHT", 20, richtig = true, timestamp = 2)
                aufgabe("ZAHLENMAUER_LEICHT", 20, richtig = false, timestamp = 3)
                close()
            }

            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .mitAllenMigrationen()
                // Kein destruktiver Fallback: Er würde den eigentlichen Fehler verdecken.
                .build()
            try {
                assertEquals(4, db.taskHistoryDao().summeErtrag("Emma", "ZAHLENMAUER_LEICHT", 20))
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
