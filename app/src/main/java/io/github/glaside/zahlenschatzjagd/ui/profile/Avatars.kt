package io.github.glaside.zahlenschatzjagd.ui.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges

/**
 * Ein wählbarer Kinderpiraten-Avatar. Die [id] ist der stabile, in [io.github.glaside.zahlenschatzjagd.data.UserProfile]
 * gespeicherte Schlüssel (unabhängig von generierten `R`-Werten); [res] ist die zugehörige
 * Grafik. Herkunft/Seeds/Prompts der Bilder stehen in `ASSETS.md`.
 */
data class AvatarOption(
    val id: String,
    @param:DrawableRes val res: Int,
    val label: String,
)

/** Registry aller verfügbaren Avatare. Reihenfolge = Anzeige-Reihenfolge im Auswahlraster. */
object Avatars {
    val all: List<AvatarOption> = listOf(
        AvatarOption("avatar_pirat_1", R.drawable.avatar_pirat_1, "Roter Freibeuter"),
        AvatarOption("avatar_pirat_2", R.drawable.avatar_pirat_2, "Kapitänin mit Feder"),
        AvatarOption("avatar_pirat_3", R.drawable.avatar_pirat_3, "Goldohrring-Pirat"),
        AvatarOption("avatar_pirat_4", R.drawable.avatar_pirat_4, "Piratin mit Tuch"),
        AvatarOption("avatar_pirat_5", R.drawable.avatar_pirat_5, "Augenklappen-Pirat"),
        AvatarOption("avatar_pirat_6", R.drawable.avatar_pirat_6, "Seglerin mit Hut"),
        AvatarOption("avatar_pirat_7", R.drawable.avatar_pirat_7, "Papageien-Freund"),
        AvatarOption("avatar_pirat_8", R.drawable.avatar_pirat_8, "Rote Kapitänin"),
    )

    /** Fester Standard-Avatar (auch der Seed-Avatar der Migration 2→3). */
    const val DEFAULT_ID: String = "avatar_pirat_1"

    /** Ressourcen-Id zu einer [id], mit Rückfall auf den Standard-Avatar. */
    @DrawableRes
    fun resFor(id: String): Int =
        all.firstOrNull { it.id == id }?.res ?: R.drawable.avatar_pirat_1
}

/**
 * Zeigt einen Avatar rund gerahmt an. Blendet die rechteckigen Ränder der generierten
 * Pergament-Grafiken radial weich zu einem Kreis aus – dieselbe Technik wie bei den
 * Insel-Kacheln auf der Startkarte (`HomeScreen.featherEdges`).
 */
@Composable
fun AvatarImage(
    avatarId: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(Avatars.resFor(avatarId)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .featherEdges()
    )
}
