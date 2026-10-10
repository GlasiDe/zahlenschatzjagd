package io.github.glaside.zahlenschatzjagd.data

/** Der aktuelle Datenbestand des Geräts, gegen den eine Sicherung abgeglichen wird. */
data class BackupSnapshot(
    val profiles: List<BackupProfile> = emptyList(),
    val history: List<BackupTask> = emptyList(),
    val bildschirmzeit: List<BackupZeitabschnitt> = emptyList(),
    val runden: List<BackupRundenabschluss> = emptyList(),
)

/**
 * Was beim Einspielen tatsächlich geschrieben werden muss – plus die Zahlen für die Rückmeldung
 * im Elternbereich. Die `new…`-Listen enthalten ausschließlich Daten, die auf dem Gerät noch
 * fehlen; nichts Bestehendes wird verändert oder gelöscht.
 */
data class MergePlan(
    val newProfiles: List<BackupProfile> = emptyList(),
    val newTasks: List<BackupTask> = emptyList(),
    val newZeitabschnitte: List<BackupZeitabschnitt> = emptyList(),
    val newRunden: List<BackupRundenabschluss> = emptyList(),
    val skippedProfiles: Int = 0,
    val skippedTasks: Int = 0,
    val skippedZeitabschnitte: Int = 0,
    val skippedRunden: Int = 0,
) {
    val hasChanges: Boolean
        get() = newProfiles.isNotEmpty() || newTasks.isNotEmpty() ||
            newZeitabschnitte.isNotEmpty() || newRunden.isNotEmpty()
}

/**
 * Fasst eine Sicherung mit dem vorhandenen Bestand zusammen ("zusammenführen", nicht "ersetzen").
 *
 * Regeln:
 * - **Profile** werden über den Namen abgeglichen – der ist zugleich Primärschlüssel und die
 *   Verbindung zur Historie (siehe [UserProfile]). Ein bereits vorhandenes Profil bleibt
 *   unangetastet, insbesondere behält es seinen aktuellen Avatar.
 * - **Aufgaben** gelten als identisch, wenn Kind, Zeitstempel, Aufgabentext und gegebene Antwort
 *   übereinstimmen. Damit ist das Einspielen wiederholbar: Dieselbe Datei ein zweites Mal
 *   einzuspielen ändert nichts mehr.
 * - **Zeitabschnitte** der Bildschirmzeit werden über Profil und Beginn abgeglichen. Mehr braucht
 *   es nicht: Ein Abschnitt ist unveränderlich, und zwei Abschnitte desselben Kindes können nicht
 *   gleichzeitig beginnen. Ohne dieses Zusammenführen stünde die Bildschirmzeit nach jedem
 *   Gerätewechsel wieder auf null.
 * - **Abgeschlossene Runden** ebenso, über Profil und Zeitpunkt – aus demselben Grund: Sonst wäre
 *   das Punktemuster des Übungskalenders nach einem Gerätewechsel leer.
 *
 * Aufgaben zu einem Kind ohne passendes Profil gehen bewusst trotzdem mit – so kann ein später
 * neu angelegtes Profil mit gleichem Namen seinen alten Fortschritt wiederfinden, statt dass er
 * beim Einspielen still verworfen wird.
 */
fun mergeBackup(existing: BackupSnapshot, imported: BackupFile): MergePlan {
    val profile = neuUndBekannt(existing.profiles, imported.profiles) { it.name }
    val aufgaben = neuUndBekannt(existing.history, imported.history) { it.duplicateKey() }
    val zeiten = neuUndBekannt(existing.bildschirmzeit, imported.bildschirmzeit) { it.duplicateKey() }
    val runden = neuUndBekannt(existing.runden, imported.runden) { it.duplicateKey() }

    return MergePlan(
        newProfiles = profile.neu,
        newTasks = aufgaben.neu,
        newZeitabschnitte = zeiten.neu,
        newRunden = runden.neu,
        skippedProfiles = profile.bekannt,
        skippedTasks = aufgaben.bekannt,
        skippedZeitabschnitte = zeiten.bekannt,
        skippedRunden = runden.bekannt,
    )
}

/** Was von einer Sorte zu ergänzen ist – und wie viel davon schon dastand. */
private data class Abgleich<T>(val neu: List<T>, val bekannt: Int)

/**
 * Trennt die eingespielten Einträge einer Sorte in „fehlt noch" und „steht schon da", anhand
 * ihres Fingerabdrucks [schluessel]. Dass der Abdruck in eine Menge *eingefügt* wird statt nur
 * nachgeschlagen, deckt auch Duplikate **innerhalb der Datei** ab.
 */
private fun <T, K> neuUndBekannt(
    vorhanden: List<T>,
    eingespielt: List<T>,
    schluessel: (T) -> K,
): Abgleich<T> {
    val bekannteSchluessel = vorhanden.mapTo(mutableSetOf(), schluessel)
    val neu = mutableListOf<T>()
    var bekannt = 0
    for (eintrag in eingespielt) {
        if (bekannteSchluessel.add(schluessel(eintrag))) neu += eintrag else bekannt++
    }
    return Abgleich(neu, bekannt)
}

/**
 * Fingerabdruck einer gerechneten Aufgabe. Der Zeitstempel allein genügt nicht (mehrere Kinder
 * könnten dieselbe Millisekunde treffen), Aufgabentext allein erst recht nicht – dieselbe Aufgabe
 * wird ja absichtlich mehrfach geübt.
 *
 * **Der Ertrag gehört bewusst nicht dazu.** Eine ältere Sicherung kennt ihn nicht und bekäme ihn
 * beim Einspielen nach [ertragOhneAngabe] – für eine teilweise richtige Mauer 0 statt der 1 auf dem
 * Gerät. Gehörte er zum Abdruck, entstünde dieselbe Mauer dann ein zweites Mal
 * (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
 *
 * **Getrennt wird mit `\u0000`, nicht mit einem Leerzeichen.** Ein Leerzeichen kommt in Namen und
 * Aufgabentexten selbst vor; damit ließen sich die vier Teile nicht mehr eindeutig auseinander
 * lesen, und zwei verschiedene Aufgaben bekämen denselben Abdruck. `\u0000` kann in keinem der
 * Teile vorkommen. Bitte als Escape stehen lassen: Das Zeichen stand hier einmal wörtlich in der
 * Zeichenkette – unsichtbar im Quelltext, und Werkzeuge hielten die Datei für binär und
 * übersprangen sie bei der Suche.
 */
private fun BackupTask.duplicateKey(): String =
    listOf(userName, timestamp.toString(), questionText, userAnswer).joinToString("\u0000")

/**
 * Fingerabdruck eines Zeitabschnitts: Profil und Beginn. Das Ende gehört bewusst **nicht** dazu –
 * ein Abschnitt ist unveränderlich, und zwei Abschnitte desselben Kindes können nicht gleichzeitig
 * beginnen. Damit ist auch das Einspielen der Bildschirmzeit wiederholbar
 * (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`).
 */
private fun BackupZeitabschnitt.duplicateKey(): Pair<String, Long> = profil to beginn

/**
 * Fingerabdruck einer abgeschlossenen Runde: Profil und Zeitpunkt. Zwei Runden desselben Kindes
 * können nicht in derselben Millisekunde enden, und ein Eintrag wird nach seinem Entstehen nie
 * wieder angefasst – damit ist auch das Einspielen der Runden wiederholbar
 * (`docs/adr/0020-abgeschlossene-runden-festgehalten.md`).
 */
private fun BackupRundenabschluss.duplicateKey(): Pair<String, Long> = profil to zeitpunkt
