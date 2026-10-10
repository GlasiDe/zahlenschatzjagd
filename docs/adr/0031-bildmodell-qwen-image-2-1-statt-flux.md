# Bildmodell Qwen-Image 2.1 statt FLUX.1 [dev]

Die gemalten Grafiken der App entstehen ab jetzt mit **Qwen-Image 2.1** (lokal in ComfyUI, über
das Bild-Werkzeug mit `model="qwen21"`), nicht mehr mit FLUX.1 [dev]. Alle Bilder werden einmal
neu erzeugt, die Avatare eingeschlossen.

Anlass waren die Ziffern: Die Ziffern auf dem Bild von „Verliebte Zahlen" kosteten mit FLUX mehrere
Anläufe, und `game_nachbarzahlen.png` brauchte neun. Ein Probelauf hat alle 36 Bilder mit
den unveränderten Prompts und je vier Seeds neu erzeugt. Qwen setzte Ziffern, Anordnungen und
Farben dabei zuverlässig nach dem Prompt um: 8 und 2, die Sieben in der Mitte und kupferrote
Münzen gelangen jeweils in allen vier Seeds. Die Seeds untereinander blieben gleichmäßig, und das
trägt Serien wie die Truhen.

## Considered Options

- **Bei FLUX bleiben.** Die Lizenz ist die sauberste: Bilder dürfen ausdrücklich für jeden Zweck
  genutzt werden. Verworfen wegen der Ziffern und weil FLUX Prompts frei auslegt, was jede
  gezielte Korrektur zum Seed-Lotto macht.
- **Qwen-Image 2512** (Apache 2.0, ohne Lizenzfrage). Verworfen wegen der Rechenzeit: Als
  20-B-Modell braucht es auf dem Entwicklerrechner rund fünf Minuten pro Bild, für einen
  Neudurchgang samt Nacharbeit also rund neun Stunden. Qwen 2.1 braucht etwa eine Minute.
- **Qwen-Image 2.1** (gewählt).

## Consequences

- **Lizenz-Grauzone, bewusst in Kauf genommen.** Die Qwen Research License beschränkt die
  *Nutzung des Modells* auf „research or evaluation". Die Bilder selbst gehören laut Lizenzgeber
  nicht zu den lizenzierten „Materials", die Rechte liegen beim Nutzer. Ob das Erzeugen der
  App-Grafiken noch Evaluation ist, regelt die Lizenz nicht. Für eine kostenlose Hobby-App ist
  das Risiko gering. Die Bilder können damit unter CC BY-SA 4.0 stehen
  ([0029](./0029-open-source-mit-starkem-copyleft.md)).

## Quellen zur Lizenz (gelesen am 03.10.2026)

- **Qwen-Image 2.1:** [Qwen RESEARCH LICENSE AGREEMENT, Release Date 20.09.2026](https://huggingface.co/Qwen/Qwen-Image-2.1/blob/main/LICENSE).
  „Non-Commercial" heißt dort *„for research or evaluation purposes only"* (1.i). „Materials"
  sind *„our proprietary Qwen and Documentation"* (1.f), also Gewichte, Code und Doku. Für alles
  nicht ausdrücklich Erlaubte verlangt 9.a eine eigene Lizenz.
- **Klarstellung zu den Bildern:** Post von @QwenDevs auf X
  ([Link](https://x.com/QwenDevs/status/2101917379785838660), verlinkt in der
  [Lizenz-Diskussion auf Hugging Face](https://huggingface.co/Qwen/Qwen-Image-2.1/discussions/6)):
  *„Outputs are not part of the licensed Materials. Users retain the rights to images and other
  content they generate using the model."*
- **FLUX.1 [dev], zum Vergleich:** [FLUX.1 [dev] Non-Commercial License v1.1.1](https://huggingface.co/black-forest-labs/FLUX.1-dev/blob/main/LICENSE.md):
  *„We claim no ownership rights in and to the Outputs … You may use Output for any purpose
  (including for commercial purposes), except as expressly prohibited herein."* Ein Hobbyprojekt
  zählt dort ausdrücklich als „Non-Commercial Purpose".
- **Qwen-Image 2512:** [Apache 2.0](https://huggingface.co/Qwen/Qwen-Image-2512).
- Die Lizenz eines Modells gilt auch für seine Encoder- und VAE-Dateien (Bezugsquellen in
  `ASSETS.md`).
- **Qwen nimmt Prompts wörtlich.** Was FLUX still ergänzt hat, etwa die Wasserfarbe, fehlt, wenn
  es nicht im Prompt steht. Und „warm sepia tones" wird tatsächlich sepia. Die Stilvorlagen in
  `ASSETS.md` werden deshalb neu formuliert und nicht übernommen.
- **Negativ-Prompts wirken jetzt** (cfg 2.5 statt 1.0).
- **Die Avatare sehen anders aus.** Ein Kind, das sich seinen Piraten ausgesucht hat, bekommt ihn
  nach dem Update leicht verändert. Hingenommen, weil die Avatare in der App klein erscheinen und
  die Grundzüge (Haare, Hautton, Accessoire) gleich bleiben.
- **Das Werkzeug behält FLUX als Standard**, weil es auch außerhalb dieses Projekts genutzt wird.
  Für die App wird `model="qwen21"` immer ausdrücklich gesetzt (`ASSETS.md`).
- Modelldateien, Prüfsummen und der genaue Graph stehen in `ASSETS.md` und `docs/bildmodelle/`,
  damit ein Austausch auf der ComfyUI-Seite auffällt.
