package io.github.glaside.zahlenschatzjagd.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prüft, dass die Sicherung **vollständig** ist – dass also keine Datenart auf dem Weg zum neuen
 * Gerät still verlorengeht. Das unterscheidet ihn von [BackupRepositoryTest], der das *Verhalten*
 * prüft (führt zusammen, ist wiederholbar); hier geht es allein um Deckung.
 *
 * Der Test nennt dafür bewusst **keine einzige Datenart beim Namen** und nimmt neue deshalb von
 * selbst mit. Er steht auf zwei Beinen:
 *
 * 1. **Der Rundlauf**: sichern, auf ein leeres Gerät einspielen, erneut sichern, die beiden
 *    Dateien Feld für Feld vergleichen. Was hineinging, muss wieder herauskommen. Fehlt einer
 *    Datenart ihr `insertAll` in [BackupRepository.restore], kommt sie leer zurück und der
 *    Vergleich fällt – anders als die Rückmeldung im Elternbereich, die zählt, was geschrieben
 *    werden *sollte*, nicht was ankam.
 * 2. **Der Deckungs-Wächter**: Der Rundlauf allein hätte ein Loch – wird eine Datenart in
 *    *beiden* Richtungen vergessen, geht leer hinein und kommt leer heraus. Deshalb läuft der
 *    Test zusätzlich über den Serialisierungs-Bauplan von [BackupFile] und besteht darauf, dass
 *    die Testdaten **jedes** Feld befüllen.
 *
 * Bewacht sind absichtlich *alle* Felder der Sicherungsdatei selbst, nicht nur ihre Listen: Wer
 * eines ergänzt – gleich welcher Form –, muss es entweder befüllen oder bewusst in [AUSGENOMMEN]
 * eintragen. Neues ist damit bewacht, bis jemand widerspricht, statt unbewacht, bis jemand daran
 * denkt. (Die Felder *innerhalb* einer Datenart, etwa die eines Profils, zählt er nicht einzeln
 * durch – dort wacht der Rundlauf, und über die eingefrorenen Namen `BackupSerializationTest`.)
 *
 * **Was „befüllt" heißt, wird gemessen, nicht geraten.** Der Wächter vergleicht die Sicherung der
 * gefüllten Datenbank mit der einer leeren: Was sich nicht unterscheidet, hat von den Testdaten
 * nichts abbekommen. Eine Liste bliebe leer, ein Einzelwert stünde auf seiner Vorgabe – ohne dass
 * der Test wissen müsste, wie diese Vorgabe aussieht. Ein Feld mit sinnvoll *belegter* Vorgabe
 * (`= true`, `= 3`) rutscht so nicht durch, nur weil die Vorgabe nicht nach „leer" aussieht.
 *
 * **Bekannte Grenze:** Er braucht Room und läuft deshalb unter `connectedAndroidTest`, also nicht
 * im Pre-Commit-Tor. Er greift erst, wenn ein Gerät angeschlossen ist.
 */
@RunWith(AndroidJUnit4::class)
class SicherungVollstaendigkeitTest {

    private lateinit var altesGeraet: AppDatabase
    private lateinit var neuesGeraet: AppDatabase

    @Before
    fun setup() {
        altesGeraet = leereDatenbank()
        neuesGeraet = leereDatenbank()
    }

    @After
    fun tearDown() {
        altesGeraet.close()
        neuesGeraet.close()
    }

    /**
     * Der Rundlauf. Jedes Feld wird einzeln verglichen, damit der Fehlschlag die fehlende Datenart
     * beim Namen nennt – ein nackter Objektvergleich ließe den Nächsten von Hand suchen.
     */
    @Test
    fun rundlauf_gibtJedeDatenartWiederHeraus() = runBlocking {
        fuelleTestdaten(altesGeraet)
        val hinein = BackupRepository(altesGeraet).createBackup(APP_VERSION)

        BackupRepository(neuesGeraet).restore(hinein)
        val heraus = BackupRepository(neuesGeraet).createBackup(APP_VERSION)

        val vorher = hinein.alsFelder()
        val nachher = heraus.alsFelder()
        for (feld in imRundlaufVerglichen) {
            assertEquals(
                "Die Datenart „$feld\" kam nicht wieder heraus, wie sie hineinging. Fehlt ihr " +
                    "das insertAll in BackupRepository.restore oder ihre Zeile in createBackup?",
                ohneReihenfolge(vorher[feld]),
                ohneReihenfolge(nachher[feld]),
            )
        }
    }

    /**
     * Der Deckungs-Wächter. Er prüft nicht die Sicherung, sondern die Testdaten dieses Tests: Sind
     * sie lückenhaft, prüft der Rundlauf darüber ins Leere.
     *
     * Der Maßstab für „leer" ist die Sicherung eines Geräts, auf dem nie geübt wurde. Damit muss
     * der Test die Vorgabewerte der Felder nicht kennen – und ein Feld, dessen Vorgabe wie ein
     * echter Wert aussieht, kann nicht unbemerkt durchrutschen.
     */
    @Test
    fun deckung_testdatenBefuellenJedesFeldDerSicherung() = runBlocking {
        fuelleTestdaten(altesGeraet)

        val mitTestdaten = BackupRepository(altesGeraet).createBackup(APP_VERSION).alsFelder()
        val frischesGeraet = BackupRepository(neuesGeraet).createBackup(APP_VERSION).alsFelder()

        for (feld in vonDenTestdatenZuBefuellen) {
            assertNotEquals(
                "Das Feld „$feld\" steht in der Sicherung genauso da wie auf einem Gerät, auf " +
                    "dem nie geübt wurde – der Rundlauf prüft darüber ins Leere. Entweder in " +
                    "fuelleTestdaten befüllen und in createBackup mitnehmen, oder, wenn es " +
                    "bewusst nicht gesichert wird, namentlich in AUSGENOMMEN eintragen.",
                frischesGeraet[feld],
                mitTestdaten[feld],
            )
        }
    }

    private fun leereDatenbank(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    /**
     * Der Bestand, mit dem der Rundlauf fährt. **Jede Datenart der Sicherung muss hier vorkommen**
     * – der Deckungs-Wächter besteht darauf. Zwei Profile, damit ein Zuordnungsfehler zwischen
     * Kindern nicht als richtig durchginge, und ein abgewähltes Stück Freigabe je Profil, damit
     * auch die späteren Felder eines Profils nicht leer durch den Vergleich rutschen.
     *
     * Ähnelt absichtlich dem Bestand in [BackupRepositoryTest], ohne ihn zu teilen: Dort dürfen
     * die Testdaten frei gewählt werden, weil sie *Verhalten* zeigen sollen; hier sind sie an den
     * Wächter gebunden. Ein gemeinsamer Helfer machte den einen Bestand zur Geisel des anderen.
     */
    private suspend fun fuelleTestdaten(db: AppDatabase) {
        db.userProfileDao().insert(
            UserProfile(
                name = "Mira",
                avatarId = "avatar_pirat_8",
                createdAt = 100,
                gesperrteKennungen = "ZAHLENMAUER_SCHWER",
                gesperrteZahlenraeume = "100",
            )
        )
        db.userProfileDao().insert(
            UserProfile(
                name = "Chris",
                avatarId = "avatar_pirat_5",
                createdAt = 200,
                gesperrteKennungen = "VERDOPPELN",
                gesperrteZahlenraeume = "50",
            )
        )
        db.taskHistoryDao().insertAll(
            listOf(
                aufgabe("Mira", timestamp = 1),
                aufgabe("Mira", timestamp = 2, richtig = false),
                aufgabe("Chris", timestamp = 3),
                // Eine teilweise richtige Mauer: Ihr Ertrag 1 lässt sich aus `isCorrect` nicht
                // ablesen. Ginge er unterwegs verloren, käme sie mit 0 wieder heraus (ADR-0027).
                aufgabe("Mira", timestamp = 4, richtig = false).copy(
                    taskCategory = "ZAHLENMAUER_MITTEL",
                    numberRange = 50,
                    ertrag = 1,
                ),
            )
        )
        db.zeitabschnittDao().insertAll(
            listOf(
                Zeitabschnitt(profil = "Mira", beginn = 1_000, ende = 61_000),
                Zeitabschnitt(profil = "Chris", beginn = 2_000, ende = 32_000),
            )
        )
        db.rundenabschlussDao().insertAll(
            listOf(
                Rundenabschluss(profil = "Mira", zeitpunkt = 61_000),
                Rundenabschluss(profil = "Chris", zeitpunkt = 32_000),
            )
        )
    }

    private fun aufgabe(profil: String, timestamp: Long, richtig: Boolean = true) = TaskHistory(
        userName = profil,
        taskCategory = "VERLIEBTE_ZAHLEN",
        numberRange = 10,
        questionText = "3 + _ = 10",
        expectedAnswer = "7",
        userAnswer = if (richtig) "7" else "6",
        isCorrect = richtig,
        timestamp = timestamp,
    )

    /**
     * Die Sicherung aufgeschlüsselt nach den Schlüsseln, unter denen sie tatsächlich in der Datei
     * steht. Über die geschriebene Datei statt über die Kotlin-Objekte, weil erst sie zeigt, was
     * beim Kind auf dem neuen Gerät ankommt.
     */
    private fun BackupFile.alsFelder(): Map<String, JsonElement> =
        Json.parseToJsonElement(BackupSerializer.encode(this)).jsonObject

    private companion object {

        private const val APP_VERSION = "1.2"

        /**
         * Die drei Felder, von denen der Deckungs-Wächter keine Testdaten verlangt: Die
         * Formatversion ist keine Datenart, und Zeitpunkt und App-Version der Sicherung stammen
         * nicht aus der Datenbank. **Nur hier darf etwas hinzukommen, und nur mit Begründung.**
         */
        val AUSGENOMMEN = setOf("formatVersion", "exportedAt", "appVersionName")

        /** Alle Felder der Sicherungsdatei, aus ihrem Serialisierungs-Bauplan gelesen statt von
         * Hand aufgezählt. Genau das nimmt eine neue Datenart von selbst mit. */
        private val felderDerDatei: List<String> =
            BackupFile.serializer().descriptor.let { bauplan ->
                (0 until bauplan.elementsCount).map(bauplan::getElementName)
            }

        /**
         * Was der Rundlauf vergleicht. Draußen bleiben allein die beiden Felder, die sich zwischen
         * zwei Sicherungen naturgemäß unterscheiden – die Formatversion gehört ausdrücklich
         * **nicht** dazu: Käme eine andere zurück, als hineinging, wäre das ein Befund.
         */
        val imRundlaufVerglichen: List<String> =
            felderDerDatei - setOf("exportedAt", "appVersionName")

        /** Was die Testdaten befüllen müssen. */
        val vonDenTestdatenZuBefuellen: List<String> = felderDerDatei - AUSGENOMMEN

        /**
         * Der Wert eines Feldes so aufbereitet, dass nur der Inhalt zählt: Listen kommen sortiert
         * zurück, denn in welcher Reihenfolge die Datenbank ihre Zeilen herausgibt, ist keine
         * Eigenschaft des Fortschritts.
         */
        fun ohneReihenfolge(wert: JsonElement?): Any? =
            if (wert is JsonArray) wert.map { it.toString() }.sorted() else wert
    }
}
