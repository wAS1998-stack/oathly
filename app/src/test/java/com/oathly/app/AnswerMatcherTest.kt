package com.oathly.app

import com.oathly.app.domain.AnswerMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerMatcherTest {
    private fun ok(given: String, vararg answers: String, need: Int = 1) =
        AnswerMatcher.check(given, answers.toList(), need).correct

    @Test fun optionalWordsInParentheses() {
        assertTrue(ok("the constitution", "(U.S.) Constitution"))
        assertTrue(ok("U.S. Constitution", "(U.S.) Constitution"))
        assertTrue(ok("Washington", "(George) Washington"))
    }

    @Test fun numbersAsWordsOrDigits() {
        assertTrue(ok("27", "Twenty-seven (27)"))
        assertTrue(ok("twenty seven", "Twenty-seven (27)"))
        assertFalse(ok("270", "Twenty-seven (27)"))
        assertTrue(ok("the twenty-second amendment", "(Because of) the 22nd Amendment"))
    }

    @Test fun neverAcceptsTheOppositeMeaning() {
        assertFalse(ok("to spread communism", "To stop the spread of communism"))
        assertFalse(ok("spread of communism", "To stop the spread of communism"))
        assertTrue(ok("to stop the spread of communism", "To stop the spread of communism"))
    }

    @Test fun countsDistinctAnswersForNameTwo() {
        val rights = arrayOf("life", "liberty", "pursuit of happiness")
        assertTrue(ok("life and liberty", *rights, need = 2))
        assertFalse(ok("life", *rights, need = 2))
        assertFalse(ok("liberty and liberty", *rights, need = 2))
    }

    @Test fun namesInitialsAndAccents() {
        assertTrue(ok("JD Vance", "J.D. Vance", "Vance"))
        assertTrue(ok("St. Paul", "Saint Paul", "St. Paul"))
        assertTrue(ok("hagatna", "Hagåtña"))
        assertFalse(ok("York", "New York"))
    }
}
