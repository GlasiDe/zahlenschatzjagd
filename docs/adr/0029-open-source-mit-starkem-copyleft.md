# Open Source mit starkem Copyleft statt „nicht kommerziell"

Der Quelltext wird unter der **GPL-3.0-or-later** veröffentlicht, Bilder, Töne und Texte, die dem
Autor gehören, unter **CC BY-SA 4.0**. Name und Symbol der App sind von keiner der beiden Lizenzen
erfasst: Ein Fork tritt unter eigenem Namen auf.

Das Ziel dahinter: Die App soll für Eltern kostenlos sein und bleiben, auch dann, wenn andere sie
weiterbauen.

## Considered Options

- **Eine Lizenz, die kommerzielle Nutzung ausschließt** (etwa PolyForm Noncommercial für den
  Code, CC BY-NC-SA für die Inhalte). Das war der erste Wunsch. Verworfen, weil eine solche Lizenz
  nach der Open-Source-Definition kein Open Source ist. Sie schließt die App aus F-Droid aus und
  bringt eine Grauzone mit sich (ist ein Fork mit Werbung „kommerziell"?).
- **MIT oder eine andere freizügige Lizenz.** Einfacher und für jeden ohne Bedenken nutzbar.
  Verworfen, weil sie das Ziel aufgibt: Jeder dürfte die App nehmen, den Quelltext schließen und
  mit Werbung oder gegen Geld verbreiten.
- **GPL-3.0** (gewählt). Wer eine veränderte Fassung weitergibt, muss den Quelltext offenlegen.
  Bei einer APK ist das immer der Fall, bei einer Webversion, die im Browser läuft, nach Lesart der
  FSF ebenso. Das ist die Hürde, um die es geht: Ein Fork bleibt offen und damit kostenlos
  weiterverbreitbar.
- **AGPL-3.0.** Ihr Zusatz greift erst, wenn jemand eine veränderte Fassung auf einem Server
  laufen lässt und nur zur Bedienung anbietet. Für eine Lern-App, die offline auf dem Gerät des
  Kindes läuft, ist das ein sehr unwahrscheinlicher Fall. Dafür haben viele Firmen, Behörden und
  Schulträger eine pauschale Regel gegen die AGPL – sie hätte also gerade die abgeschreckt, die
  mitwirken könnten. Verworfen.

## Consequences

- **Verkaufen und Werbung bleiben einem Fork erlaubt.** Er muss dann aber seinen Quelltext
  offenlegen, und jeder darf ihn kostenlos weiterverbreiten. Diese Restlücke ist bewusst in Kauf
  genommen.
- **Keine Beitragsvereinbarung (CLA).** Fremde Beiträge stehen unter derselben Lizenz wie das
  Projekt. Damit kann auch der Autor die Lizenz später nicht mehr allein ändern, sobald es
  Beiträge gibt. Das ist gewollt, denn eine CLA dient vor allem dazu, Code später kommerziell
  doppelt zu lizenzieren.
- **Der Name muss vor der Veröffentlichung feststehen**, weil er das ist, was ein Fork nicht
  übernehmen darf.
- **Verteilt wird zunächst als APK über GitHub-Releases**, signiert mit dem Schlüssel ab 2.0
  ([0014](./0014-release-signing-ueber-eigene-keystore-properties.md)).
- **Die KI-erzeugten Bilder stehen einheitlich unter CC BY-SA 4.0, soweit ein Schutz besteht.**
  Ob rein KI-erzeugte Bilder in Deutschland urheberrechtlich geschützt sind, ist nicht
  abschließend geklärt. Das muss es auch nicht sein: Besteht kein Schutz, ist die Lizenz nur
  wirkungslos, sie schadet nicht. Festgehalten ist das in `LICENSE.md`, nach dem Vorbild von
  `GlasiDe/osi-agenten`. Die
  Lizenz des Bildmodells gilt nur für das Modell ([0031](./0031-bildmodell-qwen-image-2-1-statt-flux.md)).
  Wer die Bilder zum Trainieren oder Verbessern eines KI-Modells nutzt, muss nach § 4 b der Qwen
  Research License „Built with Qwen" oder „Improved using Qwen" in der Dokumentation angeben.
- **Was nicht vom Autor stammt, behält seine eigene Lizenz.** Die Töne stehen unter CC0
  (`ASSETS.md`, Abschnitt „6. Töne"), die Schrift Andika unter der SIL Open Font License 1.1, der
  Stern `ic_star.xml` ist das Symbol „star“ aus Googles Material Icons (Apache-2.0).
