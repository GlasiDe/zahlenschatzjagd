package io.github.glaside.zahlenschatzjagd

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun repository(scope: CoroutineScope): UserPreferencesRepository {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "user.preferences_pb")
        }
        return UserPreferencesRepository(dataStore)
    }

    @Test
    fun activeUser_defaultsToDefaultUser() = runTest {
        val repo = repository(backgroundScope)
        assertEquals(UserPreferencesRepository.DEFAULT_USER, repo.activeUser.first())
    }

    @Test
    fun setActiveUser_isPersistedAndReturned() = runTest {
        val repo = repository(backgroundScope)
        repo.setActiveUser("ella")
        assertEquals("ella", repo.activeUser.first())
    }
}
