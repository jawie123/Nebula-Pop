package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.BoosterKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class EngineStepResult(
    val state: BubbleGameState,
    val sounds: List<GameSoundEvent> = emptyList(),
    val triggerHapticImpact: Boolean = false
)

object BubbleGameEngine {
    const val COLS = 8
    const val ROWS = 11
    const val BOARD_WIDTH = 16f
    const val ROW_SPACING_Y = 1.73f
    const val DANGER_Y = 1f + (ROWS - 2) * ROW_SPACING_Y
    private const val BULLET_SPEED = 52f
    private const val MIN_AIM_ANGLE = (-PI + 0.16).toFloat()
    private const val MAX_AIM_ANGLE = (-0.16).toFloat()

    fun colsInRow(r: Int): Int = if (r % 2 == 1) COLS - 1 else COLS

    fun cellCenter(r: Int, c: Int): Pair<Float, Float> {
        val isOffset = (r % 2 == 1)
        val xOffset = if (isOffset) 1f else 0f
        return (1f + xOffset + c * 2f) to (1f + r * ROW_SPACING_Y)
    }

    fun startSector(sectorNumber: Int, random: Random = Random.Default): BubbleGameState {
        val spec = SectorLevels.getSectorSpec(sectorNumber)
        val firstColor = randomAvailableColor(spec.grid, random)
        val secondColor = randomAvailableColor(spec.grid, random)
        return BubbleGameState(
            mode = GameMode.SECTOR,
            sectorNumber = spec.number,
            sectorTitle = spec.title,
            score = 0,
            movesLeft = spec.moves,
            star1Threshold = spec.star1Score,
            star2Threshold = spec.star2Score,
            star3Threshold = spec.star3Score,
            status = GameStatus.PLAYING,
            grid = spec.grid,
            currentOrb = OrbType.Normal(firstColor),
            nextOrb = OrbType.Normal(secondColor)
        )
    }

    fun startEndless(random: Random = Random.Default): BubbleGameState {
        val grid = List(ROWS) { r ->
            val cols = colsInRow(r)
            List(cols) {
                if (r < 5) CellContent.Normal(BubbleColor.entries[random.nextInt(BubbleColor.entries.size)])
                else null
            }
        }
        val firstColor = randomAvailableColor(grid, random)
        val secondColor = randomAvailableColor(grid, random)
        return BubbleGameState(
            mode = GameMode.ENDLESS,
            sectorNumber = 1,
            sectorTitle = "Endless Arcade",
            score = 0,
            movesLeft = 999,
            shotsUntilDrop = 6,
            endlessWave = 1,
            status = GameStatus.PLAYING,
            grid = grid,
            currentOrb = OrbType.Normal(firstColor),
            nextOrb = OrbType.Normal(secondColor)
        )
    }

    fun toggleArmBooster(state: BubbleGameState, kind: BoosterKind): BubbleGameState {
        if (state.status != GameStatus.PLAYING || state.bullet != null) return state
        val nextArmed = if (state.armedBooster == kind) null else kind
        return state.copy(armedBooster = nextArmed)
    }

    fun swapOrbs(state: BubbleGameState): BubbleGameState {
        if (state.status != GameStatus.PLAYING || state.bullet != null || state.armedBooster != null) {
            return state
        }
        val current = state.currentOrb ?: return state
        val next = state.nextOrb ?: return state
        return state.copy(currentOrb = next, nextOrb = current)
    }

    fun activeDisplayOrb(state: BubbleGameState): OrbType? = when (state.armedBooster) {
        BoosterKind.BOMB -> OrbType.Bomb
        BoosterKind.LIGHTNING -> OrbType.Lightning
        BoosterKind.WILD -> OrbType.Wild
        null -> state.currentOrb
    }

    fun updateAim(
        state: BubbleGameState,
        pointerX: Float,
        pointerY: Float,
        launcherX: Float,
        launcherY: Float,
        isAiming: Boolean
    ): BubbleGameState {
        if (state.status != GameStatus.PLAYING) return state
        val rawAngle = atan2(pointerY - launcherY, pointerX - launcherX)
        val clamped = rawAngle.coerceIn(MIN_AIM_ANGLE, MAX_AIM_ANGLE)
        val trajectory = if (isAiming && state.bullet == null) {
            computeAimTrajectory(
                grid = state.grid,
                launcherX = launcherX,
                launcherY = launcherY,
                angle = clamped,
                isFireball = activeDisplayOrb(state) is OrbType.Fireball
            )
        } else {
            AimTrajectory(emptyList(), null)
        }
        return state.copy(
            aimAngle = clamped,
            isAiming = isAiming,
            aimTrajectory = trajectory
        )
    }

    fun computeAimTrajectory(
        grid: List<List<CellContent?>>,
        launcherX: Float,
        launcherY: Float,
        angle: Float,
        isFireball: Boolean = false
    ): AimTrajectory {
        val points = mutableListOf(AimPoint(launcherX, launcherY))
        var x = launcherX
        var y = launcherY
        var dx = cos(angle)
        val dy = sin(angle)
        var bounces = 0
        val stepSize = 0.22f
        var impactPoint: AimPoint? = null

        for (step in 0 until 180) {
            x += dx * stepSize
            y += dy * stepSize

            if (x <= 1f) {
                x = 1f + (1f - x)
                dx = abs(dx)
                points.add(AimPoint(1f, y))
                bounces++
                if (bounces > 2) break
            } else if (x >= BOARD_WIDTH - 1f) {
                x = (BOARD_WIDTH - 1f) - (x - (BOARD_WIDTH - 1f))
                dx = -abs(dx)
                points.add(AimPoint(BOARD_WIDTH - 1f, y))
                bounces++
                if (bounces > 2) break
            }

            if (y <= 1f) {
                val end = AimPoint(x, 1f)
                points.add(end)
                impactPoint = end
                break
            }

            if (!isFireball && hasGridCollision(grid, x, y)) {
                val end = AimPoint(x, y)
                points.add(end)
                impactPoint = end
                break
            }
        }

        if (impactPoint == null) {
            points.add(AimPoint(x, y))
        }
        return AimTrajectory(points = points, impactPoint = impactPoint)
    }

    fun shoot(
        state: BubbleGameState,
        launcherX: Float,
        launcherY: Float,
        random: Random = Random.Default
    ): Pair<BubbleGameState, BoosterKind?> {
        if (state.status != GameStatus.PLAYING || state.bullet != null) {
            return state.copy(isAiming = false, aimTrajectory = AimTrajectory(emptyList(), null)) to null
        }
        if (state.mode == GameMode.SECTOR && state.movesLeft <= 0) {
            return state.copy(isAiming = false, aimTrajectory = AimTrajectory(emptyList(), null)) to null
        }

        val usedBooster = state.armedBooster
        val firingOrb = activeDisplayOrb(state)
            ?: return state.copy(isAiming = false, aimTrajectory = AimTrajectory(emptyList(), null)) to null

        val bullet = Bullet(
            x = launcherX,
            y = launcherY,
            vx = cos(state.aimAngle) * BULLET_SPEED,
            vy = sin(state.aimAngle) * BULLET_SPEED,
            orb = firingOrb
        )

        val nextState = if (usedBooster != null) {
            // Booster shot does not consume the normal chambered orb
            state.copy(
                bullet = bullet,
                armedBooster = null,
                movesLeft = if (state.mode == GameMode.SECTOR) state.movesLeft - 1 else state.movesLeft,
                isAiming = false,
                aimTrajectory = AimTrajectory(emptyList(), null)
            )
        } else {
            val next = state.nextOrb ?: OrbType.Normal(randomAvailableColor(state.grid, random))
            val queued = OrbType.Normal(randomAvailableColor(state.grid, random))
            state.copy(
                bullet = bullet,
                movesLeft = if (state.mode == GameMode.SECTOR) state.movesLeft - 1 else state.movesLeft,
                currentOrb = next,
                nextOrb = queued,
                isAiming = false,
                aimTrajectory = AimTrajectory(emptyList(), null)
            )
        }

        return nextState to usedBooster
    }

    fun step(
        state: BubbleGameState,
        dtSeconds: Float,
        random: Random = Random.Default
    ): EngineStepResult {
        val dt = dtSeconds.coerceIn(0f, 0.05f)
        if (dt <= 0f || state.status == GameStatus.PAUSED) {
            return EngineStepResult(state)
        }

        var current = updateVisualEffects(state, dt)
        val activeBullet = current.bullet ?: return EngineStepResult(current)

        val sounds = mutableListOf<GameSoundEvent>()
        val stepDistance = hypot(activeBullet.vx, activeBullet.vy) * dt
        val subSteps = max(1, (stepDistance / 0.24f).toInt() + 1)
        val subDt = dt / subSteps

        var bx = activeBullet.x
        var by = activeBullet.y
        var bvx = activeBullet.vx
        var bvy = activeBullet.vy
        val hitCells = activeBullet.hitCells.toMutableSet()
        var collided = false

        val mutableGrid = current.grid.map { it.toMutableList() }
        var extraScore = 0
        val extraParticles = current.particles.toMutableList()
        val extraPopups = current.popups.toMutableList()
        val extraWaves = current.shockwaves.toMutableList()
        var shake = current.screenShake

        for (s in 0 until subSteps) {
            bx += bvx * subDt
            by += bvy * subDt

            if (bx - 1f <= 0f) {
                bx = 1f + (1f - bx)
                bvx = abs(bvx)
                sounds.add(GameSoundEvent.BOUNCE)
            } else if (bx + 1f >= BOARD_WIDTH) {
                bx = (BOARD_WIDTH - 1f) - (bx + 1f - BOARD_WIDTH)
                bvx = -abs(bvx)
                sounds.add(GameSoundEvent.BOUNCE)
            }

            if (activeBullet.orb is OrbType.Fireball) {
                // Fireball burns straight through every cell it touches
                for (r in 0 until ROWS) {
                    for (c in mutableGrid[r].indices) {
                        val content = mutableGrid[r][c] ?: continue
                        val cell = GridCell(r, c)
                        if (cell in hitCells) continue
                        val (cx, cy) = cellCenter(r, c)
                        if (hypot(bx - cx, by - cy) < 1.95f) {
                            hitCells.add(cell)
                            mutableGrid[r][c] = null
                            extraScore += 175
                            val burstColor = colorForContent(content)
                            spawnBurst(extraParticles, cx, cy, burstColor, 10, random)
                            extraWaves.add(
                                ShockwaveRing(cx, cy, maxRadius = 2.6f, color = Color(0xFFF97316))
                            )
                            shake = max(shake, 0.35f)
                            if (GameSoundEvent.POP !in sounds) sounds.add(GameSoundEvent.POP)
                        }
                    }
                }
                if (by - 1f <= 0f) {
                    collided = true
                    break
                }
            } else {
                if (by - 1f <= 0f) {
                    by = 1f
                    collided = true
                    break
                }
                if (hasGridCollision(current.grid, bx, by)) {
                    collided = true
                    break
                }
            }
        }

        if (activeBullet.orb is OrbType.Fireball && extraScore > 0) {
            extraPopups.add(
                FloatingTextPopup(bx, by, "+$extraScore", Color(0xFFFB923C))
            )
        }

        current = current.copy(
            score = current.score + extraScore,
            grid = mutableGrid.map { it.toList() },
            particles = extraParticles,
            popups = extraPopups,
            shockwaves = extraWaves,
            screenShake = shake
        )

        if (!collided) {
            return EngineStepResult(
                state = current.copy(
                    bullet = activeBullet.copy(
                        x = bx,
                        y = by,
                        vx = bvx,
                        vy = bvy,
                        hitCells = hitCells
                    )
                ),
                sounds = sounds
            )
        }

        val resolved = resolveImpact(
            state = current,
            bx = bx,
            by = by,
            orb = activeBullet.orb,
            hitAnythingAsFireball = hitCells.isNotEmpty(),
            random = random
        )
        return EngineStepResult(
            state = resolved.state,
            sounds = sounds + resolved.sounds,
            triggerHapticImpact = resolved.triggerHapticImpact
        )
    }

    private fun updateVisualEffects(state: BubbleGameState, dt: Float): BubbleGameState {
        val nextParticles = state.particles.mapNotNull { p ->
            val nl = p.life - 2.8f * dt
            if (nl <= 0f) null else p.copy(x = p.x + p.vx * dt, y = p.y + p.vy * dt, life = nl)
        }
        val nextFalling = state.fallingBubbles.mapNotNull { fb ->
            val nvy = fb.vy + 75f * dt
            val ny = fb.y + nvy * dt
            if (ny > 38f) null else fb.copy(y = ny, vy = nvy)
        }
        val nextPopups = state.popups.mapNotNull { pop ->
            val nl = pop.life - 1.25f * dt
            if (nl <= 0f) null else pop.copy(y = pop.y - 2.4f * dt, life = nl)
        }
        val nextWaves = state.shockwaves.mapNotNull { w ->
            val np = w.progress + 3.2f * dt
            if (np >= 1f) null else w.copy(progress = np)
        }
        val nextShake = (state.screenShake - 3.5f * dt).coerceAtLeast(0f)

        return state.copy(
            particles = nextParticles,
            fallingBubbles = nextFalling,
            popups = nextPopups,
            shockwaves = nextWaves,
            screenShake = nextShake
        )
    }

    private fun resolveImpact(
        state: BubbleGameState,
        bx: Float,
        by: Float,
        orb: OrbType,
        hitAnythingAsFireball: Boolean,
        random: Random
    ): EngineStepResult {
        val mutableGrid = state.grid.map { it.toMutableList() }
        val particles = state.particles.toMutableList()
        val falling = state.fallingBubbles.toMutableList()
        val popups = state.popups.toMutableList()
        val waves = state.shockwaves.toMutableList()
        val sounds = mutableListOf<GameSoundEvent>()

        var newScore = state.score
        var newCombo = state.combo
        var newSuperCharge = state.superCharge
        var nextOrb = state.nextOrb
        var currentOrb = state.currentOrb
        var shake = state.screenShake
        var poppedCount = 0
        var hapticImpact = false

        when (orb) {
            OrbType.Fireball -> {
                if (hitAnythingAsFireball) {
                    newCombo++
                    hapticImpact = true
                    sounds.add(GameSoundEvent.EXPLOSION)
                } else {
                    newCombo = 0
                }
            }

            OrbType.Bomb -> {
                val blastRadius = 2.85f
                var cleared = 0
                for (r in 0 until ROWS) {
                    for (c in mutableGrid[r].indices) {
                        val content = mutableGrid[r][c] ?: continue
                        val (cx, cy) = cellCenter(r, c)
                        if (hypot(bx - cx, by - cy) <= blastRadius) {
                            mutableGrid[r][c] = null
                            cleared++
                            spawnBurst(particles, cx, cy, colorForContent(content), 14, random)
                        }
                    }
                }
                waves.add(ShockwaveRing(bx, by, maxRadius = 4.2f, color = Color(0xFFF43F5E)))
                shake = 1.0f
                hapticImpact = true
                sounds.add(GameSoundEvent.EXPLOSION)
                if (cleared > 0) {
                    newCombo++
                    val pts = cleared * 160
                    newScore += pts
                    poppedCount = cleared
                    popups.add(FloatingTextPopup(bx, by, "BOOM +$pts", Color(0xFFFBBF24)))
                }
            }

            OrbType.Lightning -> {
                val snapCell = findNearestEmptyCell(mutableGrid, bx, by)
                val targetRow = snapCell.r
                var cleared = 0
                for (c in mutableGrid[targetRow].indices) {
                    val content = mutableGrid[targetRow][c] ?: continue
                    val (cx, cy) = cellCenter(targetRow, c)
                    mutableGrid[targetRow][c] = null
                    cleared++
                    spawnBurst(particles, cx, cy, Color(0xFF38BDF8), 12, random)
                }
                val (_, rowY) = cellCenter(targetRow, 0)
                waves.add(ShockwaveRing(BOARD_WIDTH / 2f, rowY, maxRadius = 8.5f, color = Color(0xFF22D3EE)))
                shake = 0.75f
                hapticImpact = true
                sounds.add(GameSoundEvent.EXPLOSION)
                if (cleared > 0) {
                    newCombo++
                    val pts = cleared * 160
                    newScore += pts
                    poppedCount = cleared
                    popups.add(FloatingTextPopup(BOARD_WIDTH / 2f, rowY, "ZAP +$pts", Color(0xFF22D3EE)))
                }
            }

            OrbType.Wild -> {
                val snapCell = findNearestEmptyCell(mutableGrid, bx, by)
                val neighborColors = getNeighbors(snapCell.r, snapCell.c)
                    .mapNotNull { mutableGrid[it.r][it.c]?.matchColor }
                    .distinct()

                var bestMatches = emptyList<GridCell>()
                var bestColor = BubbleColor.CYAN
                for (candidate in neighborColors) {
                    mutableGrid[snapCell.r][snapCell.c] = CellContent.Normal(candidate)
                    val m = getMatches(mutableGrid, snapCell.r, snapCell.c, candidate)
                    if (m.size > bestMatches.size) {
                        bestMatches = m
                        bestColor = candidate
                    }
                }

                if (bestMatches.size >= 2) {
                    mutableGrid[snapCell.r][snapCell.c] = CellContent.Normal(bestColor)
                    newCombo++
                    val multiplier = 1f + min(newCombo - 1, 4) * 0.25f
                    val pts = (bestMatches.size * 150 * multiplier).roundToInt()
                    newScore += pts
                    poppedCount = bestMatches.size
                    popClusterAndThawIce(mutableGrid, bestMatches, particles, random)
                    val (sx, sy) = cellCenter(snapCell.r, snapCell.c)
                    waves.add(ShockwaveRing(sx, sy, maxRadius = 3.2f, color = Color(0xFFA855F7)))
                    popups.add(FloatingTextPopup(sx, sy, "WILD +$pts", Color(0xFFE879F9)))
                    hapticImpact = true
                    sounds.add(GameSoundEvent.POP)
                } else {
                    val fallback = neighborColors.firstOrNull() ?: randomAvailableColor(mutableGrid, random)
                    mutableGrid[snapCell.r][snapCell.c] = CellContent.Normal(fallback)
                }
            }

            is OrbType.Normal -> {
                val snapCell = findNearestEmptyCell(mutableGrid, bx, by)
                mutableGrid[snapCell.r][snapCell.c] = CellContent.Normal(orb.color)
                val matches = getMatches(mutableGrid, snapCell.r, snapCell.c, orb.color)

                if (matches.size >= 3) {
                    newCombo++
                    val multiplier = 1f + min(newCombo - 1, 4) * 0.25f
                    val pts = (matches.size * 150 * multiplier).roundToInt()
                    newScore += pts
                    poppedCount = matches.size

                    popClusterAndThawIce(mutableGrid, matches, particles, random)
                    val (sx, sy) = cellCenter(snapCell.r, snapCell.c)

                    if (matches.size >= 5) {
                        waves.add(ShockwaveRing(sx, sy, maxRadius = 3.4f, color = orb.color.primary))
                        shake = max(shake, 0.45f)
                    }

                    val label = if (newCombo >= 2) "+$pts  x$newCombo!" else "+$pts"
                    popups.add(FloatingTextPopup(sx, sy, label, orb.color.highlight))

                    newSuperCharge++
                    if (newSuperCharge >= 5) {
                        newSuperCharge = 0
                        nextOrb = OrbType.Fireball
                        popups.add(
                            FloatingTextPopup(sx, (sy - 1.4f).coerceAtLeast(1.2f), "FIREBALL READY!", Color(0xFFFB923C))
                        )
                    }

                    hapticImpact = true
                    sounds.add(GameSoundEvent.POP)
                } else {
                    newCombo = 0
                }
            }
        }

        // Drop floating orphans whenever anything was cleared (or Fireball struck)
        val orphanBonus = if (poppedCount > 0 || (orb is OrbType.Fireball && hitAnythingAsFireball)) {
            val multiplier = 1f + min(max(newCombo - 1, 0), 4) * 0.25f
            dropOrphans(mutableGrid, falling, multiplier)
        } else {
            0
        }

        if (orphanBonus > 0) {
            newScore += orphanBonus
            popups.add(
                FloatingTextPopup(
                    x = BOARD_WIDTH / 2f,
                    y = 6f,
                    text = "AVALANCHE +$orphanBonus!",
                    color = Color(0xFF34D399)
                )
            )
            shake = max(shake, 0.35f)
        }

        // Endless ceiling drop logic when a shot does not clear anything
        var nextShotsUntilDrop = state.shotsUntilDrop
        var nextEndlessWave = state.endlessWave
        if (state.mode == GameMode.ENDLESS && poppedCount == 0 && orphanBonus == 0 && !hitAnythingAsFireball) {
            nextShotsUntilDrop--
            if (nextShotsUntilDrop <= 0) {
                dropCeilingRow(mutableGrid, random)
                nextEndlessWave++
                nextShotsUntilDrop = max(6 - nextEndlessWave / 4, 4)
                shake = max(shake, 0.40f)
                popups.add(
                    FloatingTextPopup(BOARD_WIDTH / 2f, 2.5f, "WAVE $nextEndlessWave", Color(0xFF38BDF8))
                )
            }
        }

        val frozenGrid = mutableGrid.map { it.toList() }

        // Ensure chambered orbs match remaining colors if grid isn't empty
        val remainingColors = availableColors(frozenGrid)
        if (remainingColors.isNotEmpty()) {
            if (currentOrb is OrbType.Normal && currentOrb.color !in remainingColors) {
                currentOrb = OrbType.Normal(remainingColors[random.nextInt(remainingColors.size)])
            }
            if (nextOrb is OrbType.Normal && nextOrb.color !in remainingColors) {
                nextOrb = OrbType.Normal(remainingColors[random.nextInt(remainingColors.size)])
            }
        }

        // Check win / loss state
        var totalRemaining = 0
        var reachedBottom = false
        for (r in 0 until ROWS) {
            for (c in frozenGrid[r].indices) {
                if (frozenGrid[r][c] != null) {
                    totalRemaining++
                    if (r >= ROWS - 2) {
                        reachedBottom = true
                    }
                }
            }
        }

        var nextStatus = GameStatus.PLAYING
        var reason = state.gameOverReason
        var movesBonus = 0
        var earnedStars = 0
        var rewardBooster: BoosterKind? = null

        if (totalRemaining == 0) {
            if (state.mode == GameMode.SECTOR) {
                nextStatus = GameStatus.SECTOR_CLEARED
                movesBonus = state.movesLeft * 100
                newScore += movesBonus
                earnedStars = when {
                    newScore >= state.star3Threshold -> 3
                    newScore >= state.star2Threshold -> 2
                    else -> 1
                }
                if (earnedStars == 3) {
                    rewardBooster = BoosterKind.entries[random.nextInt(BoosterKind.entries.size)]
                }
                sounds.add(GameSoundEvent.VICTORY)
            } else {
                // Endless wave cleared: spawn fresh wave and award wave bonus!
                val waveBonus = 500 * nextEndlessWave
                newScore += waveBonus
                nextEndlessWave++
                popups.add(
                    FloatingTextPopup(BOARD_WIDTH / 2f, 5f, "WAVE CLEAR +$waveBonus!", Color(0xFF34D399))
                )
                val freshWave = List(ROWS) { r ->
                    val cols = colsInRow(r)
                    List(cols) {
                        if (r < 5) CellContent.Normal(BubbleColor.entries[random.nextInt(BubbleColor.entries.size)])
                        else null
                    }
                }
                sounds.add(GameSoundEvent.VICTORY)
                return EngineStepResult(
                    state = state.copy(
                        score = newScore,
                        combo = newCombo,
                        superCharge = newSuperCharge,
                        shotsUntilDrop = max(6 - nextEndlessWave / 4, 4),
                        endlessWave = nextEndlessWave,
                        grid = freshWave,
                        currentOrb = currentOrb,
                        nextOrb = nextOrb,
                        bullet = null,
                        particles = particles,
                        fallingBubbles = falling,
                        popups = popups,
                        shockwaves = waves,
                        screenShake = shake
                    ),
                    sounds = sounds,
                    triggerHapticImpact = hapticImpact
                )
            }
        } else if (reachedBottom || (state.mode == GameMode.SECTOR && state.movesLeft <= 0)) {
            nextStatus = GameStatus.GAME_OVER
            reason = if (reachedBottom) GameOverReason.CROSSED_DANGER_LINE else GameOverReason.OUT_OF_MOVES
            sounds.add(GameSoundEvent.GAME_OVER)
        }

        return EngineStepResult(
            state = state.copy(
                score = newScore,
                combo = newCombo,
                superCharge = newSuperCharge,
                shotsUntilDrop = nextShotsUntilDrop,
                endlessWave = nextEndlessWave,
                movesBonus = movesBonus,
                earnedStars = earnedStars,
                rewardBooster = rewardBooster,
                status = nextStatus,
                gameOverReason = reason,
                grid = frozenGrid,
                currentOrb = currentOrb,
                nextOrb = nextOrb,
                bullet = null,
                particles = particles,
                fallingBubbles = falling,
                popups = popups,
                shockwaves = waves,
                screenShake = shake
            ),
            sounds = sounds,
            triggerHapticImpact = hapticImpact
        )
    }

    private fun popClusterAndThawIce(
        mutableGrid: List<MutableList<CellContent?>>,
        matches: List<GridCell>,
        particles: MutableList<BurstParticle>,
        random: Random
    ) {
        val matchSet = matches.toHashSet()
        val iceToThaw = mutableSetOf<GridCell>()

        for (cell in matches) {
            val content = mutableGrid[cell.r][cell.c]
            val (px, py) = cellCenter(cell.r, cell.c)
            if (content != null) {
                spawnBurst(particles, px, py, colorForContent(content), 12, random)
            }
            for (neighbor in getNeighbors(cell.r, cell.c)) {
                if (neighbor !in matchSet && mutableGrid[neighbor.r][neighbor.c] is CellContent.Ice) {
                    iceToThaw.add(neighbor)
                }
            }
            mutableGrid[cell.r][cell.c] = null
        }

        for (iceCell in iceToThaw) {
            val ice = mutableGrid[iceCell.r][iceCell.c] as? CellContent.Ice ?: continue
            mutableGrid[iceCell.r][iceCell.c] = CellContent.Normal(ice.color)
            val (ix, iy) = cellCenter(iceCell.r, iceCell.c)
            spawnBurst(particles, ix, iy, Color(0xFFE0F2FE), 6, random)
        }
    }

    private fun dropCeilingRow(
        mutableGrid: List<MutableList<CellContent?>>,
        random: Random
    ) {
        for (r in (ROWS - 1) downTo 1) {
            val cols = colsInRow(r)
            val prevCols = colsInRow(r - 1)
            for (c in 0 until cols) {
                mutableGrid[r][c] = if (c < prevCols) mutableGrid[r - 1][c] else null
            }
        }
        for (c in 0 until colsInRow(0)) {
            mutableGrid[0][c] = CellContent.Normal(
                BubbleColor.entries[random.nextInt(BubbleColor.entries.size)]
            )
        }
    }

    private fun findNearestEmptyCell(
        grid: List<List<CellContent?>>,
        bx: Float,
        by: Float
    ): GridCell {
        var bestR = 0
        var bestC = 0
        var minDist = Float.MAX_VALUE
        for (r in 0 until ROWS) {
            for (c in grid[r].indices) {
                if (grid[r][c] == null) {
                    val (cx, cy) = cellCenter(r, c)
                    val d = hypot(bx - cx, by - cy)
                    if (d < minDist) {
                        minDist = d
                        bestR = r
                        bestC = c
                    }
                }
            }
        }
        return GridCell(bestR, bestC)
    }

    private fun hasGridCollision(grid: List<List<CellContent?>>, bx: Float, by: Float): Boolean {
        for (r in 0 until ROWS) {
            val row = grid.getOrNull(r) ?: continue
            for (c in row.indices) {
                if (row[c] != null) {
                    val (cx, cy) = cellCenter(r, c)
                    if (hypot(bx - cx, by - cy) < 1.8f) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun getMatches(
        grid: List<List<CellContent?>>,
        startR: Int,
        startC: Int,
        targetColor: BubbleColor
    ): List<GridCell> {
        val matches = mutableListOf<GridCell>()
        val visited = HashSet<GridCell>()
        val queue = ArrayDeque<GridCell>()

        val start = GridCell(startR, startC)
        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (grid[cell.r][cell.c]?.matchColor == targetColor) {
                matches.add(cell)
                for (neighbor in getNeighbors(cell.r, cell.c)) {
                    if (!visited.contains(neighbor) && grid[neighbor.r][neighbor.c] != null) {
                        visited.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }
        }
        return matches
    }

    fun getNeighbors(r: Int, c: Int): List<GridCell> {
        val isOffset = (r % 2 == 1)
        val dirs = if (isOffset) {
            arrayOf(0 to -1, 0 to 1, -1 to 0, -1 to 1, 1 to 0, 1 to 1)
        } else {
            arrayOf(0 to -1, 0 to 1, -1 to -1, -1 to 0, 1 to -1, 1 to 0)
        }
        val result = ArrayList<GridCell>(6)
        for ((dr, dc) in dirs) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until ROWS && nc in 0 until colsInRow(nr)) {
                result.add(GridCell(nr, nc))
            }
        }
        return result
    }

    private fun dropOrphans(
        mutableGrid: List<MutableList<CellContent?>>,
        fallingOut: MutableList<FallingBubble>,
        multiplier: Float
    ): Int {
        val connected = HashSet<GridCell>()
        val queue = ArrayDeque<GridCell>()

        for (c in 0 until colsInRow(0)) {
            if (mutableGrid[0][c] != null) {
                val cell = GridCell(0, c)
                connected.add(cell)
                queue.add(cell)
            }
        }

        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            for (neighbor in getNeighbors(cell.r, cell.c)) {
                if (!connected.contains(neighbor) && mutableGrid[neighbor.r][neighbor.c] != null) {
                    connected.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }

        var bonusScore = 0
        for (r in 0 until ROWS) {
            for (c in 0 until colsInRow(r)) {
                val content = mutableGrid[r][c]
                if (content != null && !connected.contains(GridCell(r, c))) {
                    val (px, py) = cellCenter(r, c)
                    fallingOut.add(FallingBubble(x = px, y = py, vy = 5f, content = content))
                    mutableGrid[r][c] = null
                    bonusScore += (100 * multiplier).roundToInt()
                }
            }
        }
        return bonusScore
    }

    private fun spawnBurst(
        out: MutableList<BurstParticle>,
        x: Float,
        y: Float,
        color: Color,
        count: Int,
        random: Random
    ) {
        repeat(count) {
            out.add(
                BurstParticle(
                    x = x,
                    y = y,
                    vx = (random.nextFloat() - 0.5f) * 24f,
                    vy = (random.nextFloat() - 0.5f) * 24f,
                    color = color,
                    life = 1.0f
                )
            )
        }
    }

    private fun colorForContent(content: CellContent): Color = when (content) {
        is CellContent.Normal -> content.color.primary
        is CellContent.Ice -> content.color.highlight
        CellContent.Stone -> Color(0xFF94A3B8)
    }

    private fun availableColors(grid: List<List<CellContent?>>): List<BubbleColor> =
        grid.flatten().mapNotNull { it?.matchColor }.distinct()

    private fun randomAvailableColor(grid: List<List<CellContent?>>, random: Random): BubbleColor {
        val pool = availableColors(grid).ifEmpty { BubbleColor.entries }
        return pool[random.nextInt(pool.size)]
    }
}
