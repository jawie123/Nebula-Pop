package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PlayerProgress
import com.example.game.BubbleGameEngine
import com.example.game.BubbleGameState
import com.example.game.BubbleGameViewModel
import com.example.game.GameStatus
import com.example.game.OrbType
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DangerLineRed
import com.example.ui.theme.SpaceCenter
import com.example.ui.theme.SpaceEdge
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private data class BoardMetrics(
    val rPx: Float,
    val originX: Float,
    val originY: Float,
    val launcherBoardX: Float,
    val launcherBoardY: Float
) {
    fun toScreen(boardX: Float, boardY: Float): Offset =
        Offset(originX + boardX * rPx, originY + boardY * rPx)

    fun toBoard(screenX: Float, screenY: Float): Offset =
        Offset((screenX - originX) / rPx, (screenY - originY) / rPx)
}

private data class StarSpec(
    val normX: Float,
    val normY: Float,
    val radiusFactor: Float,
    val alpha: Float,
    val color: Color
)

@Composable
fun GameScreen(
    viewModel: BubbleGameViewModel,
    onOpenSectorMap: () -> Unit,
    onExitToHub: () -> Unit
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    BackHandler(enabled = state.status == GameStatus.PLAYING || state.status == GameStatus.PAUSED) {
        if (state.status == GameStatus.PLAYING) {
            viewModel.pauseGame()
        } else {
            viewModel.resumeGame()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.hapticPulse.collect {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    viewModel.onFrameTick(dt)
                }
                lastNanos = frameNanos
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicVoid)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(SpaceCenter, SpaceEdge),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = max(size.width, size.height) * 0.78f
                    )
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopHud(
                state = state,
                onPauseClick = viewModel::pauseGame
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GamePlayfield(
                    state = state,
                    progress = progress,
                    onAimUpdate = viewModel::onAimUpdate,
                    onAimCancel = viewModel::onAimCancel,
                    onShoot = viewModel::onShoot
                )
            }

            BottomControlDock(
                state = state,
                progress = progress,
                onSwapOrbs = viewModel::swapOrbs,
                onToggleBooster = viewModel::toggleBooster
            )
        }

        GameOverlays(
            state = state,
            progress = progress,
            onResume = viewModel::resumeGame,
            onRestart = viewModel::restartCurrentRun,
            onNextSector = viewModel::startNextSector,
            onOpenSectorMap = onOpenSectorMap,
            onExitToHub = onExitToHub,
            onToggleSound = viewModel::toggleSound,
            onToggleHaptics = viewModel::toggleHaptics,
            onToggleColorblind = viewModel::toggleColorblind
        )
    }
}

@Composable
private fun GamePlayfield(
    state: BubbleGameState,
    progress: PlayerProgress,
    onAimUpdate: (Float, Float, Float, Float) -> Unit,
    onAimCancel: () -> Unit,
    onShoot: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val stars = remember {
        val rng = Random(99)
        val palette = listOf(Color.White, CyanNeon, Color(0xFFA855F7))
        List(48) {
            StarSpec(
                normX = rng.nextFloat(),
                normY = rng.nextFloat(),
                radiusFactor = 0.8f + rng.nextFloat() * 1.4f,
                alpha = 0.12f + rng.nextFloat() * 0.25f,
                color = palette[rng.nextInt(palette.size)]
            )
        }
    }
    val canvasDescription = stringResource(R.string.cd_game_canvas)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val maxBoardWidthPx = with(density) { 520.dp.toPx() }
        val effectiveWidth = min(canvasW, maxBoardWidthPx)
        val horizontalScale = (effectiveWidth * 0.92f) / BubbleGameEngine.BOARD_WIDTH
        val minLogicalHeight = 23.2f
        val verticalScale = canvasH / minLogicalHeight
        val rPx = min(horizontalScale, verticalScale).coerceAtLeast(1f)
        val originX = (canvasW - BubbleGameEngine.BOARD_WIDTH * rPx) / 2f
        val originY = max(rPx * 0.50f, (canvasH - minLogicalHeight * rPx) * 0.20f)
        val launcherBoardX = BubbleGameEngine.BOARD_WIDTH / 2f
        val launcherBoardY = max(
            BubbleGameEngine.DANGER_Y + 3.0f,
            (canvasH - originY) / rPx - 2.3f
        )

        val metrics = remember(rPx, originX, originY, launcherBoardX, launcherBoardY) {
            BoardMetrics(
                rPx = rPx,
                originX = originX,
                originY = originY,
                launcherBoardX = launcherBoardX,
                launcherBoardY = launcherBoardY
            )
        }

        val currentMetrics by rememberUpdatedState(metrics)
        val currentStatus by rememberUpdatedState(state.status)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_canvas")
                .semantics { contentDescription = canvasDescription }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (currentStatus != GameStatus.PLAYING) return@awaitEachGesture

                        val m = currentMetrics
                        val startBoard = m.toBoard(down.position.x, down.position.y)
                        onAimUpdate(startBoard.x, startBoard.y, m.launcherBoardX, m.launcherBoardY)
                        down.consume()

                        var pointerId = down.id
                        var completedNormally = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val activeChange = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()

                            if (activeChange == null) {
                                onAimCancel()
                                break
                            }

                            if (!activeChange.pressed) {
                                completedNormally = !activeChange.isConsumed
                                activeChange.consume()
                                break
                            }

                            pointerId = activeChange.id
                            val latestMetrics = currentMetrics
                            val boardPt = latestMetrics.toBoard(activeChange.position.x, activeChange.position.y)
                            onAimUpdate(boardPt.x, boardPt.y, latestMetrics.launcherBoardX, latestMetrics.launcherBoardY)
                            activeChange.consume()
                        }

                        if (completedNormally && currentStatus == GameStatus.PLAYING) {
                            val latestMetrics = currentMetrics
                            onShoot(latestMetrics.launcherBoardX, latestMetrics.launcherBoardY)
                        } else {
                            onAimCancel()
                        }
                    }
                }
        ) {
            // Subtle damped screen shake on explosions/mega pops
            val shakeMagnitude = state.screenShake * 6.dp.toPx()
            val shakeX = if (shakeMagnitude > 0.1f) sin(state.screenShake * 55f) * shakeMagnitude else 0f
            val shakeY = if (shakeMagnitude > 0.1f) sin(state.screenShake * 43f) * shakeMagnitude * 0.7f else 0f

            translate(left = shakeX, top = shakeY) {
                for (star in stars) {
                    drawCircle(
                        color = star.color.copy(alpha = star.alpha),
                        radius = star.radiusFactor * density.density,
                        center = Offset(star.normX * size.width, star.normY * size.height)
                    )
                }

                val leftWallX = metrics.originX
                val rightWallX = metrics.originX + BubbleGameEngine.BOARD_WIDTH * metrics.rPx
                val topWallY = metrics.originY
                val launcherPos = metrics.toScreen(metrics.launcherBoardX, metrics.launcherBoardY)

                // Playfield wall rails
                drawLine(
                    color = CyanNeon.copy(alpha = 0.16f),
                    start = Offset(leftWallX, topWallY),
                    end = Offset(leftWallX, launcherPos.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = CyanNeon.copy(alpha = 0.16f),
                    start = Offset(rightWallX, topWallY),
                    end = Offset(rightWallX, launcherPos.y),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = CyanNeon.copy(alpha = 0.25f),
                    start = Offset(leftWallX, topWallY),
                    end = Offset(rightWallX, topWallY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Danger Threshold Line
                val dangerY = metrics.originY + BubbleGameEngine.DANGER_Y * metrics.rPx
                drawLine(
                    color = DangerLineRed,
                    start = Offset(leftWallX, dangerY),
                    end = Offset(rightWallX, dangerY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                        0f
                    )
                )

                val activeOrb = BubbleGameEngine.activeDisplayOrb(state)

                // Multi-bounce Ricochet Laser Sight
                if (state.isAiming && state.bullet == null && state.status == GameStatus.PLAYING) {
                    drawRicochetTrajectory(
                        trajectory = state.aimTrajectory,
                        toScreen = metrics::toScreen,
                        radiusPx = metrics.rPx,
                        isFireball = activeOrb is OrbType.Fireball
                    )
                }

                // Grid Cells (Normal, Ice, Stone)
                for (r in state.grid.indices) {
                    val row = state.grid[r]
                    for (c in row.indices) {
                        val content = row[c] ?: continue
                        val (bx, by) = BubbleGameEngine.cellCenter(r, c)
                        drawCellContent(
                            center = metrics.toScreen(bx, by),
                            radius = metrics.rPx,
                            content = content,
                            colorblindEnabled = progress.colorblindEnabled
                        )
                    }
                }

                // Shockwave Rings
                for (wave in state.shockwaves) {
                    val center = metrics.toScreen(wave.x, wave.y)
                    val radius = (0.8f + wave.maxRadius * wave.progress) * metrics.rPx
                    drawCircle(
                        color = wave.color.copy(alpha = (1f - wave.progress).coerceIn(0f, 1f) * 0.7f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = (4.dp.toPx() * (1f - wave.progress)).coerceAtLeast(1f))
                    )
                }

                // Active Projectile
                state.bullet?.let { bullet ->
                    drawOrb(
                        center = metrics.toScreen(bullet.x, bullet.y),
                        radius = metrics.rPx,
                        orb = bullet.orb,
                        colorblindEnabled = progress.colorblindEnabled
                    )
                }

                // Launcher Turret + Chambered Orb + Fireball Ring
                drawLauncherTurret(
                    launcherPos = launcherPos,
                    radiusPx = metrics.rPx,
                    aimAngle = state.aimAngle,
                    superCharge = state.superCharge,
                    activeOrb = activeOrb,
                    bulletInFlight = state.bullet != null,
                    colorblindEnabled = progress.colorblindEnabled
                )

                // Burst Particles
                for (particle in state.particles) {
                    val pos = metrics.toScreen(particle.x, particle.y)
                    val pRadius = (0.17f * metrics.rPx * particle.life).coerceAtLeast(1f)
                    drawCircle(
                        color = particle.color.copy(alpha = particle.life.coerceIn(0f, 1f)),
                        radius = pRadius,
                        center = pos
                    )
                }

                // Falling Orphan Bubbles
                for (fb in state.fallingBubbles) {
                    drawCellContent(
                        center = metrics.toScreen(fb.x, fb.y),
                        radius = metrics.rPx,
                        content = fb.content,
                        colorblindEnabled = progress.colorblindEnabled
                    )
                }

                // Floating Score / Combo Popups
                drawFloatingPopups(
                    popups = state.popups,
                    toScreen = metrics::toScreen,
                    densityScale = density.density
                )
            }
        }
    }
}
