package io.github.glaside.zahlenschatzjagd.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.glaside.zahlenschatzjagd.R

/**
 * Vollflächiger Pergament-Hintergrund für alle Screens. Wird **einmal zentral** um den
 * NavHost gelegt (`ZahlenschatzjagdNavHost`), damit jeder Bildschirm auf dem gleichen ruhigen
 * Pergament liegt, ohne dass jeder Screen den Hintergrund selbst setzen muss. Der
 * `HomeScreen` zeichnet seine detailreiche Schatzkarte zusätzlich darüber.
 */
@Composable
fun ParchmentBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.parchment_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        content()
    }
}
