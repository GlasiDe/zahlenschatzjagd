package io.github.glaside.zahlenschatzjagd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.glaside.zahlenschatzjagd.data.Datenbankstart
import io.github.glaside.zahlenschatzjagd.ui.ZahlenschatzjagdNavHost
import io.github.glaside.zahlenschatzjagd.ui.schutzhalt.SchutzhaltScreen
import io.github.glaside.zahlenschatzjagd.ui.theme.ZahlenschatzjagdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val start = (application as ZahlenschatzjagdApp).datenbankstart
        setContent {
            ZahlenschatzjagdTheme {
                // Im Schutzhalt gibt es bewusst **keine** Navigation: Kein Bildschirm der App
                // darf die Datenbank anfassen, solange nicht feststeht, dass sie sicher zu
                // öffnen ist (`docs/adr/0021-schutzhalt-statt-destruktivem-zuruecksetzen.md`).
                when (start) {
                    is Datenbankstart.Schutzhalt ->
                        SchutzhaltScreen(start, appVersion = BuildConfig.VERSION_NAME)

                    Datenbankstart.Bereit -> ZahlenschatzjagdNavHost()
                }
            }
        }
    }
}
