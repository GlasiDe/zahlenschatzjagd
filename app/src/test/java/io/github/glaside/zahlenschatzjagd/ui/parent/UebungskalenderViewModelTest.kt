package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
import io.github.glaside.zahlenschatzjagd.data.BildschirmzeitRepository
import io.github.glaside.zahlenschatzjagd.data.FakeRundenabschlussDao
import io.github.glaside.zahlenschatzjagd.data.FakeUserProfileDao
import io.github.glaside.zahlenschatzjagd.data.FakeZeitabschnittDao
import io.github.glaside.zahlenschatzjagd.data.ParentAccessRepository
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.Rundenabschluss
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.UserProfileRepository
import io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Der Eintrag „Übungskalender" im Elternbereich: Profilwahl, das Zusammentragen der beiden Quellen
 * und der Geschwister-Hinweis.
 *
 * Die Uhr steht fest – sonst hinge der Zeitraum des Kalenders an der Uhr des Rechners.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UebungskalenderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    /** Montag, der 10. August 2026, 18 Uhr. */
    private val jetzt: ZonedDateTime = ZonedDateTime.of(2026, 8, 10, 18, 0, 0, 0, zone)

    private val rundenDao = FakeRundenabschlussDao()
    private val zeitDao = FakeZeitabschnittDao()

    private fun zeitpunkt(datum: String, uhrzeit: String): Long =
        LocalDateTime.parse("${datum}T$uhrzeit").atZone(zone).toInstant().toEpochMilli()

    private fun runde(profil: String, datum: String, uhrzeit: String) {
        rundenDao.eingefuegt.add(
            Rundenabschluss(profil = profil, zeitpunkt = zeitpunkt(datum, uhrzeit))
        )
    }

    private fun zeit(profil: String, datum: String, von: String, bis: String) {
        zeitDao.eingefuegt.add(
            Zeitabschnitt(
                profil = profil,
                beginn = zeitpunkt(datum, von),
                ende = zeitpunkt(datum, bis),
            )
        )
    }

    /**
     * Ein ViewModel, dessen Zustand schon geladen ist.
     *
     * Wie beim [FreigabeViewModelTest] antwortet der DataStore in **echter** Zeit: Es hängt ein
     * Sammler an `uiState` (sonst bliebe es beim Startwert, siehe `SharingStarted.WhileSubscribed`)
     * und der Aufruf wartet auf die erste echte Antwort.
     */
    private suspend fun viewModel(
        scope: CoroutineScope,
        profilnamen: List<String> = listOf("Mira"),
        aktivesProfil: String? = null,
        profilwechselOhnePin: Boolean = true,
    ): UebungskalenderViewModel {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "eltern.preferences_pb")
        }
        val prefs = UserPreferencesRepository(dataStore)
        val access = ParentAccessRepository(dataStore)
        val profiles = UserProfileRepository(FakeUserProfileDao())
        profilnamen.forEachIndexed { index, name ->
            profiles.createProfile(name, "avatar_pirat_${index + 1}")
        }
        aktivesProfil?.let { prefs.setActiveUser(it) }
        if (!profilwechselOhnePin) access.setzeProfilwechselOhnePin(false)

        val vm = UebungskalenderViewModel(
            profileRepository = profiles,
            userPreferencesRepository = prefs,
            parentAccessRepository = access,
            rundenRepository = RundenRepository(rundenDao),
            bildschirmzeitRepository = BildschirmzeitRepository(zeitDao, scope),
            jetzt = { jetzt },
        )
        scope.launch(testDispatcher) { vm.uiState.collect { } }
        vm.uiState.first { it.geladen }
        return vm
    }

    private fun UebungskalenderViewModel.tag(datum: LocalDate): Kalendertag =
        uiState.value.wochen.flatMap { it.tage }.single { it.datum == datum }

    private val UebungskalenderUiState.runden: Int
        get() = wochen.sumOf { woche -> woche.tage.sumOf { it.runden } }

    @Test
    fun abgeschlosseneRundenUndZeitLandenAmSelbenTag() = runTest(testDispatcher.scheduler) {
        runde("Mira", "2026-08-06", "17:30")
        zeit("Mira", "2026-08-06", "17:00", "17:35")
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        val tag = vm.tag(LocalDate.of(2026, 8, 6))
        assertTrue(tag.geuebt)
        assertEquals(35 * 60_000L, tag.bildschirmzeitMillis)
        assertEquals(1, vm.uiState.value.runden)
    }

    @Test
    fun dieDatenEinesGeschwisterkindesFaerbenDenEigenenKalenderNicht() =
        runTest(testDispatcher.scheduler) {
            runde("Ben", "2026-08-06", "17:30")
            zeit("Ben", "2026-08-06", "17:00", "17:35")
            val vm = viewModel(backgroundScope, listOf("Mira", "Ben"), aktivesProfil = "Mira")

            assertFalse(vm.tag(LocalDate.of(2026, 8, 6)).geuebt)
            assertEquals(0L, vm.tag(LocalDate.of(2026, 8, 6)).bildschirmzeitMillis)
        }

    @Test
    fun profilwechselZeigtDenKalenderDesAnderenKindes() = runTest(testDispatcher.scheduler) {
        runde("Ben", "2026-08-06", "17:30")
        val vm = viewModel(backgroundScope, listOf("Mira", "Ben"), aktivesProfil = "Mira")

        vm.waehleProfil("Ben")
        vm.uiState.first { it.gewaehltesProfil == "Ben" && it.runden > 0 }

        assertTrue(vm.tag(LocalDate.of(2026, 8, 6)).geuebt)
    }

    @Test
    fun vorbelegtIstDasKindDasGeradeSpielt() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, listOf("Mira", "Ben"), aktivesProfil = "Ben")

        assertEquals("Ben", vm.uiState.value.gewaehltesProfil)
    }

    /**
     * Der Hinweis aus `docs/adr/0019-profilverwaltung-hinter-dem-tor.md` – aber nur, wo es
     * überhaupt etwas zu vermischen gibt.
     */
    @Test
    fun geschwisterHinweisErscheintNurBeiMehrerenKindern() = runTest(testDispatcher.scheduler) {
        val allein = viewModel(backgroundScope, listOf("Mira"))
        assertFalse(allein.uiState.value.geschwisterHinweis)
    }

    @Test
    fun geschwisterHinweisErscheintSolangeDerWechselOhnePinErlaubtIst() =
        runTest(testDispatcher.scheduler) {
            val vm = viewModel(backgroundScope, listOf("Mira", "Ben"))

            assertTrue(vm.uiState.value.geschwisterHinweis)
        }

    @Test
    fun hinterDerPinBrauchtEsDenHinweisNichtMehr() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, listOf("Mira", "Ben"), profilwechselOhnePin = false)

        assertFalse(vm.uiState.value.geschwisterHinweis)
    }

    @Test
    fun ohneProfilGibtEsNichtsZuZeigen() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, profilnamen = emptyList())

        assertNull(vm.uiState.value.gewaehltesProfil)
        assertEquals(emptyList<Kalenderwoche>(), vm.uiState.value.wochen)
    }
}
