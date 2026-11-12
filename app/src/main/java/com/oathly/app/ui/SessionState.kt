package com.oathly.app.ui

import com.oathly.app.data.AuBank
import com.oathly.app.data.UsBank
import com.oathly.app.domain.Answered
import com.oathly.app.domain.AuTest
import com.oathly.app.domain.Interview
import org.json.JSONArray
import org.json.JSONObject

/**
 * The open screens and the test or deck in progress, written to the saved instance state so
 * nothing is lost when Android stops the app in the background (for example while the
 * speech recognizer is open) and starts it again.
 */
internal object SessionState {

    private val screens: Map<String, Screen> = listOf(
        Screen.Onboarding, Screen.Home, Screen.StudyMenu, Screen.Study, Screen.Interview, Screen.AuPractice,
        Screen.Results, Screen.Officials, Screen.Progress, Screen.Settings, Screen.PickState, Screen.PickRep,
        Screen.About, Screen.Pro,
    ).associateBy { it.toString() }

    data class Session(
        val stack: List<Screen>,
        val deck: Deck?,
        val interview: Interview?,
        val auTest: AuTest?,
        val result: ResultSummary?,
    )

    fun encode(stack: List<Screen>, deck: Deck?, interview: Interview?, auTest: AuTest?, result: ResultSummary?): String {
        val o = JSONObject()
        o.put("stack", JSONArray(stack.map { it.toString() }))
        deck?.let { d ->
            o.put("deck", JSONObject().put("title", d.title).put("ids", JSONArray(d.ids)).put("index", d.index)
                .put("revealed", d.revealed).put("known", d.known).put("missed", d.missed))
        }
        interview?.let { iv ->
            o.put("interview", JSONObject()
                .put("order", JSONArray(iv.questions.map { it.id }))
                .put("asked", iv.asked).put("passAt", iv.passAt).put("failAt", iv.failAt)
                .put("answers", JSONArray(iv.answers.map { a ->
                    JSONObject().put("id", a.question.id).put("ok", a.correct).put("given", a.given ?: JSONObject.NULL)
                })))
        }
        auTest?.let { t ->
            o.put("au", JSONObject()
                .put("qs", JSONArray(t.questions.map { q ->
                    JSONObject().put("id", q.id).put("options", JSONArray(q.options)).put("ans", q.answerIndex)
                }))
                .put("passMark", t.passMark).put("index", t.index)
                .put("chosen", JSONArray(t.chosen.map { it ?: -1 })))
        }
        result?.let { r ->
            o.put("result", JSONObject().put("title", r.title).put("passed", r.passed).put("right", r.right)
                .put("total", r.total).put("detail", r.detail).put("retry", r.retry.toString())
                .put("items", JSONArray(r.items.map { i ->
                    JSONObject().put("q", i.question).put("ok", i.correct).put("given", i.given ?: JSONObject.NULL)
                        .put("answer", i.answer).put("exp", i.explanation ?: JSONObject.NULL)
                })))
        }
        return o.toString()
    }

    /** Null when nothing usable was saved. Screens whose data cannot be rebuilt are dropped. */
    fun decode(json: String, us: UsBank?, au: AuBank?): Session? = runCatching {
        val o = JSONObject(json)
        val usById = us?.questions?.associateBy { it.id }.orEmpty()
        val auById = au?.questions?.associateBy { it.id }.orEmpty()

        val deck = o.optJSONObject("deck")?.let { d ->
            Deck(
                d.getString("title"), d.getJSONArray("ids").strings(), d.getInt("index"),
                d.getBoolean("revealed"), d.getInt("known"), d.getInt("missed"),
            )
        }
        val interview = o.optJSONObject("interview")?.let { i ->
            val order = i.getJSONArray("order").strings().map { usById[it] ?: return@let null }
            val answersJson = i.getJSONArray("answers")
            val answers = (0 until answersJson.length()).map { k ->
                val a = answersJson.getJSONObject(k)
                Answered(usById[a.getString("id")] ?: return@let null, a.getBoolean("ok"), a.optStringOrNull("given"))
            }
            Interview(order, i.getInt("asked"), i.getInt("passAt"), i.getInt("failAt"), answers)
        }
        val auTest = o.optJSONObject("au")?.let { t ->
            val qsJson = t.getJSONArray("qs")
            val qs = (0 until qsJson.length()).map { k ->
                val q = qsJson.getJSONObject(k)
                val base = auById[q.getString("id")] ?: return@let null
                base.copy(options = q.getJSONArray("options").strings(), answerIndex = q.getInt("ans"))
            }
            val chosenJson = t.getJSONArray("chosen")
            val chosen = (0 until chosenJson.length()).map { k -> chosenJson.getInt(k).takeIf { it >= 0 } }
            if (chosen.size != qs.size) null else AuTest(qs, t.getInt("passMark"), chosen, t.getInt("index"))
        }
        val result = o.optJSONObject("result")?.let { r ->
            val itemsJson = r.getJSONArray("items")
            ResultSummary(
                r.getString("title"), r.getBoolean("passed"), r.getInt("right"), r.getInt("total"), r.getString("detail"),
                (0 until itemsJson.length()).map { k ->
                    val i = itemsJson.getJSONObject(k)
                    ResultItem(i.getString("q"), i.getBoolean("ok"), i.optStringOrNull("given"), i.getString("answer"), i.optStringOrNull("exp"))
                },
                screens[r.getString("retry")] ?: Screen.Home,
            )
        }
        val stack = o.getJSONArray("stack").strings().mapNotNull { screens[it] }.filter { s ->
            when (s) {
                Screen.Study -> deck != null
                Screen.Interview -> interview != null
                Screen.AuPractice -> auTest != null
                Screen.Results -> result != null
                else -> true
            }
        }
        if (stack.isEmpty()) null else Session(stack, deck, interview, auTest, result)
    }.getOrNull()

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key)
}
