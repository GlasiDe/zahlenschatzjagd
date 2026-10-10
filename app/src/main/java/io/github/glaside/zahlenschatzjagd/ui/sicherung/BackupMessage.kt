package io.github.glaside.zahlenschatzjagd.ui.sicherung

import io.github.glaside.zahlenschatzjagd.data.BackupFile
import io.github.glaside.zahlenschatzjagd.data.MergePlan
import io.github.glaside.zahlenschatzjagd.ui.parent.rundenAnzahl

/** Rückmeldung zu einer Sicherung – Text plus "war das ein Problem?". */
data class BackupMessage(val text: String, val isError: Boolean = false)

// "1 Runde" / "3 Runden" nennt [rundenAnzahl] – dieselbe Regel wie im Übungskalender, deshalb
// dort und nicht hier noch einmal.

/** "1 Profil" / "3 Profile" – kleine Helfer, damit die Meldungen sauberes Deutsch bleiben. */
internal fun profileCount(count: Int): String = if (count == 1) "1 Profil" else "$count Profile"

internal fun taskCount(count: Int): String = if (count == 1) "1 Aufgabe" else "$count Aufgaben"

/** Die gemessene Bildschirmzeit, gezählt in ihren [io.github.glaside.zahlenschatzjagd.data.Zeitabschnitt]en. */
internal fun zeitabschnittCount(count: Int): String =
    if (count == 1) "1 Zeitabschnitt" else "$count Zeitabschnitte"

/** "A", "A und B", "A, B und C" – damit die Aufzählung auch mit drei Teilen Deutsch bleibt. */
internal fun aufzaehlung(teile: List<String>): String = when (teile.size) {
    0, 1 -> teile.joinToString()
    else -> teile.dropLast(1).joinToString(", ") + " und " + teile.last()
}

/** Meldung nach dem Erstellen einer Sicherung – sie nennt alles, was in der Datei steht. */
fun exportMessage(file: BackupFile): BackupMessage = BackupMessage(
    "Sicherung gespeichert: " + aufzaehlung(
        listOf(
            profileCount(file.profiles.size),
            taskCount(file.history.size),
            rundenAnzahl(file.runden.size),
            zeitabschnittCount(file.bildschirmzeit.size),
        )
    ) + "."
)

/**
 * Meldung nach dem Einspielen. Wichtig für das Vertrauen der Eltern: Es steht ausdrücklich da,
 * wenn nichts ergänzt wurde, weil bereits alles vorhanden war – das ist kein Fehler, sondern
 * der Normalfall beim zweiten Einspielen derselben Datei.
 *
 * Davon getrennt der Fall, dass die Datei **selbst** nichts enthielt: Auf einem frisch
 * installierten Gerät wäre „war bereits alles vorhanden" schlicht falsch. Unterscheiden lassen
 * sich beide daran, ob es etwas zu überspringen gab.
 */
fun restoreMessage(plan: MergePlan): BackupMessage {
    if (!plan.hasChanges) {
        return BackupMessage(
            if (plan.uebersprungen == 0) {
                "Diese Sicherung enthält keinen Fortschritt – es gab nichts zu ergänzen."
            } else {
                "Es war bereits alles vorhanden – nichts wurde verändert."
            }
        )
    }
    val added = aufzaehlung(
        buildList {
            if (plan.newProfiles.isNotEmpty()) add(profileCount(plan.newProfiles.size))
            if (plan.newTasks.isNotEmpty()) add(taskCount(plan.newTasks.size))
            if (plan.newRunden.isNotEmpty()) add(rundenAnzahl(plan.newRunden.size))
            if (plan.newZeitabschnitte.isNotEmpty()) {
                add(zeitabschnittCount(plan.newZeitabschnitte.size))
            }
        }
    )
    val suffix = when (val skipped = plan.uebersprungen) {
        0 -> ""
        1 -> " 1 Eintrag war schon vorhanden."
        else -> " $skipped Einträge waren schon vorhanden."
    }
    return BackupMessage("Eingespielt: $added ergänzt.$suffix")
}

/**
 * Wie viele Einträge der Sicherung schon dastanden – Profile, Aufgaben, Zeitabschnitte und
 * Rundenabschlüsse zusammengezählt.
 */
private val MergePlan.uebersprungen: Int
    get() = skippedProfiles + skippedTasks + skippedZeitabschnitte + skippedRunden

/** Die Datei stammt aus einer neueren App-Version. */
fun tooNewMessage(formatVersion: Int): BackupMessage = BackupMessage(
    "Diese Sicherung wurde mit einer neueren Version der Zahlenschatzjagd erstellt " +
        "(Format $formatVersion). Bitte zuerst die App aktualisieren.",
    isError = true
)

/** Die Datei ist keine (lesbare) Sicherung dieser App. */
fun invalidFileMessage(): BackupMessage = BackupMessage(
    "Diese Datei konnte nicht gelesen werden. Bitte wähle eine Sicherungsdatei der " +
        "Zahlenschatzjagd aus (Endung .json).",
    isError = true
)

/**
 * Die Datei ließ sich gar nicht erst öffnen (gelöscht, kein Zugriff, Speicher voll). [reason]
 * ist die technische Ursache – knapp angehängt, weil sie bei genau diesem Fehler oft der
 * entscheidende Hinweis ist ("Speicher voll", "Permission denied").
 */
fun fileAccessMessage(reason: String? = null): BackupMessage = BackupMessage(
    "Auf die Datei konnte nicht zugegriffen werden. Bitte versuche es noch einmal." +
        (reason?.let { " ($it)" } ?: ""),
    isError = true
)
