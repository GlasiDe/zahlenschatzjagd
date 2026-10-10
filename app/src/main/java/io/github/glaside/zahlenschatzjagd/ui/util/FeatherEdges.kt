package io.github.glaside.zahlenschatzjagd.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Blendet die Ränder eines Bildes radial weich aus (kreisförmige Vignette). So verschwindet
 * der rechteckige Pergament-Rand, den die generierten Grafiken (Inseln, Avatare, Münzen)
 * mitbringen, und das Motiv geht nahtlos in den Hintergrund über. Voraussetzung: Das Motiv
 * ist **zentriert** generiert (siehe `ASSETS.md`).
 *
 * Gemeinsame Fassung für Startkarten-Inseln (`HomeScreen`), Avatare (`AvatarImage`) und die
 * Münz-Veranschaulichung (`CoinTenFrame`).
 */
fun Modifier.featherEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color.Black,
                0.72f to Color.Black,
                1.0f to Color.Transparent,
                center = center,
                radius = size.minDimension * 0.52f
            ),
            blendMode = BlendMode.DstIn
        )
    }
