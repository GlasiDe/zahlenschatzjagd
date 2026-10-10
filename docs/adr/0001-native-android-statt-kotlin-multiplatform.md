# Native Android statt Kotlin Multiplatform

Die App bleibt eine native Android-App in Kotlin/Jetpack Compose. Ein Wechsel zu Kotlin
Multiplatform wurde geprüft und bewusst zurückgestellt: Der Umbauaufwand und die
organisatorische Abhängigkeit von einem Mac für den iOS-Build stehen derzeit nicht im
Verhältnis zum Nutzen, während der bestehende Stack praktisch die gesamte `ROADMAP.md`
abdeckt.

## Considered Options

- **Native Android, modernisiert** (gewählt): kleinster sinnvoller Schritt auf dem
  vorhandenen, aktuellen Stack. Die fehlenden Architektur-Bausteine wurden ergänzt statt
  ersetzt — ViewModel mit StateFlow für die Spiellogik, Room für die Historie, Navigation
  Compose für die Wege durch die App, DataStore für Einstellungen.
- **Kotlin Multiplatform / Compose Multiplatform**: würde iOS ermöglichen, verlangt aber einen
  Mac für jeden iOS-Build. Die Entwicklungsmaschine läuft unter Windows.

## Consequences

iOS-Geräte werden bis auf Weiteres nicht unterstützt. Diese Entscheidung ist bewusst
revidierbar — insbesondere, falls eine iOS-Unterstützung für Schul-iPads konkret wird.
