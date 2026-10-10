package io.github.glaside.zahlenschatzjagd

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.glaside.zahlenschatzjagd.data.FakeUserProfileDao
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.UserProfileRepository
import io.github.glaside.zahlenschatzjagd.ui.profile.ProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    // Ein gemeinsamer Test-Dispatcher/-Scheduler für Main (viewModelScope), runTest und den
    // DataStore-Scope, damit die in viewModelScope gestarteten Coroutinen samt
    // DataStore-Schreibvorgängen im Test deterministisch durchlaufen.
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var prefs: UserPreferencesRepository
    private lateinit var profiles: UserProfileRepository

    /**
     * Wartet auf die **echte** Antwort des DataStore, statt sie von virtueller Zeit zu erwarten:
     * `advanceUntilIdle` treibt nur den Test-Scheduler voran, der DataStore arbeitet daneben in
     * echter Zeit (`docs/agents/testing.md`). Alles, was hinter einem Schreibvorgang dort hängt –
     * auch der Rest derselben Coroutine –, ist erst danach passiert.
     */
    private suspend fun UserPreferencesRepository.warteAufAktiven(name: String) {
        activeUser.first { it == name }
    }

    private fun viewModel(scope: CoroutineScope): ProfileViewModel {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "user.preferences_pb")
        }
        prefs = UserPreferencesRepository(dataStore)
        profiles = UserProfileRepository(FakeUserProfileDao())
        return ProfileViewModel(profiles, prefs)
    }

    @Test
    fun createProfile_addsProfileAndFirstBecomesActive() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)

        vm.createProfile("Emma", "avatar_pirat_2")
        prefs.warteAufAktiven("Emma")

        val all = profiles.profiles.first()
        assertEquals(listOf("Emma"), all.map { it.name })
        assertEquals("avatar_pirat_2", all.first().avatarId)
        assertNull(vm.error.value)
    }

    @Test
    fun createProfile_blankName_isRejected() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)

        vm.createProfile("   ", "avatar_pirat_1")

        assertTrue(profiles.profiles.first().isEmpty())
        assertNotNull(vm.error.value)
    }

    @Test
    fun createProfile_duplicateName_isRejected() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1")

        vm.createProfile("Emma", "avatar_pirat_3")
        advanceUntilIdle()

        assertEquals(1, profiles.profiles.first().size)
        assertNotNull(vm.error.value)
    }

    @Test
    fun switchProfile_setsActiveUser() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1") // wird aktiv
        vm.createProfile("Max", "avatar_pirat_2")

        vm.switchProfile("Max")

        prefs.warteAufAktiven("Max")
    }

    @Test
    fun renameProfile_updatesNameAndActiveFollows() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1") // aktiv

        vm.renameProfile("Emma", "Lina")
        prefs.warteAufAktiven("Lina")

        assertEquals(listOf("Lina"), profiles.profiles.first().map { it.name })
    }

    @Test
    fun renameProfile_toExistingName_isRejected() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1")
        vm.createProfile("Max", "avatar_pirat_2")

        vm.renameProfile("Max", "Emma")
        advanceUntilIdle()

        assertEquals(setOf("Emma", "Max"), profiles.profiles.first().map { it.name }.toSet())
        assertNotNull(vm.error.value)
    }

    @Test
    fun changeAvatar_updatesAvatarId() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1")

        vm.changeAvatar("Emma", "avatar_pirat_5")
        advanceUntilIdle()

        assertEquals("avatar_pirat_5", profiles.profiles.first().first().avatarId)
    }

    @Test
    fun createProfile_firstFromEmpty_setsFirstProfileCreatedFlag() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)

        vm.createProfile("Emma", "avatar_pirat_1")
        prefs.warteAufAktiven("Emma")

        assertTrue(vm.firstProfileCreated.value)
    }

    @Test
    fun createProfile_secondProfile_doesNotSetFirstProfileCreatedFlag() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1")
        prefs.warteAufAktiven("Emma")
        vm.dismissFirstProfileHint()

        vm.createProfile("Max", "avatar_pirat_2")
        advanceUntilIdle()

        assertFalse(vm.firstProfileCreated.value)
    }

    @Test
    fun dismissFirstProfileHint_resetsFlag() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1")
        prefs.warteAufAktiven("Emma")

        vm.dismissFirstProfileHint()

        assertFalse(vm.firstProfileCreated.value)
    }

    @Test
    fun deleteProfile_active_switchesToRemaining() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)
        vm.createProfile("Emma", "avatar_pirat_1") // aktiv
        vm.createProfile("Max", "avatar_pirat_2")

        vm.deleteProfile("Emma")
        prefs.warteAufAktiven("Max")

        val remaining = profiles.profiles.first()
        assertEquals(listOf("Max"), remaining.map { it.name })
        assertFalse(remaining.any { it.name == "Emma" })
    }
}
