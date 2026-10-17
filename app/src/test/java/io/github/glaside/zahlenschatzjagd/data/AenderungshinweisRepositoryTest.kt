package io.github.glaside.zahlenschatzjagd.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Prüft, wie sich das Gerät merkt, bis zu welcher Version die Änderungshinweise gezeigt sind. */
@OptIn(ExperimentalCoroutinesApi::class)
class AenderungshinweisRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun dataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "hinweis.preferences_pb")
        }

    @Test
    fun ohneEintragIstNichtsGemerkt() = runTest {
        val repo = AenderungshinweisRepository(dataStore(backgroundScope))
        assertNull(repo.zuletztGezeigt.first())
    }

    @Test
    fun derGezeigteStandWirdGemerkt() = runTest {
        val repo = AenderungshinweisRepository(dataStore(backgroundScope))
        repo.merkeGezeigt(20)
        assertEquals(20, repo.zuletztGezeigt.first())
    }

    @Test
    fun eineAeltereFassungSenktDenStandNicht() = runTest {
        val repo = AenderungshinweisRepository(dataStore(backgroundScope))
        repo.merkeGezeigt(21)
        repo.merkeGezeigt(19)
        assertEquals(21, repo.zuletztGezeigt.first())
    }

    @Test
    fun vergessenLeertDenStand() = runTest {
        val repo = AenderungshinweisRepository(dataStore(backgroundScope))
        repo.merkeGezeigt(21)
        repo.vergiss()
        assertNull(repo.zuletztGezeigt.first())
    }

    /** Gegen den von Hand geschriebenen Namen, nicht reihum (`docs/agents/testing.md`). */
    @Test
    fun derGespeicherteNameBleibt() = runTest {
        val store = dataStore(backgroundScope)
        store.edit { it[intPreferencesKey("zuletzt_gezeigter_aenderungshinweis")] = 19 }
        assertEquals(19, AenderungshinweisRepository(store).zuletztGezeigt.first())
    }
}
