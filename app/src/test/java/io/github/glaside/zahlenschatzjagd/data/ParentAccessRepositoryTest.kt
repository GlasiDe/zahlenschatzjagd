package io.github.glaside.zahlenschatzjagd.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.security.MessageDigest

/**
 * Prüft die Ablage der optionalen PIN. Wichtigster Testfall ist der letzte: Auf dem Gerät darf
 * weder die PIN noch die Antwort auf die Sicherheitsfrage im Klartext liegen
 * (`docs/adr/0016-pin-optional-neben-der-elternfrage.md`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentAccessRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun storeFile() = File(tempFolder.root, "parent.preferences_pb")

    private fun dataStore(scope: CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope) { storeFile() }

    private fun repository(scope: CoroutineScope): ParentAccessRepository =
        ParentAccessRepository(dataStore(scope))

    /**
     * Von Hand geschriebene Ablage, nicht im Reihum-Verfahren (`docs/agents/testing.md`): Ein
     * Reihum-Test bemerkt eine Umbenennung nicht, weil Schreiber und Leser gemeinsam mitwandern.
     *
     * Hier wären die Folgen still und teuer: Heißt ein Schlüssel plötzlich anders, findet
     * [ParentAccessRepository.access] nichts, `gateFor` schaltet auf die Elternfrage zurück – und
     * die gesetzte PIN ist beim nächsten App-Start wirkungslos, ohne dass irgendetwas rot wird.
     * Dieser Test nagelt die vier Schlüsselnamen und den Aufbau der Prüfsumme (Salt **vor** Wert)
     * fest.
     */
    @Test
    fun liestEineVonHandGeschriebeneAblage() = runTest {
        val store = dataStore(backgroundScope)
        store.edit { prefs ->
            prefs[stringPreferencesKey("parent_access_salt")] = "salz"
            prefs[stringPreferencesKey("parent_access_pin_checksum")] =
                sha256Hex("salz" + "4711")
            prefs[stringPreferencesKey("parent_access_question")] = "Erste Chefin?"
            prefs[stringPreferencesKey("parent_access_answer_checksum")] =
                sha256Hex("salz" + "meier")
        }

        val zugang = ParentAccessRepository(store).access.first()
        assertNotNull("Die vier Schlüsselnamen stimmen nicht mehr", zugang)
        assertEquals("Erste Chefin?", zugang!!.question)
        assertTrue(zugang.acceptsPin("4711"))
        assertFalse(zugang.acceptsPin("1234"))
        assertTrue(zugang.acceptsAnswer("  Meier "))
    }

    /** Bewusst zweite, unabhängige Implementierung – sonst wäre auch das wieder ein Reihum-Test. */
    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }

    @Test
    fun ohneVergabeIstKeinZugangGesetzt() = runTest {
        assertNull(repository(backgroundScope).access.first())
    }

    @Test
    fun nachVergabeWirdNurDieRichtigePinAkzeptiert() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "Erste Chefin?", answer = "Meier")

        val zugang = repo.access.first()
        assertNotNull(zugang)
        assertTrue(zugang!!.acceptsPin("4711"))
        assertFalse(zugang.acceptsPin("1234"))
    }

    @Test
    fun dieSicherheitsfrageWirdImKlartextGemerkt() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "  Erste Chefin?  ", answer = "Meier")

        assertEquals("Erste Chefin?", repo.access.first()?.question)
    }

    @Test
    fun dieAntwortWirdNormalisiertVerglichen() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "Erste Chefin?", answer = "Meier")

        val zugang = repo.access.first()!!
        assertTrue(zugang.acceptsAnswer("  meier "))
        assertFalse(zugang.acceptsAnswer("Meyer"))
    }

    @Test
    fun eineNeueVergabeEntwertetDieAltePin() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "Erste Chefin?", answer = "Meier")
        repo.setPin(pin = "1234", question = "Erste Chefin?", answer = "Meier")

        val zugang = repo.access.first()!!
        assertTrue(zugang.acceptsPin("1234"))
        assertFalse(zugang.acceptsPin("4711"))
    }

    /**
     * Der Kern des optionalen Rückwegs: Ein PIN-Wechsel darf die Sicherheitsfrage nicht
     * entwerten – sonst wäre man nach dem Wechsel ausgesperrt, sobald man die neue PIN vergisst.
     */
    @Test
    fun derPinTauschLaesstFrageUndAntwortUnberuehrt() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "Erste Chefin?", answer = "Meier")
        repo.changePin("1234")

        val zugang = repo.access.first()!!
        assertTrue(zugang.acceptsPin("1234"))
        assertFalse(zugang.acceptsPin("4711"))
        assertEquals("Erste Chefin?", zugang.question)
        assertTrue(zugang.acceptsAnswer("meier"))
    }

    @Test
    fun derPinTauschOhneGesetztePinTutNichts() = runTest {
        val repo = repository(backgroundScope)
        repo.changePin("1234")

        assertNull(repo.access.first())
    }

    @Test
    fun nachDemEntfernenGiltWiederDieElternfrage() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "Erste Chefin?", answer = "Meier")
        repo.clearPin()

        assertNull(repo.access.first())
    }

    /**
     * Die Voreinstellung ist der Kern des Schalters: Ein Update darf keine Funktion wegnehmen,
     * die vorher da war (`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`). Steht hier eines
     * Tages `false`, muss nach dem Update jedes Kind auf jedem bestehenden Gerät erst die
     * Elternfrage lösen, um an sein eigenes Profil zu kommen.
     */
    @Test
    fun derProfilwechselIstVoreingestelltFrei() = runTest {
        assertTrue(repository(backgroundScope).profilwechselOhnePin.first())
    }

    @Test
    fun derSchalterWirdGemerkt() = runTest {
        val repo = repository(backgroundScope)
        repo.setzeProfilwechselOhnePin(false)
        assertFalse(repo.profilwechselOhnePin.first())

        repo.setzeProfilwechselOhnePin(true)
        assertTrue(repo.profilwechselOhnePin.first())
    }

    /** Von Hand geschrieben wie oben – der Schlüsselname ist ab jetzt eingefroren. */
    @Test
    fun liestDenVonHandGeschriebenenSchalter() = runTest {
        val store = dataStore(backgroundScope)
        store.edit { prefs -> prefs[booleanPreferencesKey("profilwechsel_ohne_pin")] = false }

        assertFalse(
            "Der Schlüsselname des Schalters stimmt nicht mehr",
            ParentAccessRepository(store).profilwechselOhnePin.first(),
        )
    }

    /**
     * Der Schalter ist keine Eigenschaft der PIN, sondern steht neben ihr: Wer die PIN entfernt,
     * will damit nicht zugleich den Profilwechsel wieder freigeben.
     */
    @Test
    fun derSchalterUeberlebtDasEntfernenDerPin() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "Erste Chefin?", answer = "Meier")
        repo.setzeProfilwechselOhnePin(false)

        repo.clearPin()

        assertFalse(repo.profilwechselOhnePin.first())
    }

    @Test
    fun wederPinNochAntwortLiegenImKlartextAufDemGeraet() = runTest {
        val repo = repository(backgroundScope)
        repo.setPin(pin = "4711", question = "Erste Chefin?", answer = "Meier")
        // Erst lesen erzwingt, dass der Schreibvorgang auf der Platte angekommen ist.
        assertNotNull(repo.access.first())

        val roh = storeFile().readBytes().decodeToString()
        assertFalse("Die PIN steht im Klartext auf dem Gerät", roh.contains("4711"))
        assertFalse("Die Antwort steht im Klartext auf dem Gerät", roh.contains("Meier"))
        assertFalse("Die Antwort steht im Klartext auf dem Gerät", roh.contains("meier"))
        // Die Frage selbst darf und muss lesbar sein – sie wird ja angezeigt.
        assertTrue(roh.contains("Erste Chefin?"))
    }
}
