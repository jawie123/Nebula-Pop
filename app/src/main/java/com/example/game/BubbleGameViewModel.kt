package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.BoosterKind
import com.example.data.PlayerProgress
import com.example.data.PlayerProgressRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BubbleGameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PlayerProgressRepository(application.applicationContext)
    private val soundEngine = SoundEngine()

    val progress: StateFlow<PlayerProgress> = repository.progressFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerProgress()
        )

    private val _gameState = MutableStateFlow(BubbleGameEngine.startSector(1))
    val gameState: StateFlow<BubbleGameState> = _gameState.asStateFlow()

    private val _hapticPulse = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val hapticPulse: SharedFlow<Unit> = _hapticPulse.asSharedFlow()

    private var recordedEndResult = false

    init {
        viewModelScope.launch {
            progress.collect { prefs ->
                soundEngine.enabled = prefs.soundEnabled
            }
        }
    }

    fun startSector(sectorNumber: Int) {
        recordedEndResult = false
        _gameState.value = BubbleGameEngine.startSector(sectorNumber)
    }

    fun startEndless() {
        recordedEndResult = false
        _gameState.value = BubbleGameEngine.startEndless()
    }

    fun restartCurrentRun() {
        val current = _gameState.value
        recordedEndResult = false
        _gameState.value = if (current.mode == GameMode.SECTOR) {
            BubbleGameEngine.startSector(current.sectorNumber)
        } else {
            BubbleGameEngine.startEndless()
        }
    }

    fun startNextSector() {
        val current = _gameState.value
        val next = (current.sectorNumber + 1).coerceAtMost(PlayerProgress.TOTAL_SECTORS)
        recordedEndResult = false
        _gameState.value = BubbleGameEngine.startSector(next)
    }

    fun pauseGame() {
        _gameState.update { state ->
            if (state.status == GameStatus.PLAYING) state.copy(status = GameStatus.PAUSED, isAiming = false)
            else state
        }
    }

    fun resumeGame() {
        _gameState.update { state ->
            if (state.status == GameStatus.PAUSED) state.copy(status = GameStatus.PLAYING)
            else state
        }
    }

    fun toggleBooster(kind: BoosterKind) {
        val count = progress.value.countOf(kind)
        if (count <= 0 && _gameState.value.armedBooster != kind) return
        _gameState.update { state ->
            BubbleGameEngine.toggleArmBooster(state, kind)
        }
    }

    fun swapOrbs() {
        _gameState.update { state ->
            BubbleGameEngine.swapOrbs(state)
        }
    }

    fun onAimUpdate(
        pointerBoardX: Float,
        pointerBoardY: Float,
        launcherBoardX: Float,
        launcherBoardY: Float
    ) {
        _gameState.update { state ->
            BubbleGameEngine.updateAim(
                state = state,
                pointerX = pointerBoardX,
                pointerY = pointerBoardY,
                launcherX = launcherBoardX,
                launcherY = launcherBoardY,
                isAiming = true
            )
        }
    }

    fun onAimCancel() {
        _gameState.update {
            it.copy(isAiming = false, aimTrajectory = AimTrajectory(emptyList(), null))
        }
    }

    fun onShoot(launcherBoardX: Float, launcherBoardY: Float) {
        var usedBooster: BoosterKind? = null
        var didShoot = false

        _gameState.update { state ->
            val (nextState, booster) = BubbleGameEngine.shoot(state, launcherBoardX, launcherBoardY)
            if (nextState.bullet != null && state.bullet == null) {
                didShoot = true
                usedBooster = booster
            }
            nextState
        }

        if (didShoot) {
            soundEngine.playShoot()
            usedBooster?.let { kind ->
                viewModelScope.launch { repository.consumeBooster(kind) }
            }
        }
    }

    fun onFrameTick(dtSeconds: Float) {
        val current = _gameState.value
        if (current.status == GameStatus.PAUSED) return
        if (current.bullet == null &&
            current.particles.isEmpty() &&
            current.fallingBubbles.isEmpty() &&
            current.popups.isEmpty() &&
            current.shockwaves.isEmpty() &&
            current.screenShake <= 0f
        ) {
            return
        }

        val result = BubbleGameEngine.step(current, dtSeconds)
        var finalState = result.state

        if (!recordedEndResult && finalState.status == GameStatus.SECTOR_CLEARED) {
            recordedEndResult = true
            val isBest = finalState.score > progress.value.bestSectorScore
            finalState = finalState.copy(isNewBest = isBest)
            viewModelScope.launch {
                repository.recordSectorClear(
                    sector = finalState.sectorNumber,
                    starsEarned = finalState.earnedStars,
                    score = finalState.score,
                    rewardBooster = finalState.rewardBooster
                )
            }
        } else if (!recordedEndResult &&
            finalState.status == GameStatus.GAME_OVER &&
            finalState.mode == GameMode.ENDLESS
        ) {
            recordedEndResult = true
            val isBest = finalState.score > progress.value.bestEndlessScore
            finalState = finalState.copy(isNewBest = isBest)
            viewModelScope.launch {
                repository.recordEndlessScore(finalState.score)
            }
        }

        _gameState.value = finalState

        for (sound in result.sounds) {
            when (sound) {
                GameSoundEvent.SHOOT -> soundEngine.playShoot()
                GameSoundEvent.BOUNCE -> soundEngine.playBounce()
                GameSoundEvent.POP -> soundEngine.playPop(finalState.combo)
                GameSoundEvent.EXPLOSION -> soundEngine.playExplosion()
                GameSoundEvent.VICTORY -> soundEngine.playVictory()
                GameSoundEvent.GAME_OVER -> soundEngine.playGameOver()
            }
        }

        if (result.triggerHapticImpact && progress.value.hapticsEnabled) {
            _hapticPulse.tryEmit(Unit)
        }
    }

    fun toggleSound() {
        viewModelScope.launch { repository.toggleSound() }
    }

    fun toggleHaptics() {
        viewModelScope.launch { repository.toggleHaptics() }
    }

    fun toggleColorblind() {
        viewModelScope.launch { repository.toggleColorblind() }
    }

    override fun onCleared() {
        soundEngine.release()
        super.onCleared()
    }
}
