package io.github.glaside.zahlenschatzjagd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft das Zusammenführen einer Sicherung mit dem vorhandenen Bestand. Kernanforderung des
 * Features: Es darf niemals etwas verloren gehen oder doppelt auftauchen.
 */
class BackupMergeTest {

    private fun task(
        user: String = "Mira",
        timestamp: Long = 1_000,
        question: String = "3 + _ = 10",
        answer: String = "7",
    ) = BackupTask(
        userName = user,
        taskCategory = "VERLIEBTE_ZAHLEN",
        numberRange = 10,
        questionText = question,
        expectedAnswer = "7",
        userAnswer = answer,
        isCorrect = true,
        timestamp = timestamp,
    )

    private fun profile(name: String, avatar: String = "avatar_pirat_1") =
        BackupProfile(name, avatar, createdAt = 500)

    private fun abschnitt(profil: String = "Mira", beginn: Long = 1_000, ende: Long = 61_000) =
        BackupZeitabschnitt(profil = profil, beginn = beginn, ende = ende)

    private fun runde(profil: String = "Mira", zeitpunkt: Long = 61_000) =
        BackupRundenabschluss(profil = profil, zeitpunkt = zeitpunkt)

    private fun backup(
        profiles: List<BackupProfile> = emptyList(),
        history: List<BackupTask> = emptyList(),
        bildschirmzeit: List<BackupZeitabschnitt> = emptyList(),
        runden: List<BackupRundenabschluss> = emptyList(),
    ) = BackupFile(
        exportedAt = 0,
        profiles = profiles,
        history = history,
        bildschirmzeit = bildschirmzeit,
        runden = runden,
    )

    @Test
    fun leererBestand_uebernimmtAllesAusDerSicherung() {
        val imported = backup(
            profiles = listOf(profile("Mira"), profile("Chris")),
            history = listOf(task(timestamp = 1), task(timestamp = 2)),
        )

        val plan = mergeBackup(BackupSnapshot(), imported)

        assertEquals(listOf("Mira", "Chris"), plan.newProfiles.map { it.name })
        assertEquals(2, plan.newTasks.size)
        assertEquals(0, plan.skippedProfiles)
        assertEquals(0, plan.skippedTasks)
        assertTrue(plan.hasChanges)
    }

    @Test
    fun identischeSicherung_aenderteNichts() {
        val profiles = listOf(profile("Mira"))
        val history = listOf(task(timestamp = 1), task(timestamp = 2))
        val existing = BackupSnapshot(profiles, history)

        val plan = mergeBackup(existing, backup(profiles, history))

        assertTrue(plan.newProfiles.isEmpty())
        assertTrue(plan.newTasks.isEmpty())
        assertEquals(1, plan.skippedProfiles)
        assertEquals(2, plan.skippedTasks)
        assertFalse(plan.hasChanges)
    }

    @Test
    fun vorhandenesProfil_behaeltSeinenAvatar() {
        val existing = BackupSnapshot(profiles = listOf(profile("Mira", "avatar_pirat_8")))

        val plan = mergeBackup(existing, backup(profiles = listOf(profile("Mira", "avatar_pirat_2"))))

        assertTrue("Ein vorhandenes Profil darf nicht überschrieben werden", plan.newProfiles.isEmpty())
        assertEquals(1, plan.skippedProfiles)
    }

    @Test
    fun neueAufgabenWerdenErgaenzt_alteBleibenUnangetastet() {
        val existing = BackupSnapshot(history = listOf(task(timestamp = 1)))
        val imported = backup(history = listOf(task(timestamp = 1), task(timestamp = 2)))

        val plan = mergeBackup(existing, imported)

        assertEquals(listOf(2L), plan.newTasks.map { it.timestamp })
        assertEquals(1, plan.skippedTasks)
    }

    @Test
    fun gleicheZeit_aberAnderesKind_sindZweiVerschiedeneAufgaben() {
        val existing = BackupSnapshot(history = listOf(task(user = "Mira", timestamp = 1)))

        val plan = mergeBackup(existing, backup(history = listOf(task(user = "Chris", timestamp = 1))))

        assertEquals(1, plan.newTasks.size)
        assertEquals("Chris", plan.newTasks.single().userName)
    }

    @Test
    fun gleicheAufgabeZuAndererZeit_giltAlsNeu() {
        val existing = BackupSnapshot(history = listOf(task(timestamp = 1)))

        val plan = mergeBackup(existing, backup(history = listOf(task(timestamp = 99))))

        assertEquals(1, plan.newTasks.size)
    }

    @Test
    fun gleicheAufgabeMitAndererAntwort_giltAlsNeu() {
        val existing = BackupSnapshot(history = listOf(task(answer = "7")))

        val plan = mergeBackup(existing, backup(history = listOf(task(answer = "6"))))

        assertEquals(1, plan.newTasks.size)
    }

    @Test
    fun duplikateInnerhalbDerDatei_werdenNurEinmalUebernommen() {
        val doppelt = task(timestamp = 5)

        val plan = mergeBackup(BackupSnapshot(), backup(history = listOf(doppelt, doppelt)))

        assertEquals(1, plan.newTasks.size)
        assertEquals(1, plan.skippedTasks)
    }

    @Test
    fun leereSicherung_aendertNichts() {
        val existing = BackupSnapshot(listOf(profile("Mira")), listOf(task()))

        val plan = mergeBackup(existing, backup())

        assertFalse(plan.hasChanges)
        assertEquals(0, plan.skippedTasks)
    }

    // --- Bildschirmzeit ---

    @Test
    fun bildschirmzeit_wirdAufLeeremGeraetVollstaendigUebernommen() {
        val imported = backup(
            bildschirmzeit = listOf(abschnitt(beginn = 1_000), abschnitt(beginn = 90_000)),
        )

        val plan = mergeBackup(BackupSnapshot(), imported)

        assertEquals(listOf(1_000L, 90_000L), plan.newZeitabschnitte.map { it.beginn })
        assertEquals(0, plan.skippedZeitabschnitte)
        assertTrue(plan.hasChanges)
    }

    @Test
    fun zweitesEinspielenDerselbenDatei_ergaenztKeineBildschirmzeit() {
        val zeiten = listOf(abschnitt(beginn = 1_000), abschnitt(beginn = 90_000))
        val datei = backup(bildschirmzeit = zeiten)

        // Erstes Einspielen auf ein leeres Gerät …
        val erster = mergeBackup(BackupSnapshot(), datei)
        // … das Ergebnis ist danach der Bestand des Geräts.
        val zweiter = mergeBackup(BackupSnapshot(bildschirmzeit = erster.newZeitabschnitte), datei)

        assertTrue(zweiter.newZeitabschnitte.isEmpty())
        assertEquals(2, zweiter.skippedZeitabschnitte)
        assertFalse("Ein zweites Einspielen darf nichts ändern", zweiter.hasChanges)
    }

    @Test
    fun gleicherBeginn_aberAnderesKind_sindZweiVerschiedeneAbschnitte() {
        val existing = BackupSnapshot(bildschirmzeit = listOf(abschnitt(profil = "Mira")))

        val plan = mergeBackup(existing, backup(bildschirmzeit = listOf(abschnitt(profil = "Chris"))))

        assertEquals(1, plan.newZeitabschnitte.size)
        assertEquals("Chris", plan.newZeitabschnitte.single().profil)
    }

    @Test
    fun gleicherBeginn_mitAnderemEnde_giltAlsDerselbeAbschnitt() {
        // Ein Abschnitt ist unveränderlich: Derselbe Beginn ist derselbe Abschnitt, sonst stünde
        // nach zwei Sicherungen derselbe Nachmittag zweimal in der Auswertung.
        val existing = BackupSnapshot(bildschirmzeit = listOf(abschnitt(beginn = 1_000, ende = 61_000)))

        val plan = mergeBackup(
            existing,
            backup(bildschirmzeit = listOf(abschnitt(beginn = 1_000, ende = 999_000))),
        )

        assertTrue(plan.newZeitabschnitte.isEmpty())
        assertEquals(1, plan.skippedZeitabschnitte)
    }

    @Test
    fun bildschirmzeitDuplikateInnerhalbDerDatei_werdenNurEinmalUebernommen() {
        val doppelt = abschnitt(beginn = 5_000)

        val plan = mergeBackup(BackupSnapshot(), backup(bildschirmzeit = listOf(doppelt, doppelt)))

        assertEquals(1, plan.newZeitabschnitte.size)
        assertEquals(1, plan.skippedZeitabschnitte)
    }

    @Test
    fun nurNeueBildschirmzeit_giltAlsAenderung() {
        // Sonst meldete der Elternbereich „es war bereits alles vorhanden", obwohl er gerade
        // die Bildschirmzeit eines ganzen Geräts ergänzt hat.
        val existing = BackupSnapshot(listOf(profile("Mira")), listOf(task()))

        val plan = mergeBackup(
            existing,
            backup(listOf(profile("Mira")), listOf(task()), listOf(abschnitt())),
        )

        assertTrue(plan.hasChanges)
        assertEquals(1, plan.newZeitabschnitte.size)
    }

    @Test
    fun alteSicherungOhneBildschirmzeit_laesstVorhandeneUnangetastet() {
        val existing = BackupSnapshot(bildschirmzeit = listOf(abschnitt()))

        val plan = mergeBackup(existing, backup(profiles = listOf(profile("Mira"))))

        assertTrue(plan.newZeitabschnitte.isEmpty())
        assertEquals(0, plan.skippedZeitabschnitte)
    }

    // --- Abgeschlossene Runden (Übungskalender) --------------------------------------------------

    @Test
    fun runden_werdenAufLeeremGeraetVollstaendigUebernommen() {
        val datei = backup(runden = listOf(runde(zeitpunkt = 1_000), runde(zeitpunkt = 90_000)))

        val plan = mergeBackup(BackupSnapshot(), datei)

        assertEquals(listOf(1_000L, 90_000L), plan.newRunden.map { it.zeitpunkt })
        assertEquals(0, plan.skippedRunden)
        assertTrue(plan.hasChanges)
    }

    @Test
    fun zweitesEinspielenDerselbenDatei_ergaenztKeineRunden() {
        val datei = backup(runden = listOf(runde(zeitpunkt = 1_000), runde(zeitpunkt = 90_000)))
        val erster = mergeBackup(BackupSnapshot(), datei)

        val zweiter = mergeBackup(BackupSnapshot(runden = erster.newRunden), datei)

        assertTrue(zweiter.newRunden.isEmpty())
        assertEquals(2, zweiter.skippedRunden)
        assertFalse(zweiter.hasChanges)
    }

    @Test
    fun gleicherZeitpunkt_aberAnderesKind_sindZweiVerschiedeneRunden() {
        val existing = BackupSnapshot(runden = listOf(runde(profil = "Mira")))

        val plan = mergeBackup(existing, backup(runden = listOf(runde(profil = "Chris"))))

        assertEquals(1, plan.newRunden.size)
        assertEquals("Chris", plan.newRunden.single().profil)
    }

    @Test
    fun rundenDuplikateInnerhalbDerDatei_werdenNurEinmalUebernommen() {
        val plan = mergeBackup(BackupSnapshot(), backup(runden = listOf(runde(), runde())))

        assertEquals(1, plan.newRunden.size)
        assertEquals(1, plan.skippedRunden)
    }

    @Test
    fun nurNeueRunden_giltAlsAenderung() {
        val existing = BackupSnapshot(profiles = listOf(profile("Mira")))

        val plan = mergeBackup(existing, backup(listOf(profile("Mira")), runden = listOf(runde())))

        assertTrue(plan.hasChanges)
        assertEquals(1, plan.newRunden.size)
    }

    @Test
    fun alteSicherungOhneRunden_laesstVorhandeneUnangetastet() {
        val existing = BackupSnapshot(runden = listOf(runde()))

        val plan = mergeBackup(existing, backup(profiles = listOf(profile("Mira"))))

        assertTrue(plan.newRunden.isEmpty())
        assertEquals(0, plan.skippedRunden)
    }

    @Test
    fun fortschrittOhnePassendesProfil_gehtTrotzdemMit() {
        // Sonst wäre der Fortschritt eines Kindes weg, dessen Profil erst später wieder
        // angelegt wird – genau der Fall beim Umzug auf ein neues Gerät.
        val plan = mergeBackup(BackupSnapshot(), backup(history = listOf(task(user = "Mira"))))

        assertEquals(1, plan.newTasks.size)
        assertTrue(plan.newProfiles.isEmpty())
    }

    /**
     * Der Ertrag gehört nicht zum Fingerabdruck einer Aufgabe. Auf dem Gerät steht eine teilweise
     * richtige Mauer mit Ertrag 1; eine ältere Sicherung bringt dieselbe Mauer ohne Ertrag mit –
     * nach der Regel für ältere Dateien wäre das 0. Sie darf trotzdem nicht doppelt entstehen
     * (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
     */
    @Test
    fun eineAlteDateiOhneErtragLegtEineVorhandeneMauerNichtDoppeltAn() {
        val mauer = task(question = "Zahlenmauer (MITTEL, 6 Lücken)", answer = "1, 2, 3, 4, 5, 7")
            .copy(taskCategory = "ZAHLENMAUER_MITTEL", numberRange = 50, isCorrect = false)
        val aufDemGeraet = BackupSnapshot(history = listOf(mauer.copy(ertrag = 1)))

        val plan = mergeBackup(aufDemGeraet, backup(history = listOf(mauer.copy(ertrag = null))))

        assertTrue(plan.newTasks.isEmpty())
        assertEquals(1, plan.skippedTasks)
    }
}
