package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** DataStore-Instanz für Nutzer-Einstellungen (app-weit ein Singleton pro Context). */
private val Context.userPreferencesDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "user_preferences")

/**
 * Einfacher, manueller Dependency-Container (kein Hilt). Bündelt die app-weiten
 * Abhängigkeiten (Repositories) an einer Stelle und wird in [io.github.glaside.zahlenschatzjagd.ZahlenschatzjagdApp]
 * erzeugt.
 */
interface AppContainer {
    val taskRepository: TaskRepository
    val userPreferencesRepository: UserPreferencesRepository
    val userProfileRepository: UserProfileRepository
    val backupRepository: BackupRepository
    val parentAccessRepository: ParentAccessRepository
    val bildschirmzeitRepository: BildschirmzeitRepository
    val rundenRepository: RundenRepository
    val tonUndVorlesenRepository: TonUndVorlesenRepository
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val taskRepository: TaskRepository by lazy {
        TaskRepository(AppDatabase.getDatabase(context).taskHistoryDao())
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context.userPreferencesDataStore)
    }

    override val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(AppDatabase.getDatabase(context).userProfileDao())
    }

    override val backupRepository: BackupRepository by lazy {
        BackupRepository(AppDatabase.getDatabase(context))
    }

    override val parentAccessRepository: ParentAccessRepository by lazy {
        ParentAccessRepository(context.userPreferencesDataStore)
    }

    override val bildschirmzeitRepository: BildschirmzeitRepository by lazy {
        BildschirmzeitRepository(AppDatabase.getDatabase(context).zeitabschnittDao())
    }

    override val rundenRepository: RundenRepository by lazy {
        RundenRepository(AppDatabase.getDatabase(context).rundenabschlussDao())
    }

    override val tonUndVorlesenRepository: TonUndVorlesenRepository by lazy {
        TonUndVorlesenRepository(context.userPreferencesDataStore)
    }
}
