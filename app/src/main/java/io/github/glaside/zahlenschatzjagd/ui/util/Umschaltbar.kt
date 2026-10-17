package io.github.glaside.zahlenschatzjagd.ui.util

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role

/**
 * Macht eine Zeile aus Beschriftung und Kästchen oder Schalter als Ganzes antippbar: Für junge
 * Kinder ist die Beschriftung das leichtere Ziel als das kleine Steuerelement daneben.
 *
 * Das Steuerelement in der Zeile bekommt `onCheckedChange = null`, sonst fängt es den Tipp
 * selbst ab, und TalkBack nennt Beschriftung und Zustand getrennt. Ohne eigenen Klick verzichtet
 * es aber auch auf seine Mindestgröße von 48 dp – die übernimmt hier die Zeile. Eine Checkbox
 * braucht sie zusätzlich selbst zurück (`minimumInteractiveComponentSize()`), sonst verliert sie
 * den Rand, der sie bisher von Beschriftung und Zeilenanfang trennte.
 *
 * Die [testTag] sitzt an der Zeile, nicht am Steuerelement: `toggleable` führt die Kinder zu
 * einem Knoten zusammen, eine Marke am Steuerelement fände ein Test dort nicht mehr, und den
 * Zustand (`assertIsOn`) trägt ohnehin nur noch die Zeile.
 */
fun Modifier.umschaltbar(an: Boolean, role: Role, testTag: String, onSetze: (Boolean) -> Unit): Modifier =
    this
        .toggleable(value = an, role = role, onValueChange = onSetze)
        .minimumInteractiveComponentSize()
        .testTag(testTag)
