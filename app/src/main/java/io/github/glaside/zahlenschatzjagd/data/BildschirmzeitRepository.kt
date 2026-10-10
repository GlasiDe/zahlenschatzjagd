package io.github.glaside.zahlenschatzjagd.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Schreibt und liest die gemessene **Bildschirmzeit**.
 *
 * Das Schreiben läuft ausdrücklich **nicht** im Lebenslauf der Oberfläche: Ein Abschnitt wird
 * genau dann fertig, wenn die App verschwindet oder das Profil wechselt – also im selben Moment,
 * in dem die Komposition entsorgt werden kann. Ein an sie gebundener Coroutine-Scope würde den
 * Schreibvorgang mitten in Room abbrechen, und der gerade gemessene Nachmittag wäre weg. Deshalb
 * hat das Repository seinen eigenen, app-weiten [scope].
 */
class BildschirmzeitRepository(
    private val dao: ZeitabschnittDao,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    /**
     * Hält einen fertigen Abschnitt fest. Absichtlich ohne `suspend` und ohne Rückmeldung: Der
     * Aufrufer ist die Oberfläche im Moment ihres Verschwindens, sie kann auf nichts mehr warten.
     */
    fun merke(abschnitt: Zeitabschnitt) {
        scope.launch { dao.insert(abschnitt) }
    }

    /** Die Bildschirmzeit eines Profils, jüngste zuerst. */
    suspend fun fuerProfil(profil: String): List<Zeitabschnitt> = dao.getForProfil(profil)
}
