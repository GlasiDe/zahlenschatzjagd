package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Der **Schutzhalt** gegen eine echte Datenbank auf einem echten Gerät – also genau das, was
 * beim Update auf einem Gerät mit installiertem Release-Build passiert
 * (`docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`).
 *
 * Die Entscheidungen selbst sind ohne Gerät geprüft (`RuecklageTest`, `DatenbankstartTest`).
 * Hier stehen die drei Aussagen, die nur ein Gerät beantworten kann:
 *
 * 1. Room **wirft** bei einem fehlenden Migrationsweg, statt zu löschen – die Zeile
 *    `fallbackToDestructiveMigration` ist wirklich fort.
 * 2. Die Datenbank überlebt diesen Fehlschlag unangetastet.
 * 3. Eine von Room geschriebene Datei trägt ihre Schema-Version genau dort, wo
 *    [leseSchemaVersion] sie sucht. Ohne diese Brücke wäre die reine Logik wertlos.
 */
@RunWith(AndroidJUnit4::class)
class SchutzhaltInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "schutzhalt_test.db"
    private val datei: File get() = context.getDatabasePath(name)
    private val ruecklage: File get() = File(context.cacheDir, "ruecklage_test")

    @Before
    fun aufraeumen() {
        context.deleteDatabase(name)
        ruecklage.deleteRecursively()
    }

    @After
    fun wegraeumen() {
        context.deleteDatabase(name)
        ruecklage.deleteRecursively()
    }

    /**
     * Eine Datenbank in einer Version, für die es **keinen** Weg nach oben gibt. Version 1 ist
     * dafür der ehrlichste Fall: Migrationen gibt es erst ab 2. Auf einem echten Gerät kam eine
     * solche Datenbank nie vor (jede je verteilte APK trug mindestens Version 3) – aber genau
     * dieser Fall trat ein, sobald jemand eine Migration vergisst.
     */
    private fun legeDatenbankOhneWegAn(): Long {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
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
            .name(name)
            .callback(callback)
            .build()
        val db = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
        db.execSQL(
            "INSERT INTO task_history (userName, taskCategory, numberRange, questionText, " +
                "expectedAnswer, userAnswer, isCorrect, timestamp) " +
                "VALUES ('Emma', 'PLUS', 10, '1 + 1 = _', '2', '2', 1, 4711)"
        )
        db.close()
        return 4711L
    }

    private fun oeffneMitAllenWegen() =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .mitAllenMigrationen()
            .build()

    /**
     * Der Kern des Schutzhalts: Statt alle Tabellen zu löschen, geht die App in den
     * Schutzhalt, und der Fortschritt liegt danach unverändert da.
     */
    @Test
    fun ohneMigrationsweg_gehtDieAppInDenSchutzhalt_undDerFortschrittBleibt() {
        val zeitpunkt = legeDatenbankOhneWegAn()

        val start = starteDatenbank(
            datenbank = datei,
            ruecklage = ruecklage,
            erwartet = AppDatabase.SCHEMA_VERSION,
            oeffne = { oeffneMitAllenWegen().openHelper.writableDatabase },
        )

        val schutzhalt = start as? Datenbankstart.Schutzhalt
            ?: error("Room hat die Datenbank geöffnet – ist der destruktive Fallback zurück?")
        assertEquals(Schutzhaltgrund.WEG_FEHLT, schutzhalt.grund)
        assertEquals(1, schutzhalt.gefunden)
        assertEquals(AppDatabase.SCHEMA_VERSION, schutzhalt.erwartet)

        // Und jetzt das Entscheidende: Die Aufgabe von vorhin ist noch da.
        val roh = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build()
        ).writableDatabase
        roh.query("SELECT timestamp FROM task_history WHERE userName = 'Emma'").use { cursor ->
            assertTrue("Die Historie wurde gelöscht – genau das sollte nie wieder passieren", cursor.moveToFirst())
            assertEquals(zeitpunkt, cursor.getLong(0))
        }
        roh.close()
    }

    /** Und die Rücklage steht – angelegt, bevor Room überhaupt an die Datei durfte. */
    @Test
    fun vorDemFehlschlag_wurdeDieRuecklageAngelegt() {
        legeDatenbankOhneWegAn()

        starteDatenbank(datei, ruecklage, AppDatabase.SCHEMA_VERSION) {
            oeffneMitAllenWegen().openHelper.writableDatabase
        }

        val kopie = File(ruecklage, name)
        assertTrue("Ohne Rücklage wäre der Schutzhalt nur die halbe Antwort", kopie.exists())
        assertEquals(1, leseSchemaVersion(kopie))
    }

    /**
     * Die Brücke zwischen der reinen Logik und der Wirklichkeit: Was Room schreibt, muss
     * [leseSchemaVersion] auch finden. Ginge das auseinander, liefe die App in einen Schutzhalt,
     * ohne dass etwas kaputt wäre – oder schlimmer, sie legte nie eine Rücklage an.
     */
    @Test
    fun eineEchteRoomDatenbank_traegtIhreSchemaVersionImDateikopf() = runBlocking {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .mitAllenMigrationen()
            .build()
        db.userProfileDao().insert(UserProfile(name = "Emma", avatarId = "avatar_pirat_1"))
        db.close()

        assertEquals(AppDatabase.SCHEMA_VERSION, leseSchemaVersion(datei))
    }

    /** Beim gewöhnlichen Start ohne Schema-Änderung wird nicht kopiert. */
    @Test
    fun ohneSchemaAenderung_bleibtDerStartUnauffaellig() = runBlocking {
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .mitAllenMigrationen()
            .build()
            .apply {
                userProfileDao().insert(UserProfile(name = "Emma", avatarId = "avatar_pirat_1"))
                close()
            }

        val geoeffnet = mutableListOf<AppDatabase>()
        val start = starteDatenbank(datei, ruecklage, AppDatabase.SCHEMA_VERSION) {
            oeffneMitAllenWegen().also { geoeffnet += it }.openHelper.writableDatabase
        }
        geoeffnet.forEach { it.close() }

        assertEquals(Datenbankstart.Bereit, start)
        assertFalse("Ohne Schema-Änderung gibt es nichts zu kopieren", File(ruecklage, name).exists())
    }
}
