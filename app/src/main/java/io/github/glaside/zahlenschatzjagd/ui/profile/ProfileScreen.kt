package io.github.glaside.zahlenschatzjagd.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.glaside.zahlenschatzjagd.data.UserProfile
import io.github.glaside.zahlenschatzjagd.ui.sicherung.BackupMessage
import io.github.glaside.zahlenschatzjagd.ui.sicherung.SicherungMeldung
import io.github.glaside.zahlenschatzjagd.ui.theme.MapInk
import io.github.glaside.zahlenschatzjagd.ui.theme.MapParchment

/**
 * Die Profilverwaltung: zeigt alle Kinder-Profile als antippbare Karten (Tippen = Profil
 * wechseln), das aktive Profil ist hervorgehoben.
 *
 * **Wechseln ist der Alltagsvorgang und bleibt frei**; alles Weitere – anlegen, umbenennen,
 * Avatar ändern, löschen – zeigt dieser Bildschirm nur, wenn [verwaltungErlaubt] gilt, also
 * hinter Elternfrage oder PIN. Andernfalls steht dort ein Knopf, der über
 * [onVerwaltungAnfordern] durch dieses Tor führt (siehe
 * `docs/adr/0019-profilverwaltung-hinter-dem-tor.md`).
 *
 * Gibt es noch **kein** Profil, ist dieser Bildschirm der einzige Einstieg in die App – und
 * dann führen von hier zwei Wege weiter: ein Profil anlegen, oder eine Sicherung einspielen
 * ([sicherungAnbieten], siehe `docs/adr/0023-erststart-bietet-die-sicherung-an.md`).
 *
 * Die Namensvalidierung erfolgt zusätzlich lokal gegen [profiles], damit Fehler sofort ohne
 * Wartezeit erscheinen; [ProfileViewModel] validiert als Sicherheitsnetz erneut.
 */
@Composable
fun ProfileScreen(
    profiles: List<UserProfile>,
    activeUser: String,
    verwaltungErlaubt: Boolean,
    onSwitch: (String) -> Unit,
    onCreate: (name: String, avatarId: String) -> Unit,
    onRename: (oldName: String, newName: String) -> Unit,
    onChangeAvatar: (name: String, avatarId: String) -> Unit,
    onDelete: (name: String) -> Unit,
    onVerwaltungAnfordern: () -> Unit,
    onBack: () -> Unit,
    sicherungAnbieten: Boolean = false,
    sicherungMeldung: BackupMessage? = null,
    onSicherungEinspielen: () -> Unit = {},
    showFirstProfileHint: Boolean = false,
    onDismissFirstProfileHint: () -> Unit = {},
) {
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<UserProfile?>(null) }
    var changingAvatar by remember { mutableStateOf<UserProfile?>(null) }
    var deleting by remember { mutableStateOf<UserProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Kopfzeile(
            // „← Zurück" nur, wenn es einen Rückweg gibt. Spielt gerade niemand – frisch
            // installiert oder gerade eingespielt –, schiebt die Startkarte sofort wieder hierher
            // (siehe `ZahlenschatzjagdNavHost`); der Knopf flackerte dann sichtbar ins Leere. Weiter
            // geht es stattdessen über einen der Wege unten oder über eine Profilkarte.
            zurueckMoeglich = profiles.any { it.name == activeUser },
            // „Tippe auf einen Piraten" ohne einen einzigen Piraten führt niemanden. Beim
            // Erststart ist das die erste Zeile, die ein Elternteil liest – und was dort wirklich
            // zu tun ist, steht darunter.
            zeigeWechselhinweis = profiles.isNotEmpty(),
            onBack = onBack,
        )

        if (profiles.isEmpty()) {
            LeererHinweis(
                sicherungAnbieten = sicherungAnbieten,
                verwaltungErlaubt = verwaltungErlaubt,
            )
        } else {
            for (profile in profiles) {
                ProfileCard(
                    profile = profile,
                    isActive = profile.name == activeUser,
                    verwaltungErlaubt = verwaltungErlaubt,
                    onSelect = { onSwitch(profile.name) },
                    onRename = { renaming = profile },
                    onChangeAvatar = { changingAvatar = profile },
                    onDelete = { deleting = profile },
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        if (verwaltungErlaubt) {
            Button(
                onClick = { showCreate = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_add_button")
            ) {
                Text("➕ Neues Profil")
            }
        } else {
            VerwaltungHinterDemTor(onVerwaltungAnfordern = onVerwaltungAnfordern)
        }

        if (sicherungAnbieten) {
            Spacer(Modifier.height(20.dp))
            SicherungAlsZweiterWeg(onSicherungEinspielen = onSicherungEinspielen)
        }
        sicherungMeldung?.let {
            Spacer(Modifier.height(16.dp))
            SicherungMeldung(it)
        }
    }

    if (showCreate) {
        val existingNames = remember(profiles) { profiles.map { it.name } }
        ProfileEditDialog(
            title = "Neues Profil",
            initialName = "",
            initialAvatarId = Avatars.DEFAULT_ID,
            confirmLabel = "Anlegen",
            confirmTestTag = "profile_create_confirm",
            forbiddenNames = existingNames,
            onConfirm = { name, avatarId ->
                onCreate(name, avatarId)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }

    renaming?.let { profile ->
        val forbidden = remember(profiles, profile) {
            profiles.map { it.name }.filter { it != profile.name }
        }
        ProfileEditDialog(
            title = "Profil umbenennen",
            initialName = profile.name,
            initialAvatarId = profile.avatarId,
            confirmLabel = "Speichern",
            confirmTestTag = "profile_rename_confirm",
            showAvatarPicker = false,
            forbiddenNames = forbidden,
            onConfirm = { name, _ ->
                onRename(profile.name, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    changingAvatar?.let { profile ->
        AvatarPickerDialog(
            initialAvatarId = profile.avatarId,
            onConfirm = { avatarId ->
                onChangeAvatar(profile.name, avatarId)
                changingAvatar = null
            },
            onDismiss = { changingAvatar = null },
        )
    }

    deleting?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Profil löschen?") },
            text = {
                Text(
                    "„${profile.name}“ und der gesamte Fortschritt gehen dabei verloren. " +
                        "Das lässt sich nicht rückgängig machen."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(profile.name)
                        deleting = null
                    },
                    modifier = Modifier.testTag("profile_delete_confirm")
                ) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Abbrechen") }
            }
        )
    }

    if (showFirstProfileHint) {
        AlertDialog(
            onDismissRequest = onDismissFirstProfileHint,
            title = { Text("Willkommen an Bord! 🏴‍☠️") },
            text = {
                Text(
                    "Dein Profil ist angelegt. Du kannst Namen und Bild jederzeit ändern – " +
                        "tippe dazu oben auf der Schatzkarte einfach auf deinen Namen."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onDismissFirstProfileHint,
                    modifier = Modifier.testTag("first_profile_hint_confirm")
                ) { Text("Alles klar!") }
            }
        )
    }
}

/** Rückweg und Wechsel-Hinweis erscheinen nur, wo sie etwas beitragen; die Überschrift immer. */
@Composable
private fun Kopfzeile(zurueckMoeglich: Boolean, zeigeWechselhinweis: Boolean, onBack: () -> Unit) {
    if (zurueckMoeglich) {
        TextButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
            Text("← Zurück")
        }
        Spacer(Modifier.height(4.dp))
    }
    Text(
        text = "Deine Piratencrew 🏴‍☠️",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    if (zeigeWechselhinweis) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tippe auf einen Piraten, um zu wechseln.",
            style = MaterialTheme.typography.bodyMedium,
            color = MapInk
        )
    }
    Spacer(Modifier.height(16.dp))
}

/** Was statt der Karten dasteht, wenn es noch kein Profil gibt – je nachdem, wie es weitergeht. */
@Composable
private fun LeererHinweis(sicherungAnbieten: Boolean, verwaltungErlaubt: Boolean) {
    Text(
        text = when {
            sicherungAnbieten -> "Noch keine Piraten an Bord. Unten geht es weiter – auf zwei Wegen."
            verwaltungErlaubt -> "Noch keine Piraten an Bord. Leg unten dein erstes Profil an!"
            else -> "Noch keine Piraten an Bord."
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MapInk,
        modifier = Modifier.testTag("profile_empty_hint")
    )
}

/**
 * Der zweite Weg für den Moment, für den die Sicherung gebaut ist: Gerätewechsel und
 * Neuinstallation. Er steht **ohne Tor** da, aus demselben Grund wie das Anlegen daneben – es
 * gibt zu diesem Zeitpunkt weder ein Profil zu schützen noch einen anderen Weg hinein. Die
 * Ausnahme endet, sobald es etwas zu schützen gibt.
 */
@Composable
private fun SicherungAlsZweiterWeg(onSicherungEinspielen: () -> Unit) {
    Text(
        text = "Schon einmal an Bord gewesen? Wenn du eine Sicherung von einem anderen Gerät " +
            "hast, hol dir damit alle Piraten und ihren Fortschritt zurück.",
        style = MaterialTheme.typography.bodyMedium,
        color = MapInk,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onSicherungEinspielen,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_restore_button")
    ) {
        Text("📥 Ich habe eine Sicherung")
    }
}

/**
 * Der Hinweis an der Stelle, an der sonst „Neues Profil" steht: Was hier fehlt, fehlt nicht
 * zufällig – und der Weg dorthin führt durch die Elternfrage bzw. die PIN.
 */
@Composable
private fun VerwaltungHinterDemTor(onVerwaltungAnfordern: () -> Unit) {
    Text(
        text = "Anlegen, Umbenennen, Avatar und Löschen sind nur für Erwachsene.",
        style = MaterialTheme.typography.bodyMedium,
        color = MapInk,
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = onVerwaltungAnfordern,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_manage_button")
    ) {
        Text("🔒 Profile verwalten")
    }
}

/**
 * Eine Profil-Karte: Avatar + Name + Aktionen. Antippen wechselt zum Profil – das bleibt auch
 * ohne [verwaltungErlaubt] möglich; nur die drei Aktionen daneben verschwinden dann.
 */
@Composable
private fun ProfileCard(
    profile: UserProfile,
    isActive: Boolean,
    verwaltungErlaubt: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onChangeAvatar: () -> Unit,
    onDelete: () -> Unit,
) {
    val border = if (isActive) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MapInk.copy(alpha = 0.25f))
    }
    Surface(
        color = MapParchment.copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("profile_card_${profile.name}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(avatarId = profile.avatarId, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MapInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isActive) {
                    Text(
                        text = "⚓ spielt gerade",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (verwaltungErlaubt) {
                IconAction("✏️", desc = "Umbenennen", profil = profile.name, onClick = onRename)
                IconAction("🎨", desc = "Avatar", profil = profile.name, onClick = onChangeAvatar)
                IconAction("🗑️", desc = "Löschen", profil = profile.name, onClick = onDelete)
            }
        }
    }
}

/**
 * Kleine, tippbare Emoji-Schaltfläche für Karten-Aktionen. Die Testmarke trägt den Profilnamen
 * mit: Ohne ihn hieße dieselbe Aktion auf jeder Karte gleich, und ein Test könnte nicht sagen,
 * welches Kind er gerade löscht.
 */
@Composable
private fun IconAction(emoji: String, desc: String, profil: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .testTag("profile_action_${desc.lowercase()}_$profil"),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}

/**
 * Dialog zum Anlegen/Umbenennen eines Profils: Namensfeld mit lokaler Validierung und
 * (optional) Avatar-Auswahlraster.
 */
@Composable
private fun ProfileEditDialog(
    title: String,
    initialName: String,
    initialAvatarId: String,
    confirmLabel: String,
    confirmTestTag: String,
    forbiddenNames: List<String>,
    showAvatarPicker: Boolean = true,
    onConfirm: (name: String, avatarId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var avatarId by remember { mutableStateOf(initialAvatarId) }
    val trimmed = name.trim()
    val duplicate = forbiddenNames.any { it.equals(trimmed, ignoreCase = true) }
    val valid = trimmed.isNotEmpty() && !duplicate
    val errorText = when {
        duplicate -> "Diesen Namen gibt es schon."
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (showAvatarPicker) {
                    Spacer(Modifier.height(12.dp))
                    Text("Wähle einen Avatar:", style = MaterialTheme.typography.bodyMedium, color = MapInk)
                    Spacer(Modifier.height(8.dp))
                    AvatarPicker(selectedId = avatarId, onSelect = { avatarId = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (valid) onConfirm(trimmed, avatarId) },
                enabled = valid,
                modifier = Modifier.testTag(confirmTestTag)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/** Dialog nur zum Avatar-Wechsel eines bestehenden Profils. */
@Composable
private fun AvatarPickerDialog(
    initialAvatarId: String,
    onConfirm: (avatarId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var avatarId by remember { mutableStateOf(initialAvatarId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avatar wählen") },
        text = { AvatarPicker(selectedId = avatarId, onSelect = { avatarId = it }) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(avatarId) },
                modifier = Modifier.testTag("profile_avatar_confirm")
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

/** Auswahlraster aller Avatare (4 pro Reihe); der ausgewählte ist umrandet. */
@Composable
private fun AvatarPicker(selectedId: String, onSelect: (String) -> Unit) {
    Column {
        Avatars.all.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (option in row) {
                    val selected = option.id == selectedId
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(60.dp)
                            .clickable { onSelect(option.id) }
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                if (selected) MapParchment else MapParchment.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .testTag("avatar_option_${option.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(avatarId = option.id, size = 52.dp)
                    }
                }
            }
        }
    }
}
