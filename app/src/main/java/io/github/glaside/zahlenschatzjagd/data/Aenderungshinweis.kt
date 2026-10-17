package io.github.glaside.zahlenschatzjagd.data

/**
 * Ein **Änderungshinweis**: wenige Sätze dazu, was sich mit einer Version für Kinder und Eltern
 * ändert. Der Text ist derselbe, den F-Droid zeigt
 * (`fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt`). Welche Versionen die App
 * selbst zeigt, legt `app/build.gradle.kts` fest; der Build erzeugt daraus [AENDERUNGSHINWEISE].
 */
data class Aenderungshinweis(
    val versionCode: Int,
    val versionName: String,
    val text: String,
)

/**
 * Welche Änderungshinweise beim Start zu zeigen sind, der neueste zuerst.
 *
 * - **Gemerkter Stand vorhanden:** alle Hinweise danach bis einschließlich [aktuelleVersion] –
 *   auch die übersprungener Versionen, sonst fehlte gerade die Neuerung, die darin steckte.
 * - **Kein Stand, keine Profile:** eine Neuinstallation. Geändert hat sich für niemanden etwas.
 * - **Kein Stand, aber Profile:** das erste Update auf eine Version, die sich überhaupt etwas
 *   merkt. Ein Gerät mit Profilen hat die App schon benutzt; es bekommt den Hinweis der
 *   aktuellen Version.
 *
 * [hinweise] enthält nur, was die App zeigen soll – unmarkierte Versionen fehlen darin und
 * fallen damit von selbst heraus.
 */
fun faelligeAenderungshinweise(
    zuletztGezeigt: Int?,
    aktuelleVersion: Int,
    profileVorhanden: Boolean,
    hinweise: List<Aenderungshinweis>,
): List<Aenderungshinweis> {
    val bisher = zuletztGezeigt ?: if (profileVorhanden) aktuelleVersion - 1 else aktuelleVersion
    return hinweise
        .filter { it.versionCode in (bisher + 1)..aktuelleVersion }
        .sortedByDescending { it.versionCode }
}
