package com.oathly.app.domain

import com.oathly.app.data.AuBank
import com.oathly.app.data.AuQuestion
import com.oathly.app.data.UsBank
import com.oathly.app.data.UsQuestion
import kotlin.math.ceil
import kotlin.random.Random

/** One answered question in a practice test. */
data class Answered<Q>(val question: Q, val correct: Boolean, val given: String?)

/**
 * A mock USCIS civics interview with the real stopping rules: the officer stops as soon
 * as you have enough right answers to pass, or enough wrong ones to fail.
 */
data class Interview(
    val questions: List<UsQuestion>,
    val asked: Int,
    val passAt: Int,
    val failAt: Int,
    val answers: List<Answered<UsQuestion>> = emptyList(),
) {
    val right: Int get() = answers.count { it.correct }
    val wrong: Int get() = answers.size - right
    val finished: Boolean
        get() = right >= passAt || wrong >= failAt || answers.size >= minOf(asked, questions.size)
    val passed: Boolean get() = right >= passAt
    val current: UsQuestion? get() = if (finished) null else questions[answers.size]
    val number: Int get() = answers.size + 1

    fun answer(correct: Boolean, given: String?): Interview {
        val q = current ?: return this
        return copy(answers = answers + Answered(q, correct, given))
    }

    /** "I was right": the user overrules the strict checker on the last answer. */
    fun overrideLast(): Interview {
        val last = answers.lastOrNull() ?: return this
        if (last.correct) return this
        return copy(answers = answers.dropLast(1) + last.copy(correct = true))
    }

    companion object {
        fun start(bank: UsBank, seniorOnly: Boolean, rnd: Random, prefer: (UsQuestion) -> Double = { 1.0 }): Interview {
            val pool = if (seniorOnly) bank.questions.filter { it.senior } else bank.questions
            val r = bank.rules
            // Weighted shuffle: questions the user knows less come up a bit more often,
            // but every question can appear, like the real test.
            val order = pool.map { it to rnd.nextDouble().let { u -> Math.pow(u, 1.0 / prefer(it).coerceAtLeast(0.05)) } }
                .sortedByDescending { it.second }
                .map { it.first }
            return if (seniorOnly) Interview(order, r.seniorAsked, r.seniorPassAt, r.seniorFailAt)
            else Interview(order, r.asked, r.passAt, r.failAt)
        }
    }
}

/**
 * An Australian practice test: [valuesAsked] values questions plus questions from the
 * other parts, mixed together. To pass you need the overall pass mark AND every values
 * question right, as in the real test.
 */
data class AuTest(
    val questions: List<AuQuestion>,
    val passMark: Int,
    val chosen: List<Int?> = List(questions.size) { null },
    val index: Int = 0,
) {
    val current: AuQuestion? get() = questions.getOrNull(index)
    val finished: Boolean get() = index >= questions.size
    val score: Int get() = questions.indices.count { chosen[it] == questions[it].answerIndex }
    val valuesTotal: Int get() = questions.count { it.values }
    val valuesRight: Int get() = questions.indices.count { questions[it].values && chosen[it] == questions[it].answerIndex }
    val passed: Boolean get() = finished && score >= passMark && valuesRight == valuesTotal
    val currentAnswered: Boolean get() = chosen.getOrNull(index) != null

    fun choose(option: Int): AuTest =
        if (finished || currentAnswered) this else copy(chosen = chosen.toMutableList().also { it[index] = option })

    fun next(): AuTest = if (finished) this else copy(index = index + 1)

    companion object {
        fun start(bank: AuBank, rnd: Random): AuTest {
            val values = bank.questions.filter { it.values }.shuffled(rnd).take(bank.valuesPerTest)
            val others = bank.questions.filter { !it.values }.groupBy { it.part }
            val otherCount = bank.questionsPerTest - values.size
            val perPart = others.mapValues { (_, qs) -> qs.shuffled(rnd).toMutableList() }
            val picked = mutableListOf<AuQuestion>()
            // Round-robin across parts so each part is covered evenly.
            while (picked.size < otherCount && perPart.values.any { it.isNotEmpty() }) {
                for (list in perPart.values) {
                    if (picked.size < otherCount && list.isNotEmpty()) picked.add(list.removeAt(0))
                }
            }
            val all = (values + picked).shuffled(rnd).map { it.shuffledOptions(rnd) }
            val passMark = ceil(all.size * bank.passPercent / 100.0).toInt()
            return AuTest(all, passMark)
        }
    }
}

/** Same question with its three options in a new order (the right answer moves too). */
fun AuQuestion.shuffledOptions(rnd: Random): AuQuestion {
    val order = options.indices.shuffled(rnd)
    return copy(options = order.map { options[it] }, answerIndex = order.indexOf(answerIndex))
}
