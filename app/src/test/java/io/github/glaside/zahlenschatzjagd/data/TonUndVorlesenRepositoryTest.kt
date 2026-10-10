package io.github.glaside.zahlenschatzjagd.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Prüft die Ablage der beiden Schalter für Ton und Vorlesefunktion.
 *
 * Zwei Dinge stehen hier im Vordergrund: die **Voreinstellung** – ein Update darf nichts
 * abschalten, was vorher zu hören war – und die **Unabhängigkeit** der beiden Schalter
 * voneinander.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TonUndVorlesenRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun dataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) {
            File(tempFolder.root, "ton.preferences_pb")
        }

    private fun repository(scope: CoroutineScope): TonUndVorlesenRepository =
        TonUndVorlesenRepository(dataStore(scope))

    @Test
    fun ohneEinstellungSindBeideAn() = runTest {
        val repo = repository(backgroundScope)
        assertTrue("Der Ton muss voreingestellt an sein", repo.tonAn.first())
        assertTrue("Die Vorlesefunktion muss voreingestellt an sein", repo.vorlesenAn.first())
    }

    @Test
    fun derTonSchalterWirdGemerkt() = runTest {
        val repo = repository(backgroundScope)
        repo.setzeTonAn(false)
        assertFalse(repo.tonAn.first())

        repo.setzeTonAn(true)
        assertTrue(repo.tonAn.first())
    }

    @Test
    fun derVorleseSchalterWirdGemerkt() = runTest {
        val repo = repository(backgroundScope)
        repo.setzeVorlesenAn(false)
        assertFalse(repo.vorlesenAn.first())

        repo.setzeVorlesenAn(true)
        assertTrue(repo.vorlesenAn.first())
    }

    /**
     * Zwei Schalter, zwei Aussagen: Wer im Wartezimmer den Ton abschaltet, will die
     * Vorlesefunktion nicht mitverlieren – und umgekehrt.
     */
    @Test
    fun dieBeidenSchalterGeltenUnabhaengigVoneinander() = runTest {
        val repo = repository(backgroundScope)
        repo.setzeTonAn(false)

        assertTrue("Der Ton-Schalter darf die Vorlesefunktion nicht mitnehmen", repo.vorlesenAn.first())

        repo.setzeVorlesenAn(false)
        repo.setzeTonAn(true)
        assertFalse("Der Ton-Schalter darf die Vorlesefunktion nicht mitnehmen", repo.vorlesenAn.first())
    }

    /**
     * Von Hand geschriebene Ablage, nicht im Reihum-Verfahren (`docs/agents/testing.md`): Die
     * beiden Schlüsselnamen sind ab jetzt eingefroren. Wandern sie, findet das Repository nichts
     * mehr und beide Schalter stehen nach dem Update wieder auf „an" – ohne dass etwas rot wird.
     */
    @Test
    fun liestDieVonHandGeschriebeneAblage() = runTest {
        val store = dataStore(backgroundScope)
        store.edit { prefs ->
            prefs[booleanPreferencesKey("ton_an")] = false
            prefs[booleanPreferencesKey("vorlesen_an")] = false
        }

        val repo = TonUndVorlesenRepository(store)
        assertFalse("Der Schlüsselname des Ton-Schalters stimmt nicht mehr", repo.tonAn.first())
        assertFalse(
            "Der Schlüsselname des Vorlese-Schalters stimmt nicht mehr",
            repo.vorlesenAn.first(),
        )
    }
}
