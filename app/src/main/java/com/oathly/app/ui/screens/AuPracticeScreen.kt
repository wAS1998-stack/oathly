package com.oathly.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.ui.AppBar
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.theme.dangerColor
import com.oathly.app.ui.theme.successColor

@Composable
fun AuPracticeScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val t = vm.auTest
    Scaffold(
        topBar = { AppBar("Practice test", onBack = { vm.quitTest() }, close = true) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (t == null) return@Column
            val q = t.current ?: return@Column
            LinearProgressIndicator(progress = { t.index.toFloat() / t.questions.size }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Question ${t.index + 1} of ${t.questions.size}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (q.values) {
                    Text("Values question", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
            LaunchedEffect(q.id) { if (settings.autoRead) vm.speak(q.text) }
            QuestionCard(q.text, label = "Part ${q.part}: ${q.partTitle}", onSpeak = { vm.speak(q.text) })

            val chosen = t.chosen[t.index]
            q.options.forEachIndexed { i, option ->
                val isAnswer = i == q.answerIndex
                val border = when {
                    chosen == null -> MaterialTheme.colorScheme.outline
                    isAnswer -> successColor()
                    i == chosen -> dangerColor()
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                }
                val prefix = when {
                    chosen != null && isAnswer -> "✓ "
                    chosen != null && i == chosen -> "✗ "
                    else -> ""
                }
                OutlinedCard(
                    onClick = { vm.chooseAu(i) },
                    enabled = chosen == null,
                    border = BorderStroke(if (chosen != null && (isAnswer || i == chosen)) 2.dp else 1.dp, border),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { role = Role.Button }
                        .testTag(if (isAnswer) "option_correct" else "option_wrong_$i"),
                ) {
                    Text("$prefix$option", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                }
            }

            if (chosen != null) {
                val right = chosen == q.answerIndex
                Text(
                    if (right) "Correct" else "Not quite",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (right) successColor() else dangerColor(),
                )
                if (q.explanation.isNotBlank()) Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                if (q.values && !right) {
                    Text(
                        "In the real test you must answer every values question correctly.",
                        style = MaterialTheme.typography.bodyMedium, color = dangerColor(),
                    )
                }
                Button(onClick = { vm.nextAu() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(if (t.index + 1 >= t.questions.size) "See your result" else "Next question")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
