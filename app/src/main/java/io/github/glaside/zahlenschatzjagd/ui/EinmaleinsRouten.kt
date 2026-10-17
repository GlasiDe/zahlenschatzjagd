package io.github.glaside.zahlenschatzjagd.ui

import androidx.navigation.NavBackStackEntry
import io.github.glaside.zahlenschatzjagd.EinmaleinsSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.EinmaleinsSettings
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.encode

/**
 * Die Pfade der Einmaleins-Runde und ihrer Auswertung (Muster: [Routes.EINMALEINS_GAME],
 * [Routes.EINMALEINS_STATS]). Neben [Routes] statt darin, weil der Navigationsgraph sonst über die
 * Grenze wüchse, ab der detekt eine Datei für zu voll hält.
 *
 * Die Reihen stehen durch Komma getrennt im Pfad, etwa `einmaleins_game/AUFSAGEN/3,7/10`.
 */
internal fun einmaleinsGameRoute(s: EinmaleinsSettings): String = "einmaleins_game/${s.pfadteil()}"

internal fun einmaleinsStatsRoute(
    richtige: Int,
    gesamt: Int,
    s: EinmaleinsSettings,
    unlocked: List<UnlockedBadge>,
): String {
    val encoded = if (unlocked.isEmpty()) "none" else unlocked.joinToString(",") { it.encode() }
    return "einmaleins_stats/$richtige/$gesamt/${s.pfadteil()}/$encoded"
}

private fun EinmaleinsSettings.pfadteil(): String =
    "${schwierigkeitsstufe.name}/${reihen.joinToString(",")}/$aufgabenzahl"

internal fun NavBackStackEntry.toEinmaleinsSettings(): EinmaleinsSettings {
    val args = requireNotNull(arguments)
    val stufenName = requireNotNull(args.getString("schwierigkeitsstufe"))
    return EinmaleinsSettings(
        schwierigkeitsstufe = EinmaleinsSchwierigkeitsstufe.valueOf(stufenName),
        reihen = requireNotNull(args.getString("reihen")).split(",").map(String::toInt),
        aufgabenzahl = args.getInt("aufgabenzahl"),
    )
}
