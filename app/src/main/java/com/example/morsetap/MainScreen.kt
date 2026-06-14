package com.example.morsetap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(
    speak: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel() }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.speakEvents.collectLatest { text -> speak(text) }
    }
    MainScreenContent(state = state, viewModel = viewModel, modifier = modifier)
}

@Composable
fun MainScreenContent(
    state: MorseUiState,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val soundManager = remember { SoundManager() }
    val haptic = LocalHapticFeedback.current

    DisposableEffect(Unit) { onDispose { soundManager.stopTone() } }

    // ── Colour palette ──────────────────────────────────────────────────────
    val darkBg = Color(0xFF0F0F12)
    val cardBg = Color(0xFF16161C)
    val neonCyan = MaterialTheme.colorScheme.primary
    val neonAmber = Color(0xFFFFB300)
    val textGreen = Color(0xFF00E676)
    val darkGrey = Color(0xFF26262E)

    // Tap pad morphs: circle → rounded-rect when dictionary opens
    val aspectRatioValue by animateFloatAsState(
        targetValue = if (state.cheatSheetOpen) 3.8f else 1.0f,
        animationSpec = tween(300)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
            .animateContentSize(animationSpec = tween(300))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (state.cheatSheetOpen) 8.dp else 16.dp)
    ) {
        // ── Header — slides up when dictionary opens ──────────────────────
        AnimatedVisibility(
            visible = !state.cheatSheetOpen,
            enter = slideInVertically(animationSpec = tween(300)) { -it } + expandVertically(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = tween(300)) { -it } + shrinkVertically(animationSpec = tween(300))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MorseTap", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = neonCyan)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TAPS: ${state.statsTapsCount}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .clip(CircleShape)
                            .background(if (state.isPressed) neonAmber else Color.DarkGray)
                    )
                }
            }
        }

        WpmSlider(wpm = state.wpm, onWpmChange = { viewModel.setWpm(it) },
            accentColor = neonCyan, inactiveTrackColor = darkGrey, cardBackgroundColor = cardBg, haptic = haptic)

        CrtTerminal(decodedText = state.decodedText, currentMorse = state.currentMorse,
            isPressed = state.isPressed, accentColor = neonCyan, amberColor = neonAmber,
            greenColor = textGreen, dividerColor = darkGrey)

        WaveformVisualizer(signalHistory = state.signalHistory,
            accentColor = neonCyan, amberColor = neonAmber, cardBackgroundColor = cardBg)

        InteractiveTapPad(
            isPressed = state.isPressed, cheatSheetOpen = state.cheatSheetOpen,
            aspectRatioValue = aspectRatioValue,
            onPressDown = { viewModel.onPressDown() }, onPressRelease = { viewModel.onPressRelease(it) },
            soundManager = soundManager, haptic = haptic,
            accentColor = neonCyan, amberColor = neonAmber, cardBackgroundColor = cardBg,
            modifier = Modifier.weight(1f)
        )

        // ── Control Buttons ───────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.clearAll() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = darkGrey),
                shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 12.dp)
            ) { Text("CLEAR", fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }

            Button(onClick = { viewModel.deleteLast() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = darkGrey),
                shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 12.dp)
            ) { Text("DELETE", fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White) }

            Button(onClick = { viewModel.triggerSpeakAll() }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan),
                shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 12.dp)
            ) { Text("SPEAK", fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black) }

            IconButton(
                onClick = { viewModel.toggleCheatSheet() },
                modifier = Modifier
                    .height(48.dp).width(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.cheatSheetOpen) neonCyan.copy(alpha = 0.2f) else darkGrey)
                    .border(1.dp, if (state.cheatSheetOpen) neonCyan else Color.Transparent, RoundedCornerShape(8.dp))
            ) { Text("📖", fontSize = 20.sp) }
        }

        MorseDictionarySheet(
            cheatSheetOpen = state.cheatSheetOpen, onClose = { viewModel.toggleCheatSheet() },
            accentColor = neonCyan, amberColor = neonAmber, cardBackgroundColor = cardBg, darkBgColor = darkBg
        )
    }
}

@Composable
fun WpmSlider(
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    accentColor: Color,
    inactiveTrackColor: Color,
    cardBackgroundColor: Color,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "WPM: $wpm",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Slider(
                value = wpm.toFloat(),
                onValueChange = { newValue ->
                    val newInt = newValue.toInt()
                    if (newInt != wpm) {
                        onWpmChange(newInt)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                valueRange = 5f..40f,
                modifier = Modifier.height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = inactiveTrackColor
                )
            )
        }
    }
}

@Composable
fun CrtTerminal(
    decodedText: String,
    currentMorse: String,
    isPressed: Boolean,
    accentColor: Color,
    amberColor: Color,
    greenColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF070709))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = "TRANSLATED TERMINAL",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = accentColor.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Output box
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val cursorAlpha = remember { Animatable(1f) }
                LaunchedEffect(isPressed) {
                    cursorAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                }

                Text(
                    text = if (decodedText.isEmpty() && currentMorse.isEmpty()) "AWAITING INPUT..."
                    else decodedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (decodedText.isEmpty() && currentMorse.isEmpty()) Color.DarkGray
                    else greenColor,
                    modifier = Modifier.fillMaxSize()
                )

                // Blinking cursor
                if (decodedText.isNotEmpty() || currentMorse.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                        Spacer(modifier = Modifier.width((decodedText.length * 10.8).dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp, 18.dp)
                                .graphicsLayer(alpha = cursorAlpha.value)
                                .background(greenColor)
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 8.dp))

            // Morse buffer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BUFFER: ", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = currentMorse.ifEmpty { "[ ]" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = amberColor
                    )
                }
                if (currentMorse.isNotEmpty()) {
                    Text(
                        text = "PENDING: ${MorseTranslator.decodeLetter(currentMorse)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = greenColor
                    )
                }
            }
        }
    }
}

@Composable
fun WaveformVisualizer(
    signalHistory: List<SignalItem>,
    accentColor: Color,
    amberColor: Color,
    cardBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(signalHistory.size) {
        if (signalHistory.isNotEmpty()) listState.animateScrollToItem(signalHistory.size - 1)
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth().height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(signalHistory, key = { it.id }) { item ->
            when (item.type) {
                SignalType.Dot -> Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accentColor))
                SignalType.Dash -> Box(modifier = Modifier.size(width = 32.dp, height = 12.dp).clip(RoundedCornerShape(6.dp)).background(accentColor))
                SignalType.CharSpace -> Box(modifier = Modifier.width(1.5.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.3f)))
                SignalType.WordSpace -> Box(modifier = Modifier.width(3.dp).height(20.dp).background(amberColor.copy(alpha = 0.6f)))
            }
        }
    }
}

@Composable
fun InteractiveTapPad(
    isPressed: Boolean,
    cheatSheetOpen: Boolean,
    aspectRatioValue: Float,
    onPressDown: () -> Unit,
    onPressRelease: (Long) -> Unit,
    soundManager: SoundManager,
    haptic: HapticFeedback,
    accentColor: Color,
    amberColor: Color,
    cardBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val padPressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f, animationSpec = tween(80)
    )
    val innerPressGlow by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 0.1f, animationSpec = tween(80)
    )
    val cornerPercent by animateIntAsState(
        targetValue = if (cheatSheetOpen) 20 else 50, animationSpec = tween(300)
    )
    val padShape = RoundedCornerShape(cornerPercent)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(aspectRatioValue)
                .graphicsLayer(scaleX = padPressScale, scaleY = padPressScale)
                .clip(padShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = innerPressGlow), cardBackgroundColor)
                    )
                )
                .border(
                    BorderStroke(
                        width = if (isPressed) 3.dp else 1.5.dp,
                        color = if (isPressed) accentColor else Color.White.copy(alpha = 0.15f)
                    ),
                    shape = padShape
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown()
                            val pressTime = System.currentTimeMillis()
                            onPressDown()
                            soundManager.startTone()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                            }

                            val duration = System.currentTimeMillis() - pressTime
                            soundManager.stopTone()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPressRelease(duration)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    text = "TAP",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = if (isPressed) accentColor else Color.LightGray
                )
                if (!cheatSheetOpen) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPressed) "ON AIR" else "TOUCH & HOLD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = if (isPressed) amberColor else Color.Gray,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}
