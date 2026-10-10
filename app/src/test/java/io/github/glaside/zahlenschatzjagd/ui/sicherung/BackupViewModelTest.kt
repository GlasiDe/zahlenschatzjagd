package io.github.glaside.zahlenschatzjagd.ui.sicherung

import io.github.glaside.zahlenschatzjagd.MainDispatcherRule
import io.github.glaside.zahlenschatzjagd.data.BackupFile
import io.github.glaside.zahlenschatzjagd.data.BackupProfile
import io.github.glaside.zahlenschatzjagd.data.BackupSerializer
import io.github.glaside.zahlenschatzjagd.data.BackupSnapshot
import io.github.glaside.zahlenschatzjagd.data.BackupStore
import io.github.glaside.zahlenschatzjagd.data.BackupTask
import io.github.glaside.zahlenschatzjagd.data.MergePlan
import io.github.glaside.zahlenschatzjagd.data.mergeBackup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Prüft die Sicherung ohne Datenbank und ohne Dateisystem: Der [FakeBackupStore] hält den
 * Bestand im Speicher, Lesen und Schreiben der Datei werden als einfache Lambdas übergeben.
 * So sind auch die unangenehmen Fälle testbar (kaputte Datei, kein Zugriff).
 */
class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBackupStore(
        var profiles: List<BackupProfile> = emptyList(),
        var history: List<BackupTask> = emptyList(),
    ) : BackupStore {
        override suspend fun createBackup(appVersionName: String) = BackupFile(
            exportedAt = 1,
            appVersionName = appVersionName,
            profiles = profiles,
            history = history,
        )

        override suspend fun restore(file: BackupFile): MergePlan {
            val plan = mergeBackup(BackupSnapshot(profiles, history), file)
            profiles = profiles + plan.newProfiles
            history = history + plan.newTasks
            return plan
        }
    }

    private fun task(timestamp: Long) = BackupTask(
        userName = "Mira",
        taskCategory = "PLUS",
        numberRange = 10,
        questionText = "1 + 1 = _",
        expectedAnswer = "2",
        userAnswer = "2",
        isCorrect = true,
        timestamp = timestamp,
    )

    private val mira = BackupProfile("Mira", "avatar_pirat_8", createdAt = 1)

    private fun viewModel(store: BackupStore) = BackupViewModel(store, appVersionName = "1.2")

    @Test
    fun sicherungErstellen_schreibtJsonUndMeldetInhalt() = runTest {
        val store = FakeBackupStore(listOf(mira), listOf(task(1), task(2)))
        val viewModel = viewModel(store)
        var geschrieben: String? = null

        viewModel.createBackup { geschrieben = it }

        assertTrue(geschrieben!!.contains("Mira"))
        assertEquals(
            "Sicherung gespeichert: 1 Profil, 2 Aufgaben, 0 Runden und 0 Zeitabschnitte.",
            viewModel.message.value?.text,
        )
        assertFalse(viewModel.message.value!!.isError)
        assertFalse(viewModel.busy.value)
    }

    @Test
    fun sicherungEinspielen_ergaenztFehlendeDaten() = runTest {
        val quelle = FakeBackupStore(listOf(mira), listOf(task(1), task(2)))
        val json = BackupSerializer.encode(quelle.createBackup("1.2"))
        val ziel = FakeBackupStore()
        val viewModel = viewModel(ziel)

        viewModel.restoreBackup { json }

        assertEquals("Eingespielt: 1 Profil und 2 Aufgaben ergänzt.", viewModel.message.value?.text)
        assertEquals(listOf("Mira"), ziel.profiles.map { it.name })
        assertEquals(2, ziel.history.size)
    }

    @Test
    fun zweitesEinspielen_aendertNichtsMehr() = runTest {
        val quelle = FakeBackupStore(listOf(mira), listOf(task(1)))
        val json = BackupSerializer.encode(quelle.createBackup("1.2"))
        val ziel = FakeBackupStore()
        val viewModel = viewModel(ziel)

        viewModel.restoreBackup { json }
        viewModel.restoreBackup { json }

        assertEquals(
            "Es war bereits alles vorhanden – nichts wurde verändert.",
            viewModel.message.value?.text
        )
        assertEquals(1, ziel.history.size)
    }

    @Test
    fun teilweiseVorhandeneDaten_werdenAlsErgaenzungGemeldet() = runTest {
        val ziel = FakeBackupStore(listOf(mira), listOf(task(1)))
        val json = BackupSerializer.encode(
            BackupFile(exportedAt = 1, profiles = listOf(mira), history = listOf(task(1), task(2)))
        )
        val viewModel = viewModel(ziel)

        viewModel.restoreBackup { json }

        assertEquals(
            "Eingespielt: 1 Aufgabe ergänzt. 2 Einträge waren schon vorhanden.",
            viewModel.message.value?.text
        )
    }

    /**
     * Lesbar, aber leer: Auf einem frisch installierten Gerät wäre „war bereits alles vorhanden"
     * falsch – hier war schlicht nichts drin.
     */
    @Test
    fun leereSicherung_sagtDassKeinFortschrittDrinStand() = runTest {
        val json = BackupSerializer.encode(BackupFile(exportedAt = 1))
        val viewModel = viewModel(FakeBackupStore())

        viewModel.restoreBackup { json }

        assertEquals(
            "Diese Sicherung enthält keinen Fortschritt – es gab nichts zu ergänzen.",
            viewModel.message.value?.text,
        )
        assertFalse(viewModel.message.value!!.isError)
    }

    @Test
    fun kaputteDatei_meldetVerstaendlichenFehler() = runTest {
        val viewModel = viewModel(FakeBackupStore())

        viewModel.restoreBackup { "das ist ein Urlaubsfoto" }

        assertTrue(viewModel.message.value!!.isError)
        assertTrue(viewModel.message.value!!.text.contains("konnte nicht gelesen werden"))
    }

    @Test
    fun neueresDateiformat_meldetVersionshinweis() = runTest {
        val viewModel = viewModel(FakeBackupStore())

        viewModel.restoreBackup { """{"formatVersion": 99, "exportedAt": 1}""" }

        assertTrue(viewModel.message.value!!.isError)
        assertTrue(viewModel.message.value!!.text.contains("neueren Version"))
    }

    @Test
    fun dateiNichtLesbar_meldetZugriffsfehler() = runTest {
        val viewModel = viewModel(FakeBackupStore())

        viewModel.restoreBackup { throw IOException("kein Zugriff") }

        assertTrue(viewModel.message.value!!.isError)
        assertTrue(viewModel.message.value!!.text.contains("nicht zugegriffen"))
        assertFalse("Nach einem Fehler darf die UI nicht blockiert bleiben", viewModel.busy.value)
    }

    @Test
    fun dateiNichtSchreibbar_meldetZugriffsfehler() = runTest {
        val viewModel = viewModel(FakeBackupStore(listOf(mira)))

        viewModel.createBackup { throw IOException("Speicher voll") }

        assertTrue(viewModel.message.value!!.isError)
        assertFalse(viewModel.busy.value)
    }

    @Test
    fun meldungLaesstSichWegklicken() = runTest {
        val viewModel = viewModel(FakeBackupStore())

        viewModel.createBackup { }
        viewModel.clearMessage()

        assertNull(viewModel.message.value)
    }
}
