package io.github.glaside.zahlenschatzjagd.ui.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.glaside.zahlenschatzjagd.data.BildschirmzeitRepository
import io.github.glaside.zahlenschatzjagd.data.ParentAccessRepository
import io.github.glaside.zahlenschatzjagd.data.RundenRepository
import io.github.glaside.zahlenschatzjagd.data.UserPreferencesRepository
import io.github.glaside.zahlenschatzjagd.data.UserProfile
import io.github.glaside.zahlenschatzjagd.data.UserProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.ZonedDateTime

/**
 * Wie lange der Zustand ohne Zuhörer weiterlebt – ein Bildschirmdreher soll den Kalender nicht neu
 * aus der Datenbank holen. Derselbe Wert wie im [FreigabeViewModel].
 */
private const val NACHLAUF_MILLIS = 5_000L

/**
 * Was der Übungskalender zeigt.
 *
 * [geladen] unterscheidet „noch nichts gelesen" von „nichts geübt". Ohne diese Angabe stünde beim
 * Öffnen für einen Moment ein leerer Kalender da, und ein Erwachsener liest dort etwas, das nicht
 * stimmt.
 */
data class UebungskalenderUiState(
    val profile: List<UserProfile> = emptyList(),
    val gewaehltesProfil: String? = null,
    val wochen: List<Kalenderwoche> = emptyList(),
    /** Können sich hier Zeiten zwischen Geschwistern vermischt haben? Siehe [UebungskalenderScreen]. */
    val geschwisterHinweis: Boolean = false,
    val geladen: Boolean = false,
)

/**
 * Zustand des Eintrags **Übungskalender** im Elternbereich: wann und wie lange ein Kind geübt hat.
 *
 * Der Kalender liest zwei Quellen, die absichtlich getrennt sind: die abgeschlossenen Runden
 * (`wann`) und die gemessene Bildschirmzeit (`wie lange`). Zusammengerechnet werden sie erst hier,
 * in [baueUebungskalender].
 *
 * [jetzt] ist die Uhr – als Parameter, weil an ihr **beides** hängt: der letzte Tag des Zeitraums
 * und die Zeitzone, in der eine Millisekunde zu einem Kalendertag wird. Nur so ist die Rechnung
 * ohne Wartezeit und ohne die Uhr des Rechners prüfbar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UebungskalenderViewModel(
    profileRepository: UserProfileRepository,
    userPreferencesRepository: UserPreferencesRepository,
    parentAccessRepository: ParentAccessRepository,
    private val rundenRepository: RundenRepository,
    private val bildschirmzeitRepository: BildschirmzeitRepository,
    private val jetzt: () -> ZonedDateTime = { ZonedDateTime.now() },
) : ViewModel() {

    /** Das im Elternbereich handverlesene Profil – siehe [bestimmeGewaehltesProfil]. */
    private val handverlesenesProfil = MutableStateFlow<String?>(null)

    /** Wessen Kalender gezeigt wird und ob der Geschwister-Hinweis nötig ist. */
    private data class Auswahl(
        val profile: List<UserProfile>,
        val gewaehltesProfil: String?,
        val profilwechselOhnePin: Boolean,
    )

    val uiState: StateFlow<UebungskalenderUiState> = combine(
        profileRepository.profiles,
        userPreferencesRepository.activeUser,
        handverlesenesProfil,
        parentAccessRepository.profilwechselOhnePin,
    ) { profile, aktivesProfil, handverlesen, profilwechselOhnePin ->
        Auswahl(
            profile = profile,
            gewaehltesProfil = bestimmeGewaehltesProfil(
                namen = profile.map { it.name },
                handverlesen = handverlesen,
                aktivesProfil = aktivesProfil,
            ),
            profilwechselOhnePin = profilwechselOhnePin,
        )
        // mapLatest: Wechselt das Profil, während noch für das vorige gelesen wird, gilt das
        // spätere. Sonst könnte die alte Antwort die neue überschreiben.
    }.mapLatest { auswahl -> zustand(auswahl) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(NACHLAUF_MILLIS),
            UebungskalenderUiState(),
        )

    fun waehleProfil(name: String) {
        handverlesenesProfil.value = name
    }

    private suspend fun zustand(auswahl: Auswahl): UebungskalenderUiState {
        val profil = auswahl.gewaehltesProfil
        val nun = jetzt()
        return UebungskalenderUiState(
            profile = auswahl.profile,
            gewaehltesProfil = profil,
            wochen = if (profil == null) {
                emptyList()
            } else {
                baueUebungskalender(
                    runden = rundenRepository.fuerProfil(profil),
                    zeitabschnitte = bildschirmzeitRepository.fuerProfil(profil),
                    heute = nun.toLocalDate(),
                    zone = nun.zone,
                )
            },
            // Nur bei mehreren Kindern eine Aussage: Wo es nichts zu vermischen gibt, wäre der
            // Hinweis eine Warnung ohne Gegenstand.
            geschwisterHinweis = auswahl.profilwechselOhnePin && auswahl.profile.size > 1,
            geladen = true,
        )
    }
}
