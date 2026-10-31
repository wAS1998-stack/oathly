package com.oathly.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.oathly.app.BuildConfig
import com.oathly.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(title: String, onBack: (() -> Unit)? = null, close: Boolean = false, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painterResource(if (close) R.drawable.ic_close else R.drawable.ic_back),
                        contentDescription = if (close) "Close" else "Back",
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val colors = CardDefaults.cardColors(containerColor = container)
    val inner: @Composable ColumnScope.() -> Unit = { Column(Modifier.padding(18.dp), content = content) }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, content = inner)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, content = inner)
    }
}

/** A tappable row used on menus: title, subtitle and an optional trailing label. */
@Composable
fun MenuRow(title: String, subtitle: String? = null, trailing: String? = null, onClick: () -> Unit) {
    InfoCard(onClick = onClick, modifier = Modifier.heightIn(min = 64.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun SpeakButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(painterResource(R.drawable.ic_volume), contentDescription = "Read aloud", tint = MaterialTheme.colorScheme.primary)
    }
}

/** Adaptive banner. Shown only on menu screens, after consent, and never to Pro users. */
@Composable
fun AdBanner(vm: AppViewModel, modifier: Modifier = Modifier) {
    val allowed by vm.bannersAllowed.collectAsStateWithLifecycle()
    val pro by vm.isPro.collectAsStateWithLifecycle()
    if (!allowed || pro) return
    val width = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, width))
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
fun ColumnGap(height: Int = 12) = Spacer(Modifier.padding(top = height.dp))

val ScreenPadding = Modifier.padding(horizontal = 16.dp)
val ListSpacing = Arrangement.spacedBy(10.dp)

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun rememberActivity(): Activity? = LocalContext.current.findActivity()

fun percent(x: Double): String = "${Math.round(x * 100)}%"
