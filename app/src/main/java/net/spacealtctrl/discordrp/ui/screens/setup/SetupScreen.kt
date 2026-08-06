package net.spacealtctrl.discordrp.ui.screens.setup

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.settings.Stash
import net.spacealtctrl.discordrp.ui.kit.AccentButton
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.Cluster
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.RowRule
import net.spacealtctrl.discordrp.ui.kit.SectionHeader
import net.spacealtctrl.discordrp.ui.kit.StepRow
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace

@Composable
fun SetupScreen(
    notificationAccess: State<Boolean>,
    onSignIn: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val stash = remember { Stash.of(context) }
    val signedIn = stash.signedIn

    var canPostNotifications by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val batteryFreedom = remember(context) {
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    val permissionAsk = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { canPostNotifications = it },
    )

    val ready = signedIn && notificationAccess.value && canPostNotifications

    val unlockPulse = remember { Animatable(1f) }
    var wasReady by remember { mutableStateOf(ready) }
    LaunchedEffect(ready) {
        if (ready && !wasReady) {
            unlockPulse.animateTo(0.96f, tween(Pace.BLINK, easing = Pace.dart))
            unlockPulse.animateTo(1f, Pace.snap())
        }
        wasReady = ready
    }

    AppScreen {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Look.gaps.gutter),
        ) {
            PageTitle(
                eyebrow = stringResource(R.string.app_name),
                title = stringResource(R.string.setup_title),
                subtitle = stringResource(R.string.setup_subtitle),
            )

            val wantsPostPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            var step = 1

            SectionHeader(stringResource(R.string.setup_required_header))
            Cluster {
                StepRow(
                    index = step++,
                    title = stringResource(R.string.setup_media_access_title),
                    subtitle = stringResource(R.string.setup_media_access_blurb),
                    done = notificationAccess.value,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    },
                )
                if (wantsPostPermission) {
                    RowRule(inset = false)
                    StepRow(
                        index = step++,
                        title = stringResource(R.string.setup_post_notifications_title),
                        subtitle = stringResource(R.string.setup_post_notifications_blurb),
                        done = canPostNotifications,
                        onClick = { permissionAsk.launch(POST_NOTIFICATIONS) },
                    )
                }
                RowRule(inset = false)
                StepRow(
                    index = step++,
                    title = stringResource(R.string.setup_sign_in_title),
                    subtitle = stringResource(R.string.setup_sign_in_blurb),
                    done = signedIn,
                    onClick = onSignIn,
                )
            }

            SectionHeader(stringResource(R.string.setup_optional_header))
            Cluster {
                StepRow(
                    index = step,
                    title = stringResource(R.string.setup_battery_title),
                    subtitle = stringResource(R.string.setup_battery_blurb),
                    done = batteryFreedom,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    },
                )
            }

            Text(
                text = stringResource(R.string.setup_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = Look.palette.inkGhost,
                modifier = Modifier.padding(top = Look.gaps.wide, bottom = Look.gaps.open),
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = Look.gaps.wide)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                AccentButton(
                    text = stringResource(R.string.setup_finish_cta),
                    onClick = onFinished,
                    enabled = ready,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = unlockPulse.value
                            scaleY = unlockPulse.value
                        },
                )
            }
        }
    }
}
