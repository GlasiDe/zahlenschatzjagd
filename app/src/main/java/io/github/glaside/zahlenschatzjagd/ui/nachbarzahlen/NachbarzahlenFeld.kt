package io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.glaside.zahlenschatzjagd.Nachbarposition
import io.github.glaside.zahlenschatzjagd.Nachbarzahlenaufgabe
import io.github.glaside.zahlenschatzjagd.ui.theme.MapCorrect
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchmentMuted
import io.github.glaside.zahlenschatzjagd.ui.theme.MapWood

/**
 * **Warum Pfeile und keine Buchstaben.** Über den Lücken stehen „←" und „→" und nicht die im
 * Schulheft üblichen Kürzel „V" (Vorgänger) und „N" (Nachfolger). Die Schulheft-Konvention ist
 * bewusst verworfen: Die App richtet sich an Kinder, die noch nicht sicher lesen – ein Kürzel muss
 * ein Erwachsener einmal erklären, ein Pfeil nicht. Wer hier später auf V/N umstellt, nimmt der
 * Übungsform ihre Selbsterklärung; dann müssten die Kürzel zusätzlich vertont werden.
 */
internal const val PFEIL_DAVOR: String = "←"
internal const val PFEIL_DANACH: String = "→"

/** Wie klein die Zahl im Feld höchstens wird, bevor ein Kind sie nicht mehr gut sieht. */
private val MIN_ZAHL_SCHRIFT = 18.sp

/** Die vier Anzeigezustände eines Feldes, aus Eingabe und Prüfstand abgeleitet. */
internal enum class Feldzustand { VORGEGEBEN, OFFEN, RICHTIG, FALSCH }

/** Fertig berechnete Darstellung eines Feldes – hält [Feld] selbst schlank und linear. */
private data class Felddarstellung(
    val hintergrund: Color,
    val rahmen: Color,
    val rahmenbreite: Dp,
    val pfeil: String?,
    val text: String,
    val hinweis: String?,
    val antippbar: Boolean,
)

/**
 * Eine Position **ohne** Eingabe ist die vorgegebene Zahl – in
 * [NachbarzahlenUiState.eingaben] stehen nur die Lücken.
 */
internal fun feldzustand(eingabe: Lueckeneingabe?, geprueft: Boolean): Feldzustand = when {
    eingabe == null -> Feldzustand.VORGEGEBEN
    !geprueft -> Feldzustand.OFFEN
    eingabe.istRichtig == true -> Feldzustand.RICHTIG
    else -> Feldzustand.FALSCH
}

/**
 * Der Pfeil über einer Lücke: nach links, wenn sie **vor** der vorgegebenen Zahl steht, sonst nach
 * rechts. Die Leserichtung steckt in der Reihenfolge von [Nachbarposition]. Über der vorgegebenen
 * Zahl steht kein Pfeil – dort ist nichts zu ergänzen.
 */
internal fun feldPfeil(
    zustand: Feldzustand,
    position: Nachbarposition,
    vorgegeben: Nachbarposition,
): String? = when {
    zustand == Feldzustand.VORGEGEBEN -> null
    position.ordinal < vorgegeben.ordinal -> PFEIL_DAVOR
    else -> PFEIL_DANACH
}

/**
 * Was im Feld steht. Nach dem Prüfen die **tatsächliche** Eingabe und nicht die Lösung – sonst
 * verschwände ein unbemerkter Vertipper spurlos.
 */
internal fun feldText(
    zustand: Feldzustand,
    aufgabe: Nachbarzahlenaufgabe,
    position: Nachbarposition,
    eingabe: Lueckeneingabe?,
): String = when (zustand) {
    Feldzustand.VORGEGEBEN -> aufgabe.zahlAn(position).toString()
    Feldzustand.OFFEN -> eingabe?.eingabe.orEmpty().ifEmpty { "?" }
    else -> eingabe?.eingabe.orEmpty()
}

private fun feldHintergrund(zustand: Feldzustand, fehlerFarbe: Color): Color = when (zustand) {
    Feldzustand.VORGEGEBEN -> MapParchmentMuted
    Feldzustand.OFFEN -> MapParchment
    Feldzustand.RICHTIG -> MapCorrect.copy(alpha = 0.3f)
    Feldzustand.FALSCH -> fehlerFarbe.copy(alpha = 0.3f)
}

private fun feldRahmen(zustand: Feldzustand, gewaehlt: Boolean, fehlerFarbe: Color): Color = when {
    zustand == Feldzustand.RICHTIG -> MapCorrect
    zustand == Feldzustand.FALSCH -> fehlerFarbe
    gewaehlt -> MapWood
    else -> MapWood.copy(alpha = 0.4f)
}

private fun felddarstellung(
    aufgabe: Nachbarzahlenaufgabe,
    position: Nachbarposition,
    state: NachbarzahlenUiState,
    fehlerFarbe: Color,
): Felddarstellung {
    val eingabe = state.eingaben[position]
    val gewaehlt = position == state.gewaehlteLuecke && !state.geprueft
    val zustand = feldzustand(eingabe, state.geprueft)
    return Felddarstellung(
        hintergrund = feldHintergrund(zustand, fehlerFarbe),
        rahmen = feldRahmen(zustand, gewaehlt, fehlerFarbe),
        rahmenbreite = if (gewaehlt) 3.dp else 2.dp,
        pfeil = feldPfeil(zustand, position, aufgabe.vorgegeben),
        text = feldText(zustand, aufgabe, position, eingabe),
        // Bei falscher Eingabe klein die richtige Lösung darunter, damit ein Kind seinen Fehler sieht.
        hinweis = if (zustand == Feldzustand.FALSCH) "richtig: ${aufgabe.zahlAn(position)}" else null,
        antippbar = zustand == Feldzustand.OFFEN,
    )
}

/**
 * Ein einzelnes Feld der Reihe: die vorgegebene Zahl fest und nicht antippbar, eine Lücke mit
 * Pfeil darüber (siehe [PFEIL_DAVOR]).
 */
@Composable
internal fun Feld(
    position: Nachbarposition,
    aufgabe: Nachbarzahlenaufgabe,
    state: NachbarzahlenUiState,
    groesse: Dp,
    onClick: () -> Unit,
) {
    val darstellung = felddarstellung(aufgabe, position, state, MaterialTheme.colorScheme.error)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            // Auch ohne Pfeil eine Zeile, damit die drei Felder auf gleicher Höhe stehen.
            text = darstellung.pfeil ?: " ",
            style = MaterialTheme.typography.headlineSmall,
            color = MapInk,
            modifier = Modifier.testTag("nachbarzahlen_pfeil_${position.name}")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = darstellung.hintergrund,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(darstellung.rahmenbreite, darstellung.rahmen),
            modifier = Modifier
                .padding(4.dp)
                .width(groesse)
                .heightIn(min = groesse)
                .then(if (darstellung.antippbar) Modifier.clickable(onClick = onClick) else Modifier)
                .testTag("nachbarzahlen_feld_${position.name}")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = darstellung.text,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Default),
                    fontWeight = FontWeight.Bold,
                    color = MapInk,
                    // Im Zahlenraum 100 stehen hier drei Ziffern („100"). Ohne die eine Zeile
                    // bräche Compose sie innerhalb der Zahl um, und aus 100 würde optisch „10 / 0".
                    // Die Schrift schrumpft stattdessen, bis die Zahl in ein schmales Feld passt.
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = MIN_ZAHL_SCHRIFT,
                        maxFontSize = MaterialTheme.typography.headlineMedium.fontSize,
                    ),
                    // Eigene Marke am Text: Der Rahmen darüber trägt die Marke zum Antippen, aber
                    // nicht dessen Inhalt – der instrumentierte Test liest hier die vorgegebene Zahl.
                    modifier = Modifier.testTag("nachbarzahlen_zahl_${position.name}")
                )
                darstellung.hinweis?.let { hinweis ->
                    Text(
                        text = hinweis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MapInk,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
