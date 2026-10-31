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
            .replace(Regex("\\bst\\.\\s"), "saint ")
            .replace(Regex("'s\\b"), "")
            .replace("'", "")
            .replace('-', ' ')
        s = s.replace(nonWord, " ")
        var tokens = s.split(space).filter { it.isNotBlank() }
        tokens = mergeInitials(tokens)
        tokens = numbersToDigits(tokens)
        return tokens.filter { it !in articles }
    }

    /** "u s" -> "us", "d c" -> "dc", "j d vance" -> "jd vance". */
    private fun mergeInitials(tokens: List<String>): List<String> {
        val out = ArrayList<String>(tokens.size)
        var i = 0
        while (i < tokens.size) {
            if (tokens[i].length == 1 && tokens[i][0].isLetter()) {
                var j = i
                val sb = StringBuilder()
                while (j < tokens.size && tokens[j].length == 1 && tokens[j][0].isLetter()) {
                    sb.append(tokens[j]); j++
                }
                if (j - i >= 2) { out.add(sb.toString()); i = j; continue }
            }
            out.add(tokens[i]); i++
        }
        return out
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
                units[t] != null && next == "hundred" -> { out.add((units.getValue(t) * 100).toString()); i += 2 }
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
        val user = normalize(given)
        if (user.isEmpty() || accepted.isEmpty()) return Result(0, needed, emptyList())
        val used = BooleanArray(user.size)
        val candidates = accepted.map { it to variants(it).sortedByDescending { v -> v.size } }
            .sortedByDescending { (_, v) -> v.firstOrNull()?.size ?: 0 }
        val hits = mutableListOf<String>()
        for ((answer, forms) in candidates) {
            for (form in forms) {
                val at = findFree(user, form, used)
                if (at >= 0) {
                    for (k in at until at + form.size) used[k] = true
                    hits.add(answer)
                    break
                }
            }
        }
        return Result(hits.size, needed, hits)
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
            return start
        }
        return -1
    }
}
