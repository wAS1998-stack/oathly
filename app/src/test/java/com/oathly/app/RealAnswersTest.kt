package com.oathly.app

import com.oathly.app.data.ContentRepository
import com.oathly.app.data.UsQuestion
import com.oathly.app.domain.AnswerMatcher
import com.oathly.app.domain.DynamicAnswers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Answers real applicants give (typed, or as a speech recognizer writes them), checked against
 * the bundled question files exactly as the app checks them.
 */
class RealAnswersTest {
    private fun bank(v: String) =
        ContentRepository.parseUsBank(File("src/main/assets/content/us_$v.json").readText()).questions.associateBy { it.number }

    private val q25 = bank("2025")
    private val q08 = bank("2008")
    private val officials = ContentRepository.parseOfficials(File("src/main/assets/officials.json").readText())

    private fun accepts(q: UsQuestion, said: String) =
        AnswerMatcher.check(said, q.answers + q.alternatives, q.need).correct

    private fun right(q: UsQuestion, vararg said: String) =
        said.forEach { assertTrue("Q${q.number} should accept \"$it\"", accepts(q, it)) }

    private fun wrong(q: UsQuestion, vararg said: String) =
        said.forEach { assertFalse("Q${q.number} should reject \"$it\"", accepts(q, it)) }

    @Test fun numbers() {
        right(q25.getValue(24), "435", "four hundred thirty five", "four hundred and thirty-five")
        wrong(q25.getValue(24), "400", "35", "534")
        right(q08.getValue(21), "435")
        right(q25.getValue(21), "100", "a hundred", "one hundred")
        right(q25.getValue(22), "six", "6 years", "six years")
        right(q25.getValue(36), "four", "4 years")
        right(q08.getValue(54), "18", "eighteen and older", "18 or older")
        right(q08.getValue(57), "18", "at 18", "at age eighteen")
    }

    @Test fun presidentIsNotVicePresident() {
        for (n in listOf(42, 43, 44, 45)) {
            right(q25.getValue(n), "the President", "president of the united states")
            wrong(q25.getValue(n), "the Vice President", "vice-president")
        }
        right(q25.getValue(40), "the vice president")
        right(q25.getValue(48), "vice president and attorney general")
        wrong(q25.getValue(105), "Theodore Roosevelt", "Teddy Roosevelt")
        right(q25.getValue(105), "Roosevelt", "Franklin Roosevelt", "FDR", "Franklin D. Roosevelt")
        wrong(q25.getValue(81), "New York, New Jersey, West Virginia, Georgia, Delaware")
        right(q25.getValue(81), "New York, New Jersey, Virginia, Georgia, Delaware")
    }

    @Test fun worldWars() {
        right(q25.getValue(100), "World War 2", "World War Two", "World War One", "WW2", "world war II", "ww 1")
        right(q25.getValue(102), "after World War One", "1920", "the 19th amendment")
        right(q25.getValue(107), "general during World War 2", "34th president", "president during the Korean War")
        right(q08.getValue(82), "World War 2", "World War Two")
        right(q08.getValue(78), "World War 2")
    }

    @Test fun holidays() {
        right(q25.getValue(126), "Christmas, Thanksgiving, and Independence Day", "MLK Day, Labor Day and Memorial Day",
            "New Years Day, Presidents Day and Veterans Day", "Fourth of July, Christmas and Juneteenth",
            "Martin Luther King Day, Veteran's Day, President's Day")
        wrong(q25.getValue(126), "Christmas and Thanksgiving", "Christmas, Christmas and Christmas")
        right(q08.getValue(100), "Christmas and Thanksgiving", "Fourth of July and Labor Day")
        right(q08.getValue(99), "July 4th", "Fourth of July", "July 4")
    }

    @Test fun flagAndColonies() {
        right(q08.getValue(96), "13 original colonies", "because there were 13 original colonies", "thirteen colonies")
        right(q08.getValue(97), "50 states", "one star for each state", "fifty states")
        right(q25.getValue(121), "because there were 13 colonies", "the original thirteen colonies")
        right(q25.getValue(122), "one for each state", "50 states")
        right(q08.getValue(61), "high taxes", "taxation without representation")
    }

    @Test fun synonymsUscisListsInParentheses() {
        right(q25.getValue(48), "Secretary of Defense and Secretary of State")
        right(q25.getValue(28), "Connecticut Compromise", "the great compromise", "equal representation")
        right(q25.getValue(51), "resolves disagreements about the law", "reviews laws")
        right(q25.getValue(59), "police", "fire departments", "give a driver's license")
        right(q25.getValue(63), "women can vote", "citizens 18 and older can vote")
        right(q25.getValue(71), "16th amendment", "required by law")
        right(q25.getValue(77), "quartering", "Coercive Acts", "taxation without representation")
        right(q25.getValue(80), "Battle of Trenton", "Yorktown", "Bunker Hill")
        right(q25.getValue(87), "Louisiana Purchase", "third president")
        right(q25.getValue(93), "Antietam", "Battle of Antietam", "Sharpsburg", "Gettysburg")
        right(q25.getValue(94), "Emancipation Proclamation", "preserved the Union", "freed the slaves", "16th president")
        right(q25.getValue(118), "cars", "lightbulb", "the light bulb", "the moon landing")
        right(q08.getValue(75), "Emancipation Proclamation")
    }

    @Test fun anyOrderAndShortForms() {
        right(q25.getValue(19), "the House and the Senate", "House of Representatives and Senate", "Senate and House")
        wrong(q25.getValue(19), "the House", "the Senate")
        right(q25.getValue(16), "executive, legislative and judicial", "legislative executive judicial")
        right(q08.getValue(45), "Democrats and Republicans", "Republican and Democratic")
        right(q08.getValue(81), "Germany, Italy and Japan")
        right(q25.getValue(65), "freedom of speech, religion and assembly")
        right(q08.getValue(51), "freedom of speech and religion")
        right(q25.getValue(86), "first president")
        right(q25.getValue(88), "fourth president")
        right(q25.getValue(17), "executive", "the executive branch")
        right(q25.getValue(8), "all men are created equal", "all people are created equal")
        right(q25.getValue(13), "nobody is above the law", "no one is above the law")
        right(q25.getValue(74), "native american", "American Indians")
        right(q25.getValue(76), "war of independence", "the revolutionary war")
        right(q25.getValue(104), "1929", "the stock market crash in 1929")
        right(q25.getValue(115), "terrorist attack", "terrorists attacked the World Trade Center")
        right(q25.getValue(116), "the Iraq War", "Afghanistan war", "war on terror")
        right(q25.getValue(64), "you must be a U.S. citizen", "citizens")
        right(q08.getValue(24), "the people of the state", "all people of the state")
        right(q08.getValue(74), "state's rights", "slavery")
    }

    @Test fun stillStrict() {
        wrong(q25.getValue(2), "the Declaration of Independence", "Bill of Rights")
        wrong(q25.getValue(117), "Aztec", "Inca")
        wrong(q25.getValue(18), "the president", "the supreme court")
        wrong(q25.getValue(78), "Washington", "Franklin")
        wrong(q25.getValue(53), "8", "10")
    }

    @Test fun answersThatDependOnWhereYouLive() {
        fun ok(key: String, state: String?, said: String): Boolean {
            val d = DynamicAnswers.resolve(key, officials, state, null)
            return AnswerMatcher.check(said, d.accepted, 1).correct
        }
        assertTrue(ok("capital", "MN", "St Paul"))
        assertTrue(ok("capital", "MN", "Saint Paul"))
        assertTrue(ok("governor", "DC", "DC doesn't have a governor"))
        assertTrue(ok("president_party", null, "Republicans"))
        assertTrue(ok("governor", "NY", "Hochul"))
        assertTrue(ok("governor", "VA", "Spanberger"))
        assertTrue(ok("governor", "AR", "Sarah Sanders"))
        assertTrue(ok("governor", "NH", "Kelly Ayotte"))
        assertTrue(ok("senators", "SC", "Graham"))
        assertTrue(ok("senators", "SC", "Tim Scott"))
        assertFalse(ok("senators", "CA", "Tim Scott"))
    }
}
