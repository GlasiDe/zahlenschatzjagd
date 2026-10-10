package io.github.glaside.zahlenschatzjagd

import android.app.Application
import io.github.glaside.zahlenschatzjagd.data.AppContainer
import io.github.glaside.zahlenschatzjagd.data.AppDataContainer
import io.github.glaside.zahlenschatzjagd.data.AppDatabase
import io.github.glaside.zahlenschatzjagd.data.Datenbankstart
import io.github.glaside.zahlenschatzjagd.data.RUECKLAGE_ORDNER
import io.github.glaside.zahlenschatzjagd.data.starteDatenbank
import java.io.File

/**
 * Application-Klasse als App-scoped Einstiegspunkt für den [AppContainer].
 * Registriert in AndroidManifest.xml via android:name.
 */
class ZahlenschatzjagdApp : Application() {

    lateinit var container: AppContainer
        private set

    /**
     * Wie der Start ausgegangen ist. Wird **hier** entschieden und nicht erst beim ersten Zugriff
     * eines Bildschirms: Die Rücklage muss stehen, bevor irgendetwas die Datenbank öffnet, und
     * ein fehlender Migrationsweg soll nicht irgendwann mitten in einer Runde auffallen.
     *
     * Das kostet einen Augenblick auf dem Haupt-Thread, aber nur dann, wenn sich die Schema-
     * Version geändert hat – also einmal nach einem Update mit Datenbank-Änderung.
     */
    lateinit var datenbankstart: Datenbankstart
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(applicationContext)
        datenbankstart = starteDatenbank(
            datenbank = getDatabasePath(AppDatabase.DATENBANK_NAME),
            ruecklage = File(filesDir, RUECKLAGE_ORDNER),
            erwartet = AppDatabase.SCHEMA_VERSION,
            // Room öffnet die Datei erst bei der ersten Abfrage. `writableDatabase` erzwingt das
            // Öffnen genau jetzt – und damit auch die Migration, um die es geht.
            oeffne = { AppDatabase.getDatabase(this).openHelper.writableDatabase },
        )
    }
}
