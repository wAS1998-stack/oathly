package com.oathly.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.R
import com.oathly.app.ui.AppBar
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.Feedback
import com.oathly.app.ui.InfoCard
import com.oathly.app.ui.theme.dangerColor
import com.oathly.app.ui.theme.successColor

@Composable
fun InterviewScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val iv = vm.interview
    val feedback = vm.feedback
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val canSpeak = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val heard = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (res.resultCode == Activity.RESULT_OK && heard.isNotEmpty()) vm.submitAnswer(heard)
    }

    Scaffold(
        topBar = { AppBar("Mock interview", onBack = { vm.quitTest() }, close = true) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (iv == null) return@Column
            // While feedback is showing, keep showing the question that was just answered.
            val questionId = feedback?.questionId ?: iv.current?.id
            val q = questionId?.let { vm.usQuestion(it) } ?: return@Column

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Question ${if (feedback?.correct != null) iv.answers.size else iv.number}",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                )
                Text("✓ ${iv.right}", color = successColor(), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Text("✗ ${iv.wrong}", color = dangerColor(), fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Pass with ${iv.passAt} right. The officer stops after ${iv.failAt} wrong.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LaunchedEffect(q.id) { if (settings.autoRead && feedback == null) vm.speak(q.text) }
            QuestionCard(q.text, label = "The officer asks", onSpeak = { vm.speak(q.text) })

            if (feedback == null) {
                var typed by rememberSaveable(q.id) { mutableStateOf("") }
                val submit = {
                    focus.clearFocus()
                    if (typed.isBlank()) vm.revealAnswer() else vm.submitAnswer(listOf(typed))
                }
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    label = { Text("Type your answer") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = false,
                    maxLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canSpeak) {
                        FilledTonalButton(
                            onClick = {
                                vm.stopSpeaking()
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your answer")
                                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                                }
                                runCatching { recognizer.launch(intent) }
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_mic), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Say it")
                        }
                    }
                    Button(onClick = { submit() }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Text(if (typed.isBlank()) "Show answer" else "Check")
                    }
                }
                TextButton(onClick = { vm.revealAnswer() }) { Text("I don't know, show me") }
            } else {
                FeedbackPanel(feedback, finished = iv.finished, vm = vm)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeedbackPanel(f: Feedback, finished: Boolean, vm: AppViewModel) {
    when (f.correct) {
        true -> Text("Correct", style = MaterialTheme.typography.headlineSmall, color = successColor())
        false -> Text("Not quite", style = MaterialTheme.typography.headlineSmall, color = dangerColor())
        null -> Text("Did you know it?", style = MaterialTheme.typography.headlineSmall)
    }
    if (f.given != null) {
        Text("You said: “${f.given}”", style = MaterialTheme.typography.bodyLarge)
    }
    val q = vm.usQuestion(f.questionId)
    AnswerCard(
        answers = f.answers,
        note = listOfNotNull(
            f.note,
            if (q != null && q.need > 1) "Give ${q.need} of these answers." else null,
            q?.explanation,
        ).joinToString("\n\n").ifBlank { null },
        onSpeak = { vm.speak(f.answers.joinToString(". ")) },
    )
    if (f.correct == null) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { vm.selfGrade(false) }, modifier = Modifier.weight(1f).height(52.dp)) { Text("No") }
            FilledTonalButton(onClick = { vm.selfGrade(true) }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Yes, I knew it") }
        }
    } else {
        if (f.canOverride) {
            TextButton(onClick = { vm.overrideCorrect() }) { Text("My answer means the same, count it as right") }
        }
        Button(onClick = { vm.nextInterviewQuestion() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (finished) "See your result" else "Next question")
        }
    }
    InfoCard(container = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            "Tip: at the real interview, the officer asks out loud and you answer out loud. Practise saying the answer.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
