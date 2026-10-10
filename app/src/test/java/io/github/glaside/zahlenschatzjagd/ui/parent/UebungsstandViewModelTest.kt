package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
import io.github.glaside.zahlenschatzjagd.data.FakeTaskHistoryDao
import io.github.glaside.zahlenschatzjagd.data.FakeUserProfileDao
import io.github.glaside.zahlenschatzjagd.data.TaskHistory
import io.github.glaside.zahlenschatzjagd.data.TaskRepository
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
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Der Eintrag „Übungsstand" im Elternbereich: Profilwahl und das Lesen des Fensters aus der
 * Historie.
 *
 * Die Uhr steht fest – sonst hinge das 30-Tage-Fenster an der Uhr des Rechners.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UebungsstandViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @get:Rule
    val tempFolder = TemporaryFolder()

    /** Mittwoch, der 12. August 2026, 12 Uhr. */
    private val jetzt: Long = LocalDateTime.parse("2026-08-12T12:00")
        .atZone(ZoneId.of("Europe/Berlin")).toInstant().toEpochMilli()

    private val tag = 24L * 60 * 60 * 1000

    private val taskDao = FakeTaskHistoryDao()

    private fun aufgaben(
        profil: String,
        kennung: String,
        anzahl: Int,
        richtig: Boolean = true,
        vorTagen: Long = 0,
    ) {
        repeat(anzahl) { index ->
            taskDao.eingefuegt.add(
                TaskHistory(
                    userName = profil,
                    taskCategory = kennung,
                    numberRange = 10,
                    questionText = "1 + 1 = _",
                    expectedAnswer = "2",
                    userAnswer = if (richtig) "2" else "3",
                    isCorrect = richtig,
                    timestamp = jetzt - vorTagen * tag - index * 60_000L,
                )
            )
        }
    }

    /**
     * Ein ViewModel, dessen Zustand schon geladen ist – wie im [UebungskalenderViewModelTest]:
     * Der DataStore antwortet in echter Zeit, also hängt ein Sammler an `uiState` und der Aufruf
     * wartet auf die erste echte Antwort.
     */
    private suspend fun viewModel(
        scope: CoroutineScope,
        profilnamen: List<String> = listOf("Mira"),
        aktivesProfil: String? = null,
    ): UebungsstandViewModel {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "eltern.preferences_pb")
        }
        val prefs = UserPreferencesRepository(dataStore)
        val profiles = UserProfileRepository(FakeUserProfileDao())
        profilnamen.forEachIndexed { index, name ->
            profiles.createProfile(name, "avatar_pirat_${index + 1}")
        }
        aktivesProfil?.let { prefs.setActiveUser(it) }

        val vm = UebungsstandViewModel(
            profileRepository = profiles,
            userPreferencesRepository = prefs,
            taskRepository = TaskRepository(taskDao),
            jetzt = { jetzt },
        )
        scope.launch(testDispatcher) { vm.uiState.collect { } }
        vm.uiState.first { it.geladen }
        return vm
    }

    private fun UebungsstandViewModel.zeile(schluessel: String): Uebungsstandzeile =
        uiState.value.zeilen.flatMap { listOf(it) + it.unterzeilen }
            .first { it.schluessel == schluessel }

    @Test
    fun dieHistorieDesProfilsWirdZumUebungsstand() = runTest(testDispatcher.scheduler) {
        aufgaben("Mira", "PLUS", anzahl = 20)
        aufgaben("Mira", "PLUS", anzahl = 5, richtig = false)
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        assertEquals(25, vm.zeile("PLUS").aufgaben)
        assertEquals(80, vm.zeile("PLUS").trefferquote)
    }

    @Test
    fun dieAufgabenEinesGeschwisterkindesZaehlenNichtMit() = runTest(testDispatcher.scheduler) {
        aufgaben("Ben", "PLUS", anzahl = 20)
        val vm = viewModel(backgroundScope, listOf("Mira", "Ben"), aktivesProfil = "Mira")

        assertEquals(0, vm.zeile("PLUS").aufgaben)
    }

    @Test
    fun profilwechselZeigtDenStandDesAnderenKindes() = runTest(testDispatcher.scheduler) {
        aufgaben("Ben", "PLUS", anzahl = 20)
        val vm = viewModel(backgroundScope, listOf("Mira", "Ben"), aktivesProfil = "Mira")

        vm.waehleProfil("Ben")
        vm.uiState.first { it.gewaehltesProfil == "Ben" && it.zeilen.any { z -> z.geuebt } }

        assertEquals(20, vm.zeile("PLUS").aufgaben)
    }

    @Test
    fun vorbelegtIstDasKindDasGeradeUebt() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, listOf("Mira", "Ben"), aktivesProfil = "Ben")

        assertEquals("Ben", vm.uiState.value.gewaehltesProfil)
    }

    /** Das Fenster wird schon in der Datenbank aufgespannt – sonst wüchse die Abfrage endlos. */
    @Test
    fun wasAelterAls30TageIstWirdGarNichtErstGelesen() = runTest(testDispatcher.scheduler) {
        aufgaben("Mira", "PLUS", anzahl = 20, vorTagen = 31)
        val vm = viewModel(backgroundScope, aktivesProfil = "Mira")

        assertEquals(0, vm.zeile("PLUS").aufgaben)
    }

    @Test
    fun ohneProfilGibtEsNichtsZuZeigen() = runTest(testDispatcher.scheduler) {
        val vm = viewModel(backgroundScope, profilnamen = emptyList())

        assertNull(vm.uiState.value.gewaehltesProfil)
        assertEquals(emptyList<Uebungsstandzeile>(), vm.uiState.value.zeilen)
    }
}
