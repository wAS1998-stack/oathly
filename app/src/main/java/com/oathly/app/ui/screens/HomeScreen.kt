package com.oathly.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.R
import com.oathly.app.data.Country
import com.oathly.app.domain.Srs
import com.oathly.app.ui.AdBanner
import com.oathly.app.ui.AppBar
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.InfoCard
import com.oathly.app.ui.MenuRow
import com.oathly.app.ui.Screen
import com.oathly.app.ui.percent

@Composable
fun HomeScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    val country = settings.country ?: Country.US
    val today = vm.graph.progress.today()

    val ids: List<String> = when (country) {
        Country.US -> vm.usPool().map { it.id }
        Country.AU -> vm.auBank?.questions?.map { it.id }.orEmpty()
    }
    val mastered = ids.count { stats[it]?.mastered == true }
    val due = ids.count { Srs.isDue(stats[it], today) }
    val todayCount = days[today] ?: 0
    val streak = vm.graph.progress.streak()

    val subtitle = when (country) {
        Country.US -> buildString {
            append("${vm.usBank?.version ?: settings.usVersion} civics test")
            if (settings.senior) append(" · 65/20")
        }
        Country.AU -> "Australian citizenship test"
    }

    Scaffold(
        topBar = {
            AppBar(title = "Oathly", actions = {
                IconButton(onClick = { vm.navigate(Screen.Settings) }) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                }
            })
        },
        bottomBar = { AdBanner(vm, Modifier.navigationBarsPadding()) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            InfoCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text("Chance of passing today", style = MaterialTheme.typography.labelLarge)
                val r = vm.readiness
                Text(
                    if (r == null) "…" else percent(r),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    if (stats.isEmpty()) "Answer a few questions and this estimate becomes personal."
                    else "Estimated from your answers so far.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (ids.isEmpty()) 0f else mastered.toFloat() / ids.size },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text("$mastered of ${ids.size} questions learned", style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { if (country == Country.US) vm.startInterview() else vm.startAuTest() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(if (country == Country.US) "Start mock interview" else "Take a practice test")
            }

            MenuRow(
                "Study cards",
                subtitle = if (due > 0) "$due due for review" else "Learn the questions one by one",
                onClick = { vm.navigate(Screen.StudyMenu) },
            )
            if (country == Country.US) {
                MenuRow("Answers that change", subtitle = "Your senators, representative, governor and more",
                    onClick = { vm.navigate(Screen.Officials) })
            }
            MenuRow("Your progress", subtitle = "Scores, history and what to review", onClick = { vm.navigate(Screen.Progress) })

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCard(Modifier.weight(1f)) {
                    Text("$streak", style = MaterialTheme.typography.headlineMedium)
                    Text("day streak", style = MaterialTheme.typography.bodySmall)
                }
                InfoCard(Modifier.weight(1f)) {
                    Text("$todayCount / ${settings.dailyGoal}", style = MaterialTheme.typography.headlineMedium)
                    Text("answered today", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "Independent study app. Not affiliated with any government agency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
