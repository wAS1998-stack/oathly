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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.data.Country
import com.oathly.app.ui.AdBanner
import com.oathly.app.ui.AppBar
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.InfoCard
import com.oathly.app.ui.MenuRow
import com.oathly.app.ui.SpeakButton
import com.oathly.app.ui.theme.QuestionStyle

@Composable
fun StudyMenuScreen(vm: AppViewModel) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    var shuffle by rememberSaveable { mutableStateOf(true) }
    val options = remember(stats) { vm.deckOptions() }
    Scaffold(
        topBar = { AppBar("Study cards", onBack = { vm.back() }) },
        bottomBar = { AdBanner(vm, Modifier.navigationBarsPadding()) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Shuffle cards", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = shuffle, onCheckedChange = { shuffle = it })
                }
            }
            items(options, key = { it.title }) { o ->
                MenuRow(o.title, subtitle = o.subtitle, onClick = { vm.startDeck(o, shuffle) })
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
fun StudyScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val deck = vm.deck
    Scaffold(
        topBar = { AppBar(deck?.title ?: "Study", onBack = { vm.back() }, close = true) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (deck == null) return@Column
            if (deck.done) {
                DeckFinished(deck.known, deck.missed, onDone = { vm.back() })
                return@Column
            }
            LinearProgressIndicator(
                progress = { deck.index.toFloat() / deck.ids.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Card ${deck.index + 1} of ${deck.ids.size}", style = MaterialTheme.typography.labelLarge)
            val id = deck.currentId ?: return@Column

            when (settings.country) {
                Country.AU -> {
                    val q = vm.auQuestion(id) ?: return@Column
                    LaunchedEffect(id) { if (settings.autoRead) vm.speak(q.text) }
                    QuestionCard(q.text, label = "Part ${q.part}", onSpeak = { vm.speak(q.text) })
                    if (deck.revealed) {
                        AnswerCard(listOf(q.options[q.answerIndex]), note = q.explanation.ifBlank { null })
                    }
                }
                else -> {
                    val q = vm.usQuestion(id) ?: return@Column
                    LaunchedEffect(id) { if (settings.autoRead) vm.speak(q.text) }
                    val label = buildString {
                        append("Question ${q.number}")
                        if (q.senior) append(" ★ 65/20")
                    }
                    QuestionCard(q.text, label = label, onSpeak = { vm.speak(q.text) })
                    if (deck.revealed) {
                        val answers = vm.answersToShow(q)
                        val note = listOfNotNull(
                            if (q.need > 1) "Give ${q.need} of these answers." else if (answers.size > 1 && q.dynamicKey == null) "Any one of these answers is correct." else null,
                            vm.dynamicAnswer(q)?.note,
                            q.explanation,
                        ).joinToString("\n\n").ifBlank { null }
                        AnswerCard(answers, note, onSpeak = { vm.speak(answers.first()) })
                    }
                }
            }

            if (!deck.revealed) {
                Button(onClick = { vm.revealCard() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Show answer") }
            } else {
                Text("Did you know it?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { vm.gradeCard(false) }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Not yet") }
                    FilledTonalButton(onClick = { vm.gradeCard(true) }, modifier = Modifier.weight(1f).height(52.dp)) { Text("I knew it") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuestionCard(text: String, label: String, onSpeak: () -> Unit) {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            SpeakButton(onSpeak)
        }
        Text(text, style = QuestionStyle, modifier = Modifier.semantics { heading() })
    }
}

@Composable
fun AnswerCard(answers: List<String>, note: String?, onSpeak: (() -> Unit)? = null) {
    InfoCard(container = MaterialTheme.colorScheme.secondaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Answer", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (onSpeak != null) SpeakButton(onSpeak)
        }
        answers.forEach { a ->
            Text("• $a", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 2.dp))
        }
        if (note != null) {
            Spacer(Modifier.height(8.dp))
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun DeckFinished(known: Int, missed: Int, onDone: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text("Deck finished", style = MaterialTheme.typography.headlineMedium)
    Text("You knew $known and are still learning $missed.", style = MaterialTheme.typography.bodyLarge)
    if (missed > 0) {
        Text("The ones you missed will come back in \"Due for review\".", style = MaterialTheme.typography.bodyMedium)
    }
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Done") }
}
