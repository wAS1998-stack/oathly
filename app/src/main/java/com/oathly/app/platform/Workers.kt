package com.oathly.app.platform

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oathly.app.OathlyApp
import com.oathly.app.data.Country
import com.oathly.app.data.OfficialsRepository
import com.oathly.app.domain.DynamicAnswers
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Once a day (on any network, battery-friendly) download the latest officeholders.
 * If an answer the user studies changed, tell them.
 */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as OathlyApp).graph
        return when (val r = graph.officials.refresh()) {
            is OfficialsRepository.Refresh.Failed -> if (runAttemptCount < 3) Result.retry() else Result.success()
            OfficialsRepository.Refresh.NotModified -> Result.success()
            is OfficialsRepository.Refresh.Updated -> {
                val s = graph.settings.state.value
                if (s.country == Country.US && r.before != null) {
                    val keys = listOf(
                        "president" to "President", "vice_president" to "Vice President",
                        "speaker" to "Speaker of the House", "chief_justice" to "Chief Justice",
                        "senators" to "your U.S. senators", "governor" to "your governor",
                        "representative" to "your U.S. representative",
                    )
                    val changed = keys.filter { (key, _) ->
                        DynamicAnswers.resolve(key, r.before, s.stateCode, s.repName).display !=
                            DynamicAnswers.resolve(key, r.after, s.stateCode, s.repName).display
                    }.map { it.second }
                    if (changed.isNotEmpty()) {
                        Notifier.show(
                            applicationContext, Notifier.CHANNEL_UPDATES, Notifier.ID_UPDATE,
                            "An answer on your test changed",
                            "New answer for ${changed.joinToString()}. Open Oathly to see it.",
                        )
                    }
                }
                Result.success()
            }
        }
    }

    companion object {
        private const val NAME = "officials-update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

/** Daily study reminder at the user's chosen time, skipped if they already studied today. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as OathlyApp).graph
        val s = graph.settings.state.value
        if (!s.reminderOn) return Result.success()
        if (graph.progress.answeredToday() == 0) {
            val streak = graph.progress.streak()
            val text = if (streak > 0) "Keep your $streak-day streak going: 5 minutes is enough."
            else "A few questions a day is the easiest way to pass."
            Notifier.show(applicationContext, Notifier.CHANNEL_REMINDERS, Notifier.ID_REMINDER, "Time to practise", text)
        }
        schedule(applicationContext, s.reminderHour, s.reminderMinute)
        return Result.success()
    }

    companion object {
        private const val NAME = "study-reminder"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val now = LocalDateTime.now()
            var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMillis()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
