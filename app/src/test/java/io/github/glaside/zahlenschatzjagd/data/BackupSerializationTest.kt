package io.github.glaside.zahlenschatzjagd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prüft das Dateiformat der Sicherung: Roundtrip, Robustheit und Versionsprüfung. */
class BackupSerializationTest {

    private val beispiel = BackupFile(
        exportedAt = 1_700_000_000_000,
        appVersionName = "1.2",
        profiles = listOf(BackupProfile("Mira", "avatar_pirat_8", createdAt = 1)),
        history = listOf(
            BackupTask(
                userName = "Mira",
                taskCategory = "VERLIEBTE_ZAHLEN",
                numberRange = 10,
                questionText = "3 + _ = 10",
                expectedAnswer = "7",
                userAnswer = "7",
                isCorrect = true,
                timestamp = 2,
            )
        ),
    )

    /**
     * Dasselbe Beispiel, zusätzlich mit dem, was der Übungskalender braucht: gemessene
     * Bildschirmzeit und abgeschlossene Runden. Beides kam erst später dazu.
     */
    private val beispielMitZeitachse = beispiel.copy(
        bildschirmzeit = listOf(BackupZeitabschnitt(profil = "Mira", beginn = 3, ende = 63_003)),
        runden = listOf(BackupRundenabschluss(profil = "Mira", zeitpunkt = 63_003)),
    )

    /**
     * Eine echte Sicherungsdatei, **von Hand** geschrieben – bewusst nicht aus [BackupFile]
     * erzeugt. Genau darin liegt der Schutz: Der Roundtrip-Test unten würde eine Umbenennung
     * nicht bemerken, weil Encoder und Decoder gemeinsam mitwandern. Diese Zeichenkette wandert
     * nicht mit. Sie steht für die Dateien, die auf den Geräten der Familien liegen.
     *
     * **Diese Schlüssel dürfen sich nie ändern.** Wer ein Feld im Kotlin-Code umbenennt, hält
     * den JSON-Schlüssel per `@SerialName` fest (siehe `BackupModels.kt`); dieser Test wird
     * sonst rot – und das ist seine Aufgabe.
     */
    private val goldenFile = """
        {
          "formatVersion": 1,
          "exportedAt": 1700000000000,
          "appVersionName": "1.2",
          "profiles": [
            {
              "name": "Mira",
              "avatarId": "avatar_pirat_8",
              "createdAt": 1
            }
          ],
          "history": [
            {
              "userName": "Mira",
              "taskCategory": "VERLIEBTE_ZAHLEN",
              "numberRange": 10,
              "questionText": "3 + _ = 10",
              "expectedAnswer": "7",
              "userAnswer": "7",
              "isCorrect": true,
              "timestamp": 2
            }
          ]
        }
    """.trimIndent()

    @Test
    fun goldenFile_wirdVollstaendigGelesen() {
        val result = BackupSerializer.decode(goldenFile)

        assertTrue("Bestehende Sicherungsdatei nicht mehr lesbar!", result is BackupReadResult.Success)
        assertEquals(beispiel, (result as BackupReadResult.Success).file)
    }

    /**
     * Die Golden-Datei stammt aus der Zeit vor der Freigabe und nennt sie nicht. Sie muss als
     * „nichts abgewählt" gelten – ein Kind darf durch das Einspielen einer alten Sicherung nicht
     * plötzlich vor gesperrten Inseln stehen.
     */
    @Test
    fun goldenFile_ohneFreigabe_giltAlsNichtsAbgewaehlt() {
        val result = BackupSerializer.decode(goldenFile) as BackupReadResult.Success

        val profil = result.file.profiles.single().toEntity()
        assertEquals(Freigabe.ALLES, profil.freigabe)
    }

    /**
     * Eine zweite, ebenfalls **von Hand** geschriebene Datei – diesmal mit gesetzter Freigabe.
     * Sie hält die Leseseite der beiden Felder fest; ein Reihum-Test über `encode`/`decode`
     * täte das nicht, weil Schreiber und Leser gemeinsam mitwandern würden
     * (`docs/agents/testing.md`).
     */
    private val goldenFileMitFreigabe = """
        {
          "formatVersion": 1,
          "exportedAt": 1700000000000,
          "appVersionName": "1.4",
          "profiles": [
            {
              "name": "Ben",
              "avatarId": "avatar_pirat_2",
              "createdAt": 3,
              "gesperrteKennungen": "MINUS,MAL",
              "gesperrteZahlenraeume": "100"
            }
          ],
          "history": []
        }
    """.trimIndent()

    /**
     * Die Golden-Datei stammt auch aus der Zeit vor der Bildschirmzeit. Sie muss weiterhin
     * lesbar sein und als „noch nichts gemessen" gelten – rückwirkende Zahlen gibt es nicht
     * (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`).
     */
    @Test
    fun goldenFile_ohneBildschirmzeit_bleibtLesbar() {
        val result = BackupSerializer.decode(goldenFile) as BackupReadResult.Success

        assertTrue(result.file.bildschirmzeit.isEmpty())
    }

    /**
     * Eine dritte, ebenfalls **von Hand** geschriebene Datei – mit den beiden Quellen des
     * Übungskalenders. Sie hält die Leseseite der Schlüssel fest, die auf den Geräten der Familien
     * stehen.
     */
    private val goldenFileMitZeitachse = """
        {
          "formatVersion": 1,
          "exportedAt": 1700000000000,
          "appVersionName": "1.6",
          "profiles": [],
          "history": [],
          "bildschirmzeit": [
            {
              "profil": "Mira",
              "beginn": 1700000000000,
              "ende": 1700000600000
            }
          ],
          "runden": [
            {
              "profil": "Mira",
              "zeitpunkt": 1700000600000
            }
          ]
        }
    """.trimIndent()

    @Test
    fun goldenFile_mitBildschirmzeit_wirdRichtigGelesen() {
        val result = BackupSerializer.decode(goldenFileMitZeitachse) as BackupReadResult.Success

        val abschnitt = result.file.bildschirmzeit.single()
        assertEquals("Mira", abschnitt.profil)
        assertEquals(1_700_000_000_000, abschnitt.beginn)
        assertEquals(1_700_000_600_000, abschnitt.ende)
        assertEquals(10 * 60_000, abschnitt.toEntity().dauerMillis)
    }

    @Test
    fun goldenFile_mitRunden_wirdRichtigGelesen() {
        val result = BackupSerializer.decode(goldenFileMitZeitachse) as BackupReadResult.Success

        val abschluss = result.file.runden.single()
        assertEquals("Mira", abschluss.profil)
        assertEquals(1_700_000_600_000, abschluss.zeitpunkt)
    }

    /**
     * Die erste Golden-Datei stammt aus der Zeit vor dem Übungskalender. Sie muss lesbar bleiben
     * und als „keine abgeschlossene Runde bekannt" gelten – rückwirkend lässt sich nicht sagen,
     * welche Runde durchgezogen wurde (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`).
     */
    @Test
    fun goldenFile_ohneRunden_bleibtLesbar() {
        val result = BackupSerializer.decode(goldenFile) as BackupReadResult.Success

        assertTrue(result.file.runden.isEmpty())
    }

    @Test
    fun goldenFile_mitFreigabe_wirdRichtigGelesen() {
        val result = BackupSerializer.decode(goldenFileMitFreigabe) as BackupReadResult.Success

        val profil = result.file.profiles.single().toEntity()
        assertEquals("MINUS,MAL", profil.gesperrteKennungen)
        assertEquals("100", profil.gesperrteZahlenraeume)
        assertEquals(ALLE_KENNUNGEN - "MINUS" - "MAL", profil.freigabe.kennungen)
        assertEquals(setOf(10, 20, 50), profil.freigabe.zahlenraeume)
    }

    /**
     * Eine **von Hand** geschriebene Datei aus der Zeit vor dem Ertrag, mit Zahlenmauern. Beim
     * Einspielen gilt für sie dieselbe Regel wie für die Migration der Datenbank: eine ganz
     * richtige Mauer bringt 2, eine falsche 0, jede andere Aufgabe 1 bzw. 0
     * (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
     */
    private val goldenFileOhneErtrag = """
        {
          "formatVersion": 1,
          "exportedAt": 1700000000000,
          "history": [
            {
              "userName": "Mira",
              "taskCategory": "ZAHLENMAUER_LEICHT",
              "numberRange": 20,
              "questionText": "Zahlenmauer (LEICHT, 3 Lücken)",
              "expectedAnswer": "4, 6, 10",
              "userAnswer": "4, 6, 10",
              "isCorrect": true,
              "timestamp": 10
            },
            {
              "userName": "Mira",
              "taskCategory": "ZAHLENMAUER_ULTRA",
              "numberRange": 100,
              "questionText": "Zahlenmauer (ULTRA, 10 Lücken)",
              "expectedAnswer": "1",
              "userAnswer": "2",
              "isCorrect": false,
              "timestamp": 11
            },
            {
              "userName": "Mira",
              "taskCategory": "PLUS",
              "numberRange": 10,
              "questionText": "3 + 4 = _",
              "expectedAnswer": "7",
              "userAnswer": "7",
              "isCorrect": true,
              "timestamp": 12
            },
            {
              "userName": "Mira",
              "taskCategory": "PLUS",
              "numberRange": 10,
              "questionText": "3 + 5 = _",
              "expectedAnswer": "8",
              "userAnswer": "9",
              "isCorrect": false,
              "timestamp": 13
            }
          ]
        }
    """.trimIndent()

    @Test
    fun goldenFile_ohneErtrag_folgtDerRegelDerMigration() {
        val result = BackupSerializer.decode(goldenFileOhneErtrag) as BackupReadResult.Success

        assertEquals(listOf(2, 0, 1, 0), result.file.history.map { it.toEntity().ertrag })
    }

    /**
     * Eine **von Hand** geschriebene Datei mit Ertrag: eine teilweise richtige Mauer (Ertrag 1),
     * die sich aus `isCorrect` allein nicht ablesen ließe.
     */
    private val goldenFileMitErtrag = """
        {
          "formatVersion": 1,
          "exportedAt": 1700000000000,
          "history": [
            {
              "userName": "Mira",
              "taskCategory": "ZAHLENMAUER_MITTEL",
              "numberRange": 50,
              "questionText": "Zahlenmauer (MITTEL, 6 Lücken)",
              "expectedAnswer": "1, 2, 3, 4, 5, 6",
              "userAnswer": "1, 2, 3, 4, 5, 7",
              "isCorrect": false,
              "timestamp": 20,
              "ertrag": 1
            }
          ]
        }
    """.trimIndent()

    @Test
    fun goldenFile_mitErtrag_wirdUebernommen() {
        val result = BackupSerializer.decode(goldenFileMitErtrag) as BackupReadResult.Success

        assertEquals(1, result.file.history.single().toEntity().ertrag)
    }

    /**
     * Die Gegenrichtung: Was die App *schreibt*, muss dieselben Schlüssel tragen wie das, was
     * ältere Installationen zu lesen erwarten. Fängt eine Umbenennung ohne `@SerialName` auch
     * dann, wenn niemand die Golden-Datei anfasst.
     */
    @Test
    fun export_verwendetDieEingefrorenenFeldnamen() {
        val text = BackupSerializer.encode(beispielMitZeitachse)

        val erwarteteSchluessel = listOf(
            "formatVersion", "exportedAt", "appVersionName", "profiles", "history",
            "name", "avatarId", "createdAt", "gesperrteKennungen", "gesperrteZahlenraeume",
            "userName", "taskCategory", "numberRange", "questionText",
            "expectedAnswer", "userAnswer", "isCorrect", "timestamp", "ertrag",
            "bildschirmzeit", "profil", "beginn", "ende",
            "runden", "zeitpunkt",
        )

        erwarteteSchluessel.forEach { schluessel ->
            assertTrue(
                "Feldname \"$schluessel\" fehlt im Export – bestehende Sicherungen brechen. " +
                    "Kotlin-Property umbenannt? Dann @SerialName(\"$schluessel\") setzen.",
                text.contains("\"$schluessel\":"),
            )
        }
    }

    @Test
    fun roundtrip_erhaeltAlleDaten() {
        val text = BackupSerializer.encode(beispielMitZeitachse)

        val result = BackupSerializer.decode(text)

        assertTrue(result is BackupReadResult.Success)
        assertEquals(beispielMitZeitachse, (result as BackupReadResult.Success).file)
    }

    @Test
    fun exportEnthaeltFormatVersion() {
        val text = BackupSerializer.encode(beispiel)

        assertTrue(text.contains("\"formatVersion\": 1"))
    }

    @Test
    fun unbekannteFelder_werdenIgnoriert() {
        // Simuliert eine Datei aus einer späteren App-Version mit zusätzlichem Feld.
        val text = """
            {
              "formatVersion": 1,
              "exportedAt": 5,
              "lieblingsfarbe": "blau",
              "profiles": [],
              "history": []
            }
        """.trimIndent()

        val result = BackupSerializer.decode(text)

        assertTrue(result is BackupReadResult.Success)
        assertEquals(5L, (result as BackupReadResult.Success).file.exportedAt)
    }

    @Test
    fun neueresFormat_wirdAbgelehnt() {
        val text = """{"formatVersion": 99, "exportedAt": 1, "profiles": [], "history": []}"""

        val result = BackupSerializer.decode(text)

        assertEquals(BackupReadResult.TooNew(99), result)
    }

    @Test
    fun keinJson_wirdAlsUngueltigErkannt() {
        assertTrue(BackupSerializer.decode("Das ist ein Foto.") is BackupReadResult.Invalid)
    }

    @Test
    fun leereDatei_wirdAlsUngueltigErkannt() {
        assertTrue(BackupSerializer.decode("") is BackupReadResult.Invalid)
    }

    @Test
    fun fremdesJson_wirdAlsUngueltigErkannt() {
        // Ohne Pflichtfeld "exportedAt" ist es keine Sicherung dieser App.
        assertTrue(BackupSerializer.decode("""{"foo": "bar"}""") is BackupReadResult.Invalid)
    }

    @Test
    fun fehlendeListen_werdenAlsLeerGelesen() {
        val result = BackupSerializer.decode("""{"formatVersion": 1, "exportedAt": 7}""")

        assertTrue(result is BackupReadResult.Success)
        val file = (result as BackupReadResult.Success).file
        assertTrue(file.profiles.isEmpty())
        assertTrue(file.history.isEmpty())
    }

    @Test
    fun entityUmwandlung_verliertKeineDaten_undVergibtNeueId() {
        val task = beispiel.history.single()

        val entity = task.toEntity()

        assertEquals(0, entity.id) // Room vergibt beim Einfügen eine frische ID
        // Das Beispiel stammt aus der Zeit vor dem Ertrag; auf dem Weg ins Gerät bekommt es ihn
        // nach der Regel für ältere Dateien – eine richtige Aufgabe den Ertrag 1.
        assertEquals(task.copy(ertrag = 1), entity.toBackup())
    }

    @Test
    fun profilUmwandlung_verliertKeineDaten() {
        val profile = beispiel.profiles.single()

        assertEquals(profile, profile.toEntity().toBackup())
    }

    @Test
    fun zeitabschnittUmwandlung_verliertKeineDaten_undVergibtNeueId() {
        val abschnitt = beispielMitZeitachse.bildschirmzeit.single()

        val entity = abschnitt.toEntity()

        assertEquals(0, entity.id) // Room vergibt beim Einfügen eine frische ID
        assertEquals(abschnitt, entity.toBackup())
    }

    @Test
    fun rundenabschlussUmwandlung_verliertKeineDaten_undVergibtNeueId() {
        val abschluss = beispielMitZeitachse.runden.single()

        val entity = abschluss.toEntity()

        assertEquals(0, entity.id) // Room vergibt beim Einfügen eine frische ID
        assertEquals(abschluss, entity.toBackup())
    }
}
