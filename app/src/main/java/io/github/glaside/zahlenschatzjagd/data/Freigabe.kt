package io.github.glaside.zahlenschatzjagd.data

import io.github.glaside.zahlenschatzjagd.BADGE_CATEGORIES
import io.github.glaside.zahlenschatzjagd.BADGE_RANGES

/**
 * Was ein Profil üben darf: eine Menge freigegebener **Fortschrittskennungen** und eine Menge
 * freigegebener **Zahlenräume**. Erreichbar ist, was in beiden vorkommt – kein Kreuzprodukt aus
 * Kennung und Zahlenraum (siehe `docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md`).
 * Die Zahlenräume wirken dabei nur auf Kennungen, bei denen das Kind den Zahlenraum selbst wählt;
 * eine Kennung mit festem Zahlenraum hängt allein an der Kennung
 * (`docs/adr/0026-zahlenraum-wirkt-nur-wo-gewaehlt-wird.md`).
 *
 * Frei von Compose und Android, damit die Schnittbildung ohne Gerät prüfbar ist – und deshalb in
 * `data/`: [UserProfileRepository] muss diese Typen kennen (`docs/agents/testing.md`, „Der
 * Schnitt bestimmt, wo Code liegt").
 */
data class Freigabe(
    val kennungen: Set<String>,
    val zahlenraeume: Set<Int>,
) {

    /**
     * Ob eine Kennung in genau diesem Zahlenraum erreichbar ist. Der Zahlenraum muss für diese
     * Kennung überhaupt vorgesehen sein – „Verliebte Zahlen bis 100" gibt es nicht, gleichgültig
     * wie großzügig die Freigabe ist. **Fest** ist der Zahlenraum, wenn genau einer vorgesehen ist;
     * dann fragt die Freigabe nur nach der Kennung.
     */
    fun erlaubt(kennung: String, zahlenraum: Int): Boolean {
        val vorgesehen = zahlenraeumeDerKennung(kennung)
        val fest = vorgesehen.size == 1
        return kennung in kennungen &&
            zahlenraum in vorgesehen &&
            (fest || zahlenraum in zahlenraeume)
    }

    /**
     * Ob eine Kennung überhaupt noch irgendwo erreichbar ist. „Zahlenmauer (mittel)" hat fest den
     * Zahlenraum 50 und bleibt erreichbar, auch wenn die 50 abgewählt ist; Nachbarzahlen hat zwei
     * Zahlenräume und ist weg, sobald beide abgewählt sind.
     */
    fun erlaubt(kennung: String): Boolean = erlaubteZahlenraeume(kennung).isNotEmpty()

    /** Ob von einer Gruppe von Kennungen – etwa denen einer Übungsform – noch eine übrig ist. */
    fun erlaubtEineVon(kennungen: List<String>): Boolean = kennungen.any { erlaubt(it) }

    /**
     * Ob gar nichts mehr zu üben bleibt. Ein erreichbarer Zustand – es gibt keine Untergrenze –,
     * der dem Kind nur noch Abzeichen und Für Eltern auf der Startkarte lässt.
     */
    fun erlaubtNichts(): Boolean = ALLE_KENNUNGEN.none { erlaubt(it) }

    /**
     * Die Zahlenräume, die für diese Kennung wählbar bleiben: der Schnitt aus den freigegebenen
     * und den für die Kennung überhaupt vorgesehenen ([io.github.glaside.zahlenschatzjagd.BadgeCategory.ranges]).
     * Eine unbekannte Kennung hat keine – so führt eine entfallene Kennung nirgends zu einem
     * scheinbar erreichbaren Rest.
     */
    fun erlaubteZahlenraeume(kennung: String): List<Int> =
        zahlenraeumeDerKennung(kennung).filter { erlaubt(kennung, it) }

    /** Was gegenüber [ALLES] fehlt – die Form, in der die Freigabe gespeichert wird. */
    fun gesperrteKennungen(): Set<String> = ALLE_KENNUNGEN - kennungen

    /** Siehe [gesperrteKennungen]. */
    fun gesperrteZahlenraeume(): Set<Int> = BADGE_RANGES.toSet() - zahlenraeume

    companion object {
        /** Nichts abgewählt: der Zustand jedes frisch angelegten Profils. */
        val ALLES = Freigabe(kennungen = ALLE_KENNUNGEN, zahlenraeume = BADGE_RANGES.toSet())
    }
}

/** Alle heute vergebenen Fortschrittskennungen. */
val ALLE_KENNUNGEN: Set<String> = BADGE_CATEGORIES.mapTo(LinkedHashSet()) { it.key }

/**
 * Baut die Freigabe aus dem, was auf dem Gerät liegt: den **gesperrten** Mengen.
 *
 * Gespeichert wird bewusst die Sperre und nicht die Freigabe, damit **neue Kennungen für
 * bestehende Profile freigegeben sind** – eine später ergänzte Übungsform darf nicht hinter einer
 * Freigabe verschwinden, die vor ihrer Existenz gesetzt wurde. Siehe
 * `docs/adr/0018-freigabe-als-sperre-gespeichert.md`.
 *
 * Werte, die es nicht (mehr) gibt, werden still übergangen: Eine entfallene Kennung soll die
 * Freigabe eines Profils nicht unlesbar machen.
 */
fun freigabeAusSperren(gesperrteKennungen: String, gesperrteZahlenraeume: String): Freigabe {
    val gesperrteZahlen = decodeSperre(gesperrteZahlenraeume).mapNotNull(String::toIntOrNull)
    return Freigabe(
        kennungen = ALLE_KENNUNGEN - decodeSperre(gesperrteKennungen).toSet(),
        zahlenraeume = BADGE_RANGES.toSet() - gesperrteZahlen.toSet(),
    )
}

/** Die gespeicherte Form einer Sperre: durch Komma getrennt, leer = nichts gesperrt. */
fun encodeSperre(werte: Set<*>): String = werte.joinToString(SPERRE_SEP)

private const val SPERRE_SEP = ","

private fun decodeSperre(gespeichert: String): List<String> =
    gespeichert.split(SPERRE_SEP).map(String::trim).filter(String::isNotEmpty)

private fun zahlenraeumeDerKennung(kennung: String): List<Int> =
    BADGE_CATEGORIES.firstOrNull { it.key == kennung }?.ranges ?: emptyList()
