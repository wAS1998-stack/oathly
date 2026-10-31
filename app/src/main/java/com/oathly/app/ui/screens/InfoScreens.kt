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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oathly.app.BuildConfig
import com.oathly.app.billing.BillingManager
import com.oathly.app.data.Country
import com.oathly.app.ui.AdBanner
import com.oathly.app.ui.AppBar
import com.oathly.app.ui.AppViewModel
import com.oathly.app.ui.InfoCard
import com.oathly.app.ui.MenuRow
import com.oathly.app.ui.Screen
import com.oathly.app.ui.SectionTitle
import com.oathly.app.ui.openUrl
import com.oathly.app.ui.percent
import com.oathly.app.ui.rememberActivity
import java.text.DateFormat
import java.util.Date

@Composable
fun OfficialsScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val officials by vm.officials.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val bank = vm.usBank
    val place = officials?.place(settings.stateCode)

    Scaffold(
        topBar = { AppBar("Answers that change", onBack = { vm.back() }) },
        bottomBar = { AdBanner(vm, Modifier.navigationBarsPadding()) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "These answers change after elections and appointments, or depend on where you live. Oathly checks for updates every day.",
                style = MaterialTheme.typography.bodyMedium,
            )
            MenuRow(
                place?.name ?: "Choose your state",
                subtitle = if (place == null) "Needed for senators, representative, governor and capital" else "Tap to change",
                onClick = { vm.navigate(Screen.PickState) },
            )
            if (place != null && place.isState && place.representatives.size > 1) {
                MenuRow(
                    settings.repName ?: "Choose your representative",
                    subtitle = "Your U.S. representative · tap to change",
                    onClick = { vm.navigate(Screen.PickRep) },
                )
            }
            bank?.questions?.filter { it.dynamicKey != null }?.forEach { q ->
                val a = vm.dynamicAnswer(q)
                InfoCard {
                    Text("Question ${q.number}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(q.text, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    a?.display?.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                    a?.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            val updated = officials?.generatedAt?.take(10)
            Text(
                "Last updated: ${updated ?: "not yet"}. Sources: Congress data (unitedstates project), Wikidata. Always check the official list before your interview.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { context.openUrl(bank?.updatesUrl ?: "https://www.uscis.gov/citizenship/testupdates") }) {
                Text("Open USCIS test updates")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val tests by vm.tests.collectAsStateWithLifecycle()
    val groups: List<Triple<String, Int, Int>> = when (settings.country) {
        Country.AU -> vm.auBank?.parts?.map { (part, title) ->
            val ids = vm.auBank?.questions?.filter { it.part == part }?.map { it.id }.orEmpty()
            Triple("Part $part: $title", ids.count { stats[it]?.mastered == true }, ids.size)
        }.orEmpty()
        else -> vm.usBank?.categories?.map { (key, title) ->
            val ids = vm.usPool().filter { it.category == key }.map { it.id }
            Triple(title, ids.count { stats[it]?.mastered == true }, ids.size)
        }.orEmpty().filter { it.third > 0 }
    }
    val kindPrefix = if (settings.country == Country.AU) "au" else "us${settings.usVersion}"
    val myTests = tests.filter { it.kind.startsWith(kindPrefix) }.reversed()

    Scaffold(
        topBar = { AppBar("Your progress", onBack = { vm.back() }) },
        bottomBar = { AdBanner(vm, Modifier.navigationBarsPadding()) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InfoCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Text("Chance of passing today", style = MaterialTheme.typography.labelLarge)
                Text(vm.readiness?.let(::percent) ?: "…", style = MaterialTheme.typography.displaySmall)
            }
            SectionTitle("Learned by topic")
            groups.forEach { (title, done, total) ->
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("$done / $total", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
                }
            }
            SectionTitle("Practice tests")
            if (myTests.isEmpty()) {
                Text("No tests yet. Your results will show up here.", style = MaterialTheme.typography.bodyMedium)
            }
            val fmt = DateFormat.getDateInstance(DateFormat.MEDIUM)
            myTests.take(15).forEach { t ->
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt.format(Date(t.time)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${t.right}/${t.total} · ${if (t.passed) "Passed" else "Not passed"}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun AboutScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val au = runCatching { vm.graph.content.auBank() }.getOrNull()
    Scaffold(
        topBar = { AppBar("About & sources", onBack = { vm.back() }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard(container = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Independent study app", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Oathly is not affiliated with, endorsed by, or connected to U.S. Citizenship and Immigration Services (USCIS), the Australian Government Department of Home Affairs, or any other government agency. It is a study aid, not legal advice. Always check the official sources below.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SectionTitle("United States")
            Text(
                "The civics questions and answers come from USCIS publications (2025 version, form M-1778, and the 2008 version). They are works of the U.S. Government and in the public domain.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { context.openUrl("https://www.uscis.gov/citizenship/find-study-materials-and-resources/study-for-the-test") }) {
                Text("uscis.gov: Study for the test")
            }
            TextButton(onClick = { context.openUrl("https://www.uscis.gov/citizenship/testupdates") }) {
                Text("uscis.gov: Test updates")
            }
            Text(
                "Current officeholders come from the public-domain congress-legislators dataset and Wikidata (CC0), refreshed daily.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SectionTitle("Australia")
            Text(
                au?.attribution?.ifBlank { null }
                    ?: "Practice questions are written by Oathly from the official booklet \"Australian Citizenship: Our Common Bond\".",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (au != null && au.licence.isNotBlank()) Text("Licence: ${au.licence}", style = MaterialTheme.typography.bodySmall)
            Text(
                "The questions in the real Australian test are not public. Oathly's practice questions are original and based only on the booklet.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { context.openUrl(au?.sourceUrl ?: "https://immi.homeaffairs.gov.au/citizenship/test-and-interview/our-common-bond") }) {
                Text("homeaffairs.gov.au: Our Common Bond")
            }
            SectionTitle("Privacy")
            Text(
                "Your progress stays on this phone. Oathly has no accounts and does not collect your name, email or location. Ads are provided by Google AdMob, which may use your device's advertising ID as described in the privacy policy.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { context.openUrl(PRIVACY_URL) }) { Text("Privacy policy") }
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProScreen(vm: AppViewModel) {
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val price by vm.proPrice.collectAsStateWithLifecycle()
    val status by vm.purchaseStatus.collectAsStateWithLifecycle()
    val activity = rememberActivity()
    Scaffold(
        topBar = { AppBar("Oathly Pro", onBack = { vm.back() }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (isPro) {
                InfoCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("You have Oathly Pro", style = MaterialTheme.typography.headlineSmall)
                    Text("No ads, ever. Thank you for supporting the app.", style = MaterialTheme.typography.bodyLarge)
                }
                return@Column
            }
            Text("Study without ads", style = MaterialTheme.typography.headlineSmall)
            Text(
                "One payment removes every ad for good. All study features (questions, mock interviews, audio and updates) stay free either way.",
                style = MaterialTheme.typography.bodyLarge,
            )
            when (status) {
                BillingManager.Status.PENDING -> Text("Your payment is pending. Pro turns on when it completes.", style = MaterialTheme.typography.bodyMedium)
                BillingManager.Status.ERROR -> Text("The purchase didn't go through. Please try again.", style = MaterialTheme.typography.bodyMedium)
                BillingManager.Status.IDLE -> Unit
            }
            Button(
                onClick = { if (activity != null) vm.buyPro(activity) },
                enabled = price != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(price?.let { "Remove ads for $it" } ?: "Loading price…") }
            OutlinedButton(onClick = { vm.restorePurchases() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Restore purchase")
            }
            Text(
                "One-time purchase through Google Play. No subscription.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
