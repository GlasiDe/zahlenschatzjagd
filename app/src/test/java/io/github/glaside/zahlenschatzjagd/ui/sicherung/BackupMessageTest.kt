package io.github.glaside.zahlenschatzjagd.ui.sicherung

import io.github.glaside.zahlenschatzjagd.data.BackupFile
import io.github.glaside.zahlenschatzjagd.data.BackupProfile
import io.github.glaside.zahlenschatzjagd.data.BackupRundenabschluss
import io.github.glaside.zahlenschatzjagd.data.BackupTask
import io.github.glaside.zahlenschatzjagd.data.BackupZeitabschnitt
import io.github.glaside.zahlenschatzjagd.data.MergePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

/** Die Meldungen im Elternbereich lesen Eltern – sie müssen sprachlich stimmen. */
class BackupMessageTest {

    private val aufgabe = BackupTask(
        userName = "Mira",
        taskCategory = "PLUS",
        numberRange = 10,
        questionText = "3 + 4",
        expectedAnswer = "7",
        userAnswer = "7",
        isCorrect = true,
        timestamp = 1,
    )

    @Test
    fun einzahlUndMehrzahl_stimmen() {
        assertEquals("1 Profil", profileCount(1))
        assertEquals("2 Profile", profileCount(2))
        assertEquals("0 Profile", profileCount(0))
        assertEquals("1 Aufgabe", taskCount(1))
        assertEquals("830 Aufgaben", taskCount(830))
        assertEquals("1 Zeitabschnitt", zeitabschnittCount(1))
        assertEquals("4 Zeitabschnitte", zeitabschnittCount(4))
    }

    @Test
    fun aufzaehlung_bleibtAuchMitVierTeilenDeutsch() {
        assertEquals("", aufzaehlung(emptyList()))
        assertEquals("A", aufzaehlung(listOf("A")))
        assertEquals("A und B", aufzaehlung(listOf("A", "B")))
        assertEquals("A, B und C", aufzaehlung(listOf("A", "B", "C")))
        assertEquals("A, B, C und D", aufzaehlung(listOf("A", "B", "C", "D")))
    }

    @Test
    fun leereSicherung_wirdKorrektGemeldet() {
        val message = exportMessage(BackupFile(exportedAt = 0))

        assertEquals(
            "Sicherung gespeichert: 0 Profile, 0 Aufgaben, 0 Runden und 0 Zeitabschnitte.",
            message.text,
        )
    }

    @Test
    fun sicherung_nenntAuchRundenUndBildschirmzeit() {
        val message = exportMessage(
            BackupFile(
                exportedAt = 0,
                profiles = listOf(BackupProfile("Mira", "a", 1)),
                history = listOf(aufgabe),
                bildschirmzeit = listOf(BackupZeitabschnitt("Mira", beginn = 1, ende = 2)),
                runden = listOf(BackupRundenabschluss("Mira", zeitpunkt = 2)),
            )
        )

        assertEquals(
            "Sicherung gespeichert: 1 Profil, 1 Aufgabe, 1 Runde und 1 Zeitabschnitt.",
            message.text,
        )
    }

    @Test
    fun nurProfileErgaenzt_meldetKeineAufgaben() {
        val plan = MergePlan(newProfiles = listOf(BackupProfile("Mira", "a", 1)))

        assertEquals("Eingespielt: 1 Profil ergänzt.", restoreMessage(plan).text)
    }

    @Test
    fun ergaenzteRundenUndBildschirmzeit_stehenInDerMeldung() {
        val plan = MergePlan(
            newProfiles = listOf(BackupProfile("Mira", "a", 1)),
            newTasks = listOf(aufgabe),
            newZeitabschnitte = listOf(BackupZeitabschnitt("Mira", beginn = 1, ende = 2)),
            newRunden = listOf(BackupRundenabschluss("Mira", zeitpunkt = 2)),
            skippedZeitabschnitte = 3,
        )

        assertEquals(
            "Eingespielt: 1 Profil, 1 Aufgabe, 1 Runde und 1 Zeitabschnitt ergänzt. " +
                "3 Einträge waren schon vorhanden.",
            restoreMessage(plan).text,
        )
    }

    @Test
    fun nurRundenErgaenzt_giltNichtAlsUnveraendert() {
        val plan = MergePlan(
            newRunden = listOf(BackupRundenabschluss("Mira", zeitpunkt = 2)),
            skippedRunden = 4,
        )

        assertEquals(
            "Eingespielt: 1 Runde ergänzt. 4 Einträge waren schon vorhanden.",
            restoreMessage(plan).text,
        )
    }

    @Test
    fun nurBildschirmzeitErgaenzt_giltNichtAlsUnveraendert() {
        val plan = MergePlan(
            newZeitabschnitte = listOf(BackupZeitabschnitt("Mira", beginn = 1, ende = 2)),
            skippedProfiles = 1,
        )

        assertEquals(
            "Eingespielt: 1 Zeitabschnitt ergänzt. 1 Eintrag war schon vorhanden.",
            restoreMessage(plan).text,
        )
    }

    /**
     * Auf einem frisch installierten Gerät ist „war bereits alles vorhanden" falsch: Es war
     * nichts vorhanden, die Datei war leer. Unterscheidbar ist der Fall daran, dass auch nichts
     * zu überspringen war.
     */
    @Test
    fun leereDatei_sagtDassNichtsDrinStand() {
        val message = restoreMessage(MergePlan())

        assertEquals(
            "Diese Sicherung enthält keinen Fortschritt – es gab nichts zu ergänzen.",
            message.text,
        )
        assertFalse("Eine leere Datei ist kein Fehler der App", message.isError)
    }

    /** Der alte Satz bleibt für seinen eigentlichen Fall: das zweite Einspielen derselben Datei. */
    @Test
    fun bereitsEingespielteDatei_meldetWeiterhinDassAllesDaWar() {
        val plan = MergePlan(skippedProfiles = 1, skippedTasks = 7)

        assertEquals(
            "Es war bereits alles vorhanden – nichts wurde verändert.",
            restoreMessage(plan).text,
        )
    }

    @Test
    fun fehlermeldungen_sindAlsFehlerMarkiert() {
        assert(tooNewMessage(9).isError)
        assert(invalidFileMessage().isError)
        assert(fileAccessMessage().isError)
    }

    /** Die Meldungen nennen die App beim Namen, unter dem sie auf dem Gerät steht. */
    @Test
    fun fehlermeldungen_nennenDieAppBeimNamen() {
        listOf(tooNewMessage(9), invalidFileMessage()).forEach { meldung ->
            assert(meldung.text.contains("Zahlenschatzjagd")) { meldung.text }
        }
    }

    /**
     * Der Name, unter dem Eltern die Sicherung im Download-Ordner finden. Er trägt den App-Namen;
     * ältere Dateien mit `mathefreund-…` lassen sich trotzdem einspielen, weil das Einspielen den
     * Dateinamen nicht prüft.
     */
    @Test
    fun dateiname_enthaeltDatumUndIstSortierbar() {
        assertEquals(
            "zahlenschatzjagd-sicherung-2026-07-26.json",
            defaultBackupFileName(LocalDate.of(2026, 7, 26))
        )
    }
}
