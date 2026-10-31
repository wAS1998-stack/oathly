package com.oathly.app

import com.oathly.app.data.AuBank
import com.oathly.app.data.AuQuestion
import com.oathly.app.data.UsBank
import com.oathly.app.data.UsQuestion
import com.oathly.app.data.UsRules
import com.oathly.app.domain.AuTest
import com.oathly.app.domain.Interview
import com.oathly.app.domain.Readiness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ExamsTest {
    private val bank = UsBank(
        "2025", "", "", UsRules(20, 12, 9, 10, 6),
        (1..128).map { UsQuestion("q$it", it, "government", "American Government", "A", "Q$it", listOf("a$it"), 1, it % 6 == 0) },
    )

    @Test fun interviewStopsAtTwelveRight() {
        var iv = Interview.start(bank, false, Random(1))
        repeat(12) { iv = iv.answer(true, null) }
        assertTrue(iv.finished && iv.passed)
        assertEquals(12, iv.answers.size)
    }

    @Test fun interviewStopsAtNineWrong() {
        var iv = Interview.start(bank, false, Random(2))
        repeat(9) { iv = iv.answer(false, null) }
        assertTrue(iv.finished)
        assertFalse(iv.passed)
    }

    @Test fun seniorModeUsesStarredQuestionsOnly() {
        val iv = Interview.start(bank, true, Random(3))
        assertTrue(iv.questions.all { it.senior })
        assertEquals(10, iv.asked); assertEquals(6, iv.passAt); assertEquals(5, iv.failAt)
    }

    @Test fun australianTestNeedsEveryValuesQuestion() {
        val qs = (1..120).map { i -> AuQuestion("au$i", (i % 4) + 1, "P", "Q$i", listOf("a", "b", "c"), i % 3, "", (i % 4) + 1 == 4) }
        val auBank = AuBank("t", "u", "l", "a", 20, 5, 75, "", 45, qs)
        var t = AuTest.start(auBank, Random(5))
        assertEquals(20, t.questions.size); assertEquals(5, t.valuesTotal); assertEquals(15, t.passMark)
        // Everything right except one values question: 19/20 but still a fail.
        var missedValues = false
        while (!t.finished) {
            val q = t.current!!
            val pick = if (q.values && !missedValues) { missedValues = true; (q.answerIndex + 1) % 3 } else q.answerIndex
            t = t.choose(pick).next()
        }
        assertEquals(19, t.score)
        assertFalse(t.passed)
    }

    @Test fun readinessIsSensible() {
        assertTrue(Readiness.usPassChance(List(128) { 0.98 }, 20, 12, 9, 7) > 0.95)
        assertTrue(Readiness.usPassChance(List(128) { 0.1 }, 20, 12, 9, 7) < 0.01)
    }
}
