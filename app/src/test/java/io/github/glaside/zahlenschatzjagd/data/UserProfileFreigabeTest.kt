package io.github.glaside.zahlenschatzjagd.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Die Freigabe auf dem Weg durch Profil-Zeile und [UserProfileRepository]. */
class UserProfileFreigabeTest {

    private val dao = FakeUserProfileDao()
    private val repository = UserProfileRepository(dao)

    @Test
    fun einFrischAngelegtesProfilDarfAlles() = runTest {
        repository.createProfile("Mira", "avatar_pirat_1")

        assertEquals(Freigabe.ALLES, repository.freigabe("Mira").first())
    }

    @Test
    fun einUnbekannterNameDarfAlles() = runTest {
        assertEquals(Freigabe.ALLES, repository.freigabe("gibt-es-nicht").first())
    }

    @Test
    fun gesetzteFreigabeKommtGenausoZurueck() = runTest {
        repository.createProfile("Mira", "avatar_pirat_1")
        val nurPlusBisZwanzig = Freigabe(kennungen = setOf("PLUS"), zahlenraeume = setOf(10, 20))

        repository.setFreigabe("Mira", nurPlusBisZwanzig)

        assertEquals(nurPlusBisZwanzig, repository.freigabe("Mira").first())
    }

    @Test
    fun dieFreigabeGiltNurFuerIhrProfil() = runTest {
        repository.createProfile("Mira", "avatar_pirat_1")
        repository.createProfile("Ben", "avatar_pirat_2")

        repository.setFreigabe("Mira", Freigabe(kennungen = emptySet(), zahlenraeume = emptySet()))

        assertEquals(Freigabe.ALLES, repository.freigabe("Ben").first())
    }

    @Test
    fun allesAbgewaehltUeberlebtDasSpeichern() = runTest {
        repository.createProfile("Mira", "avatar_pirat_1")
        val nichts = Freigabe(kennungen = emptySet(), zahlenraeume = emptySet())

        repository.setFreigabe("Mira", nichts)

        assertEquals(nichts, repository.freigabe("Mira").first())
    }

    @Test
    fun dieFreigabeFolgtDemUmbenennen() = runTest {
        repository.createProfile("Mira", "avatar_pirat_1")
        val nurMinus = Freigabe(kennungen = setOf("MINUS"), zahlenraeume = setOf(10))
        repository.setFreigabe("Mira", nurMinus)

        repository.renameProfile("Mira", "Miriam")

        assertEquals(nurMinus, repository.freigabe("Miriam").first())
    }
}
