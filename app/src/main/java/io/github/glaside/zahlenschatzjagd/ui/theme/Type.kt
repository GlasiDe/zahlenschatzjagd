package io.github.glaside.zahlenschatzjagd.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.glaside.zahlenschatzjagd.R

/**
 * Überschriften-Schrift („Andika", OFL) – bewusst **nur für Überschriften/Titel**
 * (die display- und headline-Stile). Andika wurde von SIL International gezielt für
 * Leseanfänger entwickelt (eindeutige, unverschnörkelte Buchstabenformen, z. B. klares „ß"),
 * anders als die zuvor verwendete verschnörkelte Piraten-Schrift „Pirata One". Fließtext
 * bleibt in der gut lesbaren Standardschrift; die Rechenaufgabe und die Ziffern-Tasten setzen
 * an ihrer Stelle zusätzlich `FontFamily.Default`, damit Zahlen glasklar bleiben.
 */
private val HeadlineFont = FontFamily(Font(R.font.andika_regular, FontWeight.Normal))

private val Default = Typography()

val Typography = Default.copy(
    displayLarge = Default.displayLarge.copy(fontFamily = HeadlineFont),
    displayMedium = Default.displayMedium.copy(fontFamily = HeadlineFont),
    displaySmall = Default.displaySmall.copy(fontFamily = HeadlineFont),
    headlineLarge = Default.headlineLarge.copy(fontFamily = HeadlineFont),
    headlineMedium = Default.headlineMedium.copy(fontFamily = HeadlineFont),
    headlineSmall = Default.headlineSmall.copy(fontFamily = HeadlineFont),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
