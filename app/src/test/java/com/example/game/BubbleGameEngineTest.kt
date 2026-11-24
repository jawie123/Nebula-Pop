package com.example.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BubbleGameEngineTest {

    @Test
    fun startSector_initializesExpectedGridAndMoves() {
        val state = BubbleGameEngine.startSector(1, Random(1))
        assertEquals(GameMode.SECTOR, state.mode)
        assertEquals(1, state.sectorNumber)
        assertEquals(0, state.score)
        assertEquals(25, state.movesLeft)
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(BubbleGameEngine.ROWS, state.grid.size)
        assertEquals(8, state.grid[0].size)
        assertEquals(7, state.grid[1].size)
        assertNotNull(state.currentOrb)
        assertNotNull(state.nextOrb)
    }

    @Test
    fun shootAndMatchThree_awardsScoreClearsClusterAndDropsOrphans() {
        val emptyGrid = MutableList(BubbleGameEngine.ROWS) { r ->
            MutableList<CellContent?>(BubbleGameEngine.colsInRow(r)) { null }
        }
        emptyGrid[0][3] = CellContent.Normal(BubbleColor.CYAN)
        emptyGrid[0][4] = CellContent.Normal(BubbleColor.CYAN)
        emptyGrid[1][2] = CellContent.Normal(BubbleColor.ROSE)

        val state = BubbleGameState(
            mode = GameMode.SECTOR,
            sectorNumber = 1,
            score = 0,
            movesLeft = 10,
            star1Threshold = 1000,
            star2Threshold = 2000,
            star3Threshold = 3000,
            grid = emptyGrid.map { it.toList() },
            currentOrb = OrbType.Normal(BubbleColor.CYAN),
            nextOrb = OrbType.Normal(BubbleColor.GOLD),
            aimAngle = (-Math.PI / 2.0).toFloat()
        )

        val (shotState, _) = BubbleGameEngine.shoot(state, launcherX = 8f, launcherY = 20f, random = Random(2))
        assertEquals(9, shotState.movesLeft)
        assertNotNull(shotState.bullet)

        var sim = shotState
        repeat(60) {
            sim = BubbleGameEngine.step(sim, 0.016f, Random(2)).state
        }

        // 3 cyan popped (450) + 1 orphan rose dropped (100) + moves bonus (9 * 100 = 900) = 1450
        assertNull(sim.bullet)
        assertEquals(1450, sim.score)
        assertEquals(GameStatus.SECTOR_CLEARED, sim.status)
        assertEquals(1, sim.earnedStars)
        assertTrue(sim.fallingBubbles.isNotEmpty())
    }

    @Test
    fun swapOrbs_exchangesCurrentAndNext() {
        val state = BubbleGameState(
            currentOrb = OrbType.Normal(BubbleColor.CYAN),
            nextOrb = OrbType.Normal(BubbleColor.ROSE)
        )
        val swapped = BubbleGameEngine.swapOrbs(state)
        assertEquals(OrbType.Normal(BubbleColor.ROSE), swapped.currentOrb)
        assertEquals(OrbType.Normal(BubbleColor.CYAN), swapped.nextOrb)
    }

    @Test
    fun ricochetTrajectory_bouncesOffSideWall() {
        val emptyGrid = List(BubbleGameEngine.ROWS) { r ->
            List<CellContent?>(BubbleGameEngine.colsInRow(r)) { null }
        }
        // Aim up-left toward left wall
        val angle = (-Math.PI * 0.78).toFloat()
        val trajectory = BubbleGameEngine.computeAimTrajectory(
            grid = emptyGrid,
            launcherX = 8f,
            launcherY = 20f,
            angle = angle
        )
        assertTrue(trajectory.points.size >= 3)
        assertNotNull(trajectory.impactPoint)
    }
}
