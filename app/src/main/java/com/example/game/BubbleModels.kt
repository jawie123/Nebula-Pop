package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.BoosterKind

enum class BubbleColor(
    val id: Int,
    val primary: Color,
    val glow: Color,
    val highlight: Color
) {
    CYAN(
        id = 0,
        primary = Color(0xFF38BDF8),
        glow = Color(0xFF0284C7),
        highlight = Color(0xFFE0F2FE)
    ),
    ROSE(
        id = 1,
        primary = Color(0xFFF43F5E),
        glow = Color(0xFFBE123C),
        highlight = Color(0xFFFFE4E6)
    ),
    PURPLE(
        id = 2,
        primary = Color(0xFFA855F7),
        glow = Color(0xFF7E22CE),
        highlight = Color(0xFFF3E8FF)
    ),
    GOLD(
        id = 3,
        primary = Color(0xFFFACC15),
        glow = Color(0xFFCA8A04),
        highlight = Color(0xFFFEF9C3)
    ),
    GREEN(
        id = 4,
        primary = Color(0xFF22C55E),
        glow = Color(0xFF15803D),
        highlight = Color(0xFFDCFCE7)
    )
}

sealed interface CellContent {
    val matchColor: BubbleColor?

    data class Normal(val color: BubbleColor) : CellContent {
        override val matchColor: BubbleColor = color
    }

    data class Ice(val color: BubbleColor) : CellContent {
        override val matchColor: BubbleColor = color
    }

    data object Stone : CellContent {
        override val matchColor: BubbleColor? = null
    }
}

sealed interface OrbType {
    data class Normal(val color: BubbleColor) : OrbType
    data object Fireball : OrbType
    data object Bomb : OrbType
    data object Lightning : OrbType
    data object Wild : OrbType
}

enum class GameMode {
    SECTOR,
    ENDLESS
}

data class GridCell(val r: Int, val c: Int)

data class Bullet(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val orb: OrbType,
    val hitCells: Set<GridCell> = emptySet()
)

data class AimPoint(val x: Float, val y: Float)

data class AimTrajectory(
    val points: List<AimPoint>,
    val impactPoint: AimPoint?
)

data class BurstParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val life: Float = 1.0f
)

data class FallingBubble(
    val x: Float,
    val y: Float,
    val vy: Float,
    val content: CellContent
)

data class FloatingTextPopup(
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val life: Float = 1.0f
)

data class ShockwaveRing(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    val color: Color,
    val progress: Float = 0f
)

enum class GameStatus {
    PLAYING,
    PAUSED,
    SECTOR_CLEARED,
    GAME_OVER
}

enum class GameOverReason {
    OUT_OF_MOVES,
    CROSSED_DANGER_LINE
}

enum class GameSoundEvent {
    SHOOT,
    BOUNCE,
    POP,
    EXPLOSION,
    VICTORY,
    GAME_OVER
}

data class BubbleGameState(
    val mode: GameMode = GameMode.SECTOR,
    val sectorNumber: Int = 1,
    val sectorTitle: String = "Orion Gate",
    val score: Int = 0,
    val movesLeft: Int = 25,
    val shotsUntilDrop: Int = 6,
    val endlessWave: Int = 1,
    val combo: Int = 0,
    val superCharge: Int = 0,
    val star1Threshold: Int = 1500,
    val star2Threshold: Int = 3000,
    val star3Threshold: Int = 4500,
    val earnedStars: Int = 0,
    val movesBonus: Int = 0,
    val rewardBooster: BoosterKind? = null,
    val isNewBest: Boolean = false,
    val status: GameStatus = GameStatus.PLAYING,
    val gameOverReason: GameOverReason = GameOverReason.OUT_OF_MOVES,
    val grid: List<List<CellContent?>> = emptyList(),
    val currentOrb: OrbType? = null,
    val nextOrb: OrbType? = null,
    val armedBooster: BoosterKind? = null,
    val bullet: Bullet? = null,
    val aimAngle: Float = (-Math.PI / 2.0).toFloat(),
    val isAiming: Boolean = false,
    val aimTrajectory: AimTrajectory = AimTrajectory(emptyList(), null),
    val particles: List<BurstParticle> = emptyList(),
    val fallingBubbles: List<FallingBubble> = emptyList(),
    val popups: List<FloatingTextPopup> = emptyList(),
    val shockwaves: List<ShockwaveRing> = emptyList(),
    val screenShake: Float = 0f
)
