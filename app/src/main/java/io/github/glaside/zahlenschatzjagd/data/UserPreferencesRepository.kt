package io.github.glaside.zahlenschatzjagd.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Speichert nutzerbezogene Einstellungen via DataStore. Aktuell nur der aktive Nutzer.
 * Solange es keine Profil-UI gibt, liefert [activeUser] den [DEFAULT_USER]; die
 * Verdrahtung ist damit aber vollständig für ein späteres Profil-Feature.
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    val activeUser: Flow<String> = dataStore.data.map { prefs ->
        prefs[ACTIVE_USER] ?: DEFAULT_USER
    }

    suspend fun setActiveUser(name: String) {
        dataStore.edit { prefs -> prefs[ACTIVE_USER] = name }
    }

    companion object {
        const val DEFAULT_USER = "default"
        private val ACTIVE_USER = stringPreferencesKey("active_user")
    }
}
