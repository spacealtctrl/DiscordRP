package net.spacealtctrl.discordrp.ui.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.ui.kit.AccentButton
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.QuietButton
import net.spacealtctrl.discordrp.ui.kit.SectionHeader
import net.spacealtctrl.discordrp.ui.theme.DiscordRpTheme
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import java.io.File
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val report = intent.getStringExtra(EXTRA_REPORT).orEmpty()
        setContent {
            DiscordRpTheme {
                CrashScreen(report = report)
            }
        }
    }

    companion object {
        const val EXTRA_REPORT = "crash_report"
    }
}

@Composable
private fun CrashScreen(report: String) {
    val context = LocalContext.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(Pace.LAZY, easing = Pace.settle))
    }

    AppScreen {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .graphicsLayer { alpha = appear.value }
                .padding(horizontal = Look.gaps.gutter),
        ) {
            PageTitle(
                eyebrow = stringResource(R.string.app_name),
                title = stringResource(R.string.crash_title),
                subtitle = stringResource(R.string.crash_subtitle),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Look.gaps.cozy),
            ) {
                AccentButton(
                    text = stringResource(R.string.crash_share_cta),
                    icon = Icons.Rounded.Share,
                    onClick = { shareReport(context, report) },
                    modifier = Modifier.weight(1f),
                )
                QuietButton(
                    text = stringResource(R.string.crash_close_cta),
                    onClick = { exitProcess(0) },
                )
            }

            SectionHeader(stringResource(R.string.crash_trace_header))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(Look.corners.card)
                    .background(Look.palette.abyss)
                    .padding(Look.gaps.room)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = Look.palette.inkFaint,
                )
            }

            Box(Modifier.navigationBarsPadding().padding(bottom = Look.gaps.wide))
        }
    }
}

private fun shareReport(context: android.content.Context, report: String) {
    runCatching {
        val file = File(context.filesDir, "crash-report.txt").apply { writeText(report) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, null))
    }
}
