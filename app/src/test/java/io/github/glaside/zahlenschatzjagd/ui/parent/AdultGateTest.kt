package io.github.glaside.zahlenschatzjagd.ui.parent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Prüft die reine Logik des Adult-Gates (Erzeugung + Antwortprüfung). Die Eingabe-UI selbst
 * hängt an Compose und wird im Instrumented-Test abgedeckt.
 */
class AdultGateTest {

    @Test
    fun akzeptiertNurDasKorrekteProdukt() {
        val challenge = AdultChallenge(7, 8)
        assertTrue(challenge.accepts(56))
        assertFalse(challenge.accepts(55))
        assertFalse(challenge.accepts(0))
    }

    @Test
    fun frageEnthaeltBeideFaktoren() {
        val challenge = AdultChallenge(6, 9)
        assertTrue(challenge.question.contains("6"))
        assertTrue(challenge.question.contains("9"))
    }

    @Test
    fun erzeugteFaktorenLiegenImSchwierigenBereich() {
        val random = Random(42)
        repeat(200) {
            val challenge = createAdultChallenge(random)
            assertTrue("a=${challenge.a} außerhalb 6..9", challenge.a in 6..9)
            assertTrue("b=${challenge.b} außerhalb 6..9", challenge.b in 6..9)
        }
    }
}
