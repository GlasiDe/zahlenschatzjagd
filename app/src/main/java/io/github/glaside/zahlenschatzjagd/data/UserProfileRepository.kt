package io.github.glaside.zahlenschatzjagd.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Dünner Wrapper um [UserProfileDao]. Kapselt den Datenbankzugriff und die
 * Namens-Normalisierung/-Validierung, damit das ViewModel nicht direkt gegen Room arbeitet.
 *
 * Namen werden immer getrimmt. Ein leerer Name ist ungültig; ein bereits vergebener Name
 * führt beim Anlegen zu einer Room-Ausnahme (PK-Konflikt) – das ViewModel prüft die
 * Verfügbarkeit vorab über [nameExists], um eine freundliche Fehlermeldung zu zeigen.
 */
class UserProfileRepository(private val dao: UserProfileDao) {

    val profiles: Flow<List<UserProfile>> = dao.getAllProfiles()

    suspend fun count(): Int = dao.getProfileCount()

    suspend fun nameExists(name: String): Boolean = dao.getProfile(name.trim()) != null

    suspend fun createProfile(name: String, avatarId: String) {
        dao.insert(UserProfile(name = name.trim(), avatarId = avatarId))
    }

    suspend fun renameProfile(oldName: String, newName: String) {
        dao.rename(oldName, newName.trim())
    }

    suspend fun changeAvatar(name: String, avatarId: String) {
        dao.updateAvatar(name, avatarId)
    }

    /** Was dieses Profil üben darf – siehe [freigabeVon]. */
    fun freigabe(name: String): Flow<Freigabe> = profiles.map { it.freigabeVon(name) }

    suspend fun setFreigabe(name: String, freigabe: Freigabe) {
        dao.updateFreigabe(
            name = name,
            gesperrteKennungen = encodeSperre(freigabe.gesperrteKennungen()),
            gesperrteZahlenraeume = encodeSperre(freigabe.gesperrteZahlenraeume()),
        )
    }

    suspend fun deleteProfile(name: String) {
        dao.delete(name)
    }
}
