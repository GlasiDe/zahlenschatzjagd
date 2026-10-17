package io.github.glaside.zahlenschatzjagd.ui.einmaleins

import android.app.Activity
import android.view.WindowManager
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag

/** Solange geübt wird, geht der Bildschirm nicht von selbst aus – wie in den übrigen Übungsformen. */
@Composable
internal fun BildschirmWachHalten() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

/** Das ✕ oben rechts: bricht die Runde ab, ohne Rundenabschluss. Für beide Stufen dasselbe. */
@Composable
internal fun Abbrechen(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onCancel, modifier = modifier.testTag("einmaleins_cancel_button")) {
        Text(
            text = "✕",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
