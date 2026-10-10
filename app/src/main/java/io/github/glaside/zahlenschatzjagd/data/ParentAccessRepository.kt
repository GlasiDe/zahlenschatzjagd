package io.github.glaside.zahlenschatzjagd.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Speichert, wie der Zugang auf diesem Gerät geregelt ist: die optionale PIN des Elternbereichs
 * samt Sicherheitsfrage – als Prüfsumme, nie im Klartext (siehe
 * `docs/adr/0016-pin-optional-neben-der-elternfrage.md`) – und den Schalter, ob der Profilwechsel
 * frei bleibt (`docs/adr/0019-profilverwaltung-hinter-dem-tor.md`).
 *
 * **Gerätegebunden, bewusst nicht in der Sicherung.** Die Sicherung wandert auf ein neues Gerät;
 * die PIN nicht. Dort gilt zunächst wieder die Elternfrage, hinter der sich eine neue PIN
 * vergeben lässt.
 *
 * Nutzt denselben DataStore wie [UserPreferencesRepository] – ein zweiter Store brächte nichts
 * außer einer zweiten Datei.
 */
class ParentAccessRepository(private val dataStore: DataStore<Preferences>) {

    /** Der gesetzte Zugang, oder `null`, solange keine PIN vergeben ist. */
    val access: Flow<ParentAccess?> = dataStore.data.map { prefs -> prefs.toParentAccess() }

    /**
     * Ob der Profilwechsel ohne das Tor möglich bleibt. Steht er auf `true`, kommt ein Kind über
     * das Profil-Abzeichen der Startkarte direkt zu seinem eigenen Profil und darf dort **nur**
     * wechseln; alles Weitere liegt so oder so hinter Elternfrage bzw. PIN.
     *
     * **Voreingestellt an** ([PROFILWECHSEL_OHNE_PIN_VOREINSTELLUNG]): Ein Update darf keine
     * Funktion wegnehmen, die vorher da war – sonst müsste nach dem Update jedes Kind auf jedem
     * bestehenden Gerät erst die Elternfrage lösen, um an sein eigenes Profil zu kommen.
     */
    val profilwechselOhnePin: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PROFILWECHSEL_OHNE_PIN] ?: PROFILWECHSEL_OHNE_PIN_VOREINSTELLUNG
    }

    /** Setzt den Schalter aus [profilwechselOhnePin]. */
    suspend fun setzeProfilwechselOhnePin(erlaubt: Boolean) {
        dataStore.edit { prefs -> prefs[PROFILWECHSEL_OHNE_PIN] = erlaubt }
    }

    /**
     * Nur ein vollständiger Satz zählt: Ein halber Eintrag – etwa nach einem abgebrochenen
     * Schreibvorgang – würde sonst ein Tor aufstellen, durch das niemand mehr kommt.
     */
    private fun Preferences.toParentAccess(): ParentAccess? =
        this[SALT]?.let { salt ->
            val pinChecksum = this[PIN_CHECKSUM]
            val question = this[QUESTION]
            val answerChecksum = this[ANSWER_CHECKSUM]
            if (pinChecksum != null && question != null && answerChecksum != null) {
                ParentAccess(salt, pinChecksum, question, answerChecksum)
            } else {
                null
            }
        }

    /**
     * Vergibt eine neue PIN samt Sicherheitsfrage. Jedes Vergeben erzeugt ein frisches Salt, die
     * alten Prüfsummen sind danach wertlos.
     */
    suspend fun setPin(pin: String, question: String, answer: String) {
        val salt = newSalt()
        dataStore.edit { prefs ->
            prefs[SALT] = salt
            prefs[PIN_CHECKSUM] = checksum(pin, salt)
            prefs[QUESTION] = question.trim()
            prefs[ANSWER_CHECKSUM] = checksum(normalizeAnswer(answer), salt)
        }
    }

    /**
     * Tauscht nur die PIN aus und lässt Sicherheitsfrage und Antwort unberührt.
     *
     * Das Salt bleibt dabei bewusst dasselbe: Die Antwort liegt nur als Prüfsumme vor, ein
     * frisches Salt würde sie unbrauchbar machen und damit genau den Rückweg zerstören, um den
     * es hier geht. Ohne gesetzte PIN passiert nichts – dann gibt es keine Frage, die bleiben
     * könnte.
     */
    suspend fun changePin(pin: String) {
        dataStore.edit { prefs ->
            val salt = prefs[SALT]
            if (salt != null) {
                prefs[PIN_CHECKSUM] = checksum(pin, salt)
            }
        }
    }

    /**
     * Entfernt die PIN; danach gilt wieder die Elternfrage. [profilwechselOhnePin] bleibt dabei
     * unberührt – der Schalter steht neben der PIN, nicht in ihr.
     */
    suspend fun clearPin() {
        dataStore.edit { prefs ->
            prefs.remove(SALT)
            prefs.remove(PIN_CHECKSUM)
            prefs.remove(QUESTION)
            prefs.remove(ANSWER_CHECKSUM)
        }
    }

    companion object {
        /** Voreinstellung des Schalters aus [profilwechselOhnePin]: an. */
        const val PROFILWECHSEL_OHNE_PIN_VOREINSTELLUNG = true

        private val SALT = stringPreferencesKey("parent_access_salt")
        private val PIN_CHECKSUM = stringPreferencesKey("parent_access_pin_checksum")
        private val QUESTION = stringPreferencesKey("parent_access_question")
        private val ANSWER_CHECKSUM = stringPreferencesKey("parent_access_answer_checksum")
        private val PROFILWECHSEL_OHNE_PIN = booleanPreferencesKey("profilwechsel_ohne_pin")
    }
}
