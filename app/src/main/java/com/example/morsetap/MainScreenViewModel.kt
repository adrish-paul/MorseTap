package com.example.morsetap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SignalType {
    object Dot : SignalType()
    object Dash : SignalType()
    object CharSpace : SignalType()
    object WordSpace : SignalType()
}

data class SignalItem(val id: Long, val type: SignalType)

data class MorseUiState(
    val decodedText: String = "",
    val currentMorse: String = "",
    val isPressed: Boolean = false,
    val wpm: Int = 14,           // 14 WPM → T ≈ 85 ms
    val signalHistory: List<SignalItem> = emptyList(),
    val cheatSheetOpen: Boolean = false,
    val statsTapsCount: Int = 0
)

class MainScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MorseUiState())
    val uiState: StateFlow<MorseUiState> = _uiState.asStateFlow()

    private val _speakEvents = MutableSharedFlow<String>(replay = 0)
    val speakEvents: SharedFlow<String> = _speakEvents.asSharedFlow()

    /** Overridable in unit tests to inject virtual time. */
    var timeProvider: () -> Long = { System.currentTimeMillis() }

    private var lastReleaseTime = 0L
    private var charGapTriggered = true
    private var gapCheckerJob: Job? = null
    private var signalIdCounter = 0L

    /** Base unit T in ms = 1200 / WPM. */
    private val unitT: Long get() = 1200L / _uiState.value.wpm

    /** 3.5 × T — slightly generous for manual keying accuracy. */
    private val charGapThreshold: Long get() = (3.5 * unitT).toLong()

    fun setWpm(wpm: Int) { _uiState.update { it.copy(wpm = wpm.coerceIn(5, 40)) } }

    fun toggleCheatSheet() { _uiState.update { it.copy(cheatSheetOpen = !it.cheatSheetOpen) } }

    fun onPressDown() {
        gapCheckerJob?.cancel()
        _uiState.update { it.copy(isPressed = true) }
        charGapTriggered = false
    }

    fun onPressRelease(durationMs: Long) {
        _uiState.update { it.copy(isPressed = false) }
        if (durationMs < 20) return  // ignore accidental micro-taps

        val t = unitT
        val isDot = durationMs < (2.2 * t).toLong()
        val symbol = if (isDot) "." else "-"
        val signalType = if (isDot) SignalType.Dot else SignalType.Dash

        _uiState.update { state ->
            val newHistory = (state.signalHistory + SignalItem(signalIdCounter++, signalType)).takeLast(20)
            state.copy(
                currentMorse = state.currentMorse + symbol,
                signalHistory = newHistory,
                statsTapsCount = state.statsTapsCount + 1
            )
        }
        lastReleaseTime = timeProvider()
        startGapChecker()
    }

    private fun startGapChecker() {
        gapCheckerJob?.cancel()
        gapCheckerJob = viewModelScope.launch {
            while (true) {
                delay(20)
                val elapsed = timeProvider() - lastReleaseTime
                val currentMorse = _uiState.value.currentMorse

                // Decode once the inter-character gap elapses
                if (currentMorse.isNotEmpty() && elapsed >= charGapThreshold && !charGapTriggered) {
                    charGapTriggered = true
                    val decodedChar = MorseTranslator.decodeLetter(currentMorse)
                    _uiState.update { state ->
                        val newHistory = (state.signalHistory + SignalItem(signalIdCounter++, SignalType.CharSpace)).takeLast(20)
                        // Each letter is its own "word" — trail with a space
                        state.copy(
                            decodedText = state.decodedText + decodedChar + " ",
                            currentMorse = "",
                            signalHistory = newHistory
                        )
                    }
                }
                if (charGapTriggered) break
            }
        }
    }

    fun triggerSpeakAll() {
        val text = _uiState.value.decodedText.trim()
        if (text.isNotEmpty()) viewModelScope.launch { _speakEvents.emit(text) }
    }

    fun deleteLast() {
        _uiState.update { state ->
            when {
                state.currentMorse.isNotEmpty() ->
                    state.copy(currentMorse = state.currentMorse.dropLast(1))
                state.decodedText.isNotEmpty() -> {
                    val trimmed = if (state.decodedText.endsWith(" "))
                        state.decodedText.dropLast(2) else state.decodedText.dropLast(1)
                    state.copy(decodedText = trimmed)
                }
                else -> state
            }
        }
    }

    fun clearAll() {
        gapCheckerJob?.cancel()
        _uiState.update { MorseUiState(wpm = it.wpm, cheatSheetOpen = it.cheatSheetOpen) }
        charGapTriggered = true
    }
}
