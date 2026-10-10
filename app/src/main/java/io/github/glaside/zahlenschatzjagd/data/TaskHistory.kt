package io.github.glaside.zahlenschatzjagd.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.glaside.zahlenschatzjagd.ERTRAG_GANZ_RICHTIG
import io.github.glaside.zahlenschatzjagd.ZahlenmauerDifficulty

/**
 * Eine beantwortete Aufgabe in der Historie.
 *
 * Jede Property trägt ein ausdrückliches [ColumnInfo] mit ihrem Spaltennamen: Der Kotlin-Name
 * ist damit vom Datenbank-Namen **entkoppelt**. Ein Umbenennen im Code (z. B. beim Schärfen
 * der Begriffe aus `CONTEXT.md`) ändert die Tabelle dann nicht mehr und kostet keinem Kind
 * seinen Fortschritt. Die Spaltennamen selbst sind eingefroren – siehe
 * `docs/adr/0005-persistierte-namen-entkoppelt.md`.
 */
// @Entity sagt Room: "Bitte mach aus dieser Klasse eine echte Datenbank-Tabelle!"
@Entity(tableName = "task_history")
data class TaskHistory(
    // @PrimaryKey sorgt dafür, dass jede Aufgabe eine eindeutige, fortlaufende Nummer bekommt (1, 2, 3...)
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "userName")
    val userName: String, // z.B. "Emma"

    /**
     * Die **Fortschrittskennung** der Aufgabe, z. B. `"PLUS"`, `"VERLIEBTE_ZAHLEN"`,
     * `"ZAHLENMAUER_ULTRA"`. Diese Werte stehen in den Datenbanken und Sicherungsdateien der
     * Kinder; die Abzeichen werden über sie gezählt. Katalog und Schutz-Test: `Badges.kt`
     * (`BADGE_CATEGORIES`) bzw. `BadgesTest.kategorieKeys_sindEingefroren`.
     */
    @ColumnInfo(name = "taskCategory")
    val taskCategory: String,

    @ColumnInfo(name = "numberRange")
    val numberRange: Int, // gewählter Zahlenraum der Runde: 10 / 20 / 50 / 100

    @ColumnInfo(name = "questionText")
    val questionText: String, // z.B. "3 + _ = 10"

    @ColumnInfo(name = "expectedAnswer")
    val expectedAnswer: String, // z.B. "7"

    @ColumnInfo(name = "userAnswer")
    val userAnswer: String, // Was hat das Kind getippt?

    @ColumnInfo(name = "isCorrect")
    val isCorrect: Boolean, // War es richtig? (true/false)

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(), // Speichert automatisch die aktuelle Uhrzeit in Millisekunden

    /**
     * Der **Ertrag**: wie viel diese Aufgabe zu ihrem Abzeichen beiträgt. Bei den meisten
     * Übungsformen 1 für richtig und 0 für falsch – daher der Vorgabewert. Nur die Zahlenmauer
     * setzt ihn ausdrücklich, weil sie gestaffelt zählt
     * (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
     */
    @ColumnInfo(name = "ertrag", defaultValue = "0")
    val ertrag: Int = if (isCorrect) 1 else 0,
)

/**
 * Der Ertrag einer Aufgabe, die ihn nicht selbst nennt – sie stammt aus der Zeit vor dem Ertrag.
 * Gilt für das Einspielen einer älteren Sicherung und deckt sich mit der Migration der Datenbank
 * (`AppDatabase`): **Rückwirkend nur nach oben.** Eine ganz richtige Mauer bekommt den vollen
 * Ertrag, eine falsche bleibt bei 0 – ob sie teilweise richtig war, lässt sich nachträglich nicht
 * verlässlich sagen (`docs/adr/0027-ertrag-der-zahlenmauer-gestaffelt.md`).
 */
fun ertragOhneAngabe(kennung: String, isCorrect: Boolean): Int = when {
    !isCorrect -> 0
    kennung in ZAHLENMAUER_KENNUNGEN -> ERTRAG_GANZ_RICHTIG
    else -> 1
}

private val ZAHLENMAUER_KENNUNGEN: Set<String> = ZahlenmauerDifficulty.entries.mapTo(HashSet()) { it.kennung }
