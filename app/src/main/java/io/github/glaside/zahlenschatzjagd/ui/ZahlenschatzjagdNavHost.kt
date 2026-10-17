package io.github.glaside.zahlenschatzjagd.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.glaside.zahlenschatzjagd.BuildConfig
import io.github.glaside.zahlenschatzjagd.GameSettings
import io.github.glaside.zahlenschatzjagd.NachbarzahlenSchwierigkeitsstufe
import io.github.glaside.zahlenschatzjagd.NachbarzahlenSettings
import io.github.glaside.zahlenschatzjagd.SoundPlayer
import io.github.glaside.zahlenschatzjagd.UnlockedBadge
import io.github.glaside.zahlenschatzjagd.ZahlenmauerDifficulty
import io.github.glaside.zahlenschatzjagd.ZahlenmauerSettings
import io.github.glaside.zahlenschatzjagd.ZahlenschatzjagdApp
import io.github.glaside.zahlenschatzjagd.data.AENDERUNGSHINWEISE
import io.github.glaside.zahlenschatzjagd.data.Aenderungshinweis
import io.github.glaside.zahlenschatzjagd.data.AppContainer
import io.github.glaside.zahlenschatzjagd.data.Freigabe
import io.github.glaside.zahlenschatzjagd.data.freigabeVon
import io.github.glaside.zahlenschatzjagd.decodeUnlockedBadges
import io.github.glaside.zahlenschatzjagd.encode
import io.github.glaside.zahlenschatzjagd.ui.aenderungshinweis.AENDERUNGSHINWEIS_GESPROCHEN
import io.github.glaside.zahlenschatzjagd.ui.aenderungshinweis.AenderungshinweisDialog
import io.github.glaside.zahlenschatzjagd.ui.aenderungshinweis.faelligeHinweiseFlow
import io.github.glaside.zahlenschatzjagd.ui.badges.BadgeScreen
import io.github.glaside.zahlenschatzjagd.ui.badges.BadgeViewModel
import io.github.glaside.zahlenschatzjagd.ui.game.GameScreen
import io.github.glaside.zahlenschatzjagd.ui.game.GameViewModel
import io.github.glaside.zahlenschatzjagd.ui.home.HomeScreen
import io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen.NachbarzahlenScreen
import io.github.glaside.zahlenschatzjagd.ui.nachbarzahlen.NachbarzahlenViewModel
import io.github.glaside.zahlenschatzjagd.ui.parent.FreigabeScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.FreigabeViewModel
import io.github.glaside.zahlenschatzjagd.ui.parent.ParentAccessViewModel
import io.github.glaside.zahlenschatzjagd.ui.parent.ParentEntry
import io.github.glaside.zahlenschatzjagd.ui.parent.ParentGateScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.ParentPinSetupScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.ParentScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.ParentZugang
import io.github.glaside.zahlenschatzjagd.ui.parent.TonUndVorlesen
import io.github.glaside.zahlenschatzjagd.ui.parent.TonUndVorlesenScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.TonUndVorlesenViewModel
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungskalenderScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungskalenderViewModel
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungsstandScreen
import io.github.glaside.zahlenschatzjagd.ui.parent.UebungsstandViewModel
import io.github.glaside.zahlenschatzjagd.ui.parent.ZugangScreen
import io.github.glaside.zahlenschatzjagd.ui.profile.ProfileScreen
import io.github.glaside.zahlenschatzjagd.ui.profile.ProfileViewModel
import io.github.glaside.zahlenschatzjagd.ui.selection.NachbarzahlenSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.RechnenMenuScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.SelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.VerdoppelnSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.VergleichenSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.ZahlenmauerSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.ZahlenspieleMenuScreen
import io.github.glaside.zahlenschatzjagd.ui.selection.ZahlenspieleSelectionScreen
import io.github.glaside.zahlenschatzjagd.ui.sicherung.BackupScreen
import io.github.glaside.zahlenschatzjagd.ui.sicherung.BackupViewModel
import io.github.glaside.zahlenschatzjagd.ui.sicherung.rememberSicherungEinspielen
import io.github.glaside.zahlenschatzjagd.ui.stats.StatsScreen
import io.github.glaside.zahlenschatzjagd.ui.theme.ParchmentBackground
import io.github.glaside.zahlenschatzjagd.ui.zahlenmauer.ZahlenmauerScreen
import io.github.glaside.zahlenschatzjagd.ui.zahlenmauer.ZahlenmauerViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Zentrale Navigation der App.
 * Einstellungen und Ergebnis werden als Navigations-Argumente übergeben, sodass kein
 * hochgezogener remember-Zustand mehr nötig ist. Das [GameViewModel] wird pro
 * Game-Ziel (NavBackStackEntry) erzeugt, also automatisch pro Runde frisch.
 *
 * [aenderungshinweise] und [aktuelleVersion] kommen nur herein, damit ein Test einen Hinweis
 * zeigen kann, ohne eine Version zu markieren; die App nimmt immer die Vorgaben.
 */
@Composable
fun ZahlenschatzjagdNavHost(
    aenderungshinweise: List<Aenderungshinweis> = AENDERUNGSHINWEISE,
    aktuelleVersion: Int = BuildConfig.VERSION_CODE,
) {
    val navController = rememberNavController()
    val appContainer = (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
    val scope = rememberCoroutineScope()

    // Änderungshinweise nach einem Update, einmal je Gerät (#8). Auf dem Weg hierher ist der
    // Schutzhalt schon ausgeschlossen: Dort gibt es diesen Bildschirm gar nicht, und der Stand
    // bleibt ungemerkt, bis eine Version wieder normal startet.
    val faelligeHinweise =
        faelligeHinweiseFlow(appContainer, aenderungshinweise, aktuelleVersion).geladen()?.wert
    // Nichts zu zeigen – Neuinstallation oder unmarkierte Version: still als gesehen merken.
    LaunchedEffect(faelligeHinweise) {
        if (faelligeHinweise?.isEmpty() == true) {
            appContainer.aenderungshinweisRepository.merkeGezeigt(aktuelleVersion)
        }
    }

    // Wessen Bildschirmzeit gerade zählt – siehe [gemessenesProfil].
    val aktuelleRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val profilFuerMessung = gemessenesProfil(appContainer, aktuelleRoute)

    // Die beiden Schalter aus dem Elternbereich zu den Stellen bringen, die sie befolgen.
    // [SoundPlayer] und [Narrator] sind prozessweit, deshalb genügt hier ein Sammler an der
    // Wurzel – gegenüber „jede Aufrufstelle liest den Schalter selbst" kann ihn so kein
    // Bildschirm vergessen. Er hängt bewusst außerhalb des NavHost: Der Schalter gilt auch
    // während einer Runde, und die Ziele darin kommen und gehen.
    val tonUndVorlesen = tonUndVorlesenFlow(appContainer)
    LaunchedEffect(tonUndVorlesen) {
        tonUndVorlesen.collect { einstellung ->
            SoundPlayer.setzeTonAn(einstellung.tonAn)
            Narrator.setzeVorlesenAn(einstellung.vorlesenAn)
        }
    }

    // Startkarte und Menüs begrüßen bei jedem Besuch, nach dem Drehen aber nicht noch einmal.
    // Dafür meldet die Navigation jeden sichtbar werdenden Eintrag, siehe [Besuchsgedaechtnis].
    val besuchsgedaechtnis = rememberBesuchsgedaechtnis()
    DisposableEffect(navController) {
        val melder = NavController.OnDestinationChangedListener { controller, _, _ ->
            controller.currentBackStackEntry?.let { besuchsgedaechtnis.wirdSichtbar(it.id) }
        }
        navController.addOnDestinationChangedListener(melder)
        onDispose { navController.removeOnDestinationChangedListener(melder) }
    }

    // Zentraler Pergament-Hintergrund für alle Screens; der Scaffold selbst bleibt
    // transparent, damit die Textur durchscheint.
    ParchmentBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // imePadding an **einer** Stelle für alle Bildschirme: Seit `enableEdgeToEdge`
            // (MainActivity) verkleinert Android das Fenster nicht mehr selbst, wenn die
            // Tastatur hochkommt – ohne diese Zeile legt sie sich über die Eingabefelder, und
            // was darunter liegt (weitere Felder, „Antwort anzeigen", der Speichern-Knopf) ist
            // unerreichbar, bis man die Tastatur wegtippt. Der Pergament-Hintergrund bleibt
            // dabei bewusst außen vor und füllt weiter den ganzen Bildschirm.
            // Die Messung der Bildschirmzeit liegt hier ganz außen, damit sie jeden Tipp sieht.
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .bildschirmzeitMessen(
                    profil = profilFuerMessung,
                    speichere = appContainer.bildschirmzeitRepository::merke,
                ),
            containerColor = Color.Transparent
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Routes.HOME) { eintrag ->
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    // Drei Datenströme, die alle erst antworten müssen – siehe [geladen]. Ohne
                    // das Warten würde jede Frischinstallation kurz fälschlich als profillos
                    // erkannt, und beim Schalter wäre jede Annahme falsch: „frei" öffnete die
                    // Profilverwaltung an einem Tor vorbei, „nicht frei" stellte einem Kind eines
                    // in den Weg, das gar keines haben soll. Auch der aktive Name muss warten:
                    // Vor der ersten Antwort steht dort der Vorgabe-Name, und der passt auf kein
                    // angelegtes Profil – die Weiche darunter schösse sofort los.
                    val profile = container.userProfileRepository.profiles.geladen()
                    val profilwechselOhnePin =
                        container.parentAccessRepository.profilwechselOhnePin.geladen()
                    val aktiverName = container.userPreferencesRepository.activeUser.geladen()
                    // Wer spielt gerade? Ohne Antwort darauf hat die Startkarte nichts zu suchen:
                    // Ohne passendes Profil gilt [freigabeVon] als „alles offen" – nach dem
                    // Einspielen einer Sicherung stünden alle Inseln offen, obwohl die
                    // eingespielten Freigaben etwas anderes sagen. Der Profilbildschirm fragt
                    // stattdessen nach; bei leerer Liste bietet er dort auch die Sicherung an.
                    LaunchedEffect(profile, aktiverName) {
                        if (profile == null || aktiverName == null) return@LaunchedEffect
                        if (profile.wert.none { it.name == aktiverName.wert }) {
                            navController.navigate(
                                Routes.profile(ohneProfil = profile.wert.isEmpty())
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }
                    // Auch auf die Änderungshinweise warten: Steht einer an, spricht Ella seinen
                    // Satz statt der Begrüßung. Kämen beide, schnitte der spätere den früheren
                    // ab, und welcher später kommt, wäre Zufall.
                    if (profile == null || profilwechselOhnePin == null || aktiverName == null) {
                        return@composable
                    }
                    if (faelligeHinweise == null) return@composable
                    val activeUser = aktiverName.wert
                    val activeProfile = profile.wert.firstOrNull { it.name == activeUser }
                    HomeScreen(
                        // Aus denselben Daten wie oben – ein zweiter Datenstrom brächte nichts.
                        freigabe = profile.wert.freigabeVon(activeUser),
                        activeAvatarId = activeProfile?.avatarId,
                        activeName = activeProfile?.name,
                        onOpenRechnen = { navController.navigate(Routes.RECHNEN_MENU) },
                        onOpenZahlenspiele = { navController.navigate(Routes.ZAHLENSPIELE_MENU) },
                        onOpenBadges = { navController.navigate(Routes.BADGES) },
                        onOpenProfil = {
                            // Beim freien Profilwechsel geht es direkt zum Bildschirm – der zeigt
                            // dann nur das Wechseln. Sonst steht das Tor schon davor, und wer es
                            // passiert, darf gleich alles.
                            navController.navigate(
                                if (profilwechselOhnePin.wert) {
                                    Routes.profile()
                                } else {
                                    Routes.parentGate(Torziel.PROFILVERWALTUNG)
                                }
                            )
                        },
                        onOpenEltern = {
                            navController.navigate(Routes.parentGate(Torziel.ELTERNBEREICH))
                        },
                        // Das Besuchsgedächtnis zuerst fragen, damit der Besuch auch dann als
                        // begrüßt gilt, wenn der Änderungshinweis den Satz übernimmt.
                        begruessungFaellig = {
                            besuchsgedaechtnis.begruessungFaellig(eintrag.id) &&
                                faelligeHinweise.isEmpty()
                        },
                    )
                }

                composable(Routes.RECHNEN_MENU) { eintrag ->
                    val freigabe = aktiveFreigabe() ?: return@composable
                    RechnenMenuScreen(
                        freigabe = freigabe,
                        onOpenGrundrechenarten = { navController.navigate(Routes.SELECTION) },
                        onOpenVerdoppeln = { navController.navigate(Routes.VERDOPPELN_SELECTION) },
                        onOpenZahlenmauer = { navController.navigate(Routes.ZAHLENMAUER_SELECTION) },
                        begruessungFaellig = { besuchsgedaechtnis.begruessungFaellig(eintrag.id) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.VERDOPPELN_SELECTION) {
                    val freigabe = aktiveFreigabe() ?: return@composable
                    VerdoppelnSelectionScreen(
                        freigabe = freigabe,
                        onStartGame = { settings ->
                            navController.navigate(Routes.game(settings))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.ZAHLENMAUER_SELECTION) {
                    val freigabe = aktiveFreigabe() ?: return@composable
                    ZahlenmauerSelectionScreen(
                        freigabe = freigabe,
                        onStartGame = { settings ->
                            navController.navigate(Routes.zahlenmauerGame(settings))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SELECTION) {
                    val freigabe = aktiveFreigabe() ?: return@composable
                    SelectionScreen(
                        freigabe = freigabe,
                        onStartGame = { settings ->
                            navController.navigate(Routes.game(settings))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.ZAHLENSPIELE_MENU) { eintrag ->
                    val freigabe = aktiveFreigabe() ?: return@composable
                    ZahlenspieleMenuScreen(
                        freigabe = freigabe,
                        onOpenVerliebte = { navController.navigate(Routes.ZAHLENSPIELE_SELECTION) },
                        onOpenVergleichen = { navController.navigate(Routes.VERGLEICHEN_SELECTION) },
                        onOpenNachbarzahlen = { navController.navigate(Routes.NACHBARZAHLEN_SELECTION) },
                        begruessungFaellig = { besuchsgedaechtnis.begruessungFaellig(eintrag.id) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.ZAHLENSPIELE_SELECTION) {
                    val freigabe = aktiveFreigabe() ?: return@composable
                    ZahlenspieleSelectionScreen(
                        freigabe = freigabe,
                        onStartGame = { settings ->
                            navController.navigate(Routes.game(settings))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.VERGLEICHEN_SELECTION) {
                    val freigabe = aktiveFreigabe() ?: return@composable
                    VergleichenSelectionScreen(
                        freigabe = freigabe,
                        onStartGame = { settings ->
                            navController.navigate(Routes.game(settings))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.NACHBARZAHLEN_SELECTION) {
                    val freigabe = aktiveFreigabe() ?: return@composable
                    NachbarzahlenSelectionScreen(
                        freigabe = freigabe,
                        onStartGame = { settings ->
                            navController.navigate(Routes.nachbarzahlenGame(settings))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.GAME,
                    arguments = listOf(
                        navArgument("types") { type = NavType.StringType },
                        navArgument("range") { type = NavType.IntType },
                        navArgument("count") { type = NavType.IntType },
                        navArgument("coins") { type = NavType.BoolType },
                        navArgument("mc") { type = NavType.BoolType },
                    )
                ) { backStackEntry ->
                    val settings = backStackEntry.toGameSettings()
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val gameViewModel: GameViewModel = viewModel {
                        GameViewModel(
                            settings = settings,
                            taskRepository = container.taskRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                            rundenRepository = container.rundenRepository,
                        )
                    }
                    GameScreen(
                        viewModel = gameViewModel,
                        onFinish = { score, unlocked ->
                            navController.navigate(Routes.stats(score, settings, unlocked)) {
                                popUpTo(Routes.GAME) { inclusive = true }
                            }
                        },
                        onCancel = {
                            navController.popBackStack(Routes.selectionFor(settings), inclusive = false)
                        }
                    )
                }

                composable(
                    route = Routes.STATS,
                    arguments = listOf(
                        navArgument("correct") { type = NavType.IntType },
                        navArgument("total") { type = NavType.IntType },
                        navArgument("types") { type = NavType.StringType },
                        navArgument("range") { type = NavType.IntType },
                        navArgument("count") { type = NavType.IntType },
                        navArgument("coins") { type = NavType.BoolType },
                        navArgument("mc") { type = NavType.BoolType },
                        navArgument("unlocked") { type = NavType.StringType },
                    )
                ) { backStackEntry ->
                    val args = backStackEntry.arguments!!
                    val settings = backStackEntry.toGameSettings()
                    StatsScreen(
                        correct = args.getInt("correct"),
                        total = args.getInt("total"),
                        unlockedBadges = decodeUnlockedBadges(args.getString("unlocked") ?: "none"),
                        onOpenBadges = { navController.navigate(Routes.BADGES) },
                        onPlayAgain = {
                            navController.navigate(Routes.game(settings)) {
                                popUpTo(Routes.STATS) { inclusive = true }
                            }
                        },
                        onBackToSelection = {
                            navController.popBackStack(Routes.selectionFor(settings), inclusive = false)
                        }
                    )
                }

                composable(
                    route = Routes.ZAHLENMAUER_GAME,
                    arguments = listOf(
                        navArgument("difficulty") { type = NavType.StringType },
                        navArgument("wallCount") { type = NavType.IntType },
                    )
                ) { backStackEntry ->
                    val settings = backStackEntry.toZahlenmauerSettings()
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val zahlenmauerViewModel: ZahlenmauerViewModel = viewModel {
                        ZahlenmauerViewModel(
                            settings = settings,
                            taskRepository = container.taskRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                            rundenRepository = container.rundenRepository,
                        )
                    }
                    ZahlenmauerScreen(
                        viewModel = zahlenmauerViewModel,
                        onFinish = { correct, total, unlocked ->
                            navController.navigate(Routes.zahlenmauerStats(correct, total, settings, unlocked)) {
                                popUpTo(Routes.ZAHLENMAUER_GAME) { inclusive = true }
                            }
                        },
                        onCancel = {
                            navController.popBackStack(Routes.ZAHLENMAUER_SELECTION, inclusive = false)
                        }
                    )
                }

                composable(
                    route = Routes.ZAHLENMAUER_STATS,
                    arguments = listOf(
                        navArgument("correctWalls") { type = NavType.IntType },
                        navArgument("totalWalls") { type = NavType.IntType },
                        navArgument("difficulty") { type = NavType.StringType },
                        navArgument("wallCount") { type = NavType.IntType },
                        navArgument("unlocked") { type = NavType.StringType },
                    )
                ) { backStackEntry ->
                    val args = backStackEntry.arguments!!
                    val settings = backStackEntry.toZahlenmauerSettings()
                    StatsScreen(
                        correct = args.getInt("correctWalls"),
                        total = args.getInt("totalWalls"),
                        unlockedBadges = decodeUnlockedBadges(args.getString("unlocked") ?: "none"),
                        onOpenBadges = { navController.navigate(Routes.BADGES) },
                        onPlayAgain = {
                            navController.navigate(Routes.zahlenmauerGame(settings)) {
                                popUpTo(Routes.ZAHLENMAUER_STATS) { inclusive = true }
                            }
                        },
                        onBackToSelection = {
                            navController.popBackStack(Routes.ZAHLENMAUER_SELECTION, inclusive = false)
                        }
                    )
                }

                composable(
                    route = Routes.NACHBARZAHLEN_GAME,
                    arguments = listOf(
                        navArgument("schwierigkeitsstufe") { type = NavType.StringType },
                        navArgument("zahlenraum") { type = NavType.IntType },
                        navArgument("aufgabenzahl") { type = NavType.IntType },
                    )
                ) { backStackEntry ->
                    val settings = backStackEntry.toNachbarzahlenSettings()
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val nachbarzahlenViewModel: NachbarzahlenViewModel = viewModel {
                        NachbarzahlenViewModel(
                            settings = settings,
                            taskRepository = container.taskRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                            rundenRepository = container.rundenRepository,
                        )
                    }
                    NachbarzahlenScreen(
                        viewModel = nachbarzahlenViewModel,
                        onFinish = { richtige, gesamt, unlocked ->
                            navController.navigate(
                                Routes.nachbarzahlenStats(richtige, gesamt, settings, unlocked)
                            ) {
                                popUpTo(Routes.NACHBARZAHLEN_GAME) { inclusive = true }
                            }
                        },
                        onCancel = {
                            navController.popBackStack(Routes.NACHBARZAHLEN_SELECTION, inclusive = false)
                        }
                    )
                }

                composable(
                    route = Routes.NACHBARZAHLEN_STATS,
                    arguments = listOf(
                        navArgument("richtige") { type = NavType.IntType },
                        navArgument("gesamt") { type = NavType.IntType },
                        navArgument("schwierigkeitsstufe") { type = NavType.StringType },
                        navArgument("zahlenraum") { type = NavType.IntType },
                        navArgument("aufgabenzahl") { type = NavType.IntType },
                        navArgument("unlocked") { type = NavType.StringType },
                    )
                ) { backStackEntry ->
                    val args = backStackEntry.arguments!!
                    val settings = backStackEntry.toNachbarzahlenSettings()
                    StatsScreen(
                        correct = args.getInt("richtige"),
                        total = args.getInt("gesamt"),
                        unlockedBadges = decodeUnlockedBadges(args.getString("unlocked") ?: "none"),
                        onOpenBadges = { navController.navigate(Routes.BADGES) },
                        onPlayAgain = {
                            navController.navigate(Routes.nachbarzahlenGame(settings)) {
                                popUpTo(Routes.NACHBARZAHLEN_STATS) { inclusive = true }
                            }
                        },
                        onBackToSelection = {
                            navController.popBackStack(Routes.NACHBARZAHLEN_SELECTION, inclusive = false)
                        }
                    )
                }

                composable(Routes.BADGES) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val badgeViewModel: BadgeViewModel = viewModel {
                        BadgeViewModel(
                            taskRepository = container.taskRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                        )
                    }
                    BadgeScreen(
                        viewModel = badgeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    Routes.PROFILE_PATTERN,
                    arguments = listOf(
                        navArgument("ohneProfil") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument("verwaltung") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { backStackEntry ->
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val profileViewModel: ProfileViewModel = viewModel {
                        ProfileViewModel(
                            profileRepository = container.userProfileRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                        )
                    }
                    // Steht unbedingt da, auch wenn der Erststart-Weg gar nicht gezeigt wird: An
                    // ihm hängt die Meldung zur eingespielten Datei, und sobald das Einspielen
                    // geklappt hat, gibt es Profile – die Bedingung schlüge also genau in dem
                    // Moment um, in dem die Meldung erscheinen soll, und nähme sie mit.
                    val backupViewModel: BackupViewModel = viewModel {
                        BackupViewModel(
                            backupRepository = container.backupRepository,
                            appVersionName = BuildConfig.VERSION_NAME,
                        )
                    }
                    val sicherungMeldung by backupViewModel.message.collectAsStateWithLifecycle()
                    val einspielen = rememberSicherungEinspielen(backupViewModel)
                    val showFirstProfileHint by
                        profileViewModel.firstProfileCreated.collectAsStateWithLifecycle()
                    // Wieder das Warten (siehe [geladen]), und hier wiegt es am schwersten: Eine
                    // Liste, die noch keine Antwort ist, sieht genauso aus wie „keine Profile" –
                    // und daran hängt unten die Ausnahme vom Tor. Ohne das Warten stünden nach
                    // einem Neustart der App kurz beide Wege ohne Tor offen, obwohl es längst
                    // Profile zu schützen gibt.
                    val geladeneProfile = container.userProfileRepository.profiles.geladen()
                    val aktiverName = container.userPreferencesRepository.activeUser.geladen()
                    if (geladeneProfile == null || aktiverName == null) return@composable
                    val profiles = geladeneProfile.wert
                    val verwaltung = backStackEntry.arguments?.getBoolean("verwaltung") ?: false
                    // Ohne Profil kommt man ohne Tor hierher und muss trotzdem weiterkommen: Es
                    // gibt weder ein Profil zu schützen noch einen anderen Weg hinein – die App
                    // bestünde sonst aus diesem einen verschlossenen Bildschirm. Das gilt für
                    // beide Wege, das Anlegen wie das Einspielen. Die Ausnahme endet mit dem
                    // ersten Profil, also genau dann, wenn es etwas zu schützen gibt.
                    val ohneProfil = (backStackEntry.arguments?.getBoolean("ohneProfil") ?: false) &&
                        profiles.isEmpty()
                    ProfileScreen(
                        profiles = profiles,
                        activeUser = aktiverName.wert,
                        verwaltungErlaubt = verwaltung || ohneProfil,
                        sicherungAnbieten = ohneProfil,
                        sicherungMeldung = sicherungMeldung,
                        onSicherungEinspielen = einspielen,
                        showFirstProfileHint = showFirstProfileHint,
                        onSwitch = profileViewModel::switchProfile,
                        onCreate = profileViewModel::createProfile,
                        onRename = profileViewModel::renameProfile,
                        onChangeAvatar = profileViewModel::changeAvatar,
                        onDelete = profileViewModel::deleteProfile,
                        onVerwaltungAnfordern = {
                            navController.navigate(Routes.parentGate(Torziel.PROFILVERWALTUNG))
                        },
                        onDismissFirstProfileHint = {
                            profileViewModel.dismissFirstProfileHint()
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.PARENT_GATE_PATTERN,
                    arguments = listOf(
                        navArgument("ziel") {
                            type = NavType.StringType
                            defaultValue = Torziel.ELTERNBEREICH.name
                        },
                    ),
                ) { backStackEntry ->
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    // Warten wie beim HOME-Ziel (siehe [geladen]): Ohne blitzte einen Frame lang
                    // die Elternfrage auf, obwohl eine PIN gilt.
                    val zugang = container.parentAccessRepository.access.geladen()
                    val ziel = backStackEntry.torziel()
                    zugang?.let { state ->
                        ParentGateScreen(
                            access = state.wert,
                            onPass = {
                                navController.navigate(ziel.route) {
                                    // Alles über der Startkarte abräumen: Das passierte Tor darf
                                    // nicht im Rücken stehen bleiben, und beim Ziel
                                    // Profilverwaltung liegt darunter noch der Bildschirm, von dem
                                    // aus es angefordert wurde.
                                    popUpTo(Routes.HOME)
                                }
                            },
                            // Der Rückweg bei vergessener PIN endet immer beim Vergeben einer
                            // neuen und damit im Elternbereich – ganz gleich, wozu das Tor
                            // aufgemacht werden sollte.
                            onResetPassed = {
                                navController.navigate(Routes.PARENT_PIN_SETUP) {
                                    popUpTo(Routes.HOME)
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.PARENT) {
                    ParentScreen(
                        onOpenEntry = { navController.navigate(Routes.parentEntry(it)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.parentEntry(ParentEntry.ZUGANG)) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val accessViewModel: ParentAccessViewModel = viewModel {
                        ParentAccessViewModel(container.parentAccessRepository)
                    }
                    // Wieder das Warten: Beide Angaben stammen aus dem DataStore, und beide sagen
                    // vor der ersten Antwort das Gegenteil dessen, was eingestellt sein kann.
                    val zugangFlow = remember(container) {
                        combine(
                            container.parentAccessRepository.access,
                            container.parentAccessRepository.profilwechselOhnePin,
                        ) { access, profilwechselOhnePin ->
                            ParentZugang(
                                pinIsSet = access != null,
                                profilwechselOhnePin = profilwechselOhnePin,
                            )
                        }
                    }
                    val zugang = zugangFlow.geladen()
                    zugang?.let { state ->
                        ZugangScreen(
                            zugang = state.wert,
                            onManagePin = { navController.navigate(Routes.PARENT_PIN_SETUP) },
                            onRemovePin = accessViewModel::clearPin,
                            onSetzeProfilwechsel = accessViewModel::setzeProfilwechselOhnePin,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.parentEntry(ParentEntry.FREIGABE)) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val freigabeViewModel: FreigabeViewModel = viewModel {
                        FreigabeViewModel(
                            profileRepository = container.userProfileRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                        )
                    }
                    FreigabeScreen(
                        viewModel = freigabeViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.parentEntry(ParentEntry.UEBUNGSSTAND)) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val standViewModel: UebungsstandViewModel = viewModel {
                        UebungsstandViewModel(
                            profileRepository = container.userProfileRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                            taskRepository = container.taskRepository,
                        )
                    }
                    val stand by standViewModel.uiState.collectAsStateWithLifecycle()
                    UebungsstandScreen(
                        state = stand,
                        onWaehleProfil = standViewModel::waehleProfil,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.parentEntry(ParentEntry.UEBUNGSKALENDER)) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val kalenderViewModel: UebungskalenderViewModel = viewModel {
                        UebungskalenderViewModel(
                            profileRepository = container.userProfileRepository,
                            userPreferencesRepository = container.userPreferencesRepository,
                            parentAccessRepository = container.parentAccessRepository,
                            rundenRepository = container.rundenRepository,
                            bildschirmzeitRepository = container.bildschirmzeitRepository,
                        )
                    }
                    val kalender by kalenderViewModel.uiState.collectAsStateWithLifecycle()
                    UebungskalenderScreen(
                        state = kalender,
                        onWaehleProfil = kalenderViewModel::waehleProfil,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.parentEntry(ParentEntry.TON_UND_VORLESEN)) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val tonViewModel: TonUndVorlesenViewModel = viewModel {
                        TonUndVorlesenViewModel(container.tonUndVorlesenRepository)
                    }
                    // Wieder das Warten (siehe [geladen]): Vor der ersten Antwort stünden beide
                    // Schalter auf „an", und ein Elternteil, das gerade ausgeschaltet hat, sähe
                    // beim Öffnen für einen Frame das Gegenteil.
                    val einstellung = tonUndVorlesenFlow(container).geladen()
                    einstellung?.let { state ->
                        TonUndVorlesenScreen(
                            einstellung = state.wert,
                            onSetzeTon = tonViewModel::setzeTonAn,
                            onSetzeVorlesen = tonViewModel::setzeVorlesenAn,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.parentEntry(ParentEntry.SICHERUNG)) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val backupViewModel: BackupViewModel = viewModel {
                        BackupViewModel(
                            backupRepository = container.backupRepository,
                            appVersionName = BuildConfig.VERSION_NAME,
                        )
                    }
                    BackupScreen(
                        viewModel = backupViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.PARENT_PIN_SETUP) {
                    val container =
                        (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
                    val accessViewModel: ParentAccessViewModel = viewModel {
                        ParentAccessViewModel(container.parentAccessRepository)
                    }
                    // Auch hier wird gewartet: Das Formular merkt sich die vorhandene
                    // Sicherheitsfrage beim ersten Zeichnen. Ohne das Warten stünde dort für
                    // immer der leere Wert aus dem Frame vor der ersten DataStore-Antwort.
                    val zugang = container.parentAccessRepository.access.geladen()
                    // Nach dem Vergeben landet man in der Übersicht – für beide Wege gleich, den
                    // aus dem Zugang wie den über die Sicherheitsfrage, der den Elternbereich nie
                    // betreten hat. Der Zugang wäre das nähere Ziel, kostete aber einen zweiten
                    // navigate-Aufruf direkt hinter dem ersten; ein Sprung, der sich nicht in
                    // einem Schritt beschreiben lässt, gehört nicht in einen Rückweg, den man nur
                    // im Ausnahmefall geht. Von der Übersicht ist der Zugang die erste Zeile.
                    val openParent: () -> Unit = {
                        navController.navigate(Routes.PARENT) { popUpTo(Routes.HOME) }
                    }
                    zugang?.let { state ->
                        ParentPinSetupScreen(
                            existingQuestion = state.wert?.question,
                            onSave = { pin, question, answer ->
                                accessViewModel.setPin(pin, question, answer)
                                openParent()
                            },
                            onChangePinOnly = { pin ->
                                accessViewModel.changePin(pin)
                                openParent()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        // Außerhalb des NavHost, damit er über jedem Ziel liegt – auch über dem
        // Profilbildschirm, auf den die Startkarte ohne aktives Profil umleitet. So kommt erst
        // der Hinweis und dann die Profilfrage.
        if (!faelligeHinweise.isNullOrEmpty()) {
            VorlesenBeimErstenErscheinen(AENDERUNGSHINWEIS_GESPROCHEN)
            AenderungshinweisDialog(
                hinweise = faelligeHinweise,
                onVerstanden = {
                    scope.launch {
                        appContainer.aenderungshinweisRepository.merkeGezeigt(aktuelleVersion)
                    }
                },
            )
        }
    }
}

/**
 * Die Freigabe des Profils, das gerade spielt – `null`, solange sie noch nicht geladen ist.
 *
 * Der Sentinel ist hier kein Schönheitsfehler, sondern Absicht (wie beim Zugang zum
 * Elternbereich): Würde stattdessen bis zur ersten Antwort „alles erlaubt" angenommen, blitzten
 * gesperrte Inseln und Kacheln für einen Frame auf. Die Aufrufer zeichnen deshalb erst, wenn die
 * Antwort da ist.
 */
@Composable
private fun aktiveFreigabe(): Freigabe? {
    val container = (LocalContext.current.applicationContext as ZahlenschatzjagdApp).container
    val flow = remember(container) {
        combine(
            container.userProfileRepository.profiles,
            container.userPreferencesRepository.activeUser,
        ) { profile, aktiv -> profile.freigabeVon(aktiv) as Freigabe? }
    }
    val freigabe by flow.collectAsStateWithLifecycle(initialValue = null)
    return freigabe
}

/**
 * Was auf diesem Gerät zu hören ist. Beide Schalter kommen aus demselben DataStore, der bei
 * **jeder** Änderung an **irgendeiner** Einstellung neu ausliefert – deshalb das
 * `distinctUntilChanged`: Ohne es setzte ein Profilwechsel die Schalter erneut und schnitte dabei
 * einen gerade laufenden Vorlese-Satz ab.
 */
@Composable
private fun tonUndVorlesenFlow(container: AppContainer): Flow<TonUndVorlesen> =
    remember(container) {
        combine(
            container.tonUndVorlesenRepository.tonAn,
            container.tonUndVorlesenRepository.vorlesenAn,
        ) { tonAn, vorlesenAn ->
            TonUndVorlesen(tonAn = tonAn, vorlesenAn = vorlesenAn)
        }.distinctUntilChanged()
    }

/**
 * Das Profil, dessen **Bildschirmzeit** gerade läuft – `null`, wenn keine gemessen wird.
 *
 * Zwei Gründe für `null`, und beide bedeuten „diese Zeit gehört keinem Kind":
 * - Es gibt kein aktives Profil. Der Vorgabe-Name aus dem DataStore genügt nicht; erst ein
 *   wirklich angelegtes Profil zählt, sonst sammelte die Frischinstallation Zeit für ein Kind,
 *   das es nicht gibt.
 * - Der Elternbereich ist offen (siehe [zaehltAlsBildschirmzeit]).
 */
@Composable
private fun gemessenesProfil(container: AppContainer, route: String?): String? {
    val flow = remember(container) {
        combine(
            container.userProfileRepository.profiles,
            container.userPreferencesRepository.activeUser,
        ) { profile, aktiv -> profile.firstOrNull { it.name == aktiv }?.name }
    }
    val aktiv by flow.collectAsStateWithLifecycle(initialValue = null)
    return aktiv?.takeIf { zaehltAlsBildschirmzeit(route) }
}

/**
 * Zählt die Zeit auf dieser Route als Bildschirmzeit eines Kindes?
 *
 * Alles unter [Routes.PARENT] ist Elternbereich – die Übersicht, ihre Unter-Bildschirme, das Tor
 * davor und das Vergeben der PIN. Diese Zeit wird verworfen, sonst misst die Auswertung die
 * Erwachsenen und wächst bei jedem Hineinschauen
 * (`docs/adr/0017-bildschirmzeit-gemessen-statt-geschaetzt.md`).
 *
 * Die **Profilverwaltung** zählt dagegen mit: Das Wechseln ist der Alltagsvorgang eines Kindes,
 * kein Blick der Eltern. Wer sie über das Tor betritt, verbringt die Torzeit ohnehin schon
 * außerhalb der Messung.
 *
 * Ohne Route – vor dem ersten Ziel – wird nicht gemessen.
 */
internal fun zaehltAlsBildschirmzeit(route: String?): Boolean =
    route != null && !route.startsWith(Routes.PARENT)

/** Zentrale Routen-Definitionen inkl. Encodierung der [GameSettings] in den Pfad. */
internal object Routes {
    const val HOME = "home"
    const val SELECTION = "selection"
    const val RECHNEN_MENU = "rechnen_menu"
    const val VERDOPPELN_SELECTION = "verdoppeln"
    const val ZAHLENMAUER_SELECTION = "zahlenmauer"
    const val ZAHLENMAUER_GAME = "zahlenmauer_game/{difficulty}/{wallCount}"
    const val ZAHLENMAUER_STATS = "zahlenmauer_stats/{correctWalls}/{totalWalls}/{difficulty}/{wallCount}/{unlocked}"
    const val ZAHLENSPIELE_MENU = "zahlenspiele_menu"
    const val ZAHLENSPIELE_SELECTION = "zahlenspiele"
    const val VERGLEICHEN_SELECTION = "vergleichen"
    const val NACHBARZAHLEN_SELECTION = "nachbarzahlen"
    const val NACHBARZAHLEN_GAME = "nachbarzahlen_game/{schwierigkeitsstufe}/{zahlenraum}/{aufgabenzahl}"
    const val NACHBARZAHLEN_STATS =
        "nachbarzahlen_stats/{richtige}/{gesamt}/{schwierigkeitsstufe}/{zahlenraum}/{aufgabenzahl}/{unlocked}"
    const val GAME = "game/{types}/{range}/{count}/{coins}/{mc}"
    const val STATS = "stats/{correct}/{total}/{types}/{range}/{count}/{coins}/{mc}/{unlocked}"
    const val BADGES = "badges"
    const val PROFILE_PATTERN = "profile?ohneProfil={ohneProfil}&verwaltung={verwaltung}"

    /**
     * Die Profilverwaltung. [verwaltung] entscheidet, ob dort mehr als das Wechseln möglich ist –
     * es steht im Pfad und nicht in einem gemerkten Zustand, damit ein Zurück auf die Startkarte
     * die Freischaltung mitnimmt statt sie liegen zu lassen.
     *
     * [ohneProfil] sagt, dass es noch gar kein Profil gibt: Dann führen von hier ohne Tor beide
     * Wege weiter – anlegen und eine Sicherung einspielen.
     */
    fun profile(ohneProfil: Boolean = false, verwaltung: Boolean = false) =
        "profile?ohneProfil=$ohneProfil&verwaltung=$verwaltung"

    const val PARENT_GATE_PATTERN = "parent_gate?ziel={ziel}"
    fun parentGate(ziel: Torziel) = "parent_gate?ziel=${ziel.name}"
    const val PARENT = "parent"
    const val PARENT_PIN_SETUP = "parent/pin"

    /** Das Ziel hinter einem Eintrag der Elternbereichs-Übersicht. */
    fun parentEntry(entry: ParentEntry): String = "parent/${entry.slug}"

    fun game(s: GameSettings) =
        "game/${s.types.joinToString(",")}/${s.range}/${s.count}/${s.showCoins}/${s.multipleChoice}"

    /**
     * Das Auswahl-Ziel, aus dem eine Runde gestartet wurde – nötig, damit „Abbrechen" (✕) und
     * „Andere Aufgaben" zum passenden Konfigurations-Bildschirm zurückspringen. Die Zahlenspiele
     * und Rechenspiele haben je Spiel einen eigenen Konfig-Screen („Verliebte Zahlen" →
     * [ZAHLENSPIELE_SELECTION], „Zahlen vergleichen" → [VERGLEICHEN_SELECTION], „Verdoppeln" →
     * [VERDOPPELN_SELECTION]); alles andere (Plus/Minus/Mal/Geteilt) kommt aus [SELECTION].
     */
    fun selectionFor(s: GameSettings): String = when {
        s.types.contains("Verliebte") -> ZAHLENSPIELE_SELECTION
        s.types.contains("Vergleichen") -> VERGLEICHEN_SELECTION
        s.types.contains("Verdoppeln") -> VERDOPPELN_SELECTION
        else -> SELECTION
    }

    fun stats(correct: Int, s: GameSettings, unlocked: List<UnlockedBadge>): String {
        val encoded = if (unlocked.isEmpty()) {
            "none"
        } else {
            unlocked.joinToString(",") { it.encode() }
        }
        val types = s.types.joinToString(",")
        return "stats/$correct/${s.count}/$types/${s.range}/${s.count}/${s.showCoins}/${s.multipleChoice}/$encoded"
    }

    fun zahlenmauerGame(s: ZahlenmauerSettings) = "zahlenmauer_game/${s.difficulty.name}/${s.wallCount}"

    fun zahlenmauerStats(correct: Int, total: Int, s: ZahlenmauerSettings, unlocked: List<UnlockedBadge>): String {
        val encoded = if (unlocked.isEmpty()) "none" else unlocked.joinToString(",") { it.encode() }
        return "zahlenmauer_stats/$correct/$total/${s.difficulty.name}/${s.wallCount}/$encoded"
    }

    fun nachbarzahlenGame(s: NachbarzahlenSettings) =
        "nachbarzahlen_game/${s.schwierigkeitsstufe.name}/${s.zahlenraum}/${s.aufgabenzahl}"

    fun nachbarzahlenStats(
        richtige: Int,
        gesamt: Int,
        s: NachbarzahlenSettings,
        unlocked: List<UnlockedBadge>,
    ): String {
        val encoded = if (unlocked.isEmpty()) "none" else unlocked.joinToString(",") { it.encode() }
        val runde = "${s.schwierigkeitsstufe.name}/${s.zahlenraum}/${s.aufgabenzahl}"
        return "nachbarzahlen_stats/$richtige/$gesamt/$runde/$encoded"
    }
}

/**
 * Der erste echte Wert eines Datenstroms, in eine Hülle gesteckt – `null`, solange noch keiner
 * angekommen ist.
 *
 * Die Hülle ist der Grund für diese Funktion: Ohne sie ließe sich „noch nicht geladen" nicht von
 * einer Antwort unterscheiden, die selbst `null`, leer oder `false` ist. Genau daran hing hier
 * schon dreimal etwas Sichtbares — die Elternfrage blitzte auf, obwohl eine PIN galt; die
 * Frischinstallation galt kurz als profillos. Die Aufrufer zeichnen deshalb erst, wenn die
 * Antwort da ist, statt bis dahin etwas anzunehmen.
 */
@Composable
private fun <T> Flow<T>.geladen(): Geladen<T>? {
    val gehuellt = remember(this) { map { Geladen(it) } }
    return gehuellt.collectAsStateWithLifecycle(initialValue = null).value
}

private data class Geladen<T>(val wert: T)

/**
 * Wozu das Tor aufgemacht wird. **Ein** Tor, zwei Ziele: Die Prüfung – Elternfrage oder PIN – ist
 * dieselbe, nur der Weg dahinter unterscheidet sich (siehe
 * `docs/adr/0019-profilverwaltung-hinter-dem-tor.md`).
 */
internal enum class Torziel {
    ELTERNBEREICH,
    PROFILVERWALTUNG,
    ;

    /** Wohin es nach bestandener Prüfung weitergeht. */
    val route: String
        get() = when (this) {
            ELTERNBEREICH -> Routes.PARENT
            PROFILVERWALTUNG -> Routes.profile(verwaltung = true)
        }
}

/** Das Ziel aus den Navigations-Argumenten; ohne oder mit unbekannter Angabe der Elternbereich. */
private fun NavBackStackEntry.torziel(): Torziel {
    val angegeben = arguments?.getString("ziel")
    return Torziel.entries.firstOrNull { it.name == angegeben } ?: Torziel.ELTERNBEREICH
}

private fun NavBackStackEntry.toGameSettings(): GameSettings {
    val args = requireNotNull(arguments)
    return GameSettings(
        types = requireNotNull(args.getString("types")).split(","),
        range = args.getInt("range"),
        count = args.getInt("count"),
        showCoins = args.getBoolean("coins"),
        multipleChoice = args.getBoolean("mc"),
    )
}

private fun NavBackStackEntry.toZahlenmauerSettings(): ZahlenmauerSettings {
    val args = requireNotNull(arguments)
    return ZahlenmauerSettings(
        difficulty = ZahlenmauerDifficulty.valueOf(requireNotNull(args.getString("difficulty"))),
        wallCount = args.getInt("wallCount"),
    )
}

private fun NavBackStackEntry.toNachbarzahlenSettings(): NachbarzahlenSettings {
    val args = requireNotNull(arguments)
    val stufenName = requireNotNull(args.getString("schwierigkeitsstufe"))
    return NachbarzahlenSettings(
        schwierigkeitsstufe = NachbarzahlenSchwierigkeitsstufe.valueOf(stufenName),
        zahlenraum = args.getInt("zahlenraum"),
        aufgabenzahl = args.getInt("aufgabenzahl"),
    )
}
