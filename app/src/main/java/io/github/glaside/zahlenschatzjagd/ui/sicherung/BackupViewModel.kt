package io.github.glaside.zahlenschatzjagd.ui.sicherung

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.data.BackupReadResult
import io.github.glaside.zahlenschatzjagd.data.BackupSerializer
import io.github.glaside.zahlenschatzjagd.data.BackupStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Zustand und Aktionen rund um die Sicherung: erstellen und einspielen.
 *
 * Es hängt bewusst an keinem Bildschirm: Der Elternbereich erstellt und spielt ein, der
 * Erststart ohne Profil spielt nur ein. Beide benutzen dasselbe – das Erstellen ist dort
 * mitgeliefertes totes Gewicht und billiger als eine zweite Form.
 *
 * Das Lesen und Schreiben der Datei bleibt bewusst außerhalb: Der Screen kennt den vom Nutzer
 * ausgewählten Speicherort (Storage Access Framework) und reicht nur zwei Funktionen herein.
 * Dadurch hängt dieses ViewModel an keiner Android-Framework-Klasse und ist normal testbar.
 */
class BackupViewModel(
    private val backupRepository: BackupStore,
    private val appVersionName: String,
) : ViewModel() {

    private val _busy = MutableStateFlow(false)

    /** True, solange geschrieben oder eingelesen wird – blockt Doppelklicks. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<BackupMessage?>(null)
    val message: StateFlow<BackupMessage?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    /** [writeText] schreibt den übergebenen JSON-Text an den vom Nutzer gewählten Ort. */
    fun createBackup(writeText: (String) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val file = backupRepository.createBackup(appVersionName)
                writeText(BackupSerializer.encode(file))
                _message.value = exportMessage(file)
            } catch (e: IOException) {
                _message.value = fileAccessMessage(e.message)
            } finally {
                _busy.value = false
            }
        }
    }

    /** [readText] liefert den Inhalt der vom Nutzer gewählten Datei. */
    fun restoreBackup(readText: () -> String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                when (val result = BackupSerializer.decode(readText())) {
                    is BackupReadResult.Success ->
                        _message.value = restoreMessage(backupRepository.restore(result.file))

                    is BackupReadResult.TooNew ->
                        _message.value = tooNewMessage(result.formatVersion)

                    is BackupReadResult.Invalid ->
                        _message.value = invalidFileMessage()
                }
            } catch (e: IOException) {
                _message.value = fileAccessMessage(e.message)
            } finally {
                _busy.value = false
            }
        }
    }
}
