package io.github.glaside.zahlenschatzjagd.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Merkt sich, bis zu welcher Version die Änderungshinweise auf diesem Gerät gezeigt sind.
 *
 * **Gerätegebunden, nicht am Profil**, wie Ton und Vorlesen
 * (`docs/adr/0024-ton-und-vorlesen-am-geraet.md`): Der Hinweis richtet sich an die Eltern, und die
 * lesen ihn einmal – nicht einmal je Geschwisterkind. Aus demselben Grund reist der Stand nicht in
 * der Sicherung mit.
 */
class AenderungshinweisRepository(private val dataStore: DataStore<Preferences>) {

    /** Der `versionCode`, bis zu dem alles gezeigt ist – `null`, solange nichts gemerkt ist. */
    val zuletztGezeigt: Flow<Int?> = dataStore.data.map { prefs -> prefs[ZULETZT_GEZEIGT] }

    /**
     * Hält fest, dass bis einschließlich [versionCode] nichts mehr zu zeigen ist. Der Stand steigt
     * nur: Liefe einmal eine ältere Fassung, zeigte das nächste Update sonst längst Gesehenes noch
     * einmal.
     */
    suspend fun merkeGezeigt(versionCode: Int) {
        dataStore.edit { prefs ->
            prefs[ZULETZT_GEZEIGT] = maxOf(prefs[ZULETZT_GEZEIGT] ?: versionCode, versionCode)
        }
    }

    /** Vergisst den Stand – nur für Tests, die sonst nie unter einen einmal gemerkten kämen. */
    internal suspend fun vergiss() {
        dataStore.edit { prefs -> prefs.remove(ZULETZT_GEZEIGT) }
    }

    companion object {
        // Der gespeicherte Name bleibt, wie er ist. Ginge er verloren, sähen Eltern nur den
        // aktuellen Hinweis ein zweites Mal – Fortschritt hängt nicht daran.
        private val ZULETZT_GEZEIGT = intPreferencesKey("zuletzt_gezeigter_aenderungshinweis")
    }
}
