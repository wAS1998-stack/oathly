package com.oathly.app.data

import com.oathly.app.domain.QuestionStat
import com.oathly.app.domain.Srs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/** A finished practice test, for the progress screen. */
data class TestRecord(val time: Long, val kind: String, val right: Int, val total: Int, val passed: Boolean)

/**
 * Study progress. Lives only on the device (no account, nothing uploaded).
 * Writes go to a temp file and are renamed, so a crash can't corrupt progress.
 */
class ProgressStore(private val file: File, private val scope: CoroutineScope) {

    private val _stats = MutableStateFlow<Map<String, QuestionStat>>(emptyMap())
    val stats: StateFlow<Map<String, QuestionStat>> = _stats.asStateFlow()

    private val _days = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val days: StateFlow<Map<Long, Int>> = _days.asStateFlow()

    private val _tests = MutableStateFlow<List<TestRecord>>(emptyList())
    val tests: StateFlow<List<TestRecord>> = _tests.asStateFlow()

    private val writeLock = Mutex()

    fun today(): Long = LocalDate.now().toEpochDay()

    fun load() {
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())
            val s = root.optJSONObject("stats") ?: JSONObject()
            val stats = HashMap<String, QuestionStat>()
            for (key in s.keys()) {
                val a = s.getJSONArray(key)
                stats[key] = QuestionStat(
                    attempts = a.getInt(0), correct = a.getInt(1), streak = a.getInt(2),
                    ease = a.getDouble(3), intervalDays = a.getInt(4), dueDay = a.getLong(5),
                    lastCorrect = a.getInt(6) == 1, lastDay = a.getLong(7),
                )
            }
            val d = root.optJSONObject("days") ?: JSONObject()
            val days = HashMap<Long, Int>()
            for (key in d.keys()) days[key.toLong()] = d.getInt(key)
            val t = root.optJSONArray("tests") ?: JSONArray()
            val tests = (0 until t.length()).map { i ->
                val o = t.getJSONObject(i)
                TestRecord(o.getLong("t"), o.getString("k"), o.getInt("r"), o.getInt("n"), o.getBoolean("p"))
            }
            _stats.value = stats
            _days.value = days
            _tests.value = tests
        }
    }

    fun record(id: String, correct: Boolean) {
        val today = today()
        val old = _stats.value[id] ?: QuestionStat()
        _stats.value = _stats.value + (id to Srs.record(old, correct, today))
        _days.value = _days.value + (today to ((_days.value[today] ?: 0) + 1))
        save()
    }

    /** Replace a question's stats without counting another answer for today. */
    fun replace(id: String, stat: QuestionStat) {
        _stats.value = _stats.value + (id to stat)
        save()
    }

    fun addTest(record: TestRecord) {
        _tests.value = (_tests.value + record).takeLast(50)
        save()
    }

    fun answeredToday(): Int = _days.value[today()] ?: 0

    /** Days in a row with at least one question answered, counting today or yesterday. */
    fun streak(): Int {
        val days = _days.value
        var day = today()
        if ((days[day] ?: 0) == 0) day -= 1
        var count = 0
        while ((days[day] ?: 0) > 0) { count++; day-- }
        return count
    }

    fun reset(idPrefix: String) {
        _stats.value = _stats.value.filterKeys { !it.startsWith(idPrefix) }
        _tests.value = _tests.value.filterNot { it.kind.startsWith(idPrefix) }
        save()
    }

    private fun save() {
        val stats = _stats.value
        val days = _days.value
        val tests = _tests.value
        scope.launch(Dispatchers.IO) {
            writeLock.withLock {
                val root = JSONObject()
                val s = JSONObject()
                for ((id, st) in stats) {
                    s.put(id, JSONArray().apply {
                        put(st.attempts); put(st.correct); put(st.streak); put(st.ease); put(st.intervalDays)
                        put(st.dueDay); put(if (st.lastCorrect) 1 else 0); put(st.lastDay)
                    })
                }
                root.put("stats", s)
                val d = JSONObject()
                for ((day, n) in days) d.put(day.toString(), n)
                root.put("days", d)
                root.put("tests", JSONArray().apply {
                    tests.forEach { put(JSONObject().put("t", it.time).put("k", it.kind).put("r", it.right).put("n", it.total).put("p", it.passed)) }
                })
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(root.toString())
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }
}
