package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProgress
import com.example.ui.theme.AmberLabel
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.CyanLabel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HudBorder
import com.example.ui.theme.ModalBorderPurple
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NextSectorEmerald
import com.example.ui.theme.PinkScore
import com.example.ui.theme.PlayAgainBlue
import com.example.ui.theme.PlayAgainCyan
import com.example.ui.theme.SlateText
import com.example.ui.theme.SpaceCenter
import com.example.ui.theme.SpaceEdge
import kotlin.math.max

@Composable
fun HomeScreen(
    progress: PlayerProgress,
    onPlaySector: (Int) -> Unit,
    onOpenSectorMap: () -> Unit,
    onPlayEndless: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleColorblind: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicVoid)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(SpaceCenter, SpaceEdge),
                        center = Offset(size.width / 2f, size.height * 0.3f),
                        radius = max(size.width, size.height) * 0.85f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 580.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeroBannerCard(progress = progress)

                StatsStrip(progress = progress)

                // Primary Play Next Sector CTA
                val targetSector = progress.highestUnlockedSector
                val ctaShape = RoundedCornerShape(20.dp)
                Button(
                    onClick = { onPlaySector(targetSector) },
                    shape = ctaShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(PlayAgainCyan, PlayAgainBlue)),
                            shape = ctaShape
                        )
                        .testTag("play_next_sector_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_play_sector, targetSector).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    )
                }

                // Dual Mode Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeActionCard(
                        title = stringResource(R.string.home_sector_map),
                        subtitle = "${progress.highestUnlockedSector}/${PlayerProgress.TOTAL_SECTORS} Unlocked",
                        icon = Icons.Rounded.Explore,
                        accent = Color(0xFFA855F7),
                        onClick = onOpenSectorMap,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sector_map_button")
                    )

                    ModeActionCard(
                        title = stringResource(R.string.home_endless_arcade),
                        subtitle = "Best: ${progress.bestEndlessScore}",
                        icon = Icons.Rounded.AllInclusive,
                        accent = NextSectorEmerald,
                        onClick = onPlayEndless,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("endless_mode_button")
                    )
                }

                // Quick Accessibility & Audio Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingPillToggle(
                        label = stringResource(R.string.setting_sound),
                        icon = if (progress.soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        active = progress.soundEnabled,
                        onClick = onToggleSound,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_sound_button")
                    )
                    SettingPillToggle(
                        label = stringResource(R.string.setting_haptics),
                        icon = Icons.Rounded.Vibration,
                        active = progress.hapticsEnabled,
                        onClick = onToggleHaptics,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_haptics_button")
                    )
                    SettingPillToggle(
                        label = stringResource(R.string.setting_colorblind),
                        icon = Icons.Rounded.Category,
                        active = progress.colorblindEnabled,
                        onClick = onToggleColorblind,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_colorblind_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBannerCard(progress: PlayerProgress) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SpaceCenter,
        border = BorderStroke(1.dp, ModalBorderPurple),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cosmic_hero_banner_1795521127673),
                contentDescription = stringResource(R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to CosmicVoid.copy(alpha = 0.35f),
                            1.0f to CosmicVoid.copy(alpha = 0.92f)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 30.sp,
                        brush = Brush.horizontalGradient(listOf(CyanNeon, PinkScore, AmberLabel))
                    )
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateText
                )
            }
        }
    }
}

@Composable
private fun StatsStrip(progress: PlayerProgress) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SpaceCenter.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, HudBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(
                label = stringResource(R.string.home_stars_label),
                value = "${progress.totalStars}/${progress.maxPossibleStars}",
                valueColor = AmberLabel
            )
            StatItem(
                label = stringResource(R.string.home_best_score_label),
                value = max(progress.bestSectorScore, progress.bestEndlessScore).toString(),
                valueColor = PinkScore
            )
            StatItem(
                label = stringResource(R.string.home_sectors_label),
                value = "${progress.highestUnlockedSector}/${PlayerProgress.TOTAL_SECTORS}",
                valueColor = CyanLabel
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor
        )
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SpaceCenter.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.16f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = MutedSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingPillToggle(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (active) CyanNeon.copy(alpha = 0.6f) else MutedSlate.copy(alpha = 0.25f)
    val contentColor = if (active) CyanLabel else MutedSlate
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) CyanNeon.copy(alpha = 0.12f) else SpaceCenter.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
