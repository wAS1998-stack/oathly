package com.oathly.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboarded: Boolean = false,
    val country: Country? = null,
    val usVersion: String = "2025",
    val senior: Boolean = false,
    val stateCode: String? = null,
    val repName: String? = null,
    val reminderOn: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val autoRead: Boolean = true,
    val speechRate: Float = 0.9f,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dailyGoal: Int = 10,
)

/** User choices, kept on the device in SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        write(next)
        _state.value = next
    }

    private fun read() = AppSettings(
        onboarded = prefs.getBoolean("onboarded", false),
        country = Country.from(prefs.getString("country", null)),
        usVersion = prefs.getString("usVersion", "2025") ?: "2025",
        senior = prefs.getBoolean("senior", false),
        stateCode = prefs.getString("stateCode", null),
        repName = prefs.getString("repName", null),
        reminderOn = prefs.getBoolean("reminderOn", false),
        reminderHour = prefs.getInt("reminderHour", 19),
        reminderMinute = prefs.getInt("reminderMinute", 0),
        autoRead = prefs.getBoolean("autoRead", true),
        speechRate = prefs.getFloat("speechRate", 0.9f),
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM),
        dailyGoal = prefs.getInt("dailyGoal", 10),
    )

    private fun write(s: AppSettings) = prefs.edit {
        putBoolean("onboarded", s.onboarded)
        putString("country", s.country?.code)
        putString("usVersion", s.usVersion)
        putBoolean("senior", s.senior)
        putString("stateCode", s.stateCode)
        putString("repName", s.repName)
        putBoolean("reminderOn", s.reminderOn)
        putInt("reminderHour", s.reminderHour)
        putInt("reminderMinute", s.reminderMinute)
        putBoolean("autoRead", s.autoRead)
        putFloat("speechRate", s.speechRate)
        putString("theme", s.theme.name)
        putInt("dailyGoal", s.dailyGoal)
    }

    // Small counters that the UI doesn't observe.
    var officialsEtag: String?
        get() = prefs.getString("officialsEtag", null)
        set(v) = prefs.edit { putString("officialsEtag", v) }

    var proCached: Boolean
        get() = prefs.getBoolean("proCached", false)
        set(v) = prefs.edit { putBoolean("proCached", v) }

    var lastInterstitialAt: Long
        get() = prefs.getLong("lastInterstitialAt", 0L)
        set(v) = prefs.edit { putLong("lastInterstitialAt", v) }

    var testsSinceInterstitial: Int
        get() = prefs.getInt("testsSinceInterstitial", 0)
        set(v) = prefs.edit { putInt("testsSinceInterstitial", v) }

    var askedForReview: Boolean
        get() = prefs.getBoolean("askedForReview", false)
        set(v) = prefs.edit { putBoolean("askedForReview", v) }
}
