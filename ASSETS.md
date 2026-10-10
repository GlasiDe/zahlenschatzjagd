# Bild-Assets & KI-Generierung

Diese Datei ist die **verbindliche Referenz** für alle gerasterten Grafiken der App
(Startkarte, Inseln, Pergament-Hintergrund), dazu für Herkunft und Lizenz der Töne
(Abschnitt 6). Sie hält fest, **womit** und **wie** die Grafiken
erzeugt wurden, damit später nachgenerierte Grafiken **denselben Stil** treffen und den
einheitlichen Piraten-/Pergament-Look nicht zerstören.

> **Wichtig:** Neue Bild-Assets (z. B. zusätzliche Inseln) **immer** nach der Stil-Vorlage
> unten erzeugen und den verwendeten Prompt **inklusive Seed** hier in der Asset-Tabelle
> nachtragen. Die PNGs selbst enthalten **keine** eingebetteten Prompt-Metadaten – diese
> Datei ist die einzige Quelle der Wahrheit.

## 1. Verwendeter Generator

- **Qwen-Image 2.1 lokal in ComfyUI**, angesprochen über einen lokalen MCP-Server
  `image-generator` (Tool `generate_image`, nicht Teil dieses Repos) **mit `model="qwen21"`**.
  Das Werkzeug hat FLUX als Standard, weil es auch außerhalb dieses Projekts genutzt wird. Für die App muss das Modell deshalb immer
  ausdrücklich gesetzt werden. Warum Qwen und nicht mehr FLUX:
  [`docs/adr/0031-bildmodell-qwen-image-2-1-statt-flux.md`](docs/adr/0031-bildmodell-qwen-image-2-1-statt-flux.md).
  Läuft **privat/offline** auf dem Rechner des Entwicklers, rund eine Minute pro Bild.
- **cfg 2.5: Negativ-Prompts wirken.** Unter FLUX (cfg 1.0) liefen sie ins Leere.
- **Qwen nimmt Prompts wörtlich.** Was nicht im Prompt steht, fehlt: Ohne „blue water" gibt es
  kein Wasser, und „warm sepia tones" wird wirklich sepia. Umgekehrt setzt Qwen Ziffern,
  Zeichen, Anordnungen und Farben zuverlässig um.
- **Gerechnet wird in nativer Größe** (1024×1024, Hintergründe 832×1792) und danach per
  Lanczos auf die Zielgröße verkleinert. Kleiner zu rechnen verschenkt Qualität. Seed fest
  setzen und hier notieren. 25 Schritte sind der Standard des Werkzeugs.
- **Cloud-Fallback:** Ist ComfyUI nicht erreichbar, liefert das Tool den Sentinel
  `COMFYUI_UNAVAILABLE`. Erst **nach Rückfrage/Zustimmung** darf mit
  `allow_cloud_fallback=true` die freie Pollinations-Cloud genutzt werden (nicht mehr
  privat/offline!).
- **Ablauf:** Mehrere Kandidaten erzeugen, das gewählte Bild verkleinert nach
  `app/src/main/res/drawable/` legen (Dateinamen zwingend `lowercase_snake_case`). Kandidaten
  und Vergleichsbilder werden nicht versioniert und nach der Auswahl gelöscht.
- **Hinweis:** Die Modelldateien liegen in der ComfyUI-Installation des Entwicklers
  (außerhalb dieses Repos). Damit sie sich wiederbeschaffen und auf Austausch prüfen lassen,
  stehen Dateien, Prüfsummen und Graphen im nächsten Abschnitt.

### Bildmodelle, Dateien und Graphen

Das Werkzeug baut den ComfyUI-Graphen selbst, unabhängig von den Workflows in der
ComfyUI-Oberfläche. Der genaue Graph je Modell liegt als API-JSON unter
[`docs/bildmodelle/`](docs/bildmodelle/) und lässt sich per Drag & Drop in ComfyUI laden. Die
Platzhalter `{PROMPT}`/`{NEGATIVE_PROMPT}` werden dabei ersetzt, Seed und Größe angepasst.
Erzeugt werden die Dateien mit `server.py --export-workflows <ordner>`. Die Wahl trifft der
Parameter `model` von `generate_image`, Standard ist `flux`.

| `model` | Rezept | Datei | SHA-256¹ | Quelle | Lizenz |
|---|---|---|---|---|---|
| `flux` (Standard) | 20 Schritte, cfg 1.0, FluxGuidance 3.5, euler/simple | `checkpoints/flux1-dev-fp8.safetensors` (Modell, Encoder, VAE in einer Datei) | `8e91b680…32f3d88a` | [Comfy-Org/flux1-dev](https://huggingface.co/Comfy-Org/flux1-dev) | FLUX.1 [dev] Non-Commercial License v1.1.1 |
| `qwen21` | 25 Schritte, **cfg 2.5** (Negativ-Prompt wirkt), euler/simple | `diffusion_models/qwen_image_2.1-Q8_0.gguf` | `f39746ef…d8b17bac` | [vantagewithai/Qwen-Image-2.1-ComfyUI-GGUF](https://huggingface.co/vantagewithai/Qwen-Image-2.1-ComfyUI-GGUF) | Qwen Research License |
| | | `text_encoders/qwen3vl_8b_int8_convrot.safetensors` (Original, **nicht** die abliterierte „heretic"-Fassung) | `8bfd0f6e…60452e8f` | [Comfy-Org/Qwen-Image-2.1](https://huggingface.co/Comfy-Org/Qwen-Image-2.1) | |
| | | `vae/qwen_image_2.1_vae_bf16.safetensors` | `bb21f747…4876b7c9` | [Comfy-Org/Qwen-Image-2.1](https://huggingface.co/Comfy-Org/Qwen-Image-2.1) | |

¹ Vollständige Prüfsummen in [`docs/bildmodelle/SHA256SUMS`](docs/bildmodelle/SHA256SUMS). Pfade
sind relativ zu `ComfyUI-Shared/models/`. Prüfen mit `sha256sum -c` in diesem Ordner.

**Voraussetzungen des Qwen-Graphen:** ComfyUI **0.38.2** (für den Knoten
`TextEncodeQwenImage21`), die Custom Nodes **ComfyUI-GGUF** (Knoten `UnetLoaderGGUF`, hier
aus dem Fork [`RealRebelAI/ComfyUI-GGUF_KREA-2`](https://github.com/RealRebelAI/ComfyUI-GGUF_KREA-2),
Stand `5c3588f`) und [`SeanScripts/ComfyUI-Unload-Model`](https://github.com/SeanScripts/ComfyUI-Unload-Model)
(Stand `ac5ffb4`). Die Entlade-Knoten sind bei 16 GB VRAM **tragend**: Ohne das Entladen des
Bildmodells vor dem VAE-Decode dauert allein der Decode Minuten statt Sekunden.

**Lizenzlage:** Die Lizenz in der Tabelle gilt für alle Dateien eines Modells, auch für
Encoder und VAE. Was sie für die erzeugten Bilder bedeutet, steht samt Quellen und Zitaten in
[`docs/adr/0031-bildmodell-qwen-image-2-1-statt-flux.md`](docs/adr/0031-bildmodell-qwen-image-2-1-statt-flux.md).

## 2. Stil-Konventionen (verbindlich)

**Kanonischer Stilrahmen** – hängt an jedes Motiv außer den Hintergründen und den Avataren. Er
macht die Motive bunt und kindgerecht, das Pergament bleibt als Grund:

> **{MOTIV}**, bright cheerful children's book illustration with vivid saturated colors and
> bold clean outlines, friendly cartoon style for young children, on an aged parchment
> background, part of a matching pirate treasure map set, centered composition, soft faded
> edges blending into the parchment, no text

- **Negativ (Grundform):** „text, words, letters, monochrome, grayscale, sepia, muted colors,
  photo, frame, border". „sepia" und „muted colors" sind wichtig: Ohne sie rutscht Qwen beim
  Pergament-Grund in gedämpfte Töne. Mit „warm sepia tones" im Prompt wirkten die Bilder blass.

**Inseln auf der Startkarte** – Rahmen vor dem Stilrahmen:

> Top-down pirate treasure map illustration of a single small island, **{MOTIV}**, small sandy
> beach, surrounded by hand-drawn wavy ocean contour lines on the parchment, …Stilrahmen…

- **Kein blaues Wasser bei Inseln**, nur Wellenlinien auf dem Pergament, einheitlich für alle
  Inseln. So fügen sie sich in die Schatzkarte, die selbst nicht blau ist. Negativ zusätzlich
  „blue water, sea water".
- **Echtes Wasser gibt es nur, wo das Bild ein Schauplatz ist und keine Karte**: die Bucht der
  Zahlenspiele und der Fluss bei Nachbarzahlen. Dann „soft turquoise blue water" – reines
  „turquoise" wird grell.

**Avatare** – eigener, vollständiger Rahmen ohne den bunten Stilrahmen, nur das **{MOTIV}**
(Kind, Aussehen, Piraten-Accessoire) wechselt:

> Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered,
> **{MOTIV}**, big friendly smile, detailed painted children's book illustration with soft
> shading, depth and warm lighting, vivid saturated colors, friendly style for young children,
> on an aged parchment background, part of a matching pirate treasure map set, centered
> composition, soft faded edges blending into the parchment, no text

- **Eigener, gemalter Stilrahmen statt des bunten:** Mit „bold clean outlines, friendly
  cartoon style" wirkten die Avatare flach. „detailed painted … soft shading, depth" gibt ihnen
  Plastizität. Eine Variante mit „soft 3D cartoon look"
  wurde für die Avatare verworfen; nur das Launcher-Symbol nutzt weiches 3D. Negativ zusätzlich „flat colors".
- **Alter ausdrücklich nennen**, wo ein Kind zu jung wirkt („about eight years old, primary
  school age"): Ohne Angabe malt Qwen leicht Kindergartenkinder.

- **Maße Avatare:** **512×512**. Wie die Inseln auf **pergamentfarbenem Grund zentriert**
  generieren. Die App blendet die Ränder zur Laufzeit per `AvatarImage`
  (`ui/profile/Avatars.kt`, gleiche `featherEdges`-Technik) kreisförmig weich aus.
- **Diversität ist Absicht:** Hauttöne, Haare und Geschlechter bewusst mischen. Negativ
  zusätzlich „scary".

**Launcher-Symbol** – eigener Rahmen, bewusst **ohne** Pergament und ohne ausgeblendete Ränder:
Auf dem Startbildschirm des Geräts muss das Symbol randlos gefüllt und kräftig sein, eine
Pergament-Kachel ginge dort unter. Der Stil ist weiches 3D statt gemalt. Ein Gegenversuch im
gemalten Stil der Avatare sah kaum anders aus, setzte aber häufiger `÷`.

- **Randlos:** „full-bleed artwork filling the entire square edge to edge with no border, no
  frame and no rounded corners“, negativ „border, frame, rounded corners, app icon frame, white
  margin, mockup“. Ohne das malt Qwen die abgerundete Kachel samt Rand mit ins Bild.
- **Bildschnitt:** Kopf und Oberkörper, das Gesicht groß in der Mitte, der Hut vollständig. Bei
  48 dp zählt nur, was dann noch erkennbar ist.
- **Kein Name im Prompt**, sonst schreibt Qwen ihn ins Bild.
- **Einbetten:** Android zeigt vom 108-dp-Vordergrund nur die mittleren 72 dp. Das ganze Bild
  füllt deshalb genau diesen Bereich, außen liegt eine vergrößerte, unscharfe Kopie. So bleiben
  Hut, Zahlen und Truhe sichtbar, und Masken oder Animationen des Launchers zeigen keine Kante.

**Zahlen und Zeichen** – Qwen kann beides, also richtig:

- **Rechenzeichen wie im Spiel: `+ - · :`**, nicht „×" und „÷". Im Prompt ausschreiben („a
  single centered dot for multiplication, a colon made of two stacked dots for division"),
  negativ „x, multiplication cross, division sign with a line".
- **Gezeigte Rechnungen stimmen** (Zahlenmauer 2, 3, 4 → 5, 7 → 12). Für eine Mathe-App
  zählt das, auch wenn es kaum jemand nachrechnet.

Weitere Regeln:

- **Maße in der App:** Inseln und Truhen **768×768**, Spiel-Icons, Münzen, Belohnungen und
  Avatare **512×512**, Hintergründe **832×1792** (Telefon-Hochformat, minimaler Beschnitt). Das
  Launcher-Symbol hat eigene Maße in den `mipmap`-Sätzen (siehe sein Rahmen oben).
- **Auf pergamentfarbenem Grund generieren** (das Modell liefert keine zuverlässige Transparenz),
  außer beim Launcher-Symbol.
  Die App blendet die rechteckigen Ränder der Insel-Kacheln zur Laufzeit per **`featherEdges`**
  (radialer Alpha-Verlauf, `ui/util/FeatherEdges.kt`) weich zu einem Kreis aus – deshalb muss die
  Insel **zentriert** sein und braucht keinen transparenten Hintergrund.
- **Zwei-Hintergrund-Prinzip:** Die detailreiche Schatzkarte (`map_background.png`) liegt
  **nur** hinter der Startseite; hinter allen anderen Screens liegt die ruhige Textur
  `parchment_bg.png` (siehe Abschnitt 5).
- **Text vermeiden:** „no text" im Stilrahmen und „text, words, letters" im Negativ-Prompt
  genügen bei Qwen.

## 3. Asset-Tabelle (exakte Prompts & Seeds)

Alle unter `app/src/main/res/drawable/` (nur das Launcher-Symbol unter `mipmap-*`), erzeugt mit **Qwen-Image 2.1** (`model="qwen21"`).
Gerechnet in der angegebenen Größe, eingebettet verkleinert auf die Zielgröße. Prompts sind der
wörtliche englische Text. Eine Insel fürs Profil gibt es nicht: Das Profil öffnet sich über das
Profil-Badge auf der Startkarte.

### Hintergründe

#### `map_background.png` — Startkarte
- **Seed 6042**, gerechnet 832×1792. Hintergrund der Startkarte (`ui/home/HomeScreen.kt`).
- Bewusst **ohne** den bunten Stilrahmen: Als Hintergrund soll die Karte ruhig bleiben.
- **Prompt:** „Antique pirate treasure map on aged weathered parchment, tall portrait format, a small ornate compass rose tucked in the top-right corner only, the entire top-center and top-left kept as empty clean parchment, a hand-drawn sailing ship and a coiling sea serpent doodle in the lower third, faint wavy coastline contours along the left and right edges, torn burnt scroll edges, warm sepia and tan tones, hand-drawn ink cartography, no islands in the center, no text“
- **Negativ:** „text, letters, words, centered compass, islands, modern, bright colors, photo“

#### `parchment_bg.png` — Pergament-Hintergrund
- **Seed 7501**, gerechnet 832×1792. Liegt zentral hinter allen übrigen Bildschirmen.
- Ohne Stilrahmen, wie die Startkarte. Der Seed ist beliebig, jede Fassung liefert ruhiges Pergament.
- **Prompt:** „Plain aged parchment paper texture, tall portrait format, warm cream and tan tones, subtle mottling, darkened worn edges, empty, no drawings, no compass, no text“
- **Negativ:** „text, drawings, compass, islands, ship, photo“

#### `zahlenspiele_bay.png` — Bucht der Zahlenspiele
- **Seed 7404**, gerechnet 832×1792. Hintergrund von `ZahlenspieleMenuScreen`. Auf der freien Sandfläche liegen die gemalten Spiel-Icons als Touch-Ziele.
- Im Kartenstil wie die Startkarte, nicht bunt: Mit dem Stilrahmen wurde der Sand grell gelb. Ein Schauplatz, deshalb mit Wasser (gedämpft, nur am unteren Rand). Ohne „no island outline“ malt Qwen eine ganze Insel statt eines Strandes.
- **Prompt:** „Antique pirate treasure map close-up of a sandy pirate beach on aged weathered parchment, tall portrait format, the sand fills the entire picture from edge to edge, no island outline visible, gentle hand-drawn waves washing in only along the bottom edge in soft muted teal, a few shells and small palm leaves along the left and right edges, a big open empty sandy area in the center with plenty of room, warm sepia and tan tones, hand-drawn ink cartography, torn burnt scroll edges, no text“
- **Negativ:** „text, letters, words, island, island outline, buildings, bright saturated colors, neon yellow, photo“

### Inseln der Startkarte

#### `island_rechnen.png` — Insel „Rechnen“
- **Seed 4231**, gerechnet 1024×1024. Startkarte, `ISLAND_VISUALS` in `ui/home/HomeScreen.kt`.
- „four arms of equal length“ ist nötig: Ohne liest sich das Plus als Kreuz.
- **Prompt:** „Top-down pirate treasure map illustration of a single small island, on the island stand two giant carved stone symbols side by side: a big stone plus sign with four arms of equal length and a big flat stone minus sign, both standing firmly on the grass, small sandy beach, surrounded by hand-drawn wavy ocean contour lines on the parchment, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, blue water, sea water“

#### `island_abzeichen.png` — Insel „Abzeichen“
- **Seed 7001**, gerechnet 1024×1024. Startkarte.
- Truhen **verschlossen**, damit die Insel nicht verrät, was hineinkommt. Ohne Negativ „gemstones, pink, black“ kamen Edelsteine, die es in der App nicht gibt.
- **Prompt:** „Top-down pirate treasure map illustration of a single small island, a tall rocky cliff island rising high above the sea like a plateau, on its grassy flat top stand three closed wooden treasure chests with iron bands and shut lids, surrounded by hand-drawn wavy ocean contour lines on the parchment, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, blue water, sea water, open chest, gemstones, jewels, pink, black“

#### `island_zahlenspiele.png` — Insel „Zahlenspiele“
- **Seed 7003**, gerechnet 1024×1024. Startkarte.
- Die Zahlen spielen selbst. Qwen setzt dabei mehr Zahlen als 1, 2, 3 und doppelt einige – hingenommen.
- **Prompt:** „Top-down pirate treasure map illustration of a single small island, a tiny colorful playground where big colorful cartoon numbers with happy faces are playing: the number 1 is sliding down the slide, the number 2 is swinging on the swing, the number 3 is riding the seesaw, small sandy beach, surrounded by hand-drawn wavy ocean contour lines on the parchment, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, blue water, sea water“

#### `island_eltern.png` — Insel „Für Eltern“
- **Seed 4214**, gerechnet 1024×1024. Startkarte, Zugang zum Elternbereich.
- Kapitänin (ausdrücklich erwachsen) am Kajütenfenster: Sie behält den Überblick. Eine Innenansicht der Kajüte wurde verworfen, weil die Inseln Außenansichten sind. Wie alle Inseln ohne blaues Wasser.
- **Prompt:** „Top-down pirate treasure map illustration of a single small pirate ship drawn on the parchment, the big stern cabin window of the ship is open and a friendly grown-up woman pirate captain, clearly an adult, leans out of it looking through a brass telescope, surrounded by hand-drawn wavy ocean contour lines on the parchment, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, blue water, sea water, child, kid, children“

### Spiel-Icons und Zählhilfe

#### `game_verliebte.png` — Spiel-Icon „Verliebte Zahlen“
- **Seed 7400**, gerechnet 1024×1024. `ZahlenspieleMenuScreen` und Kopf von `ZahlenspieleSelectionScreen`.
- Zwei Ziffern brauchen etwas **zwischen** sich, sonst liest man „82“. Die Gesichter stehen bewusst im Prompt.
- **Prompt:** „The big chunky number 8 and the big chunky number 2 carved from sandstone, standing free on a small sandy patch, both numbers with cute happy cartoon faces looking at each other in love, one big bright red love heart between them, a few little red hearts floating around them, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `game_vergleich.png` — Spiel-Icon „Zahlen vergleichen“
- **Seed 7405**, gerechnet 1024×1024. `ZahlenspieleMenuScreen` und Kopf von `VergleichenSelectionScreen`.
- Bewusst ohne `<`/`>`: Die Zeichen erscheinen erst als Antwort-Tasten im Spiel.
- **Prompt:** „A tall high stack of many shiny gold coins next to a small short stack of just a few gold coins, side by side on a small sandy patch, clearly showing a big amount versus a small amount, more versus less, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `game_nachbarzahlen.png` — Spiel-Icon „Nachbarzahlen“
- **Seed 7410**, gerechnet 1024×1024. `ZahlenspieleMenuScreen` und Kopf des zugehörigen Auswahl-Bildschirms.
- Ein **Fluss** zwischen zwei Ufern, sonst wirkt das Wasser wie eine Pfütze, um die man herumgeht. Nur der mittlere Pfahl trägt eine Ziffer: die Mechanik der Übungsform.
- **Prompt:** „A small river with soft turquoise blue water flowing from the top to the bottom of the picture, a sandy bank on the left side and a grassy bank on the right side, three round wooden stepping posts in a straight horizontal row leading across the river from the left bank to the right bank, water flowing all around and between the posts, all three posts the same size, the middle post shows the big carved digit 7 on its top, the left and right posts are plain wood without any carving, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, question mark“

#### `game_verdoppeln.png` — Spiel-Icon „Verdoppeln“
- **Seed 7404**, gerechnet 1024×1024. `RechnenMenuScreen` und Kopf von `VerdoppelnSelectionScreen`.
- Zählbar nur bei **kleinen** Zahlen: 2 und 4 traf Qwen in 2 von 4 Seeds, 3 und 6 in keinem. Ohne Rechenzeichen, siehe LEHRPLAN.md, Abschnitt „Verdoppeln“.
- **Prompt:** „Two small stacks of thick shiny gold pirate coins standing side by side on a small sandy patch, the left stack is made of exactly two coins, the right stack is made of exactly four coins, each coin thick and clearly separated so they can be counted, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, sparkles, glow, magic trail, loose coins“

#### `game_zahlenmauer.png` — Spiel-Icon „Zahlenmauer“
- **Seed 7407**, gerechnet 1024×1024. `RechnenMenuScreen` und Kopf von `ZahlenmauerSelectionScreen`.
- Die Zahlen sind vorgegeben und **richtig gerechnet**.
- **Prompt:** „A small number wall pyramid built from flat sandstone bricks on the sand, three rows: the bottom row has three bricks showing the numbers 2, 3 and 4, the middle row has two bricks showing 5 and 7, the top brick shows 12, each number carved big and clear, a few scattered gold pirate coins nearby, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `game_grundrechenarten.png` — Spiel-Icon „Grundrechenarten“
- **Seed 7405**, gerechnet 1024×1024. `RechnenMenuScreen`.
- `+ - · :` wie im Spiel. Das gewählte Bild zeigt den Doppelpunkt korrekt; trotz Negativ-Prompt setzte Qwen aber in 2 von 4 Seeds `÷`, Kandidaten deshalb immer nachprüfen.
- **Prompt:** „Four big square sandstone tiles standing in a row on the sand, each carved with one big clear math symbol: a plus sign, a minus sign, a single centered dot for multiplication, and a colon made of two stacked dots for division, a few scattered gold pirate coins nearby, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, x, multiplication cross, division sign with a line“

#### `coin_gold.png` — Goldmünze
- **Seed 7103**, gerechnet 1024×1024. Zählhilfe bei „Verliebte Zahlen“ (`CoinTenFrame` in `ui/game/GameScreen.kt`). Leere Plätze sind dieselbe Grafik, entsättigt.
- **Prompt:** „A single round shiny gold pirate coin with an embossed skull in the center, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

### Schatztruhen (Stufen des Abzeichens)

#### `chest_stage_0.png` — Schatztruhe, Stufe 0
- **Seed 7300**, gerechnet 1024×1024. Abzeichen (`ui/badges/BadgeVisuals.kt`) und Ordensverleihung.
- **Prompt:** „A single open wooden pirate treasure chest with iron bands and a big rounded open lid, the chest completely empty with nothing inside, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `chest_stage_1.png` — Schatztruhe, Stufe 1
- **Seed 7302**, gerechnet 1024×1024. Abzeichen (`ui/badges/BadgeVisuals.kt`) und Ordensverleihung.
- **Prompt:** „A single open wooden pirate treasure chest with iron bands and a big rounded open lid, a small pile of coins made of dull reddish-brown copper and bronze metal, warm coppery brown color with aged patina, no gold coins, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, gold, golden, yellow coins“

#### `chest_stage_2.png` — Schatztruhe, Stufe 2
- **Seed 7301**, gerechnet 1024×1024. Abzeichen (`ui/badges/BadgeVisuals.kt`) und Ordensverleihung.
- **Prompt:** „A single open wooden pirate treasure chest with iron bands and a big rounded open lid, a pile of coins made of bright polished silver metal, cool light grey and shiny white silver color, no gold coins, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, gold, golden, yellow coins“

#### `chest_stage_3.png` — Schatztruhe, Stufe 3
- **Seed 7300**, gerechnet 1024×1024. Abzeichen (`ui/badges/BadgeVisuals.kt`) und Ordensverleihung.
- **Prompt:** „A single open wooden pirate treasure chest with iron bands and a big rounded open lid, a big heap of shiny gold coins, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `chest_stage_4.png` — Schatztruhe, Stufe 4
- **Seed 7303**, gerechnet 1024×1024. Abzeichen (`ui/badges/BadgeVisuals.kt`) und Ordensverleihung.
- **Prompt:** „A single open wooden pirate treasure chest with iron bands and a big rounded open lid, a heap of shiny gold coins with one deep blue sapphire gemstone resting on top of the coins, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `chest_stage_5.png` — Schatztruhe, Stufe 5
- **Seed 7303**, gerechnet 1024×1024. Abzeichen (`ui/badges/BadgeVisuals.kt`) und Ordensverleihung.
- **Prompt:** „A single open wooden pirate treasure chest with iron bands and a big rounded open lid, a heap of shiny gold coins with a large deep blue sapphire gemstone and a sparkling clear white diamond gemstone resting on top, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `chest_stage_6.png` — Schatztruhe, Stufe 6
- **Seed 7304**, gerechnet 1024×1024. Abzeichen (`ui/badges/BadgeVisuals.kt`) und Ordensverleihung.
- **Prompt:** „A single closed wooden pirate treasure chest with iron bands and a rounded lid shut tight, a shiny golden tiara diadem encrusted with sparkling blue sapphire, white diamond and red ruby gemstones resting on top of the closed lid, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, open chest, coins spilling“

### Belohnungen der Ordensverleihung

#### `reward_coin_bronze.png` — Bronzemünzen
- **Seed 7201**, gerechnet 1024×1024. Ordensverleihung (`RewardItem` in `ui/badges/BadgeVisuals.kt`).
- **Prompt:** „a small neat pile of a few shiny coins made of dull reddish-brown copper and bronze metal, warm coppery brown color with aged patina, no gold coins, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, gold, golden, yellow coins, silver, chest, box“

#### `reward_coin_silver.png` — Silbermünzen
- **Seed 7203**, gerechnet 1024×1024. Ordensverleihung (`RewardItem` in `ui/badges/BadgeVisuals.kt`).
- **Prompt:** „a small neat pile of a few shiny silver coins, cool bright polished silver metallic tones, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `reward_coin_gold.png` — Goldmünzen
- **Seed 7202**, gerechnet 1024×1024. Ordensverleihung (`RewardItem` in `ui/badges/BadgeVisuals.kt`).
- **Prompt:** „a small neat pile of a few shiny gold coins, warm bright golden yellow metallic tones, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `reward_gem_saphir.png` — Saphir
- **Seed 7206**, gerechnet 1024×1024. Ordensverleihung (`RewardItem` in `ui/badges/BadgeVisuals.kt`).
- **Prompt:** „a single large sparkling deep blue sapphire gemstone, faceted, glowing vivid blue, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `reward_gem_diamant.png` — Diamant
- **Seed 7207**, gerechnet 1024×1024. Ordensverleihung (`RewardItem` in `ui/badges/BadgeVisuals.kt`).
- **Prompt:** „a single large sparkling clear white diamond gemstone, brilliant faceted, bright white and pale cyan highlights, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

#### `reward_tiara.png` — Diadem
- **Seed 7209**, gerechnet 1024×1024. Ordensverleihung (`RewardItem` in `ui/badges/BadgeVisuals.kt`).
- Dieselben Edelsteine wie auf der Platin-Truhe: Rubin, Saphir, Diamant.
- **Prompt:** „a small shiny golden princess tiara diadem encrusted with sparkling red ruby, deep blue sapphire and clear white diamond gemstones, bright gold, bright cheerful children's book illustration with vivid saturated colors and bold clean outlines, friendly cartoon style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border“

### Avatare

Mit eigenem, gemaltem Stilrahmen statt des bunten (siehe Abschnitt 2): Im bunten Rahmen wirkten die
Avatare flach.

#### `avatar_pirat_1.png` — Avatar 1
- **Seed 8101**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- „headband“ statt „bandana“: Mit „bandana“ malte Qwen ein Kopftuch, gewünscht ist das Stirnband mit Strubbelhaaren.
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a boy with light skin and messy tousled brown hair sticking up over a red headband with white polka dots tied around his forehead, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors“

#### `avatar_pirat_2.png` — Avatar 2
- **Seed 8105**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a girl with light skin and blonde hair in two braids wearing a black pirate tricorn hat with a small red feather, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors“

#### `avatar_pirat_3.png` — Avatar 3
- **Seed 8106**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a boy with dark brown skin and short black curly hair wearing a gold hoop earring and a red and white striped shirt, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors“

#### `avatar_pirat_4.png` — Avatar 4
- **Seed 8107**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a girl with dark brown skin and black hair in two afro puffs wearing a red bandana, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors“

#### `avatar_pirat_5.png` — Avatar 5
- **Seed 8106**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a boy with olive tan skin and black hair wearing a black eye patch and a blue bandana, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors“

#### `avatar_pirat_6.png` — Avatar 6
- **Seed 8107**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a girl with tan skin and long dark wavy hair wearing a brown pirate tricorn hat, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors“

#### `avatar_pirat_7.png` — Avatar 7
- **Seed 8109**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- Ohne „natural skin tone on nose and cheeks“ und die Negative wirkte er comichaft mit sehr roter Nase; ohne „exactly one“ saßen teils zwei Papageien da.
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a boy with fair freckled skin with a natural skin tone on nose and cheeks and ginger red hair wearing a green bandana, with exactly one small green parrot sitting on his shoulder, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors, red nose, rosy nose, exaggerated comic style, two parrots“

#### `avatar_pirat_8.png` — Avatar 8
- **Seed 8109**, gerechnet 1024×1024. `ui/profile/Avatars.kt` (`Avatars.all`).
- „about eight years old, primary school age“: Ohne Altersangabe sah sie nach Kindergarten aus.
- **Prompt:** „Cartoon portrait of a cheerful young child pirate, head and shoulders bust centered, a girl of about eight years old, primary school age, with East Asian features and straight black bob hair wearing a red pirate captain coat and hat, big friendly smile, detailed painted children's book illustration with soft shading, depth and warm lighting, vivid saturated colors, friendly style for young children, on an aged parchment background, part of a matching pirate treasure map set, centered composition, soft faded edges blending into the parchment, no text“
- **Negativ:** „text, words, letters, monochrome, grayscale, sepia, muted colors, photo, frame, border, scary, flat colors, toddler, baby face“

### Launcher-Symbol

#### `ic_launcher*.webp` — Symbol der App
- **Seed 9315**, gerechnet 1024×1024, Rahmen „Launcher-Symbol“ aus Abschnitt 2. Liegt in allen
  `mipmap-*`-Sätzen: `ic_launcher_foreground.webp` als Vordergrund des adaptiven Symbols (108 dp,
  das Bild auf 72 dp, außen unscharf erweitert), `ic_launcher.webp` und `ic_launcher_round.webp`
  (48 dp, abgerundet bzw. rund maskiert). Der Hintergrund `ic_launcher_background` (`#65CEE0`)
  liegt vollständig darunter. Eine `<monochrome>`-Ebene gibt es bewusst nicht: Aus dem farbigen
  Vollbild würde unter Android 13+ bei Designsymbolen nur ein einfarbiger Block, ohne sie zeigt
  das System das farbige Symbol.
- Unter sieben Seeds der einzige, bei dem die Weste genau `+ - · :` trägt; die anderen setzten
  `÷` oder `×`. Bei 48 dp lesen sich `·` und `:` eher als Knöpfe, erkennbar bleiben `+` und `-`.
  Die 1 steht zweimal im Hintergrund – hingenommen.
- **Herkunft des Prompts:** Ausgangspunkt war der Prompt eines früheren, mit Nano Banana (Google
  Gemini) erzeugten Symbols. Neu erzeugt mit Qwen für eine einheitliche Basis aller Bilder.
- **Lizenz: keine.** Das Symbol der App ist von keiner Lizenz erfasst, ein Fork tritt mit eigenem
  Symbol auf ([`docs/adr/0029-open-source-mit-starkem-copyleft.md`](docs/adr/0029-open-source-mit-starkem-copyleft.md)).
- **Prompt:** „Square app icon illustration for a children's math learning game, full-bleed artwork filling the entire square edge to edge with no border, no frame and no rounded corners. Close-up of a friendly, confident young pirate girl of about eight years old with curly brown hair, head and upper body, her cheerful face large in the center of the picture, the whole pirate hat visible with a little space above it. She wears a black pirate tricorn hat with a white skull and crossbones over a red bandana with white polka dots, a black eyepatch over one eye, a red and white striped shirt and a blue vest decorated with a few big colorful math symbols: a plus sign, a minus sign, a single centered dot for multiplication and a colon made of two stacked dots for division. A small open golden treasure chest at the lower right edge, the big colorful numbers 1, 2 and 3 floating in the background, bright sunny turquoise sea and a piece of an old treasure map behind her. Soft 3D cartoon render, smooth textures, vivid saturated colors, clean simple shapes that stay readable at small size, centered composition, no text.“
- **Negativ:** „text, words, letters, name, border, frame, rounded corners, app icon frame, white margin, drop shadow, mockup, blurred background, x, multiplication cross, division sign with a line, scary, toddler, baby face“

## 4. Neues Bild hinzufügen – Kurzanleitung

1. **Prompt bauen:** Motiv beschreiben und den passenden Rahmen aus Abschnitt 2 davor- bzw.
   dahintersetzen. Was im Bild sein soll, muss im Prompt stehen – Qwen ergänzt nichts.
2. **Generieren:** `generate_image(..., model="qwen21")` in 1024×1024 (Hintergründe 832×1792),
   mehrere feste Seeds. Ziffern, Zeichen und Mengen in jedem Kandidaten nachzählen.
3. **Einbetten:** gewähltes Bild per Lanczos auf die Zielgröße verkleinern und nach
   `app/src/main/res/drawable/<name>.png` legen. Das Launcher-Symbol geht stattdessen in alle
   `mipmap-*`-Sätze, eingebettet wie in seinem Rahmen in Abschnitt 2 beschrieben.
4. **Verdrahten:** bei einer Insel Eintrag in `ISLAND_VISUALS` (`ui/home/HomeScreen.kt`, Bild +
   Position) und in `HOME_ISLANDS` (`ui/home/Islands.kt`).
5. **Hier nachtragen:** Datei, Seed, Größe und vollen Prompt samt Negativ in Abschnitt 3.

## 5. Farben, Theme und Schrift

Die Grafiken sind nur die eine Hälfte des Looks – die andere ist das Farbschema, in das sie
eingebettet sind. Beides gehört zusammen: Ein neues Asset muss zu diesen Tönen passen.

**Das Pergament-Schema** (`ui/theme/`) nutzt warme Holz- und Pergamenttöne:

| Rolle | Ton | Verwendung |
|---|---|---|
| `MapWood` | Holzbraun | `primary` – Buttons, Titel |
| `MapParchment` | heller Pergamentton | `background`/`surface`/`onPrimary` |
| `MapInk` | dunkles Sepia | `onBackground`/`onSurface` – Text |
| `MapParchmentMuted` | gedämpft | nicht gewählte Umschalt-Buttons |
| `MapCorrect` / `MapWrong` | Grün / Rot | Rückmeldung richtig/falsch |

`SeaBlue`, `SoftCyan` und `MapRibbon` (ein Burgunder) sind Altlasten und praktisch nicht im
Einsatz.

**Ein Schema, kein Dunkelmodus** – bewusst, siehe
`docs/adr/0009-nur-ein-farbschema-ohne-dunkelmodus.md`. Auch dynamische Material-You-Farben
sind abgeschaltet, damit die gemalten Grafiken nicht mit vom System gewürfelten Tönen
kollidieren.

**Gemeinsamer Hintergrund:** Eine Pergament-Textur (`parchment_bg.png`) liegt zentral hinter
**allen** Bildschirmen – einmal um den transparent geschalteten Scaffold gelegt, nicht je
Screen wiederholt. Die Startkarte zeichnet ihre detailreiche Schatzkarte zusätzlich darüber.

**Schrift:** Die Überschriften nutzen **Andika** (`res/font/andika_regular.ttf`, SIL Open Font
License 1.1, Lizenztext in `app/src/main/assets/OFL.txt`) –
gezielt für Leseanfänger entwickelt. Sie liegt **nur** auf den Überschriften-Stilen.
Fließtext, Rechenaufgaben und Ziffern-Tasten bleiben in der Standardschrift, weil die dort
besser lesbar ist.

**Ränder:** Grafiken werden per Rand-Vignette weich in den Pergament-Hintergrund
ausgeblendet, statt mit harter Kante zu stehen – gilt für Inseln, Avatare, Münzen und
Truhen gleichermaßen.

## 6. Töne

Die drei Töne der Runde liegen in `app/src/main/res/raw/` und werden über `GameSound` in
`SoundPlayer.kt` abgespielt. **Nur CC0.** Lizenzen wie die von Pixabay verbieten, Inhalte
unverändert einzeln weiterzugeben. Damit sind sie nicht frei, und F-Droid markierte die App als
„Non-Free Assets“. CC BY wäre zwar
auch frei, verlangt aber eine Namensnennung, die die App nirgends zeigt. `TonQuellenTest`
prüft, dass Tabelle und `res/raw` dieselben Dateien nennen und jede davon CC0 trägt.

| Datei | Ereignis | Quelle | Urheber | Lizenz |
|---|---|---|---|---|
| `correct.mp3` | richtige Antwort | [„You find a treasure“ (Freesound 632661)](https://freesound.org/people/xkeril/sounds/632661/) | xkeril | CC0 1.0 |
| `wrong.mp3` | falsche Antwort | [„Failure Drum Sound Effect 2.mp3“ (Freesound 456963)](https://freesound.org/people/FunWithSound/sounds/456963/) | FunWithSound | CC0 1.0 |
| `finish.mp3` | Rundenende | [„Success Fanfare Trumpets.mp3“ (Freesound 456966)](https://freesound.org/people/FunWithSound/sounds/456966/) | FunWithSound | CC0 1.0 |

**Bearbeitung:** Grundlage ist jeweils die hq-Vorschau von Freesound (MP3, 128 kbit/s), sie
genügt für einen Handylautsprecher. Stille am Anfang wurde entfernt, am Ende nur bei
`finish.mp3`. Gekürzt sind `correct.mp3` auf 0,9 s (ab 0,5 s ausgeblendet) und `wrong.mp3`
auf 1,0 s (ab 0,65 s ausgeblendet), weil sie nach jeder Aufgabe kommen.

**Lautheit:** einheitlich um **-14 LUFS** (EBU R128), damit alle Töne gleich laut wirken.
`ffmpeg loudnorm` misst bei Clips um eine Sekunde unzuverlässig. Deshalb erst messen (`ebur128`),
dann linear verstärken. `wrong.mp3` ist ein Trommelschlag mit hoher Spitze. Er stößt schon bei
-19 LUFS an die Grenze und ist deshalb mit `alimiter` auf -16 LUFS angehoben (Spitze -1,4 dBTP).
Kodiert mit `libmp3lame -q:a 2`, 44,1 kHz.
