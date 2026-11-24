package com.example.game

import kotlin.math.abs

data class SectorSpec(
    val number: Int,
    val title: String,
    val zoneName: String,
    val moves: Int,
    val star1Score: Int,
    val star2Score: Int,
    val star3Score: Int,
    val grid: List<List<CellContent?>>
)

object SectorLevels {

    fun getSectorSpec(sectorNumber: Int): SectorSpec {
        val s = sectorNumber.coerceIn(1, 20)
        val moves = when {
            s <= 5 -> 26 - s
            s <= 10 -> 24 - (s - 5)
            s <= 15 -> 23 - (s - 10)
            else -> 21 - (s - 15)
        }.coerceAtLeast(16)

        val grid = buildPatternGrid(s)
        val bubbleCount = grid.sumOf { row -> row.count { it != null } }
        val baseEstimate = bubbleCount * 140
        val star1 = (baseEstimate * 0.55f).toInt().coerceAtLeast(1200)
        val star2 = (baseEstimate * 0.95f).toInt().coerceAtLeast(2400)
        val star3 = (baseEstimate * 1.35f).toInt().coerceAtLeast(3600)

        return SectorSpec(
            number = s,
            title = sectorTitle(s),
            zoneName = zoneName(s),
            moves = moves,
            star1Score = star1,
            star2Score = star2,
            star3Score = star3,
            grid = grid
        )
    }

    fun zoneName(sector: Int): String = when (sector) {
        in 1..5 -> "Orion Rim"
        in 6..10 -> "Plasma Belt"
        in 11..15 -> "Asteroid Rift"
        else -> "Singularity Core"
    }

    fun sectorTitle(sector: Int): String = when (sector) {
        1 -> "Orion Gate"
        2 -> "Twin Moons"
        3 -> "Prism Wave"
        4 -> "Solar Crown"
        5 -> "Starfall Bridge"
        6 -> "Cryo Halo"
        7 -> "Glacier Wing"
        8 -> "Frost Matrix"
        9 -> "Aurora Spire"
        10 -> "Plasma Vault"
        11 -> "Iron Sentinel"
        12 -> "Meteor Arch"
        13 -> "Rift Pillars"
        14 -> "Basalt Diamond"
        15 -> "Titan Bastion"
        16 -> "Warp Vortex"
        17 -> "Nebula Crown"
        18 -> "Quantum Cross"
        19 -> "Eclipse Citadel"
        else -> "Singularity Heart"
    }

    private fun buildPatternGrid(sector: Int): List<List<CellContent?>> {
        val colors = BubbleColor.entries
        val colorCount = if (sector <= 2) 4 else 5
        val rows = BubbleGameEngine.ROWS
        val activeColors = colors.take(colorCount)

        return List(rows) { r ->
            val cols = BubbleGameEngine.colsInRow(r)
            List(cols) { c ->
                val center = (cols - 1) / 2f
                val distFromCenter = abs(c - center)

                when (sector) {
                    // Zone 1: Orion Rim (Classic clearable geometric patterns)
                    1 -> if (r < 4) CellContent.Normal(activeColors[(c / 2 + r) % colorCount]) else null
                    2 -> if (r < 5 && distFromCenter <= 3.6f - r * 0.45f) {
                        CellContent.Normal(activeColors[(r + c / 2) % colorCount])
                    } else null
                    3 -> if (r < 5 && ((r + c) % 2 == 0 || r == 0)) {
                        CellContent.Normal(activeColors[(r * 2 + c / 2) % colorCount])
                    } else null
                    4 -> if (r < 5 && (r == 0 || distFromCenter >= 1.2f || r == 4)) {
                        CellContent.Normal(activeColors[(c + r) % colorCount])
                    } else null
                    5 -> if (r < 6 && (r == 0 || (c % 3 != 1) || r >= 4)) {
                        CellContent.Normal(activeColors[(r + c) % colorCount])
                    } else null

                    // Zone 2: Plasma Belt (Introduces Ice-Locked Orbs)
                    6 -> if (r < 5) {
                        val color = activeColors[(c / 2 + r) % colorCount]
                        if (r == 3 && c in 1..(cols - 2)) CellContent.Ice(color)
                        else CellContent.Normal(color)
                    } else null
                    7 -> if (r < 6 && distFromCenter <= 3.5f - abs(r - 2) * 0.5f) {
                        val color = activeColors[(r + c) % colorCount]
                        if (r in 2..3 && distFromCenter < 1.6f) CellContent.Ice(color)
                        else CellContent.Normal(color)
                    } else null
                    8 -> if (r < 6 && (r == 0 || (r + c) % 2 == 1)) {
                        val color = activeColors[(c / 2 + r * 2) % colorCount]
                        if (r >= 3) CellContent.Ice(color) else CellContent.Normal(color)
                    } else null
                    9 -> if (r < 6 && (r == 0 || distFromCenter <= 2.6f || r == 5)) {
                        val color = activeColors[(r + c / 2) % colorCount]
                        if (r == 2 || r == 4) CellContent.Ice(color) else CellContent.Normal(color)
                    } else null
                    10 -> if (r < 6) {
                        val color = activeColors[(r * 2 + c) % colorCount]
                        if (r in 3..4 && (c % 2 == 0)) CellContent.Ice(color)
                        else CellContent.Normal(color)
                    } else null

                    // Zone 3: Asteroid Rift (Introduces Stone Asteroids to drop or blast)
                    11 -> if (r < 6) {
                        if (r == 3 && (c == 1 || c == cols - 2)) CellContent.Stone
                        else CellContent.Normal(activeColors[(r + c / 2) % colorCount])
                    } else null
                    12 -> if (r < 6 && (r == 0 || distFromCenter >= 0.8f)) {
                        if (r == 4 && distFromCenter in 1.2f..2.6f) CellContent.Stone
                        else CellContent.Normal(activeColors[(c + r * 2) % colorCount])
                    } else null
                    13 -> if (r < 6 && (r == 0 || c == 0 || c == cols - 1 || abs(c - center) <= 1.2f)) {
                        if (r == 4 && abs(c - center) <= 0.6f) CellContent.Stone
                        else CellContent.Normal(activeColors[(r + c) % colorCount])
                    } else null
                    14 -> if (r < 7 && distFromCenter <= 3.6f - abs(r - 2.5f) * 0.6f) {
                        if (r == 3 && abs(c - center) <= 1.1f) CellContent.Stone
                        else CellContent.Normal(activeColors[(r + c / 2) % colorCount])
                    } else null
                    15 -> if (r < 6) {
                        if (r == 3 && (c == 2 || c == cols - 3)) CellContent.Stone
                        else if (r == 4 && (c == 1 || c == cols - 2)) CellContent.Ice(activeColors[c % colorCount])
                        else CellContent.Normal(activeColors[(r * 2 + c) % colorCount])
                    } else null

                    // Zone 4: Singularity Core (Master hybrid puzzles)
                    16 -> if (r < 6 && (r == 0 || (r + c) % 2 == 0 || r == 5)) {
                        when {
                            r == 4 && abs(c - center) <= 1.0f -> CellContent.Stone
                            r >= 3 -> CellContent.Ice(activeColors[(r + c) % colorCount])
                            else -> CellContent.Normal(activeColors[(c / 2 + r) % colorCount])
                        }
                    } else null
                    17 -> if (r < 6) {
                        when {
                            r == 2 && (c == 0 || c == cols - 1) -> CellContent.Stone
                            r == 4 && abs(c - center) <= 1.6f -> CellContent.Ice(activeColors[c % colorCount])
                            else -> CellContent.Normal(activeColors[(r + c) % colorCount])
                        }
                    } else null
                    18 -> if (r < 7 && (r == 0 || abs(c - center) <= 1.1f || r == 3)) {
                        when {
                            r == 3 && (c == 0 || c == cols - 1) -> CellContent.Stone
                            r >= 4 -> CellContent.Ice(activeColors[(r + c) % colorCount])
                            else -> CellContent.Normal(activeColors[(c + r * 2) % colorCount])
                        }
                    } else null
                    19 -> if (r < 7 && distFromCenter <= 3.8f - r * 0.2f) {
                        when {
                            r == 3 && (c == 1 || c == cols - 2) -> CellContent.Stone
                            r == 4 && distFromCenter <= 1.5f -> CellContent.Ice(activeColors[(r + c) % colorCount])
                            else -> CellContent.Normal(activeColors[(r * 2 + c / 2) % colorCount])
                        }
                    } else null
                    else -> if (r < 7) {
                        when {
                            r == 3 && (c == 1 || c == cols - 2 || abs(c - center) < 0.6f) -> CellContent.Stone
                            r == 5 && c in 1..(cols - 2) -> CellContent.Ice(activeColors[(r + c) % colorCount])
                            else -> CellContent.Normal(activeColors[(r + c / 2) % colorCount])
                        }
                    } else null
                }
            }
        }
    }
}
