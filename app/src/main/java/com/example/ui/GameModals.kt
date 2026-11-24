package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BoosterKind
import com.example.data.PlayerProgress
import com.example.game.BubbleGameState
import com.example.game.GameMode
import com.example.game.GameOverReason
import com.example.game.GameStatus
import com.example.ui.theme.AmberLabel
import com.example.ui.theme.AmberValue
import com.example.ui.theme.CyanLabel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GameOverPink
import com.example.ui.theme.GameOverRed
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudGlassBg
import com.example.ui.theme.ModalBorderPurple
import com.example.ui.theme.ModalGlassBg
import com.example.ui.theme.ModalGlowPurple
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NextSectorEmerald
import com.example.ui.theme.NextSectorTeal
import com.example.ui.theme.PinkScore
import com.example.ui.theme.PlayAgainBlue
import com.example.ui.theme.PlayAgainCyan
import com.example.ui.theme.ScoreBoxBg
import com.example.ui.theme.ScoreBoxBorder
import com.example.ui.theme.SlateText
import com.example.ui.theme.SpaceCenter
import com.example.ui.theme.VictoryCyan
import com.example.ui.theme.VictoryEmerald

@Composable
fun GameTopHud(
    state: BubbleGameState,
    onPauseClick: () -> Unit
) {
    Surface(
        color = HudGlassBg,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = HudBorder,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = stringResource(R.string.cd_pause_game),
                            tint = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = if (state.mode == GameMode.SECTOR) {
                                stringResource(R.string.hud_sector_label, state.sectorNumber)
                            } else {
                                stringResource(R.string.hud_endless_label)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanLabel
                        )
                        Text(
                            text = if (state.mode == GameMode.SECTOR) state.sectorTitle else "Wave ${state.endlessWave}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.testTag("sector_display")
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.hud_score_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = PinkScore
                    )
                    Text(
                        text = state.score.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = PinkScore,
                        modifier = Modifier.testTag("score_display")
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    val isMovesLow = state.mode == GameMode.SECTOR && state.movesLeft <= 5
                    Text(
                        text = if (state.mode == GameMode.SECTOR) {
                            stringResource(R.string.hud_moves_label)
                        } else {
                            stringResource(R.string.hud_drop_in_label)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMovesLow) GameOverRed else AmberLabel
                    )
                    Text(
                        text = if (state.mode == GameMode.SECTOR) {
                            state.movesLeft.toString()
                        } else {
                            state.shotsUntilDrop.toString()
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isMovesLow) GameOverRed else AmberValue,
                        modifier = Modifier.testTag("moves_display")
                    )
                }
            }

            // Star progress bar (Sector mode) + Combo Pill
            if (state.mode == GameMode.SECTOR) {
                val starRatio = (state.score.toFloat() / state.star3Threshold.coerceAtLeast(1)).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { starRatio },
                        color = AmberLabel,
                        trackColor = SpaceCenter,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    val liveStars = when {
                        state.score >= state.star3Threshold -> 3
                        state.score >= state.star2Threshold -> 2
                        state.score >= state.star1Threshold -> 1
                        else -> 0
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = if (i <= liveStars) AmberLabel else MutedSlate.copy(alpha = 0.3f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (state.combo >= 2) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFB923C).copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color(0xFFFB923C))
                ) {
                    Text(
                        text = stringResource(R.string.hud_combo_badge, state.combo),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFFFED7AA),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomControlDock(
    state: BubbleGameState,
    progress: PlayerProgress,
    onSwapOrbs: () -> Unit,
    onToggleBooster: (BoosterKind) -> Unit
) {
    Surface(
        color = HudGlassBg,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = HudBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Next Orb + Swap Action
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SpaceCenter,
                    border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onSwapOrbs)
                        .testTag("swap_orb_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(28.dp)) {
                            state.nextOrb?.let { orb ->
                                drawOrb(
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.minDimension * 0.44f,
                                    orb = orb,
                                    colorblindEnabled = progress.colorblindEnabled
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(R.string.cd_swap_bubble),
                            tint = CyanLabel,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 3 Power-up Booster Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoosterChip(
                        label = stringResource(R.string.booster_bomb),
                        count = progress.bombCount,
                        icon = Icons.Rounded.LocalFireDepartment,
                        accent = GameOverRed,
                        armed = state.armedBooster == BoosterKind.BOMB,
                        onClick = { onToggleBooster(BoosterKind.BOMB) },
                        modifier = Modifier.testTag("booster_bomb_button")
                    )
                    BoosterChip(
                        label = stringResource(R.string.booster_lightning),
                        count = progress.lightningCount,
                        icon = Icons.Rounded.Bolt,
                        accent = CyanNeon,
                        armed = state.armedBooster == BoosterKind.LIGHTNING,
                        onClick = { onToggleBooster(BoosterKind.LIGHTNING) },
                        modifier = Modifier.testTag("booster_lightning_button")
                    )
                    BoosterChip(
                        label = stringResource(R.string.booster_wild),
                        count = progress.wildCount,
                        icon = Icons.Rounded.AutoAwesome,
                        accent = Color(0xFFA855F7),
                        armed = state.armedBooster == BoosterKind.WILD,
                        onClick = { onToggleBooster(BoosterKind.WILD) },
                        modifier = Modifier.testTag("booster_wild_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun BoosterChip(
    label: String,
    count: Int,
    icon: ImageVector,
    accent: Color,
    armed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = count > 0 || armed
    val border = when {
        armed -> Color.White
        enabled -> accent.copy(alpha = 0.55f)
        else -> MutedSlate.copy(alpha = 0.25f)
    }
    val bg = when {
        armed -> accent.copy(alpha = 0.30f)
        enabled -> SpaceCenter
        else -> SpaceCenter.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(if (armed) 2.dp else 1.dp, border),
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) accent else MutedSlate.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = if (enabled) Color.White else MutedSlate.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun GameOverlays(
    state: BubbleGameState,
    progress: PlayerProgress,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onNextSector: () -> Unit,
    onOpenSectorMap: () -> Unit,
    onExitToHub: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleColorblind: () -> Unit
) {
    val visible = state.status != GameStatus.PLAYING

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = state.status == GameStatus.PAUSED,
                enter = scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ModalCardShell {
                    Text(
                        text = stringResource(R.string.pause_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = CyanNeon
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickToggleChip(
                            text = stringResource(R.string.setting_sound),
                            active = progress.soundEnabled,
                            onClick = onToggleSound,
                            modifier = Modifier.weight(1f)
                        )
                        QuickToggleChip(
                            text = stringResource(R.string.setting_haptics),
                            active = progress.hapticsEnabled,
                            onClick = onToggleHaptics,
                            modifier = Modifier.weight(1f)
                        )
                        QuickToggleChip(
                            text = stringResource(R.string.setting_colorblind),
                            active = progress.colorblindEnabled,
                            onClick = onToggleColorblind,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GradientActionButton(
                        text = stringResource(R.string.pause_resume).uppercase(),
                        gradient = Brush.horizontalGradient(listOf(PlayAgainCyan, PlayAgainBlue)),
                        onClick = onResume,
                        modifier = Modifier.testTag("pause_resume_button")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRestart,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pause_restart_button")
                        ) {
                            Text(stringResource(R.string.pause_restart), color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onExitToHub,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pause_exit_button")
                        ) {
                            Text(stringResource(R.string.pause_exit), color = SlateText)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.status == GameStatus.SECTOR_CLEARED,
                enter = scaleIn(initialScale = 0.90f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ModalCardShell {
                    Text(
                        text = stringResource(R.string.sector_cleared_title),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 24.sp,
                            brush = Brush.horizontalGradient(listOf(VictoryEmerald, VictoryCyan))
                        ),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = stringResource(R.string.cd_victory_star),
                                tint = if (i <= state.earnedStars) AmberLabel else MutedSlate.copy(alpha = 0.28f),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                    ModalScoreBox(
                        label = stringResource(R.string.hud_score_label),
                        score = state.score,
                        scoreColor = PinkScore
                    )
                    if (state.movesBonus > 0) {
                        Text(
                            text = stringResource(R.string.moves_bonus_label, state.movesBonus),
                            style = MaterialTheme.typography.bodyMedium,
                            color = VictoryEmerald
                        )
                    }
                    if (state.rewardBooster != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AmberLabel.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, AmberLabel)
                        ) {
                            Text(
                                text = stringResource(R.string.booster_reward_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberValue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (state.sectorNumber < PlayerProgress.TOTAL_SECTORS) {
                        GradientActionButton(
                            text = stringResource(R.string.next_sector_button),
                            gradient = Brush.horizontalGradient(listOf(NextSectorEmerald, NextSectorTeal)),
                            onClick = onNextSector,
                            modifier = Modifier.testTag("next_sector_button")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRestart,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.pause_restart), color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onOpenSectorMap,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(stringResource(R.string.sector_map_button), color = SlateText)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.status == GameStatus.GAME_OVER,
                enter = scaleIn(initialScale = 0.90f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                val reasonText = when (state.gameOverReason) {
                    GameOverReason.CROSSED_DANGER_LINE -> stringResource(R.string.game_over_reason_danger)
                    GameOverReason.OUT_OF_MOVES -> stringResource(R.string.game_over_reason_moves)
                }
                ModalCardShell {
                    Text(
                        text = if (state.mode == GameMode.ENDLESS) {
                            stringResource(R.string.endless_over_title)
                        } else {
                            stringResource(R.string.game_over_title)
                        },
                        style = MaterialTheme.typography.displaySmall.copy(
                            brush = Brush.horizontalGradient(listOf(GameOverRed, GameOverPink))
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = reasonText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateText,
                        textAlign = TextAlign.Center
                    )
                    if (state.isNewBest) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AmberLabel.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, AmberLabel)
                        ) {
                            Text(
                                text = stringResource(R.string.new_best_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberValue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                    ModalScoreBox(
                        label = stringResource(R.string.final_score_label),
                        score = state.score,
                        scoreColor = AmberLabel
                    )
                    GradientActionButton(
                        text = stringResource(R.string.play_again_button),
                        gradient = Brush.horizontalGradient(listOf(PlayAgainCyan, PlayAgainBlue)),
                        onClick = onRestart,
                        modifier = Modifier.testTag("play_again_button")
                    )
                    OutlinedButton(
                        onClick = onExitToHub,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MutedSlate.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.pause_exit), color = SlateText)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToggleChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) CyanNeon.copy(alpha = 0.16f) else SpaceCenter,
        border = BorderStroke(1.dp, if (active) CyanNeon else MutedSlate.copy(alpha = 0.3f)),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (active) CyanLabel else MutedSlate
            )
        }
    }
}

@Composable
private fun ModalCardShell(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ModalGlassBg,
        border = BorderStroke(1.dp, ModalBorderPurple),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ModalGlowPurple,
                spotColor = ModalGlowPurple
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ModalScoreBox(
    label: String,
    score: Int,
    scoreColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScoreBoxBg, RoundedCornerShape(16.dp))
            .border(1.dp, ScoreBoxBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MutedSlate
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = scoreColor
        )
    }
}

@Composable
private fun GradientActionButton(
    text: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(brush = gradient, shape = shape)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White
        )
    }
}
