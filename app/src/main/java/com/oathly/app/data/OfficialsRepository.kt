package com.oathly.app.data

import com.oathly.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Current officeholders, state capitals and members of Congress.
 * A copy ships inside the app; a fresher copy is downloaded daily from the app's own
 * data feed (built by GitHub Actions from public-domain sources) and used when newer.
 */
class OfficialsRepository(
    private val content: ContentRepository,
    private val settings: SettingsStore,
    private val file: File,
) {
    private val _state = MutableStateFlow<Officials?>(null)
    val state: StateFlow<Officials?> = _state.asStateFlow()

    sealed interface Refresh {
        data object NotModified : Refresh
        data class Updated(val before: Officials?, val after: Officials) : Refresh
        data class Failed(val reason: String) : Refresh
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        val bundled = runCatching { ContentRepository.parseOfficials(content.read("officials.json")) }.getOrNull()
        val saved = runCatching { ContentRepository.parseOfficials(file.readText()) }.getOrNull()
        _state.value = listOfNotNull(bundled, saved).filter(::isValid).maxByOrNull { it.generatedAt }
    }

    suspend fun refresh(): Refresh = withContext(Dispatchers.IO) {
        if (_state.value == null) load()
        val conn = (URL(BuildConfig.DATA_BASE_URL + "officials.json").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            settings.officialsEtag?.let { setRequestProperty("If-None-Match", it) }
        }
        try {
            when (conn.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> Refresh.NotModified
                HttpURLConnection.HTTP_OK -> {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val fresh = ContentRepository.parseOfficials(body)
                    if (!isValid(fresh)) return@withContext Refresh.Failed("invalid data")
                    settings.officialsEtag = conn.getHeaderField("ETag")
                    val before = _state.value
                    if (before != null && fresh.generatedAt <= before.generatedAt) return@withContext Refresh.NotModified
                    val tmp = File(file.parentFile, file.name + ".tmp")
                    tmp.writeText(body)
                    if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
                    _state.value = fresh
                    Refresh.Updated(before, fresh)
                }
                else -> Refresh.Failed("HTTP ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Refresh.Failed(e.javaClass.simpleName)
        } finally {
            conn.disconnect()
        }
    }

    /** Never replace good data with a broken download. */
    private fun isValid(o: Officials): Boolean =
        o.president != null && o.places.size >= 50 &&
            o.places.count { it.isState && it.senators.size == 2 } >= 48
}
