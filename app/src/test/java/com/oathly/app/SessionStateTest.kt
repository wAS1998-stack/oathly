package com.oathly.app

import com.oathly.app.data.ContentRepository
import com.oathly.app.domain.AuTest
import com.oathly.app.domain.Interview
import com.oathly.app.ui.Deck
import com.oathly.app.ui.ResultItem
import com.oathly.app.ui.ResultSummary
import com.oathly.app.ui.Screen
import com.oathly.app.ui.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SessionStateTest {
    private val us = ContentRepository.parseUsBank(File("src/main/assets/content/us_2025.json").readText())
    private val au = ContentRepository.parseAuBank(File("src/main/assets/content/au.json").readText())

    @Test fun interviewInProgressSurvives() {
        val iv = Interview.start(us, false, Random(1)).answer(true, "the Constitution").answer(false, null)
        val json = SessionState.encode(listOf(Screen.Home, Screen.Interview), null, iv, null, null)
        val back = SessionState.decode(json, us, null)!!
        assertEquals(listOf(Screen.Home, Screen.Interview), back.stack)
        assertEquals(iv, back.interview)
    }

    @Test fun australianTestKeepsOptionOrderAndPicks() {
        val t = AuTest.start(au, Random(2)).choose(1).next().choose(0)
        val json = SessionState.encode(listOf(Screen.Home, Screen.AuPractice), null, null, t, null)
        assertEquals(t, SessionState.decode(json, null, au)!!.auTest)
    }

    @Test fun deckAndResultsSurvive() {
        val deck = Deck("All questions", us.questions.take(5).map { it.id }, index = 2, revealed = true, known = 1, missed = 1)
        val result = ResultSummary("You passed!", true, 12, 15, "detail",
            listOf(ResultItem("Q", true, null, "A", null), ResultItem("Q2", false, "x", "B", "why")), Screen.Interview)
        val json = SessionState.encode(listOf(Screen.Home, Screen.StudyMenu, Screen.Study, Screen.Results), deck, null, null, result)
        val back = SessionState.decode(json, us, null)!!
        assertEquals(deck, back.deck)
        assertEquals(result, back.result)
        assertEquals(4, back.stack.size)
    }

    @Test fun screensWithoutTheirDataAreDropped() {
        val json = SessionState.encode(listOf(Screen.Home, Screen.Interview), null, null, null, null)
        assertEquals(listOf(Screen.Home), SessionState.decode(json, us, null)!!.stack)
        assertNull(SessionState.decode("not json", us, null))
    }
}
