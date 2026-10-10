package io.github.glaside.zahlenschatzjagd.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein zusammenhängender Zeitabschnitt der **Bildschirmzeit**: ein Profil, ein Beginn, ein Ende.
 *
 * Eine eigene Tabelle statt einer Schätzung aus der [TaskHistory] – sonst fiele alles unter den
 * Tisch, was keine beantwortete Aufgabe ist (der Weg über die Startkarte, das Zusammenstellen
 * einer Runde, das Bestaunen der Schatztruhen). Siehe
 * `docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`.
 *
 * Ein Abschnitt ist **unveränderlich**: Er wird erst geschrieben, wenn er zu Ende ist, und danach
 * nie wieder angefasst. Genau darauf baut das Zusammenführen beim Einspielen einer Sicherung
 * (Profil + [beginn] als Fingerabdruck, siehe [mergeBackup]).
 *
 * Die ausdrücklichen [ColumnInfo]-Namen entkoppeln die Kotlin-Namen von den Spaltennamen –
 * siehe `docs/adr/0005-persistierte-namen-entkoppelt.md`.
 */
@Entity(tableName = "zeitabschnitte")
data class Zeitabschnitt(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    /** Das Profil, dem diese Zeit gehört. Entspricht [UserProfile.name]. */
    @ColumnInfo(name = "profil")
    val profil: String,

    @ColumnInfo(name = "beginn")
    val beginn: Long,

    @ColumnInfo(name = "ende")
    val ende: Long,
) {
    /** Wie lange dieser Abschnitt gedauert hat. */
    val dauerMillis: Long get() = ende - beginn
}
