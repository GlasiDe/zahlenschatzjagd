package io.github.glaside.zahlenschatzjagd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Durchgängiges Pergament-/Piraten-Farbschema. Warme Holz-/Pergamenttöne mit dunkler
// Sepia-Schrift (MapInk). Bewusst nur EIN Schema: Der Pergament-Look ist inhärent hell,
// ein separater Dunkelmodus würde ihn brechen (siehe ZahlenschatzjagdTheme unten).
private val ParchmentColorScheme = lightColorScheme(
    primary = MapWood, // Buttons und Titel (Holzbraun)
    onPrimary = MapParchment, // helle Schrift auf Holz-Buttons
    secondary = TreasureGold, // Akzent (z. B. Bestätigen-Taste, Sterne)
    onSecondary = MapInk, // dunkle Schrift auf Gold
    tertiary = AdventureOrange, // Akzent (z. B. Löschen-Taste)
    onTertiary = MapInk,
    background = MapParchment, // Fallback hinter der Pergament-Textur
    onBackground = MapInk, // Standard-Textfarbe
    surface = MapParchment, // Cards/Boxen
    onSurface = MapInk,
    secondaryContainer = MapParchmentMuted, // nicht-selektierte Toggle-Buttons
    onSecondaryContainer = MapInk,
    error = MapWrong,
    onError = Color.White,
)

/**
 * App-Theme. Nutzt bewusst **immer** das [ParchmentColorScheme] – unabhängig von der
 * Systemeinstellung (Hell/Dunkel) und ohne dynamische Material-You-Farben, damit der
 * Pergament-/Piraten-Look auf jedem Gerät konsistent bleibt.
 */
@Composable
fun ZahlenschatzjagdTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ParchmentColorScheme,
        typography = Typography,
        content = content
    )
}
