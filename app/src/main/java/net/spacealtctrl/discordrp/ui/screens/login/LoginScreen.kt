package net.spacealtctrl.discordrp.ui.screens.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.spacealtctrl.discordrp.R
import net.spacealtctrl.discordrp.discord.FirstHandshake
import net.spacealtctrl.discordrp.ui.kit.AccentButton
import net.spacealtctrl.discordrp.ui.kit.AppScreen
import net.spacealtctrl.discordrp.ui.kit.ArcSpinner
import net.spacealtctrl.discordrp.ui.kit.CalloutBanner
import net.spacealtctrl.discordrp.ui.kit.CalloutTone
import net.spacealtctrl.discordrp.ui.kit.PageTitle
import net.spacealtctrl.discordrp.ui.kit.RoundIconButton
import net.spacealtctrl.discordrp.ui.theme.Look
import net.spacealtctrl.discordrp.ui.theme.Pace
import javax.inject.Inject

sealed interface LoginStage {
    data object Idle : LoginStage
    data object Browsing : LoginStage
    data object Verifying : LoginStage
    data object Failed : LoginStage
    data object Done : LoginStage
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val handshake: FirstHandshake,
) : ViewModel() {
    private val _stage = MutableStateFlow<LoginStage>(LoginStage.Idle)
    val stage: StateFlow<LoginStage> = _stage.asStateFlow()

    fun openBrowser() {
        _stage.value = LoginStage.Browsing
    }

    fun dismissBrowser() {
        if (_stage.value == LoginStage.Browsing) _stage.value = LoginStage.Idle
    }

    fun onToken(token: String) {
        if (_stage.value == LoginStage.Verifying) return
        _stage.value = LoginStage.Verifying
        viewModelScope.launch {
            _stage.value = if (handshake.run(token)) LoginStage.Done else LoginStage.Failed
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val stage by viewModel.stage.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(stage) {
        if (stage == LoginStage.Done) onDone()
    }

    AppScreen {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = Look.gaps.gutter),
        ) {
            PageTitle(
                title = "",
                leading = {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        onClick = onBack,
                    )
                },
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = Look.gaps.vast),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Look.palette.accentFaint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_discord_logo),
                        contentDescription = null,
                        tint = Look.palette.accent,
                        modifier = Modifier.size(46.dp),
                    )
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = Look.palette.ink,
                    modifier = Modifier.padding(top = Look.gaps.open),
                )
                Text(
                    text = stringResource(R.string.login_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Look.palette.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Look.gaps.snug),
                )
                Text(
                    text = stringResource(R.string.login_token_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = Look.palette.inkGhost,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        top = Look.gaps.wide,
                        start = Look.gaps.open,
                        end = Look.gaps.open,
                    ),
                )

                AnimatedVisibility(
                    visible = stage == LoginStage.Failed,
                    enter = Pace.reveal,
                    exit = Pace.conceal,
                ) {
                    val shake = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        shake.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 280
                                delayMillis = Pace.EASY
                                0f at 0
                                -6f at 56
                                5f at 112
                                -3f at 168
                                2f at 224
                                0f at 280
                            },
                        )
                    }
                    CalloutBanner(
                        title = stringResource(R.string.login_failed_title),
                        body = stringResource(R.string.login_failed_body),
                        tone = CalloutTone.Danger,
                        modifier = Modifier
                            .padding(top = Look.gaps.open)
                            .graphicsLayer { translationX = shake.value.dp.toPx() },
                    )
                }

                AnimatedContent(
                    targetState = stage == LoginStage.Verifying,
                    transitionSpec = {
                        fadeIn(tween(210, delayMillis = Pace.BLINK, easing = Pace.settle))
                            .togetherWith(fadeOut(tween(Pace.BLINK, easing = Pace.dart)))
                            .using(
                                SizeTransform(clip = false) { _, _ ->
                                    Pace.glidespring(IntSize.VisibilityThreshold)
                                }
                            )
                    },
                    modifier = Modifier
                        .padding(top = Look.gaps.vast)
                        .height(46.dp),
                    contentAlignment = Alignment.Center,
                    label = "loginCta",
                ) { verifying ->
                    if (verifying) {
                        ArcSpinner()
                    } else {
                        AccentButton(
                            text = stringResource(R.string.login_cta),
                            onClick = viewModel::openBrowser,
                            modifier = Modifier.fillMaxWidth(0.8f),
                        )
                    }
                }
            }
        }

        if (stage == LoginStage.Browsing) {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissBrowser,
                sheetState = sheetState,
                containerColor = Look.palette.panel,
            ) {
                TokenCapture(onToken = viewModel::onToken)
            }
        }
    }
}
