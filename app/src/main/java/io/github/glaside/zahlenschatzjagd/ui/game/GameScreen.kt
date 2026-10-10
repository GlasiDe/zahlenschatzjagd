package io.github.glaside.zahlenschatzjagd.ui.game

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.glaside.zahlenschatzjagd.R
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.equation
import io.github.glaside.zahlenschatzjagd.ui.AusstehenderTon
import io.github.glaside.zahlenschatzjagd.ui.theme.MapCorrect
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment
import io.github.glaside.zahlenschatzjagd.ui.theme.MapWood
import io.github.glaside.zahlenschatzjagd.ui.util.MIN_TOUCH_TARGET
import io.github.glaside.zahlenschatzjagd.ui.util.adaptiveElementSize
import io.github.glaside.zahlenschatzjagd.ui.util.densityScale
import io.github.glaside.zahlenschatzjagd.ui.util.featherEdges

/** Grob geschätzte "natürliche" Gesamthöhe dieses Screens bei normaler Anzeigegröße - siehe
 * [densityScale]. Orientiert sich am höchsten Bedarf: Verdoppeln im schweren Modus zeigt eine
 * drei statt einzeilige Aufgaben-Tafel ("Verdoppeln" + Zahl + Eingabe) und braucht dadurch mehr
 * Platz als die übrigen Aufgabentypen. */
private val NATURAL_CONTENT_HEIGHT = 900.dp

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onFinish: (Int, List<UnlockedBadge>) -> Unit,
    onCancel: () -> Unit, // Parameter für den Abbrechen-Button
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Das hier sorgt dafür, dass der Bildschirm an bleibt
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // Wenn wir den Screen verlassen, wird die Sperre wieder aufgehoben
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Der Ton bleibt in der Oberfläche (kein Context im ViewModel) und kommt einmal je geprüfter
    // Aufgabe – nach dem Drehen nicht noch einmal.
    AusstehenderTon(state.ausstehenderTon, viewModel::tonGespielt)

    // Rundenende an die Navigation melden (inkl. frisch freigeschalteter Abzeichen)
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinish(state.correctAnswers, state.newlyUnlockedBadges)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Verkleinert die rein dekorativen Abstände oben/unterhalb der Aufgaben-Tafel, wenn bei
        // größerer Android-Anzeigegröße weniger Höhe zur Verfügung steht - insbesondere bei
        // Verdoppeln (schwer) mit seiner dreizeiligen Tafel reicht sonst der Platz für die
        // letzte Ziffernblock-Reihe nicht mehr, ohne dass man erst scrollen muss.
        val scale = densityScale(available = maxHeight, natural = NATURAL_CONTENT_HEIGHT)
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Sicherheitsnetz für echte Extremfälle: scrollt nur, wenn der Inhalt trotz der
                // Verkleinerung oben wirklich nicht mehr passt.
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Aufgabe ${state.taskNumber} von ${state.totalTasks}",
                style = MaterialTheme.typography.bodyLarge
            )

            // Münz-Veranschaulichung (Zehnerfeld) für „verliebte Zahlen" im leichteren Modus:
            // die bekannten Münzen golden, die gesuchten als leere Plätze. Sonst nur Abstand.
            val coinTask = state.currentTask
            if (state.showCoins && coinTask != null && coinTask.category == "VERLIEBTE_ZAHLEN") {
                Spacer(modifier = Modifier.height(60.dp * scale))
                CoinTenFrame(known = coinTask.num1, target = coinTask.num2)
                Spacer(modifier = Modifier.height(24.dp * scale))
            } else {
                Spacer(modifier = Modifier.height(60.dp * scale))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp * scale),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val wasAngezeigtWird = if (state.userInput.isEmpty()) "?" else state.userInput
                val task = state.currentTask
                // Aufgabe auf einer gerahmten Pergament-„Tafel". Bewusst in der gut lesbaren
                // Standardschrift (nicht der Themen-Display-Schrift), damit die Zahlen glasklar sind.
                val isVergleich = task?.category == "GROESSER_KLEINER"
                val isVerdoppeln = task?.category == "VERDOPPELN_LEICHT" || task?.category == "VERDOPPELN_SCHWER"
                if (task != null && isVerdoppeln) {
                    VerdoppelnPrompt(num1 = task.num1, userInput = wasAngezeigtWird)
                } else {
                    Surface(
                        color = MapParchment,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(3.dp, MapWood),
                        // Beim Zahlvergleich eine feste Breite (Anteil der Bildschirmbreite) statt
                        // Wrap-Content: verhindert, dass die Tafel beim Wechsel von „?" zu „<"/„>"
                        // (unterschiedlich breite Glyphen) sichtbar ihre Größe ändert.
                        modifier = if (isVergleich) Modifier.fillMaxWidth(0.85f) else Modifier
                    ) {
                        Text(
                            text = if (task != null) task.equation(wasAngezeigtWird) else "",
                            style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Default),
                            fontWeight = FontWeight.Bold,
                            color = MapInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .testTag("equation_text")
                                .then(if (isVergleich) Modifier.fillMaxWidth() else Modifier)
                                .padding(horizontal = 28.dp, vertical = 14.dp)
                        )
                    }
                }
            }

            val feedback = state.feedback
            if (feedback != null) {
                Spacer(modifier = Modifier.height(100.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (feedback.isCorrect) "Super! Richtig! 🎉" else "Leider falsch. Richtig ist: ${feedback.correctAnswer}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (feedback.isCorrect) MapCorrect else MaterialTheme.colorScheme.error
                    )
                    feedback.extraExplanation?.let { explanation ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MapInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("verdoppeln_explanation")
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.onNext() },
                        modifier = Modifier.testTag("next_button")
                    ) {
                        Text(if (state.taskNumber < state.totalTasks) "Nächste Aufgabe" else "Fertig!")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp * scale))

                // Zahlvergleich: statt des Ziffernblocks drei große Auswahl-Tasten „<" / „=" / „>",
                // im leichten Verdoppeln-Modus drei Zahlen. Ein Tippen wählt nur aus (die Auswahl
                // steht dann in der Aufgabe), abgegeben wird mit dem ✓ darunter – wie überall.
                val answerOptions = state.currentTask?.answerOptions
                val istVergleich = state.currentTask?.category == "GROESSER_KLEINER"
                if (istVergleich || answerOptions != null) {
                    if (istVergleich) {
                        ComparisonButtons(
                            onLess = { viewModel.onCompare("<") },
                            onEqual = { viewModel.onCompare("=") },
                            onGreater = { viewModel.onCompare(">") },
                        )
                    } else if (answerOptions != null) {
                        AnswerOptionButtons(
                            options = answerOptions,
                            onSelect = { value -> viewModel.onSelectOption(value) },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp * scale))
                    AbgebenZeile(onPruefen = viewModel::onCheck)
                } else {
                    Ziffernblock(
                        onZiffer = viewModel::onDigit,
                        onLoeschen = viewModel::onClear,
                        onPruefen = viewModel::onCheck,
                        zeilenAbstand = 8.dp * scale,
                    )
                }
            }
        }
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 8.dp)
                .testTag("game_cancel_button")
        ) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Die drei Antwort-Tasten des Zahlvergleichs: „<", „=" und „>", in dieser Reihenfolge (das
 * Gleichheitszeichen bewusst zwischen den beiden Ungleichheitszeichen). Bewusst groß und in
 * der gut lesbaren Standardschrift, damit auch Nicht-Leser die Zeichen klar erkennen. Ein
 * Tippen wählt nur aus; abgegeben wird mit dem ✓ der [AbgebenZeile].
 */
@Composable
fun ComparisonButtons(onLess: () -> Unit, onEqual: () -> Unit, onGreater: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val size = adaptiveElementSize(maxWidth, count = 3, ideal = 110.dp, min = MIN_TOUCH_TARGET, spacing = 8.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SymbolButton(label = "<", testTag = "symbol_lt", onClick = onLess, size = size)
            SymbolButton(label = "=", testTag = "symbol_eq", onClick = onEqual, size = size)
            SymbolButton(label = ">", testTag = "symbol_gt", onClick = onGreater, size = size)
        }
    }
}

/**
 * Die drei Auswahl-Tasten des leichten Verdoppeln-Modus (eine korrekte, zwei Distraktoren aus
 * [io.github.glaside.zahlenschatzjagd.GameLogic]). Der [testTag] enthält den jeweiligen Zahlenwert,
 * damit Tests gezielt die korrekte Option antippen können, ohne die (bewusst zufällige)
 * Button-Reihenfolge zu kennen. Ein Tippen wählt nur aus; abgegeben wird mit dem ✓ der
 * [AbgebenZeile].
 */
@Composable
fun AnswerOptionButtons(options: List<Int>, onSelect: (Int) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val size = adaptiveElementSize(
            maxWidth,
            count = options.size,
            ideal = 100.dp,
            min = MIN_TOUCH_TARGET,
            spacing = 6.dp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { value ->
                Button(
                    onClick = { onSelect(value) },
                    modifier = Modifier
                        .testTag("answer_option_$value")
                        .size(size)
                        .padding(6.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Default),
                        fontWeight = FontWeight.Bold,
                        // Schrumpft die Schrift automatisch, statt sie bei dreistelligen
                        // Zahlen in einer geschrumpften Kachel hart abzuschneiden.
                        autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 32.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Aufgaben-Anzeige für „Verdoppeln": bewusst **kein** Gleichungs-Format (kein „=", kein
 * Platzhalter „?") – nur die Überschrift „Verdoppeln" und darunter die zu verdoppelnde Zahl,
 * wie vom Nutzer gewünscht. Die beiden Texte werden über `semantics(mergeDescendants = true)`
 * zu einem gemeinsamen `equation_text`-Knoten zusammengefasst, damit Tests wie bei den übrigen
 * Aufgabentypen einen einzigen Text lesen können.
 */
@Composable
private fun VerdoppelnPrompt(num1: Int, userInput: String) {
    Surface(
        color = MapParchment,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(3.dp, MapWood),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .testTag("equation_text")
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Verdoppeln",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MapInk,
            )
            Text(
                text = num1.toString(),
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Default),
                fontWeight = FontWeight.Bold,
                color = MapInk,
            )
            // Die Eingabe – getippt (schwer) oder ausgewählt (leicht) – bzw. „?". Ohne sie tippte
            // man blind; und im leichten Modus sieht das Kind so, was es vor dem ✓ gewählt hat.
            Text(
                text = userInput,
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Default),
                fontWeight = FontWeight.Bold,
                color = MapInk,
            )
        }
    }
}

@Composable
private fun SymbolButton(label: String, testTag: String, onClick: () -> Unit, size: Dp = 110.dp) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .testTag(testTag)
            .size(size)
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Default),
            fontWeight = FontWeight.Bold,
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 40.sp),
            maxLines = 1,
        )
    }
}

/** [size] ist die Kantenlänge des quadratischen Buttons; wird auch von der Zahlenmauer
 * ([io.github.glaside.zahlenschatzjagd.ui.zahlenmauer.ZahlenmauerScreen]) wiederverwendet, die ihren
 * eigenen, aus der verfügbaren Breite berechneten Wert übergibt. */
@Composable
fun NumberButton(label: String, size: Dp = 80.dp, onClick: () -> Unit) {
    val containerColor = when (label) {
        "✓" -> MaterialTheme.colorScheme.secondary
        "C" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val contentColor = when (label) {
        "✓" -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onPrimary
    }

    val testTagName = when (label) {
        "C" -> "digit_clear"
        "✓" -> "digit_check"
        else -> "digit_$label"
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .testTag(testTagName)
            .size(size)
            .padding(4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = label,
            // Ziffern bewusst in der Standardschrift (nicht der Themen-Display-Schrift).
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Default),
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 32.sp),
            maxLines = 1,
        )
    }
}

/**
 * Veranschaulicht eine „verliebte Zahlen"-Aufgabe als Piraten-Zehnerfeld: [target] Münz-
 * Plätze in Reihen zu fünf (didaktisch das klassische Zehnerfeld). Die ersten [known] Plätze
 * sind goldene Münzen (der bekannte Summand), die restlichen leere/graue Plätze (der gesuchte
 * Partner) – das Kind zählt die leeren Plätze. Es genügt **ein** Bild-Asset (`coin_gold`):
 * leere Plätze sind dieselbe Grafik, entsättigt und abgeblendet.
 */
@Composable
fun CoinTenFrame(known: Int, target: Int) {
    val coinSize = 44.dp
    // Entsättigung (Graustufen) für die noch leeren Plätze.
    val emptyFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag("coin_ten_frame")
    ) {
        (0 until target step 5).forEach { rowStart ->
            Row(horizontalArrangement = Arrangement.Center) {
                for (i in rowStart until minOf(rowStart + 5, target)) {
                    val isGold = i < known
                    Image(
                        painter = painterResource(R.drawable.coin_gold),
                        contentDescription = null,
                        colorFilter = if (isGold) null else emptyFilter,
                        modifier = Modifier
                            .size(coinSize)
                            .padding(3.dp)
                            .alpha(if (isGold) 1f else 0.35f)
                            .featherEdges()
                            .testTag(if (isGold) "coin_gold" else "coin_empty")
                    )
                }
            }
        }
    }
}
