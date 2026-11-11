package com.oathly.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.ui.AppBar
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.InfoCard
import com.oathly.app.ui.Screen
import com.oathly.app.ui.openStorePage
import com.oathly.app.ui.rememberActivity
import com.oathly.app.ui.shareApp
import com.oathly.app.ui.theme.dangerColor
import com.oathly.app.ui.theme.successColor

@Composable
fun ResultsScreen(vm: AppViewModel) {
    val r = vm.result
    val activity = rememberActivity()
    val context = LocalContext.current
    val isPro by vm.adFree.collectAsStateWithLifecycle()
    val tests by vm.tests.collectAsStateWithLifecycle()
    // Ask for a rating only once the user has passed more than one practice test.
    val askForRating = tests.count { it.passed } >= 2
    val leave: () -> Unit = {
        if (activity != null) vm.leaveResults(activity) else vm.back()
    }
    BackHandler { leave() }
    Scaffold(
        topBar = { AppBar("Result") },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { leave() }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Done") }
                Button(onClick = { vm.retryFromResults(activity) }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Try again") }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (r == null) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                InfoCard(container = if (r.passed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                    Text(r.title, style = MaterialTheme.typography.headlineMedium, color = if (r.passed) successColor() else dangerColor())
                    Text("${r.right} of ${r.total} correct", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(r.detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (r.passed && askForRating) {
                item {
                    InfoCard {
                        Text("Is Oathly helping you?", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A rating on Google Play helps other people preparing for their test find the app.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { context.openStorePage() }) { Text("Rate Oathly") }
                            TextButton(onClick = { context.shareApp() }) { Text("Share with a friend") }
                        }
                    }
                }
            }
            if (!isPro) {
                item {
                    TextButton(onClick = { vm.navigate(Screen.Pro) }) { Text("Study without ads") }
                }
            }
            item { Text("Your answers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            itemsIndexed(r.items) { i, item ->
                InfoCard {
                    Text(
                        "${if (item.correct) "✓" else "✗"}  ${i + 1}. ${item.question}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (item.correct) successColor() else dangerColor(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (item.given != null) Text("You: ${item.given}", style = MaterialTheme.typography.bodyMedium)
                    Text("Answer: ${item.answer}", style = MaterialTheme.typography.bodyMedium)
                    if (!item.correct && !item.explanation.isNullOrBlank()) {
                        Text(item.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
