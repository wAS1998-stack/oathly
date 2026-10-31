package com.oathly.app.data

import android.content.res.AssetManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** Reads the bundled question banks. All content ships inside the app and works offline. */
class ContentRepository(private val assets: AssetManager) {

    private val usCache = ConcurrentHashMap<String, UsBank>()
    @Volatile private var auCache: AuBank? = null

    fun read(path: String): String = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun usBank(version: String): UsBank =
        usCache.getOrPut(version) { parseUsBank(read("content/us_$version.json")) }

    fun auBank(): AuBank = auCache ?: parseAuBank(read("content/au.json")).also { auCache = it }

    companion object {
        fun parseUsBank(json: String): UsBank {
            val root = JSONObject(json)
            val source = root.getJSONObject("source")
            val rules = root.getJSONObject("rules")
            val qs = root.getJSONArray("questions")
            val list = (0 until qs.length()).map { i ->
                val q = qs.getJSONObject(i)
                UsQuestion(
                    id = q.getString("id"),
                    number = q.getInt("n"),
                    category = q.getString("cat"),
                    categoryTitle = q.getString("catTitle"),
                    subcategory = q.getString("sub"),
                    text = q.getString("q"),
                    answers = q.getJSONArray("a").strings(),
                    need = q.optInt("need", 1),
                    senior = q.optBoolean("senior", false),
                    dynamicKey = q.optStringOrNull("dyn"),
                    dynamicNote = q.optStringOrNull("dynNote"),
                    explanation = q.optStringOrNull("exp"),
                )
            }
            return UsBank(
                version = root.getString("version"),
                sourceUrl = source.getString("url"),
                updatesUrl = source.getString("updatesUrl"),
                rules = UsRules(
                    asked = rules.getInt("asked"),
                    passAt = rules.getInt("passAt"),
                    failAt = rules.getInt("failAt"),
                    seniorAsked = rules.getInt("seniorAsked"),
                    seniorPassAt = rules.getInt("seniorPassAt"),
                ),
                questions = list,
            )
        }

        fun parseAuBank(json: String): AuBank {
            val root = JSONObject(json)
            val source = root.getJSONObject("source")
            val rules = root.getJSONObject("rules")
            val qs = root.getJSONArray("questions")
            val list = (0 until qs.length()).map { i ->
                val q = qs.getJSONObject(i)
                AuQuestion(
                    id = q.getString("id"),
                    part = q.getInt("part"),
                    partTitle = q.getString("partTitle"),
                    text = q.getString("question"),
                    options = q.getJSONArray("options").strings(),
                    answerIndex = q.getInt("answerIndex"),
                    explanation = q.optString("explanation", ""),
                    values = q.optBoolean("values", false),
                )
            }
            return AuBank(
                sourceTitle = source.getString("title"),
                sourceUrl = source.getString("url"),
                licence = source.optString("licence", ""),
                attribution = source.optString("attribution", ""),
                questionsPerTest = rules.optInt("questions", 20),
                valuesPerTest = rules.optInt("valuesQuestions", 5),
                passPercent = rules.optInt("passMarkPercent", 75),
                valuesRule = rules.optString("valuesRule", ""),
                timeLimitMinutes = if (rules.has("timeLimitMinutes")) rules.getInt("timeLimitMinutes") else null,
                questions = list,
            )
        }

        fun parseOfficials(json: String): Officials {
            val root = JSONObject(json)
            val fed = root.getJSONObject("federal")
            val places = root.getJSONArray("places")
            return Officials(
                generatedAt = root.getString("generatedAt"),
                president = fed.optPerson("president"),
                vicePresident = fed.optPerson("vicePresident"),
                speaker = fed.optPerson("speaker"),
                chiefJustice = fed.optPerson("chiefJustice"),
                justices = fed.optInt("justices", 9),
                places = (0 until places.length()).map { i ->
                    val p = places.getJSONObject(i)
                    Place(
                        code = p.getString("code"),
                        name = p.getString("name"),
                        type = p.getString("type"),
                        capital = p.optStringOrNull("capital"),
                        capitalAlt = p.optJSONArray("capitalAlt")?.strings() ?: emptyList(),
                        governor = p.optPerson("governor"),
                        senators = p.optJSONArray("senators")?.persons() ?: emptyList(),
                        representatives = p.optJSONArray("representatives")?.persons() ?: emptyList(),
                        delegate = p.optPerson("delegate"),
                    )
                },
            )
        }

        private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

        private fun JSONArray.persons(): List<Person> = (0 until length()).map { getJSONObject(it).toPerson() }

        private fun JSONObject.toPerson() = Person(
            name = getString("name"),
            short = optString("short", getString("name").split(" ").last()),
            party = optStringOrNull("party"),
            district = if (has("district") && !isNull("district")) getInt("district") else null,
            title = optStringOrNull("title"),
        )

        private fun JSONObject.optPerson(key: String): Person? =
            if (has(key) && !isNull(key)) getJSONObject(key).toPerson() else null

        private fun JSONObject.optStringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) getString(key) else null
    }
}
