package io.github.glaside.zahlenschatzjagd.ui.util

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kindgerechte Mindest-Tap-Größe für interaktive Elemente (Buttons, Zellen). Wird bei
 * dynamischer Größenberechnung nie unterschritten, damit Elemente auch bei sehr wenig
 * verfügbarem Platz (große Android-Anzeigegröße + viele Elemente) noch sicher antippbar
 * bleiben.
 */
val MIN_TOUCH_TARGET = 48.dp

/**
 * Größe für [count] gleich große, nebeneinander angeordnete Elemente (z. B. Buttons in einer
 * Reihe, Zellen einer Pyramiden-Etage), berechnet aus der tatsächlich verfügbaren Breite/Höhe
 * [available] statt eines fest verdrahteten dp-Werts. Das Ergebnis liegt nie über [ideal] (auf
 * großen Bildschirmen soll es nicht unnötig überdimensioniert wirken) und nie unter [min]
 * (siehe [MIN_TOUCH_TARGET]) – wird [min] dadurch unterschritten, muss der Aufrufer selbst per
 * [fitsWithoutScrollFallback] entscheiden, ob stattdessen gescrollt werden soll.
 */
fun adaptiveElementSize(available: Dp, count: Int, ideal: Dp, min: Dp, spacing: Dp = 0.dp): Dp {
    if (count <= 0) return ideal
    val totalSpacing = spacing * (count - 1)
    val perElement = (available - totalSpacing) / count
    return perElement.coerceIn(min, ideal)
}

/**
 * true, wenn [count] Elemente mit je mindestens [min] Größe (plus [spacing] dazwischen) in
 * [available] passen. false bedeutet: selbst die Mindestgröße reicht nicht mehr – hier soll der
 * Aufrufer als Fallback ein Scrollen aktivieren, statt Elemente abzuschneiden oder unlesbar klein
 * werden zu lassen.
 */
fun fitsWithoutScrollFallback(available: Dp, count: Int, min: Dp, spacing: Dp = 0.dp): Boolean {
    if (count <= 0) return true
    val required = min * count + spacing * (count - 1)
    return available >= required
}

/**
 * Skalierungsfaktor für rein dekorative, feste Größen (Bilder, Abstände), wenn die verfügbare
 * Höhe/Breite [available] kleiner ist als die für den Screen kalkulierte "natürliche" Größe
 * [natural] bei normaler Anzeigegröße. Nie kleiner als [minScale], damit z. B. ein Bild nicht
 * komplett verschwindet, sondern nur bis zu einer sinnvollen Untergrenze schrumpft.
 */
fun densityScale(available: Dp, natural: Dp, minScale: Float = 0.5f): Float =
    (available.value / natural.value).coerceIn(minScale, 1f)

/**
 * Ermittelt einmalig eine gemeinsame Schriftgröße für mehrere nebeneinander angezeigte
 * Titel-Pillen (z. B. Insel- oder Spiel-Kacheln), damit alle **gleich groß** wirken - misst dazu
 * unsichtbar (alpha 0), wie klein die **längste einzelne Zeile** unter allen [titles] bei
 * [maxWidth] werden müsste, und wendet dieses Ergebnis auf alle an. Ohne dies würde jede Pille
 * unabhängig voneinander automatisch schrumpfen: kurze Titel blieben groß, lange Titel würden
 * auffällig kleiner dargestellt - uneinheitlich und unruhig auf einen Blick.
 *
 * [titles] können bereits manuell umgebrochene (mehrzeilige, per "\n" getrennte) Titel enthalten
 * - z. B. ein korrekt getrenntes deutsches Kompositum wie "Grundrechen-\narten". Für die Messung
 * zählt dann nur die längste einzelne Zeile, nicht der gesamte (mehrzeilige) Titel: ein einzelnes
 * langes Kompositum soll nicht die Schriftgröße aller anderen, einzeiligen Titel mit nach unten
 * ziehen, wenn es ohnehin schon korrekt in kürzere Zeilen aufgeteilt ist.
 */
@Composable
fun rememberSharedTitleFontSize(
    titles: List<String>,
    maxWidth: Dp,
    style: TextStyle,
    fontWeight: FontWeight? = null,
    minFontSize: TextUnit = 10.sp,
    maxFontSize: TextUnit = 18.sp,
): TextUnit {
    var resolved by remember { mutableStateOf(maxFontSize) }
    val longestLine = remember(titles) {
        titles.flatMap { it.split("\n") }.maxByOrNull { it.length } ?: ""
    }
    Text(
        text = longestLine,
        style = style,
        fontWeight = fontWeight,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = minFontSize, maxFontSize = maxFontSize),
        modifier = Modifier
            .widthIn(max = maxWidth)
            .alpha(0f),
        onTextLayout = { resolved = it.layoutInput.style.fontSize },
    )
    return resolved
}
