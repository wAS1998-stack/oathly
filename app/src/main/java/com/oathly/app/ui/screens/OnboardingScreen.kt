package com.oathly.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.R
import com.oathly.app.data.Country
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.InfoCard
import com.oathly.app.ui.MenuRow
import com.oathly.app.ui.openUrl

private const val STEP_WELCOME = 0
private const val STEP_COUNTRY = 1
private const val STEP_VERSION = 2
private const val STEP_SENIOR = 3
private const val STEP_STATE = 4
private const val STEP_REP = 5
private const val STEP_REMINDER = 6

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val officials by vm.officials.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(STEP_WELCOME) }
    var country by rememberSaveable { mutableStateOf<String?>(null) }
    var version by rememberSaveable { mutableStateOf("2025") }
    var senior by rememberSaveable { mutableStateOf(false) }
    var stateCode by rememberSaveable { mutableStateOf<String?>(null) }
    var repName by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun finish(reminder: Pair<Int, Int>?) {
        vm.finishOnboarding(Country.from(country) ?: Country.US, version, senior, stateCode, repName, reminder)
    }

    var pendingReminder by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        finish(if (granted) pendingReminder else null)
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (step > STEP_WELCOME) {
            TextButton(onClick = {
                step = when (step) {
                    STEP_REMINDER -> if (country == "AU") STEP_COUNTRY else if (hasManyReps(vm, stateCode)) STEP_REP else STEP_STATE
                    STEP_REP -> STEP_STATE
                    else -> step - 1
                }
            }) { Text("Back") }
        }
        when (step) {
            STEP_WELCOME -> Welcome(onStart = { step = STEP_COUNTRY }, onPrivacy = { context.openUrl(PRIVACY_URL) })

            STEP_COUNTRY -> Choice(
                title = "Which test are you preparing for?",
                options = listOf(
                    Triple("US", "U.S. citizenship test", "The civics test at your naturalization interview"),
                    Triple("AU", "Australian citizenship test", "The 20-question test, including Australian values"),
                ),
            ) { picked ->
                country = picked
                step = if (picked == "AU") STEP_REMINDER else STEP_VERSION
            }

            STEP_VERSION -> Choice(
                title = "When did you file Form N-400?",
                subtitle = "This decides which version of the civics test you take.",
                options = listOf(
                    Triple("2025", "On or after October 20, 2025", "2025 test: 128 questions, up to 20 asked, 12 to pass"),
                    Triple("2025n", "I haven't filed yet", "You'll take the 2025 test"),
                    Triple("2008", "Before October 20, 2025", "2008 test: 100 questions, up to 10 asked, 6 to pass"),
                ),
            ) { picked ->
                version = if (picked == "2008") "2008" else "2025"
                step = STEP_SENIOR
            }

            STEP_SENIOR -> Choice(
                title = "Are you 65 or older, with 20+ years as a permanent resident?",
                subtitle = "If so, USCIS lets you study only the 20 questions marked with a star (the \"65/20\" rule).",
                options = listOf(
                    Triple("no", "No", "Study all questions"),
                    Triple("yes", "Yes, 65/20 applies to me", "Study the 20 starred questions"),
                ),
            ) { picked ->
                senior = picked == "yes"
                step = STEP_STATE
            }

            STEP_STATE -> {
                Text("Where do you live?", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Some answers depend on your state: your senators, representative, governor and capital.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val places = officials?.places.orEmpty()
                var query by rememberSaveable { mutableStateOf("") }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(places.filter { it.name.contains(query.trim(), ignoreCase = true) }, key = { it.code }) { p ->
                        MenuRow(p.name, onClick = {
                            focusManager.clearFocus()
                            keyboard?.hide()
                            stateCode = p.code
                            repName = null
                            step = if (p.isState && p.representatives.size > 1) STEP_REP else STEP_REMINDER
                        })
                    }
                }
            }

            STEP_REP -> {
                val place = officials?.place(stateCode)
                Text("Who is your U.S. representative?", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${place?.name ?: "Your state"} has ${place?.representatives?.size ?: 0} districts. Not sure? Look it up with your ZIP code, or skip and choose later in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { context.openUrl("https://www.house.gov/representatives/find-your-representative") }) {
                    Text("Find my representative on house.gov")
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(place?.representatives.orEmpty(), key = { it.name + it.district }) { r ->
                        MenuRow(r.name, subtitle = if (r.district == 0) "At-large" else "District ${r.district}", onClick = {
                            repName = r.name
                            step = STEP_REMINDER
                        })
                    }
                }
                TextButton(onClick = { step = STEP_REMINDER }) { Text("Skip for now") }
            }

            STEP_REMINDER -> Choice(
                title = "Want a daily study reminder?",
                subtitle = "Five minutes a day is enough. You can change this any time.",
                options = listOf(
                    Triple("19", "Yes, in the evening", "7:00 PM"),
                    Triple("9", "Yes, in the morning", "9:00 AM"),
                    Triple("no", "No reminders", ""),
                ),
            ) { picked ->
                if (picked == "no") {
                    finish(null)
                } else {
                    val reminder = picked.toInt() to 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pendingReminder = reminder
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        finish(reminder)
                    }
                }
            }
        }
    }
}

private fun hasManyReps(vm: AppViewModel, code: String?): Boolean =
    vm.officials.value?.place(code)?.let { it.isState && it.representatives.size > 1 } == true

@Composable
private fun Welcome(onStart: () -> Unit, onPrivacy: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(96.dp))
        Text("Oathly", style = MaterialTheme.typography.displaySmall)
        Text(
            "Practise for your citizenship test with the official questions, a realistic mock interview, and answers that stay up to date.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        InfoCard(container = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                "Oathly is an independent study app. It is not affiliated with USCIS, the Australian Department of Home Affairs, or any government agency, and it is not legal advice.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Get started") }
        TextButton(onClick = onPrivacy) { Text("Privacy policy") }
    }
}

@Composable
private fun Choice(
    title: String,
    subtitle: String? = null,
    options: List<Triple<String, String, String>>,
    onPick: (String) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        options.forEach { (key, label, detail) ->
            MenuRow(label, subtitle = detail.ifBlank { null }, onClick = { onPick(key) })
        }
    }
}

const val PRIVACY_URL = "https://oathly.pages.dev/privacy"
