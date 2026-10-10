package io.github.glaside.zahlenschatzjagd.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Prüft die Zugangslogik des Elternbereichs: Prüfsummenbildung, Normalisierung der Antwort auf
 * die Sicherheitsfrage und die Frage, welches Tor gilt. Die Eingabe-Oberfläche hängt an Compose
 * und wird instrumentiert abgedeckt (`ParentPinInstrumentedTest`).
 */
class ParentAccessTest {

    private fun access(
        pin: String = "1234",
        answer: String = "Meier",
        question: String = "Wie hieß meine erste Chefin?",
        salt: String = "abc123",
    ) = ParentAccess(
        salt = salt,
        pinChecksum = checksum(pin, salt),
        question = question,
        answerChecksum = checksum(normalizeAnswer(answer), salt),
    )

    @Test
    fun pruefsummeIstDeterministisch() {
        assertEquals(checksum("1234", "salz"), checksum("1234", "salz"))
    }

    @Test
    fun gleicherWertMitAnderemSalzErgibtAnderePruefsumme() {
        assertNotEquals(checksum("1234", "salz"), checksum("1234", "pfeffer"))
    }

    @Test
    fun pruefsummeEnthaeltDenKlartextNicht() {
        val summe = checksum("1234", "salz")
        assertFalse(summe.contains("1234"))
        assertFalse(summe.contains("salz"))
    }

    @Test
    fun normalisierungSchneidetAussenAbUndSchreibtKlein() {
        assertEquals("meier", normalizeAnswer("  Meier "))
    }

    @Test
    fun normalisierungDampftInnereLeerzeichenEin() {
        assertEquals("von meier", normalizeAnswer(" von   Meier\t"))
    }

    @Test
    fun pinWirdNurExaktAkzeptiert() {
        val zugang = access(pin = "4711")
        assertTrue(zugang.acceptsPin("4711"))
        assertFalse(zugang.acceptsPin("4712"))
        assertFalse(zugang.acceptsPin(""))
    }

    /** Der Fall, der ADR-0016 zugrunde liegt: Die Rettung darf nicht an Schreibweise scheitern. */
    @Test
    fun antwortWirdUnabhaengigVonSchreibweiseUndLeerzeichenAkzeptiert() {
        val zugang = access(answer = "Meier")
        assertTrue(zugang.acceptsAnswer("  meier "))
        assertTrue(zugang.acceptsAnswer("MEIER"))
        assertFalse(zugang.acceptsAnswer("Meyer"))
    }

    @Test
    fun ohnePinGiltDieElternfrage() {
        assertEquals(ParentGate.ELTERNFRAGE, gateFor(null))
    }

    @Test
    fun mitPinGiltDiePin() {
        assertEquals(ParentGate.PIN, gateFor(access()))
    }

    @Test
    fun nurVierZiffernSindEineGueltigePin() {
        assertTrue(isValidPin("0000"))
        assertTrue(isValidPin("9182"))
        assertFalse(isValidPin("123"))
        assertFalse(isValidPin("12345"))
        assertFalse(isValidPin("12a4"))
        assertFalse(isValidPin(""))
    }

    @Test
    fun salzIstReproduzierbarUndNichtZweimalDasselbe() {
        assertEquals(newSalt(Random(42)), newSalt(Random(42)))
        val random = Random(42)
        assertNotEquals(newSalt(random), newSalt(random))
    }

    private fun aktion(
        existingQuestion: String? = null,
        pin: String = "1234",
        pinRepeat: String = pin,
        question: String = "Erste Chefin?",
        answer: String = "Meier",
    ) = pinSetupAction(existingQuestion, pin, pinRepeat, question, answer)

    @Test
    fun ersteVergabeSetztAllesGemeinsam() {
        assertEquals(
            PinSetupAction.SetAll("1234", "Erste Chefin?", "Meier"),
            aktion(),
        )
    }

    @Test
    fun unvollstaendigeVergabeWirdBenannt() {
        assertTrue(aktion(pin = "12") is PinSetupAction.Rejected)
        assertTrue(aktion(pinRepeat = "1235") is PinSetupAction.Rejected)
        assertTrue(aktion(question = "   ") is PinSetupAction.Rejected)
        assertTrue(aktion(answer = "  ") is PinSetupAction.Rejected)
    }

    /** Wer nur die PIN wechselt, soll seine Frage nicht neu erfinden müssen. */
    @Test
    fun unveraenderteFrageOhneAntwortTauschtNurDiePin() {
        assertEquals(
            PinSetupAction.SetPinOnly("1234"),
            aktion(existingQuestion = "Erste Chefin?", question = "Erste Chefin?", answer = ""),
        )
    }

    @Test
    fun geaenderteFrageVerlangtEineNeueAntwort() {
        val ohneAntwort =
            aktion(existingQuestion = "Erste Chefin?", question = "Erster Hund?", answer = "")
        assertTrue(ohneAntwort is PinSetupAction.Rejected)
    }

    @Test
    fun geaenderteFrageMitAntwortSetztBeidesNeu() {
        assertEquals(
            PinSetupAction.SetAll("1234", "Erster Hund?", "Bello"),
            aktion(existingQuestion = "Erste Chefin?", question = "Erster Hund?", answer = "Bello"),
        )
    }

    @Test
    fun neueAntwortZurGleichenFrageSetztBeidesNeu() {
        assertEquals(
            PinSetupAction.SetAll("1234", "Erste Chefin?", "Meyer"),
            aktion(existingQuestion = "Erste Chefin?", question = "Erste Chefin?", answer = "Meyer"),
        )
    }

    /** Ohne vorhandene Frage gibt es nichts, was bleiben könnte – die Antwort bleibt Pflicht. */
    @Test
    fun beimErstenEinrichtenIstDieAntwortPflicht() {
        assertTrue(aktion(existingQuestion = null, answer = "") is PinSetupAction.Rejected)
    }
}
