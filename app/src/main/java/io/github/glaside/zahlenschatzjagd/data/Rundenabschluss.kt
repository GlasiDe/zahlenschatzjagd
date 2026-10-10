package io.github.glaside.zahlenschatzjagd.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Eine **abgeschlossene Runde**: ein Profil, ein Zeitpunkt. Das ist der Punkt, den der
 * Übungskalender an einem Tag setzt.
 *
 * Ein eigener Eintrag statt einer Ableitung aus der [TaskHistory] – die weiß nicht, wie viele
 * Aufgaben eine Runde hatte, und kann darum eine durchgezogene nicht von einer abgebrochenen
 * unterscheiden. Genau das ist hier aber die Frage: Ein Tag soll sich nicht schon färben, wenn
 * das Kind die App öffnet und einmal danebentippt. Siehe
 * `docs/adr/0020-abgeschlossene-runden-festgehalten.md`.
 *
 * Wie der [Zeitabschnitt] ist der Eintrag **unveränderlich**: Er entsteht mit dem Ende der Runde
 * und wird danach nie wieder angefasst. Darauf beruht das Zusammenführen beim Einspielen einer
 * Sicherung (Profil + [zeitpunkt] als Fingerabdruck, siehe [mergeBackup]).
 *
 * Die ausdrücklichen [ColumnInfo]-Namen entkoppeln die Kotlin-Namen von den Spaltennamen –
 * siehe `docs/adr/0005-persistierte-namen-entkoppelt.md`.
 */
@Entity(tableName = "rundenabschluesse")
data class Rundenabschluss(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    /** Das Profil, das die Runde geübt hat. Entspricht [UserProfile.name]. */
    @ColumnInfo(name = "profil")
    val profil: String,

    /** Wann die Runde zu Ende war – nicht wann sie begann. */
    @ColumnInfo(name = "zeitpunkt")
    val zeitpunkt: Long,
)
