# Zahlenmauer bekommt eigene Logik und einen eigenen Bildschirm

Die Zahlenmauer benutzt den generischen Spielbildschirm nicht mit, sondern hat eine eigene
Logik, ein eigenes ViewModel und einen eigenen Bildschirm. Grund: Eine Pyramide ist eine
Aufgabe mit **mehreren gleichzeitigen Lücken** und passt nicht in die flache Datenklasse, die
eine Frage und eine Antwort kennt.

## Consequences

- Statt Rückmeldung je Zelle wird die ganze Mauer auf einmal geprüft — erst wenn alle Lücken
  gefüllt sind, wird ausgewertet. Abgegeben wird mit dem ✓ des gemeinsamen Ziffernblocks, damit
  überall mit demselben Knopf abgegeben wird.
- **Folgefehler werden nicht doppelt bestraft.** Aufbau-Lücken werden gegen die tatsächlich
  eingegebenen Nachbarwerte geprüft, nicht gegen die ursprüngliche Lösung. Wer korrekt aus
  seinem eigenen Fehler weiterrechnet, bekommt nur den ersten Fehler angerechnet. Das sieht
  im Test überraschend aus (ein „falscher" Wert gilt als richtig) und ist Absicht. Nur die
  Ultra-Basislücke wird fest gegen ihre Lösung geprüft — ihre Nachbarn sind dort immer
  vorgegeben.
- Für Abzeichen zählt die **ganze Mauer als eine Aufgabe**, nicht jede Zelle. Sie gilt nur
  als richtig, wenn alle Lücken stimmen.
- Der Ziffernblock wird aus dem generischen Spielbildschirm wiederverwendet, damit die
  Eingabe sich überall gleich anfühlt.
