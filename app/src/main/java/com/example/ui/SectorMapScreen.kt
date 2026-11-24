package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProgress
import com.example.game.SectorLevels
import com.example.ui.theme.AmberLabel
import com.example.ui.theme.CosmicVoid
import com.example.ui.theme.CyanLabel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudGlassBg
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NextSectorEmerald
import com.example.ui.theme.PinkScore
import com.example.ui.theme.SpaceCenter
import com.example.ui.theme.SpaceEdge
import kotlin.math.max

@Composable
fun SectorMapScreen(
    progress: PlayerProgress,
    onSelectSector: (Int) -> Unit,
    onBack: () -> Unit
) {
    val sectors = (1..PlayerProgress.TOTAL_SECTORS).toList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicVoid)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(SpaceCenter, SpaceEdge),
                        center = Offset(size.width / 2f, size.height * 0.4f),
                        radius = max(size.width, size.height) * 0.85f
                    )
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("map_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.map_title).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.map_subtitle),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = MutedSlate
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = AmberLabel,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${progress.totalStars}/${progress.maxPossibleStars}",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberLabel
                        )
                    }
                }
            }

            // Grid of 20 Sectors
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 96.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    items(sectors, key = { it }) { sector ->
                        val unlocked = sector <= progress.highestUnlockedSector
                        val stars = progress.sectorStars[sector] ?: 0
                        val zoneAccent = when (sector) {
                            in 1..5 -> CyanNeon
                            in 6..10 -> Color(0xFFA855F7)
                            in 11..15 -> PinkScore
                            else -> NextSectorEmerald
                        }
                        SectorTileCard(
                            sectorNumber = sector,
                            title = SectorLevels.sectorTitle(sector),
                            unlocked = unlocked,
                            stars = stars,
                            isCurrent = sector == progress.highestUnlockedSector,
                            accent = zoneAccent,
                            onClick = { if (unlocked) onSelectSector(sector) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorTileCard(
    sectorNumber: Int,
    title: String,
    unlocked: Boolean,
    stars: Int,
    isCurrent: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCurrent -> accent
        unlocked -> accent.copy(alpha = 0.45f)
        else -> MutedSlate.copy(alpha = 0.22f)
    }
    val bgColor = when {
        isCurrent -> accent.copy(alpha = 0.16f)
        unlocked -> SpaceCenter.copy(alpha = 0.90f)
        else -> SpaceEdge.copy(alpha = 0.75f)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .height(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = unlocked, onClick = onClick)
            .testTag("sector_tile_$sectorNumber")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (unlocked) {
                Text(
                    text = sectorNumber.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isCurrent) CyanLabel else Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = stringResource(R.string.cd_locked_sector),
                    tint = MutedSlate.copy(alpha = 0.6f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = if (unlocked) Color.White.copy(alpha = 0.85f) else MutedSlate.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..3) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (i <= stars) AmberLabel else MutedSlate.copy(alpha = 0.25f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
