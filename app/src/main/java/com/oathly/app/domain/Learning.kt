package com.oathly.app.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/** What the app remembers about one question. Stored on the device only. */
data class QuestionStat(
    val attempts: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
    val ease: Double = 2.3,
    val intervalDays: Int = 0,
    val dueDay: Long = 0,
    val lastCorrect: Boolean = false,
    val lastDay: Long = 0,
) {
    val seen: Boolean get() = attempts > 0
    /** Known well: answered right at least twice in a row. */
    val mastered: Boolean get() = streak >= 2
}

/** Spaced repetition, a light version of SM-2. Days are epoch days. */
object Srs {
    fun record(stat: QuestionStat, correct: Boolean, today: Long): QuestionStat {
        return if (correct) {
            val interval = when (stat.streak) {
                0 -> 1
                1 -> 3
                else -> max(stat.intervalDays + 1, (stat.intervalDays * stat.ease).roundToInt())
            }
            stat.copy(
                attempts = stat.attempts + 1, correct = stat.correct + 1, streak = stat.streak + 1,
                ease = min(2.8, stat.ease + 0.1), intervalDays = interval, dueDay = today + interval,
                lastCorrect = true, lastDay = today,
            )
        } else {
            stat.copy(
                attempts = stat.attempts + 1, streak = 0, ease = max(1.3, stat.ease - 0.2),
                intervalDays = 0, dueDay = today, lastCorrect = false, lastDay = today,
            )
        }
    }

    fun isDue(stat: QuestionStat?, today: Long): Boolean = stat != null && stat.seen && stat.dueDay <= today

    /** Estimated chance of answering correctly right now. */
    fun knowChance(stat: QuestionStat?, unseenGuess: Double): Double {
        if (stat == null || !stat.seen) return unseenGuess
        val base = (stat.correct + 1.0) / (stat.attempts + 2.0)
        val recent = if (stat.lastCorrect) 0.08 else -0.15
        val streakBonus = min(stat.streak, 4) * 0.05
        return (base + recent + streakBonus).coerceIn(0.05, 0.98)
    }
}

/** Estimated chance of passing, by simulating the real test many times. */
object Readiness {

    fun usPassChance(chances: List<Double>, asked: Int, passAt: Int, failAt: Int, seed: Long, runs: Int = 3000): Double {
        if (chances.isEmpty()) return 0.0
        val rnd = Random(seed)
        val idx = chances.indices.toMutableList()
        var passes = 0
        repeat(runs) {
            idx.shuffle(rnd)
            var right = 0
            var wrong = 0
            for (k in 0 until min(asked, idx.size)) {
                if (rnd.nextDouble() < chances[idx[k]]) right++ else wrong++
                if (right >= passAt || wrong >= failAt) break
            }
            if (right >= passAt) passes++
        }
        return passes.toDouble() / runs
    }

    /** Australian rule: overall pass mark, and every values question right. */
    fun auPassChance(
        valuesChances: List<Double>, otherChances: List<Double>,
        valuesAsked: Int, otherAsked: Int, passMark: Int, seed: Long, runs: Int = 3000,
    ): Double {
        if (valuesChances.size < valuesAsked || otherChances.size < otherAsked) return 0.0
        val rnd = Random(seed)
        val v = valuesChances.toMutableList()
        val o = otherChances.toMutableList()
        var passes = 0
        repeat(runs) {
            v.shuffle(rnd); o.shuffle(rnd)
            var valuesRight = 0
            var total = 0
            for (k in 0 until valuesAsked) if (rnd.nextDouble() < v[k]) { valuesRight++; total++ }
            for (k in 0 until otherAsked) if (rnd.nextDouble() < o[k]) total++
            if (valuesRight == valuesAsked && total >= passMark) passes++
        }
        return passes.toDouble() / runs
    }
}
