# Detekt statt Ktlint

Als Linter läuft **Detekt** (inklusive `detekt-formatting`) mit einer Baseline für den bei
Einführung bestehenden Code. Ktlint wurde versucht und wieder verworfen: Sein Gradle-Plugin
erkennt die Kotlin-Quellsets dieses Projekts nicht, weil AGP 9 die Kotlin-Unterstützung
bereits eingebaut mitbringt.

## Consequences

Wer Ktlint für den Standard hält, wird es einführen wollen — es scheitert am selben Punkt.
Die Baseline (`app/detekt-baseline.xml`) unterdrückt Altlasten bewusst; neue Fundstellen
sollen auffallen, statt in der Masse unterzugehen.
