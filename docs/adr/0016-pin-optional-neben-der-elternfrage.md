# PIN optional neben der Elternfrage, Rückweg über eine selbst formulierte Sicherheitsfrage

Der Elternbereich schützt künftig nicht mehr nur die Sicherung, sondern auch die Freigabe —
also etwas, das ein Kind **aufheben will**. Die Elternfrage (ein Produkt aus zwei Faktoren
6–9) ist dafür zu dünn, sobald ein Kind rechnen kann. Deshalb gibt es zusätzlich eine
optionale PIN.

Die Elternfrage bleibt und gilt, solange keine PIN gesetzt ist; erst hinter ihr lässt sich
eine PIN vergeben. Ohne diese Reihenfolge könnte ein Kind auf einem frisch aktualisierten
Gerät selbst eine PIN setzen und die Erwachsenen aussperren.

Beim Vergeben wird eine **Sicherheitsfrage** abgefragt, Frage und Antwort frei formuliert.
Feste Fragenlisten sind im Haushalt wertlos: Haustier, Geburtsort und Lieblingsessen kennt das
Kind besser als die Eltern.

## Considered Options

- **Nur eine härtere Elternfrage** (z. B. dreistellig × einstellig) statt einer PIN. Verworfen,
  weil daran auch Erwachsene scheitern — eine Elternfrage, die Eltern nicht lösen, ist keine.
- **Kein Rückweg bei vergessener PIN.** Hieße Neuinstallation und damit den Verlust des
  Fortschritts aller Kinder auf dem Gerät.
- **Notausgang, der nur den Export der Sicherung freigibt.** Rettet den Fortschritt, erzwingt
  aber trotzdem die Neuinstallation.
- **PIN-Rücksetzen über die Elternfrage.** Wertlos: Genau diesen Knopf drückt das Kind, das
  die Freigabe loswerden will.

## Consequences

- **PIN und Antwort liegen nur als Prüfsumme auf dem Gerät.** Die PIN wird deshalb nie
  angezeigt; nach richtiger Antwort vergibt man eine neue.
- **Die Sicherung enthält sie nicht.** Sie wandert auf ein neues Gerät, die PIN nicht: Dort
  gilt zunächst wieder die Elternfrage, hinter der sich eine neue PIN vergeben lässt. Das
  Dateiformat der Sicherung bleibt dadurch unverändert
  (`docs/adr/0003-sicherung-eigenes-format-und-zusammenfuehren.md`).
- **Die Antwort wird vor dem Vergleich normalisiert** (Leerzeichen entfernt, Kleinschreibung).
  Sonst scheitert die Rettung an „Meier" gegen „meier" — im ungünstigsten Moment.
- **Fehlversuche bleiben unbegrenzt und ohne Wartezeit.** Eine harte Sperre nach N Versuchen
  wäre eine Selbstsperre und würde den gerade erst gebauten Rückweg entwerten; eine wachsende
  Wartezeit müsste den App-Neustart überleben und damit dauerhaft mitgeschrieben werden.
- **Der Schutz ist nur so gut wie die gewählte Frage.** Der Einrichtungsbildschirm sagt das
  ausdrücklich: etwas wählen, das das eigene Kind nicht weiß.
- **Die App bleibt ohne Android-Berechtigung und ohne Konto.** Ein Rückweg über E-Mail oder
  Server stand deshalb nie zur Wahl.
