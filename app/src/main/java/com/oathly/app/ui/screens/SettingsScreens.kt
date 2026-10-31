package com.oathly.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.data.Country
import com.oathly.app.data.ThemeMode
import com.oathly.app.ui.AppBar
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.InfoCard
import com.oathly.app.ui.MenuRow
import com.oathly.app.ui.Screen
import com.oathly.app.ui.SectionTitle
import com.oathly.app.ui.openUrl
import com.oathly.app.ui.rememberActivity

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val officials by vm.officials.collectAsStateWithLifecycle()
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val activity = rememberActivity()
    val context = LocalContext.current
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setReminder(true, s.reminderHour, s.reminderMinute)
    }

    Scaffold(
        topBar = { AppBar("Settings", onBack = { vm.back() }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Your test")
            MenuRow("Test", subtitle = s.country?.title ?: "Not chosen", trailing = "Change", onClick = { dialog = "country" })
            if (s.country == Country.US) {
                MenuRow(
                    "Test version",
                    subtitle = if (s.usVersion == "2008") "2008 version (filed before Oct 20, 2025)" else "2025 version (filed on or after Oct 20, 2025)",
                    trailing = "Change", onClick = { dialog = "version" },
                )
                ToggleRow("65/20 rule", "Study only the 20 starred questions (65+, 20 years as a permanent resident)", s.senior) { vm.setSenior(it) }
                val place = officials?.place(s.stateCode)
                MenuRow("State or territory", subtitle = place?.name ?: "Not chosen", trailing = "Change", onClick = { vm.navigate(Screen.PickState) })
                if (place != null && place.isState && place.representatives.size > 1) {
                    MenuRow("Your U.S. representative", subtitle = s.repName ?: "Not chosen", trailing = "Change", onClick = { vm.navigate(Screen.PickRep) })
                }
            }

            SectionTitle("Study")
            ToggleRow("Read questions aloud", "Hear each question like at the interview", s.autoRead) { vm.setAutoRead(it) }
            InfoCard {
                Text("Speaking speed", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = s.speechRate,
                    onValueChange = { vm.setSpeechRate(it) },
                    valueRange = 0.6f..1.3f,
                    onValueChangeFinished = { vm.speak("This is how fast questions will be read.") },
                )
            }
            ToggleRow(
                "Daily reminder",
                if (s.reminderOn) "Every day at %d:%02d".format(s.reminderHour, s.reminderMinute) else "Off",
                s.reminderOn,
            ) { on ->
                if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.setReminder(on, s.reminderHour, s.reminderMinute)
            }
            if (s.reminderOn) {
                MenuRow("Reminder time", subtitle = "%d:%02d".format(s.reminderHour, s.reminderMinute), trailing = "Change", onClick = { dialog = "time" })
            }
            MenuRow("Theme", subtitle = s.theme.name.lowercase().replaceFirstChar { it.uppercase() }, trailing = "Change", onClick = { dialog = "theme" })

            SectionTitle("App")
            if (!isPro) MenuRow("Remove ads", subtitle = "One-time Oathly Pro purchase", onClick = { vm.navigate(Screen.Pro) })
            else MenuRow("Oathly Pro", subtitle = "Active: no ads", onClick = { vm.navigate(Screen.Pro) })
            if (vm.privacyOptionsRequired() && activity != null) {
                MenuRow("Privacy choices", subtitle = "Change your ad consent", onClick = { vm.showPrivacyOptions(activity) })
            }
            MenuRow("Privacy policy", onClick = { context.openUrl(PRIVACY_URL) })
            MenuRow("About & sources", subtitle = "Disclaimer, official sources, licences", onClick = { vm.navigate(Screen.About) })
            MenuRow("Reset progress", subtitle = "Clear your answers for this test", onClick = { dialog = "reset" })
            Spacer(Modifier.height(24.dp))
        }
    }

    when (dialog) {
        "country" -> PickDialog(
            "Which test?",
            listOf("US" to "U.S. citizenship test", "AU" to "Australian citizenship test"),
            onPick = { vm.setCountry(Country.from(it) ?: Country.US) }, onDismiss = { dialog = null },
        )
        "version" -> PickDialog(
            "Test version",
            listOf("2025" to "2025 (filed on or after Oct 20, 2025, or not yet filed)", "2008" to "2008 (filed before Oct 20, 2025)"),
            onPick = { vm.setUsVersion(it) }, onDismiss = { dialog = null },
        )
        "theme" -> PickDialog(
            "Theme",
            ThemeMode.entries.map { it.name to it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
            onPick = { vm.setTheme(ThemeMode.valueOf(it)) }, onDismiss = { dialog = null },
        )
        "time" -> PickDialog(
            "Reminder time",
            listOf(7, 9, 12, 17, 19, 21).map { h -> "$h" to "%d:00".format(h) },
            onPick = { vm.setReminder(true, it.toInt(), 0) }, onDismiss = { dialog = null },
        )
        "reset" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Reset progress?") },
            text = { Text("This clears your answers and test history for the current test. It can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); dialog = null }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    InfoCard(onClick = { onChange(!checked) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun PickDialog(title: String, options: List<Pair<String, String>>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (key, label) ->
                    TextButton(onClick = { onPick(key); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun PickStateScreen(vm: AppViewModel) {
    val officials by vm.officials.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Scaffold(
        topBar = { AppBar("Choose your state", onBack = { vm.back() }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("Search") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(officials?.places.orEmpty().filter { it.name.contains(query.trim(), ignoreCase = true) }, key = { it.code }) { p ->
                    MenuRow(p.name, onClick = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        vm.setState(p.code)
                        vm.back()
                        if (p.isState && p.representatives.size > 1) vm.navigate(Screen.PickRep)
                    })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun PickRepScreen(vm: AppViewModel) {
    val officials by vm.officials.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val place = officials?.place(settings.stateCode)
    Scaffold(
        topBar = { AppBar("Your representative", onBack = { vm.back() }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    "Not sure which district you live in? Look it up with your ZIP code.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { context.openUrl("https://www.house.gov/representatives/find-your-representative") }) {
                    Text("Find my representative on house.gov")
                }
            }
            items(place?.representatives.orEmpty(), key = { it.name + it.district }) { r ->
                MenuRow(
                    r.name,
                    subtitle = if (r.district == 0) "At-large" else "District ${r.district}",
                    trailing = if (r.name == settings.repName) "Selected" else null,
                    onClick = { vm.setRep(r.name); vm.back() },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
