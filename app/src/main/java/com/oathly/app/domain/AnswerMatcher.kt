package com.oathly.app.domain

import java.text.Normalizer

/**
 * Checks a spoken or typed answer against the official acceptable answers.
 *
 * Deliberately stricter than a real officer: an answer is accepted only when one of the
 * official answers (with its optional words in parentheses either kept or dropped) appears
 * in what the user said. Extra words around it are fine ("I think it's the Constitution").
 * Partial phrases are not ("spread of communism" never matches "stop the spread of
 * communism"). The study screens always let the user overrule a strict "wrong".
 */
object AnswerMatcher {

    data class Result(val matched: Int, val needed: Int, val matchedAnswers: List<String>) {
        val correct: Boolean get() = needed > 0 && matched >= needed
    }

    private val articles = setOf("a", "an", "the")
    private val units = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
        "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19,
    )
    private val tens = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
    )
    private val ordinals = mapOf(
        "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5, "sixth" to 6,
        "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10, "eleventh" to 11,
        "twelfth" to 12, "thirteenth" to 13, "fourteenth" to 14, "fifteenth" to 15,
        "sixteenth" to 16, "seventeenth" to 17, "eighteenth" to 18, "nineteenth" to 19,
        "twentieth" to 20, "thirtieth" to 30,
    )
    /**
     * A match does not count when the word just before it turns it into a different answer:
     * "vice president" is not "president", "West Virginia" is not "Virginia", "Theodore
     * Roosevelt" is not "(Franklin) Roosevelt".
     */
    private val changesMeaning = setOf("vice", "west", "theodore", "teddy")
    private val marks = Regex("\\p{M}+")
    private val nonWord = Regex("[^a-z0-9\\s]")
    private val space = Regex("\\s+")
    private val ordinalSuffix = Regex("^(\\d+)(st|nd|rd|th)$")

    /** Lower-case word tokens with accents, punctuation, articles and number words normalised. */
    fun normalize(raw: String): List<String> {
        var s = Normalizer.normalize(raw, Normalizer.Form.NFD).replace(marks, "")
        s = s.lowercase()
            .replace('’', '\'').replace('‘', '\'')
            .replace("&", " and ")
            .replace(Regex("\\bst\\.?\\s+"), "saint ")
            .replace(Regex("'s\\b"), "")
            .replace("'", "")
            .replace('-', ' ')
        s = s.replace(nonWord, " ")
        var tokens = s.split(space).filter { it.isNotBlank() }
        tokens = worldWars(tokens)
        tokens = mergeInitials(tokens)
        tokens = numbersToDigits(tokens)
        return tokens.filter { it !in articles }.map(::singular)
    }

    /** "states" and "state", "colonies" and "colony" count as the same word. */
    private fun singular(t: String): String = when {
        t.length <= 3 || t[0].isDigit() -> t
        t.endsWith("ies") && t.length > 4 -> t.dropLast(3) + "y"
        t.endsWith("s") && !t.endsWith("ss") && !t.endsWith("us") && !t.endsWith("is") -> t.dropLast(1)
        else -> t
    }

    /** "World War II", "World War 2", "WW2" and "World War Two" are the same answer. */
    private fun worldWars(tokens: List<String>): List<String> {
        val out = ArrayList<String>(tokens.size + 2)
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val next = tokens.getOrNull(i + 1)
            when {
                t == "ww1" || t == "wwi" -> { out += listOf("world", "war", "1"); i++ }
                t == "ww2" || t == "wwii" -> { out += listOf("world", "war", "2"); i++ }
                t == "ww" && next in warNumbers -> { out += listOf("world", "war", warNumbers.getValue(next!!)); i += 2 }
                t == "war" && (next == "i" || next == "ii") -> { out += listOf("war", warNumbers.getValue(next)); i += 2 }
                else -> { out += t; i++ }
            }
        }
        return out
    }

    private val warNumbers = mapOf("i" to "1", "ii" to "2", "1" to "1", "2" to "2", "one" to "1", "two" to "2")

    /** "u s" -> "us", "d c" -> "dc", "j d vance" -> "jd vance". */
    private fun mergeInitials(tokens: List<String>): List<String> {
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            if (isInitial(tokens[i])) {
                var j = i
                val sb = StringBuilder()
                while (j < tokens.size && isInitial(tokens[j])) {
                    sb.append(tokens[j]); j++
                }
                if (j - i >= 2) { out.add(sb.toString()); i = j; continue }
            }
            out.add(tokens[i]); i++
        }
        return out
    }

    /** Single letters that are initials. "a" and "i" are words ("a U.S. citizen"). */
    private fun isInitial(t: String) = t.length == 1 && t[0].isLetter() && t != "a" && t != "i"

    /** A number below 100 written as words at [k]: (value, words used), or (0, 0). */
    private fun smallNumberAt(tokens: List<String>, k: Int): Pair<Int, Int> {
        val t = tokens.getOrNull(k) ?: return 0 to 0
        val ten = tens[t]
        val unitNext = tokens.getOrNull(k + 1)?.let { units[it] }?.takeIf { it in 1..9 }
        return when {
            ten != null && unitNext != null -> (ten + unitNext) to 2
            ten != null -> ten to 1
            units[t] != null && units.getValue(t) > 0 -> units.getValue(t) to 1
            else -> 0 to 0
        }
    }

    private fun numbersToDigits(tokens: List<String>): List<String> {
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val next = tokens.getOrNull(i + 1)
            val ten = tens[t]
            val unitNext = next?.let { units[it] }?.takeIf { it in 1..9 }
            val ordinalNext = next?.let { ordinals[it] }?.takeIf { it in 1..9 }
            val suffixed = ordinalSuffix.matchEntire(t)
            when {
                suffixed != null -> { out.add(suffixed.groupValues[1]); i++ }
                ten != null && unitNext != null -> { out.add((ten + unitNext).toString()); i += 2 }
                ten != null && ordinalNext != null -> { out.add((ten + ordinalNext).toString()); i += 2 }
                ten != null -> { out.add(ten.toString()); i++ }
                (units[t] != null || t == "a" || t == "an") && next == "hundred" -> {
                    // "four hundred (and) thirty-five" -> 435, "a hundred" -> 100
                    var value = (units[t] ?: 1) * 100
                    var j = i + 2
                    val afterAnd = if (tokens.getOrNull(j) == "and") j + 1 else j
                    val (rest, used) = smallNumberAt(tokens, afterAnd)
                    if (used > 0) { value += rest; j = afterAnd + used }
                    out.add(value.toString()); i = j
                }
                t == "hundred" -> { out.add("100"); i++ }
                units[t] != null -> { out.add(units.getValue(t).toString()); i++ }
                ordinals[t] != null -> { out.add(ordinals.getValue(t).toString()); i++ }
                else -> { out.add(t); i++ }
            }
        }
        return out
    }

    /** Every way the official answer may be said: each "(...)" group kept or dropped. */
    fun variants(answer: String): List<List<String>> {
        val withoutNotes = answer.replace(Regex("\\[[^\\]]*\\]"), " ")
        val texts = mutableSetOf<String>()
        expand(withoutNotes, texts, depth = 0)
        return texts.map { normalize(it) }.filter { it.isNotEmpty() }.distinct()
    }

    private fun expand(text: String, out: MutableSet<String>, depth: Int) {
        val open = text.indexOf('(')
        val close = if (open >= 0) text.indexOf(')', open) else -1
        if (open < 0 || close < 0 || depth > 5) {
            out.add(text); return
        }
        val before = text.substring(0, open)
        val inside = text.substring(open + 1, close)
        val after = text.substring(close + 1)
        expand("$before $inside $after", out, depth + 1)
        expand("$before $after", out, depth + 1)
    }

    /**
     * Counts how many different official answers appear in [given]. Each word of the user's
     * answer can only count towards one official answer, so "freedom of speech" said once
     * never counts twice.
     */
    fun check(given: String, accepted: List<String>, needed: Int): Result {
        val user = normalize(expandFreedoms(given))
        if (user.isEmpty() || accepted.isEmpty()) return Result(0, needed, emptyList())
        val used = BooleanArray(user.size)
        val candidates = accepted.map { it to variants(it).sortedByDescending { v -> v.size } }
            .sortedByDescending { (_, v) -> v.firstOrNull()?.size ?: 0 }
        val hits = mutableListOf<String>()
        for ((answer, forms) in candidates) {
            var hit = false
            for (form in forms) {
                val at = findFree(user, form, used)
                if (at >= 0) {
                    for (k in at until at + form.size) used[k] = true
                    hit = true
                    break
                }
            }
            if (!hit) hit = matchAllParts(user, answer, used)
            if (hit) hits.add(answer)
        }
        return Result(hits.size, needed, hits)
    }

    /**
     * "Senate and House" said as "the House and the Senate": an answer made of several parts
     * joined by "and" or commas also counts when every part is said, in any order.
     */
    private fun matchAllParts(user: List<String>, answer: String, used: BooleanArray): Boolean {
        if (!answer.contains(partSplit)) return false
        val texts = mutableSetOf<String>()
        expand(answer.replace(Regex("\\[[^\\]]*\\]"), " "), texts, depth = 0)
        for (text in texts) {
            val parts = text.split(partSplit).map { normalize(it) }.filter { it.isNotEmpty() }
            if (parts.size < 2) continue
            val trial = used.copyOf()
            val ok = parts.sortedByDescending { it.size }.all { part ->
                val at = findFree(user, part, trial)
                if (at >= 0) { for (k in at until at + part.size) trial[k] = true }
                at >= 0
            }
            if (ok) { trial.copyInto(used); return true }
        }
        return false
    }

    private val partSplit = Regex(",|\\band\\b|&")

    /** "freedom of speech, religion and assembly" -> "freedom of speech freedom of religion freedom of assembly". */
    private fun expandFreedoms(raw: String): String {
        val at = raw.lowercase().indexOf("freedom of ")
        if (at < 0) return raw
        val items = raw.substring(at + "freedom of ".length)
            .split(Regex(",|;|&|\\band\\b|\\bor\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }.filter { it.isNotEmpty() }
        val expanded = items.joinToString(" ") { item ->
            val l = item.lowercase()
            if (l.startsWith("freedom") || l.startsWith("right") || l.startsWith("the right")) item else "freedom of $item"
        }
        return raw.substring(0, at) + expanded
    }

    /** Best result across several speech-recognition alternatives. */
    fun checkAny(alternatives: List<String>, accepted: List<String>, needed: Int): Result =
        alternatives.map { check(it, accepted, needed) }.maxByOrNull { it.matched }
            ?: Result(0, needed, emptyList())

    private fun findFree(haystack: List<String>, needle: List<String>, used: BooleanArray): Int {
        if (needle.isEmpty() || needle.size > haystack.size) return -1
        outer@ for (start in 0..haystack.size - needle.size) {
            for (k in needle.indices) {
                if (used[start + k] || haystack[start + k] != needle[k]) continue@outer
            }
            val before = haystack.getOrNull(start - 1)
            if (before != null && before in changesMeaning && before != needle[0]) continue@outer
            return start
        }
        return -1
    }
}
