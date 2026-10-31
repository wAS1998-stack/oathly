package com.oathly.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.ui.screens.AboutScreen
import com.oathly.app.ui.screens.AuPracticeScreen
import com.oathly.app.ui.screens.HomeScreen
import com.oathly.app.ui.screens.InterviewScreen
import com.oathly.app.ui.screens.OfficialsScreen
import com.oathly.app.ui.screens.OnboardingScreen
import com.oathly.app.ui.screens.PickRepScreen
import com.oathly.app.ui.screens.PickStateScreen
import com.oathly.app.ui.screens.ProScreen
import com.oathly.app.ui.screens.ProgressScreen
import com.oathly.app.ui.screens.ResultsScreen
import com.oathly.app.ui.screens.SettingsScreen
import com.oathly.app.ui.screens.StudyMenuScreen
import com.oathly.app.ui.screens.StudyScreen
import com.oathly.app.ui.theme.OathlyTheme

@Composable
fun AppRoot(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    OathlyTheme(settings.theme) {
        BackHandler(enabled = vm.backStack.size > 1) { vm.back() }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            AnimatedContent(
                targetState = vm.current,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { screen ->
                when (screen) {
                    Screen.Onboarding -> OnboardingScreen(vm)
                    Screen.Home -> HomeScreen(vm)
                    Screen.StudyMenu -> StudyMenuScreen(vm)
                    Screen.Study -> StudyScreen(vm)
                    Screen.Interview -> InterviewScreen(vm)
                    Screen.AuPractice -> AuPracticeScreen(vm)
                    Screen.Results -> ResultsScreen(vm)
                    Screen.Officials -> OfficialsScreen(vm)
                    Screen.Progress -> ProgressScreen(vm)
                    Screen.Settings -> SettingsScreen(vm)
                    Screen.PickState -> PickStateScreen(vm)
                    Screen.PickRep -> PickRepScreen(vm)
                    Screen.About -> AboutScreen(vm)
                    Screen.Pro -> ProScreen(vm)
                }
            }
        }
    }
}
