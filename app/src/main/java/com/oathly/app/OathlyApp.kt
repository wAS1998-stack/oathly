package com.oathly.app

import android.app.Application
import com.oathly.app.ads.AdsManager
import com.oathly.app.billing.BillingManager
import com.oathly.app.data.ContentRepository
import com.oathly.app.data.OfficialsRepository
import com.oathly.app.data.ProgressStore
import com.oathly.app.data.SettingsStore
import com.oathly.app.platform.Notifier
import com.oathly.app.platform.ReminderWorker
import com.oathly.app.platform.Speaker
import com.oathly.app.platform.UpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/** Everything the app needs, created once. */
class AppGraph(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val settings = SettingsStore(app)
    val content = ContentRepository(app.assets)
    val progress = ProgressStore(File(app.filesDir, "progress.json"), scope)
    val officials = OfficialsRepository(content, settings, File(app.filesDir, "officials.json"))
    val ads = AdsManager(app, settings, scope)
    val billing = BillingManager(app, settings)
    val speaker = Speaker(app)
}

class OathlyApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.progress.load()
        graph.billing.onProChanged = { pro -> graph.ads.setPro(pro) }
        graph.scope.launch { graph.officials.load() }
        Notifier.createChannels(this)
        UpdateWorker.schedule(this)
        val s = graph.settings.state.value
        if (s.reminderOn) ReminderWorker.schedule(this, s.reminderHour, s.reminderMinute)
    }
}
