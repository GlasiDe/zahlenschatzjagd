package io.github.glaside.zahlenschatzjagd.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein Kinder-Profil. Der [name] ist zugleich der Primärschlüssel und die Verbindung zur
 * [TaskHistory] (dort `userName`) – so bleibt der Eingriff in die bestehende, namensbasierte
 * Historie minimal. Namen sind dadurch eindeutig; das Umbenennen wird kaskadierend auf die
 * Historie gespiegelt (siehe [UserProfileDao.rename]), damit der Fortschritt am Profil hängt.
 *
 * [avatarId] ist der stabile Schlüssel eines Avatars aus der Registry
 * [io.github.glaside.zahlenschatzjagd.ui.profile.Avatars] (kein Ressourcen-Int, damit die DB
 * unabhängig von generierten `R`-Werten bleibt).
 *
 * Die ausdrücklichen [ColumnInfo]-Namen entkoppeln die Kotlin-Namen von den Spaltennamen –
 * siehe `docs/adr/0005-persistierte-namen-entkoppelt.md`.
 *
 * Die **Freigabe** liegt als zwei Spalten am Profil selbst statt in einer eigenen Tabelle: So
 * folgt sie dem Umbenennen und dem Löschen ohne weitere Kaskade. Gespeichert wird jeweils, was
 * **abgewählt** ist – siehe [freigabe] und `docs/adr/0018-freigabe-als-sperre-gespeichert.md`.
 */
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "avatarId")
    val avatarId: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "gesperrteKennungen", defaultValue = "")
    val gesperrteKennungen: String = "",

    @ColumnInfo(name = "gesperrteZahlenraeume", defaultValue = "")
    val gesperrteZahlenraeume: String = "",
)

/** Was dieses Profil üben darf. Ein Profil ohne Eintrag darf alles. */
val UserProfile.freigabe: Freigabe
    get() = freigabeAusSperren(gesperrteKennungen, gesperrteZahlenraeume)

/**
 * Die Freigabe eines Profils aus einer Liste. Ein Name ohne Profil – etwa der Vorgabe-Name auf
 * einem noch leeren Gerät – darf alles: Die Freigabe soll nichts verstecken, solange es
 * niemanden gibt, für den sie gesetzt worden wäre.
 */
fun List<UserProfile>.freigabeVon(profilName: String): Freigabe =
    firstOrNull { it.name == profilName }?.freigabe ?: Freigabe.ALLES
