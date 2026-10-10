package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.compose.runtime.Composable
import io.github.glaside.zahlenschatzjagd.data.ParentAccess
import io.github.glaside.zahlenschatzjagd.data.ParentGate
import io.github.glaside.zahlenschatzjagd.data.gateFor

/**
 * Das Tor vor dem Elternbereich – und die einzige Stelle, die entscheidet, welches gilt:
 * die Elternfrage, solange keine PIN gesetzt ist, sonst die PIN.
 *
 * [access] ist der bereits geladene Zustand. Das Unterscheiden von „noch nicht geladen"
 * übernimmt der Aufrufer (`ZahlenschatzjagdNavHost`); hier wäre es nur ein zweiter, stiller
 * Bildschirmzustand.
 */
@Composable
fun ParentGateScreen(
    access: ParentAccess?,
    onPass: () -> Unit,
    onResetPassed: () -> Unit,
    onCancel: () -> Unit,
) {
    when (gateFor(access)) {
        ParentGate.ELTERNFRAGE -> AdultGate(onPass = onPass, onCancel = onCancel)

        // gateFor liefert PIN nur für einen gesetzten Zugang – checkNotNull hält das fest,
        // statt es an dieser Stelle zu wiederholen.
        ParentGate.PIN -> ParentPinGate(
            access = checkNotNull(access),
            onPass = onPass,
            onResetPassed = onResetPassed,
            onCancel = onCancel,
        )
    }
}
