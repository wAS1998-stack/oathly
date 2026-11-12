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
        graph.ads.gatherConsent(this)
        setContent { AppRoot(viewModel) }
    }

    override fun onStart() {
        super.onStart()
        // Also picks up purchases finished outside the app (promo codes, delayed payments).
        (application as OathlyApp).graph.billing.connect()
        viewModel.refreshReadiness()
    }

    override fun onStop() {
        super.onStop()
        (application as OathlyApp).graph.speaker.stop()
    }
}
