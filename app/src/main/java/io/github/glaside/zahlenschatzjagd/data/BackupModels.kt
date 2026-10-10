package io.github.glaside.zahlenschatzjagd.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Das Dateiformat der Sicherung (Elternbereich → "Sicherung erstellen").
 *
 * Bewusst eigene Datenklassen statt der Room-Entities: Die technische [TaskHistory.id]
 * (autoGenerate) darf nicht mitwandern, sie wird beim Einspielen neu vergeben. Außerdem bleibt
 * das Dateiformat so unabhängig von künftigen Änderungen am Datenbank-Schema.
 *
 * [formatVersion] erlaubt spätere Formatänderungen: Eine Datei aus einer *neueren* App-Version
 * wird erkannt und mit einer verständlichen Meldung abgelehnt, statt Daten falsch zu deuten.
 *
 * **Jedes Feld trägt ein ausdrückliches [SerialName]** mit seinem JSON-Schlüssel. Damit ist der
 * Kotlin-Name vom Dateiformat entkoppelt: Umbenennen im Code lässt bestehende Sicherungsdateien
 * auf den Geräten der Familien lesbar. Die JSON-Schlüssel selbst sind eingefroren und durch
 * `BackupSerializationTest.goldenFile_*` abgesichert – siehe
 * `docs/adr/0005-persistierte-namen-entkoppelt.md`.
 */
@Serializable
data class BackupFile(
    @SerialName("formatVersion") val formatVersion: Int = CURRENT_FORMAT_VERSION,
    @SerialName("exportedAt") val exportedAt: Long,
    @SerialName("appVersionName") val appVersionName: String = "",
    @SerialName("profiles") val profiles: List<BackupProfile> = emptyList(),
    @SerialName("history") val history: List<BackupTask> = emptyList(),
    /**
     * Die gemessene **Bildschirmzeit**. Kam später dazu und trägt deshalb – wie die beiden
     * Freigabe-Felder – einen Vorgabewert: Eine ältere Sicherungsdatei kennt sie nicht und wird
     * als „noch keine Messung" gelesen, das Dateiformat bleibt bei Version 1.
     */
    @SerialName("bildschirmzeit") val bildschirmzeit: List<BackupZeitabschnitt> = emptyList(),
    /**
     * Die **abgeschlossenen Runden**, aus denen der Übungskalender seine Punkte setzt. Wie die
     * Bildschirmzeit mit Vorgabewert: Ohne sie stünde der Kalender nach einem Gerätewechsel wieder
     * leer da, obwohl die Historie mitgewandert ist – siehe
     * `docs/adr/0020-abgeschlossene-runden-festgehalten.md`.
     */
    @SerialName("runden") val runden: List<BackupRundenabschluss> = emptyList(),
) {
    companion object {
        /** Version des Dateiformats, die diese App schreibt und lesen kann. */
        const val CURRENT_FORMAT_VERSION = 1
    }
}

/**
 * Ein Kinder-Profil in der Sicherungsdatei (Gegenstück zu [UserProfile]).
 *
 * Die beiden Freigabe-Felder tragen einen Vorgabewert und kamen später dazu: Eine ältere
 * Sicherungsdatei kennt sie nicht und wird als „nichts abgewählt" gelesen – das Dateiformat
 * bleibt deshalb bei Version 1.
 */
@Serializable
data class BackupProfile(
    @SerialName("name") val name: String,
    @SerialName("avatarId") val avatarId: String,
    @SerialName("createdAt") val createdAt: Long,
    @SerialName("gesperrteKennungen") val gesperrteKennungen: String = "",
    @SerialName("gesperrteZahlenraeume") val gesperrteZahlenraeume: String = "",
)

/**
 * Eine gerechnete Aufgabe in der Sicherungsdatei (Gegenstück zu [TaskHistory], ohne `id`).
 *
 * [ertrag] kam später dazu und ist deshalb optional: Eine ältere Sicherungsdatei kennt ihn nicht,
 * und beim Einspielen gilt für sie [ertragOhneAngabe] – dieselbe Regel wie für die Migration der
 * Datenbank. Zum Fingerabdruck beim Zusammenführen gehört er **nicht** (siehe `BackupMerge.kt`).
 */
@Serializable
data class BackupTask(
    @SerialName("userName") val userName: String,
    @SerialName("taskCategory") val taskCategory: String,
    @SerialName("numberRange") val numberRange: Int,
    @SerialName("questionText") val questionText: String,
    @SerialName("expectedAnswer") val expectedAnswer: String,
    @SerialName("userAnswer") val userAnswer: String,
    @SerialName("isCorrect") val isCorrect: Boolean,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("ertrag") val ertrag: Int? = null,
)

/**
 * Ein Abschnitt der Bildschirmzeit in der Sicherungsdatei (Gegenstück zu [Zeitabschnitt], ohne
 * `id`). Profil und Beginn sind zugleich sein Fingerabdruck beim Zusammenführen.
 */
@Serializable
data class BackupZeitabschnitt(
    @SerialName("profil") val profil: String,
    @SerialName("beginn") val beginn: Long,
    @SerialName("ende") val ende: Long,
)

/**
 * Eine abgeschlossene Runde in der Sicherungsdatei (Gegenstück zu [Rundenabschluss], ohne `id`).
 * Profil und Zeitpunkt sind zugleich ihr Fingerabdruck beim Zusammenführen.
 */
@Serializable
data class BackupRundenabschluss(
    @SerialName("profil") val profil: String,
    @SerialName("zeitpunkt") val zeitpunkt: Long,
)

fun UserProfile.toBackup(): BackupProfile =
    BackupProfile(name, avatarId, createdAt, gesperrteKennungen, gesperrteZahlenraeume)

fun BackupProfile.toEntity(): UserProfile =
    UserProfile(name, avatarId, createdAt, gesperrteKennungen, gesperrteZahlenraeume)

fun TaskHistory.toBackup(): BackupTask = BackupTask(
    userName = userName,
    taskCategory = taskCategory,
    numberRange = numberRange,
    questionText = questionText,
    expectedAnswer = expectedAnswer,
    userAnswer = userAnswer,
    isCorrect = isCorrect,
    timestamp = timestamp,
    ertrag = ertrag,
)

/** `id = 0` lässt Room beim Einfügen eine neue, kollisionsfreie ID vergeben. */
fun BackupTask.toEntity(): TaskHistory = TaskHistory(
    userName = userName,
    taskCategory = taskCategory,
    numberRange = numberRange,
    questionText = questionText,
    expectedAnswer = expectedAnswer,
    userAnswer = userAnswer,
    isCorrect = isCorrect,
    timestamp = timestamp,
    ertrag = ertrag ?: ertragOhneAngabe(taskCategory, isCorrect),
)

fun Zeitabschnitt.toBackup(): BackupZeitabschnitt = BackupZeitabschnitt(profil, beginn, ende)

/** `id = 0` lässt Room beim Einfügen eine neue, kollisionsfreie ID vergeben. */
fun BackupZeitabschnitt.toEntity(): Zeitabschnitt =
    Zeitabschnitt(profil = profil, beginn = beginn, ende = ende)

fun Rundenabschluss.toBackup(): BackupRundenabschluss = BackupRundenabschluss(profil, zeitpunkt)

/** `id = 0` lässt Room beim Einfügen eine neue, kollisionsfreie ID vergeben. */
fun BackupRundenabschluss.toEntity(): Rundenabschluss =
    Rundenabschluss(profil = profil, zeitpunkt = zeitpunkt)

/** Ergebnis des Einlesens einer Sicherungsdatei – bewusst ohne Exceptions, damit die UI jeden
 * Fall gezielt und freundlich formulieren kann. */
sealed interface BackupReadResult {
    data class Success(val file: BackupFile) : BackupReadResult

    /** Die Datei stammt aus einer neueren App-Version und wird nicht verstanden. */
    data class TooNew(val formatVersion: Int) : BackupReadResult

    /**
     * Kein gültiges JSON bzw. keine Sicherungsdatei dieser App. [reason] hält die technische
     * Ursache fest – sie wird nicht ins UI gehoben, geht aber auch nicht verloren.
     */
    data class Invalid(val reason: String? = null) : BackupReadResult
}

/**
 * Liest und schreibt [BackupFile] als JSON. `prettyPrint` hält die Datei für Menschen lesbar
 * (sie darf notfalls von Hand geprüft werden), `ignoreUnknownKeys` macht sie abwärtskompatibel:
 * Felder, die eine spätere App-Version ergänzt, lassen ältere Installationen nicht scheitern.
 */
object BackupSerializer {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(file)

    fun decode(text: String): BackupReadResult = try {
        val file = json.decodeFromString<BackupFile>(text)
        if (file.formatVersion > BackupFile.CURRENT_FORMAT_VERSION) {
            BackupReadResult.TooNew(file.formatVersion)
        } else {
            BackupReadResult.Success(file)
        }
    } catch (e: SerializationException) {
        BackupReadResult.Invalid(e.message)
    } catch (e: IllegalArgumentException) {
        BackupReadResult.Invalid(e.message)
    }
}
