package io.github.glaside.zahlenschatzjagd.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-Memory-Fake des Profil-DAO für Unit-Tests. Die Kaskade auf `task_history` und
 * `zeitabschnitte` ist hier ein No-Op – sie wird separat im instrumentierten
 * [io.github.glaside.zahlenschatzjagd.data.UserProfileDaoTest] geprüft.
 */
internal class FakeUserProfileDao : UserProfileDao {
    private val store = MutableStateFlow<List<UserProfile>>(emptyList())

    override fun getAllProfiles(): Flow<List<UserProfile>> = store.asStateFlow()
    override suspend fun getAllProfilesOnce(): List<UserProfile> = store.value
    override suspend fun getProfileCount(): Int = store.value.size
    override suspend fun getProfile(name: String): UserProfile? =
        store.value.firstOrNull { it.name == name }

    override suspend fun insert(profile: UserProfile) {
        check(store.value.none { it.name == profile.name }) { "duplicate" }
        store.update { it + profile }
    }

    override suspend fun insertAll(profiles: List<UserProfile>) {
        profiles.forEach { insert(it) }
    }

    override suspend fun updateAvatar(name: String, avatarId: String) {
        store.update { list -> list.map { if (it.name == name) it.copy(avatarId = avatarId) else it } }
    }

    override suspend fun updateFreigabe(
        name: String,
        gesperrteKennungen: String,
        gesperrteZahlenraeume: String,
    ) {
        store.update { list ->
            list.map {
                if (it.name == name) {
                    it.copy(
                        gesperrteKennungen = gesperrteKennungen,
                        gesperrteZahlenraeume = gesperrteZahlenraeume,
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun renameProfileRow(oldName: String, newName: String) {
        store.update { list -> list.map { if (it.name == oldName) it.copy(name = newName) else it } }
    }

    override suspend fun renameHistoryRows(oldName: String, newName: String) = Unit

    override suspend fun renameZeitabschnittRows(oldName: String, newName: String) = Unit

    override suspend fun deleteProfileRow(name: String) {
        store.update { list -> list.filterNot { it.name == name } }
    }

    override suspend fun deleteHistoryRows(name: String) = Unit

    override suspend fun deleteZeitabschnittRows(name: String) = Unit
}
