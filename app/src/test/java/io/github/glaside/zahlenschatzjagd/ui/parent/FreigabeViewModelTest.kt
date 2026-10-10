package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
import io.github.glaside.zahlenschatzjagd.data.ALLE_KENNUNGEN
import io.github.glaside.zahlenschatzjagd.data.FakeUserProfileDao
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Der Eintrag „Freigabe" im Elternbereich: Profilwahl und das An- und Abwählen. */
@OptIn(ExperimentalCoroutinesApi::class)
class FreigabeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var prefs: UserPreferencesRepository
    private lateinit var profiles: UserProfileRepository

    private fun preferences(scope: CoroutineScope, datei: String): UserPreferencesRepository {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, datei)
        }
        return UserPreferencesRepository(dataStore)
    }

    /**
     * Ein ViewModel mit zwei Profilen, das schon einen Zustand hat.
     *
     * Der DataStore antwortet in **echter** Zeit, nicht in der virtuellen des Test-Schedulers –
     * `advanceUntilIdle` wartet also nicht auf ihn. Deshalb hängt hier ein Sammler an `uiState`
     * (ohne den bliebe es beim Startwert, siehe `SharingStarted.WhileSubscribed`) und der Aufruf
     * wartet auf die erste echte Antwort.
     */
    private suspend fun viewModel(scope: CoroutineScope, aktivesProfil: String? = null): FreigabeViewModel {
        prefs = preferences(scope, "user.preferences_pb")
        profiles = UserProfileRepository(FakeUserProfileDao())
        profiles.createProfile("Mira", "avatar_pirat_1")
        profiles.createProfile("Ben", "avatar_pirat_2")
        aktivesProfil?.let { prefs.setActiveUser(it) }
        val vm = FreigabeViewModel(profiles, prefs)
        scope.launch(testDispatcher) { vm.uiState.collect { } }
        vm.uiState.first { it.gewaehltesProfil != null }
        return vm
    }

    @Test
    fun vorbelegtIstDasKindDasGeradeSpielt() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, aktivesProfil = "Ben")

        assertEquals("Ben", vm.uiState.value.gewaehltesProfil)
        assertEquals(Freigabe.ALLES, vm.uiState.value.freigabe)
    }

    @Test
    fun ohneAktivesProfilStehtDasErsteDa() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope)

        assertEquals("Mira", vm.uiState.value.gewaehltesProfil)
    }

    @Test
    fun einAbgewaehlterZahlenraumWirdGespeichert() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        vm.setzeZahlenraum(100, freigegeben = false)

        assertEquals(setOf(10, 20, 50), profiles.freigabe("Mira").first().zahlenraeume)
    }

    @Test
    fun eineAbgewaehlteKennungWirdGespeichert() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        vm.setzeKennung("MINUS", freigegeben = false)

        assertEquals(ALLE_KENNUNGEN - "MINUS", profiles.freigabe("Mira").first().kennungen)
    }

    @Test
    fun abwaehlenUndWiederAnwaehlenFuehrtZurueck() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        vm.setzeKennung("MAL", freigegeben = false)
        assertEquals(ALLE_KENNUNGEN - "MAL", profiles.freigabe("Mira").first().kennungen)

        vm.setzeKennung("MAL", freigegeben = true)

        assertEquals(Freigabe.ALLES, profiles.freigabe("Mira").first())
    }

    @Test
    fun dieAenderungTrifftNurDasGewaehlteProfil() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        vm.waehleProfil("Ben")
        vm.setzeKennung("PLUS", freigegeben = false)

        assertEquals(ALLE_KENNUNGEN - "PLUS", profiles.freigabe("Ben").first().kennungen)
        assertEquals(Freigabe.ALLES, profiles.freigabe("Mira").first())
    }

    @Test
    fun allesAbzuwaehlenIstErlaubt() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        ALLE_KENNUNGEN.forEach { vm.setzeKennung(it, freigegeben = false) }
        listOf(10, 20, 50, 100).forEach { vm.setzeZahlenraum(it, freigegeben = false) }

        val freigabe = profiles.freigabe("Mira").first()
        assertEquals(emptySet<String>(), freigabe.kennungen)
        assertEquals(emptySet<Int>(), freigabe.zahlenraeume)
    }

    @Test
    fun ohneProfilGibtEsNichtsEinzustellen() = runTest(testDispatcher.scheduler) {
        val leer = UserProfileRepository(FakeUserProfileDao())
        val vm = FreigabeViewModel(leer, preferences(backgroundScope, "leer.preferences_pb"))
        backgroundScope.launch(testDispatcher) { vm.uiState.collect { } }

        // Kein Absturz, kein Schreibversuch – es gibt schlicht niemanden, für den etwas gälte.
        vm.setzeKennung("PLUS", freigegeben = false)

        assertNull(vm.uiState.value.gewaehltesProfil)
        assertEquals(emptyList<Any>(), leer.profiles.first())
    }
}
