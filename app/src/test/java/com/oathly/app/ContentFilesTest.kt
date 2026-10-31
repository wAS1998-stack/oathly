package com.oathly.app

import com.oathly.app.data.ContentRepository
import com.oathly.app.domain.AnswerMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled content: a broken file must never reach users. */
class ContentFilesTest {
    private fun asset(path: String) = File("src/main/assets/$path").readText()

    @Test fun us2025Bank() {
        val b = ContentRepository.parseUsBank(asset("content/us_2025.json"))
        assertEquals(128, b.questions.size)
        assertEquals(20, b.questions.count { it.senior })
        assertEquals(128, b.questions.map { it.id }.toSet().size)
        assertEquals(listOf(20, 12, 9), listOf(b.rules.asked, b.rules.passAt, b.rules.failAt))
        assertEquals(8, b.questions.count { it.dynamicKey != null })
        b.questions.filter { it.dynamicKey == null }.forEach { q ->
            assertTrue("Q${q.number} has no answers", q.answers.isNotEmpty())
            val said = q.answers.take(q.need).joinToString(", ")
            assertTrue("Q${q.number} official answer rejected", AnswerMatcher.check(said, q.answers, q.need).correct)
        }
    }

    @Test fun us2008Bank() {
        val b = ContentRepository.parseUsBank(asset("content/us_2008.json"))
        assertEquals(100, b.questions.size)
        assertEquals(20, b.questions.count { it.senior })
        assertEquals(10, b.questions.count { it.dynamicKey != null })
        assertEquals(listOf(10, 6, 5), listOf(b.rules.asked, b.rules.passAt, b.rules.failAt))
    }

    @Test fun australianBank() {
        val b = ContentRepository.parseAuBank(asset("content/au.json"))
        assertTrue(b.questions.size >= 120)
        assertEquals(setOf(1, 2, 3, 4), b.questions.map { it.part }.toSet())
        assertTrue(b.questions.all { it.options.size == 3 && it.answerIndex in 0..2 })
        assertTrue(b.questions.all { it.values == (it.part == 4) })
        assertEquals(b.questions.size, b.questions.map { it.id }.toSet().size)
        assertTrue(b.attribution.isNotBlank())
    }

    @Test fun officialsSnapshot() {
        val o = ContentRepository.parseOfficials(asset("officials.json"))
        assertTrue(o.places.size >= 56)
        assertEquals(100, o.places.sumOf { it.senators.size })
        assertTrue(o.president != null && o.vicePresident != null)
    }
}
