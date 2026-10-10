package io.github.glaside.zahlenschatzjagd.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Speichert, was auf diesem Gerät zu hören ist: die Töne der Runde und die Vorlesefunktion.
 *
 * **Gerätegebunden, nicht am Profil** – die einzige Einstellung, die so gehalten wird
 * (`docs/adr/0024-ton-und-vorlesen-am-geraet.md`). „Ton aus" ist eine Aussage über den Ort
 * (Wartezimmer, Auto, Schule), nicht über das Kind; am Profil müsste man den Schalter für jedes
 * Geschwisterkind einzeln umlegen. Aus demselben Grund reist er auch nicht in der Sicherung mit:
 * Die beschreibt den Fortschritt eines Kindes, nicht die Lautstärke eines Geräts.
 *
 * Nutzt denselben DataStore wie [UserPreferencesRepository] und [ParentAccessRepository] – ein
 * dritter Store brächte nichts außer einer dritten Datei.
 */
class TonUndVorlesenRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Ob die Töne der Runde – richtig, falsch, geschafft – zu hören sind.
     *
     * **Voreingestellt an** ([VOREINSTELLUNG]): Ein Update darf nichts abschalten, was vorher zu
     * hören war.
     */
    val tonAn: Flow<Boolean> = dataStore.data.map { prefs -> prefs[TON_AN] ?: VOREINSTELLUNG }

    /** Ob das Maskottchen vorliest. Voreingestellt an, aus demselben Grund wie [tonAn]. */
    val vorlesenAn: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[VORLESEN_AN] ?: VOREINSTELLUNG }

    /** Schaltet die Töne der Runde an oder aus. */
    suspend fun setzeTonAn(an: Boolean) {
        dataStore.edit { prefs -> prefs[TON_AN] = an }
    }

    /** Schaltet die Vorlesefunktion an oder aus. */
    suspend fun setzeVorlesenAn(an: Boolean) {
        dataStore.edit { prefs -> prefs[VORLESEN_AN] = an }
    }

    companion object {
        /** Voreinstellung beider Schalter: an. */
        const val VOREINSTELLUNG = true

        private val TON_AN = booleanPreferencesKey("ton_an")
        private val VORLESEN_AN = booleanPreferencesKey("vorlesen_an")
    }
}
