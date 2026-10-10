package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Wir melden die Tabellen an. Die Schema-Versionen tragen Namen, weil sie an mehreren Stellen
// stehen – in `@Database` und an beiden Enden jeder Migration – und eine bloße Zahl dort nicht
// verrät, was in dieser Version dazukam.
private const val VERSION_MIT_PROFILEN = 3
private const val VERSION_MIT_FREIGABE = 4
private const val VERSION_MIT_BILDSCHIRMZEIT = 5
private const val VERSION_MIT_RUNDENABSCHLUESSEN = 6
private const val VERSION_MIT_ERTRAG = 7
private const val VERSION_MIT_HILFE_STUFEN = 8

@Database(
    entities = [
        TaskHistory::class,
        UserProfile::class,
        Zeitabschnitt::class,
        Rundenabschluss::class,
    ],
    version = VERSION_MIT_HILFE_STUFEN,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    // 2. Wir stellen die Bedienpulte (DAOs) zur Verfügung
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun zeitabschnittDao(): ZeitabschnittDao
    abstract fun rundenabschlussDao(): RundenabschlussDao

    // 3. Der "Türsteher" (Singleton-Pattern)
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Echte Migration 2 → 3: legt die Profil-Tabelle an und überführt bereits vorhandene
         * Historie in Profile. Damit bleibt der Lernfortschritt beim Update erhalten (das
         * Kernziel der Benutzerverwaltung), statt verloren zu gehen. Bisheriger Fortschritt lief
         * unter `userName = "default"`; er wird so als Profil „default" sichtbar und ist per
         * Umbenennen zu personalisieren.
         */
        val MIGRATION_2_3 = object : Migration(2, VERSION_MIT_PROFILEN) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_profiles (" +
                        "name TEXT NOT NULL PRIMARY KEY, " +
                        "avatarId TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT OR IGNORE INTO user_profiles (name, avatarId, createdAt) " +
                        "SELECT DISTINCT userName, 'avatar_pirat_1', $now FROM task_history"
                )
            }
        }

        /**
         * Migration 3 → 4: die **Freigabe** je Profil. Beide Spalten sind leer vorbelegt – leer
         * heißt „nichts abgewählt", bestehende Kinder dürfen also unverändert alles üben
         * (`docs/adr/0018-freigabe-als-sperre-gespeichert.md`).
         */
        val MIGRATION_3_4 = object : Migration(VERSION_MIT_PROFILEN, VERSION_MIT_FREIGABE) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN gesperrteKennungen TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN gesperrteZahlenraeume TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration 4 → 5: die Tabelle für die gemessene **Bildschirmzeit**. Sie kommt leer dazu –
         * rückwirkende Zahlen gibt es bewusst nicht, die Messung beginnt mit dem Update
         * (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`). Vorhandene Historie und
         * Profile bleiben unangetastet.
         */
        val MIGRATION_4_5 = object : Migration(VERSION_MIT_FREIGABE, VERSION_MIT_BILDSCHIRMZEIT) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS zeitabschnitte (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "profil TEXT NOT NULL, " +
                        "beginn INTEGER NOT NULL, " +
                        "ende INTEGER NOT NULL)"
                )
            }
        }

        /**
         * Migration 5 → 6: die Tabelle der **abgeschlossenen Runden**, aus der der Übungskalender
         * seine Punkte setzt. Sie kommt leer dazu – für die Zeit davor lässt sich nicht
         * rekonstruieren, welche Runde durchgezogen und welche abgebrochen wurde
         * (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`). Vorhandene Historie, Profile
         * und Bildschirmzeit bleiben unangetastet.
         */
        val MIGRATION_5_6 = object : Migration(VERSION_MIT_BILDSCHIRMZEIT, VERSION_MIT_RUNDENABSCHLUESSEN) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS rundenabschluesse (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "profil TEXT NOT NULL, " +
                        "zeitpunkt INTEGER NOT NULL)"
                )
            }
        }

        /**
         * Migration 6 → 7: der **Ertrag** je Aufgabe, also wie viel sie zu ihrem Abzeichen
         * beiträgt. **Rückwirkend nur nach oben**: Eine bereits gespeicherte ganz richtige
         * Zahlenmauer bekommt den vollen Ertrag 2, eine falsche bleibt bei 0 – ob sie teilweise
         * richtig war, lässt sich nachträglich nicht verlässlich sagen. Alle anderen Aufgaben
         * bekommen 1 bzw. 0, ihre Truhen bleiben also, wie sie waren
         * (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`). Dieselbe Regel gilt für ältere
         * Sicherungsdateien ([ertragOhneAngabe]).
         *
         * Die Kennungen stehen hier wörtlich und nicht aus `ZahlenmauerDifficulty` gelesen: Eine
         * Migration beschreibt den Schritt, wie er zu dieser Version war, und darf nicht
         * mitwandern. Die Kennungen sind ohnehin eingefroren (ADR-0005).
         */
        val MIGRATION_6_7 = object : Migration(VERSION_MIT_RUNDENABSCHLUESSEN, VERSION_MIT_ERTRAG) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE task_history ADD COLUMN ertrag INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE task_history SET ertrag = CASE " +
                        "WHEN isCorrect = 0 THEN 0 " +
                        "WHEN taskCategory IN ('ZAHLENMAUER_LEICHT', 'ZAHLENMAUER_MITTEL', " +
                        "'ZAHLENMAUER_SCHWER', 'ZAHLENMAUER_ULTRA') THEN 2 " +
                        "ELSE 1 END"
                )
            }
        }

        /**
         * Migration 7 → 8: Verliebte Zahlen ist seither zwei Schwierigkeitsstufen, und die alte
         * Kennung `VERLIEBTE_ZAHLEN` meint nur noch „Mit Hilfe". Wer die Übungsform vorher
         * abgewählt hatte, soll sie nicht über die neue Stufe „Ohne Hilfe" zurückbekommen – deren
         * Kennung wäre als neue sonst freigegeben (`docs/adr/0018-freigabe-als-sperre-gespeichert.md`).
         * Sie wird deshalb überall mitgesperrt, wo die alte gesperrt war
         * (`docs/adr/0028-verliebte-zahlen-mit-und-ohne-hilfe-getrennt.md`). Am Schema ändert sich
         * nichts; die Version steigt nur, damit der Schritt genau einmal läuft.
         *
         * Gesucht wird mit `instr` auf der von Kommas umschlossenen Liste statt mit `LIKE`, weil
         * der Unterstrich dort ein Platzhalter wäre. Leerzeichen um die Kommas gibt es nicht –
         * `encodeSperre` schreibt keine. Die Kennungen stehen wörtlich da, wie in
         * [MIGRATION_6_7].
         */
        val MIGRATION_7_8 = object : Migration(VERSION_MIT_ERTRAG, VERSION_MIT_HILFE_STUFEN) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE user_profiles " +
                        "SET gesperrteKennungen = gesperrteKennungen || ',VERLIEBTE_ZAHLEN_OHNE_HILFE' " +
                        "WHERE instr(',' || gesperrteKennungen || ',', ',VERLIEBTE_ZAHLEN,') > 0 " +
                        "AND instr(',' || gesperrteKennungen || ',', ',VERLIEBTE_ZAHLEN_OHNE_HILFE,') = 0"
                )
            }
        }

        /** Die Schema-Version, die diese App erwartet – gelesen auch von [starteDatenbank]. */
        const val SCHEMA_VERSION = VERSION_MIT_HILFE_STUFEN

        /** So heißt die Datei auf dem Handy. */
        const val DATENBANK_NAME = "schatztruhe_db"

        fun getDatabase(context: Context): AppDatabase {
            // Wenn der Tresor schon offen ist, gib ihn zurück.
            // Wenn nicht, baue ihn exakt EINMAL auf.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATENBANK_NAME
                )
                    // Jede bekannte Version hat einen echten Weg nach oben. **Kein destruktiver
                    // Fallback**: Fehlt ein Weg, wirft Room – und die App geht in den
                    // Schutzhalt, statt den Fortschritt wegzuwerfen. Siehe
                    // `docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`.
                    .mitAllenMigrationen()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Trägt **alle** Migrationen ein, in der Reihenfolge ihrer Versionen.
 *
 * An einer Stelle, weil zwei Aufrufer dieselbe Liste brauchen: das Öffnen der App-Datenbank und
 * die Migrations-Tests, die eine alte Datenbank bis zur heutigen Version hochziehen. Trüge ein
 * Test nur „seine" Migration ein, bliebe er auf halbem Weg stehen und würde bei der nächsten
 * Schema-Version rot – obwohl an ihm nichts falsch ist. Wer eine Migration ergänzt, ergänzt sie
 * hier, und alle Wege gehen mit.
 */
fun RoomDatabase.Builder<AppDatabase>.mitAllenMigrationen(): RoomDatabase.Builder<AppDatabase> =
    addMigrations(
        AppDatabase.MIGRATION_2_3,
        AppDatabase.MIGRATION_3_4,
        AppDatabase.MIGRATION_4_5,
        AppDatabase.MIGRATION_5_6,
        AppDatabase.MIGRATION_6_7,
        AppDatabase.MIGRATION_7_8,
    )
