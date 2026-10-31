package com.oathly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.oathly.app.ui.AppRoot
import com.oathly.app.ui.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as OathlyApp).graph
        graph.billing.connect()
        graph.ads.gatherConsent(this)
        setContent { AppRoot(viewModel) }
    }

    override fun onStop() {
        super.onStop()
        (application as OathlyApp).graph.speaker.stop()
    }
}
