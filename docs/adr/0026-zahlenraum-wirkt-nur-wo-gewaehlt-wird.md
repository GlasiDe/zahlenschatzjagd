# Der Zahlenraum der Freigabe wirkt nur dort, wo das Kind ihn wählt

Ein abgewählter Zahlenraum sperrt nur noch Übungen, bei denen das Kind den Zahlenraum **selbst
wählt** – Plus, Minus, Mal, Geteilt, Zahlen vergleichen, Nachbarzahlen. Übungen mit **festem**
Zahlenraum – Verdoppeln, Verliebte Zahlen, die vier Stufen der Zahlenmauer – hängen allein an
ihrem eigenen Kästchen.

Das kehrt eine Konsequenz aus `docs/adr/0015-freigabe-entlang-der-fortschrittskennung.md` um
(„Der Zahlenraum wirkt auch dort, wo das Kind ihn nicht wählt"). Das Modell aus zwei Mengen
bleibt, ebenso die gespeicherte Form als Sperre
(`docs/adr/0018-freigabe-als-sperre-gespeichert.md`).

Anlass war ein Fall, der beim Einrichten nicht auffiel: Wer „bis 10" abwählt, weil das Kind über
die kleinen Aufgaben hinaus ist, verliert damit Verdoppeln (beide Stufen) und Verliebte Zahlen
ganz – drei Übungen, deren fester Zahlenraum 10 fachlich nichts darüber sagt, ob sie dem Kind
noch zu schwer oder schon zu leicht sind. Der feste Zahlenraum ist eine Eigenschaft der Übung,
keine Einstellung; ihn zu sperren hieß, eine Übung über einen Umweg abzuwählen, den der
Erwachsene nicht vor Augen hat.

## Considered Options

- **Zahlenräume je Übung** (das volle Kreuzprodukt, das ADR-0015 schon verworfen hatte). Erlaubt
  „Plus bis 100, Mal nur bis 50". Das wird mit einer Erweiterung auf Klasse 3 und 4 womöglich
  gebraucht, weil sich größere Zahlenräume je Rechenart unterschiedlich gut im Kopf beherrschen
  lassen – ist heute aber nicht gefragt und vom Lehrplan noch nicht abgeglichen. Diese
  Entscheidung verbaut den Weg nicht: Sie ändert nichts an der gespeicherten Form.
- **Beides** – globale Zahlenräume plus Feineinstellung je Übung. Zwei Stellen für dieselbe
  Frage, und ein Erwachsener müsste verstehen, welche gewinnt.
- **Alles lassen und den Hinweis im Freigabe-Bildschirm deutlicher machen.** Erklärt den Umweg,
  statt ihn zu beseitigen.

## Consequences

- **„Bis 50 abwählen" sperrt Zahlenmauer (mittel) nicht mehr mit.** Wer große Zahlen über die
  Zahlenmauer ausschließen will, wählt deren Stufe ab. Die Aussage „Zahlen über 50 sind für
  dieses Kind zu groß" gilt damit nur noch für Übungen, in denen das Kind den Zahlenraum wählt.
- **Bestehende Sperren bleiben gültig und ändern still ihre Wirkung.** Ein Profil, dem heute
  „bis 10" abgewählt ist, sieht nach dem Update Verdoppeln und Verliebte Zahlen wieder. Das ist
  gewollt – genau diese Wirkung hat niemand bewusst eingestellt.
- Der Hinweis „Zurzeit gesperrt – dafür fehlt der Zahlenraum …" im Freigabe-Bildschirm kann bei
  Übungen mit festem Zahlenraum nicht mehr auftreten.
