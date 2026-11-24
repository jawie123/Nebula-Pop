package com.example.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.game.AimTrajectory
import com.example.game.BubbleColor
import com.example.game.CellContent
import com.example.game.FloatingTextPopup
import com.example.game.OrbType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SpaceCenter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawCellContent(
    center: Offset,
    radius: Float,
    content: CellContent,
    colorblindEnabled: Boolean
) {
    when (content) {
        is CellContent.Normal -> {
            drawGlossySphere(center, radius, content.color)
            if (colorblindEnabled) {
                drawColorblindGlyph(center, radius, content.color)
            }
        }
        is CellContent.Ice -> {
            drawGlossySphere(center, radius, content.color)
            if (colorblindEnabled) {
                drawColorblindGlyph(center, radius, content.color)
            }
            // Frosted crystalline ice rim & facet cross
            drawCircle(
                color = Color(0xFFE0F2FE).copy(alpha = 0.55f),
                radius = radius * 0.94f,
                center = center,
                style = Stroke(width = (radius * 0.16f).coerceAtLeast(2f))
            )
            val rIn = radius * 0.65f
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(center.x - rIn, center.y),
                end = Offset(center.x + rIn, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(center.x, center.y - rIn),
                end = Offset(center.x, center.y + rIn),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        CellContent.Stone -> {
            drawStoneAsteroid(center, radius)
        }
    }
}

fun DrawScope.drawOrb(
    center: Offset,
    radius: Float,
    orb: OrbType,
    colorblindEnabled: Boolean
) {
    when (orb) {
        is OrbType.Normal -> {
            drawGlossySphere(center, radius, orb.color)
            if (colorblindEnabled) {
                drawColorblindGlyph(center, radius, orb.color)
            }
        }
        OrbType.Fireball -> {
            drawCircle(
                color = Color(0xFFF97316).copy(alpha = 0.48f),
                radius = radius * 1.24f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFEF08A),
                    0.45f to Color(0xFFF97316),
                    1.0f to Color(0xFFDC2626),
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = center
            )
        }
        OrbType.Bomb -> {
            drawCircle(
                color = Color(0xFFF43F5E).copy(alpha = 0.45f),
                radius = radius * 1.2f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFF64748B),
                    0.5f to Color(0xFF1E293B),
                    1.0f to Color(0xFF020617),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                    radius = radius * 1.15f
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFFF43F5E),
                radius = radius * 0.38f,
                center = center
            )
            drawCircle(
                color = Color(0xFFFACC15),
                radius = radius * 0.16f,
                center = center
            )
        }
        OrbType.Lightning -> {
            drawCircle(
                color = Color(0xFF22D3EE).copy(alpha = 0.48f),
                radius = radius * 1.2f,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.White,
                    0.4f to Color(0xFF22D3EE),
                    1.0f to Color(0xFF0369A1),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            val bolt = Path().apply {
                moveTo(center.x + radius * 0.12f, center.y - radius * 0.55f)
                lineTo(center.x - radius * 0.28f, center.y + radius * 0.05f)
                lineTo(center.x + radius * 0.05f, center.y + radius * 0.05f)
                lineTo(center.x - radius * 0.12f, center.y + radius * 0.55f)
                lineTo(center.x + radius * 0.32f, center.y - radius * 0.05f)
                lineTo(center.x - radius * 0.02f, center.y - radius * 0.05f)
                close()
            }
            drawPath(bolt, color = Color(0xFFFEF08A))
        }
        OrbType.Wild -> {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFFF43F5E),
                        Color(0xFFA855F7),
                        Color(0xFFFACC15),
                        Color(0xFF22C55E),
                        Color(0xFF38BDF8)
                    ),
                    center = center
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.35f,
                center = center
            )
        }
    }
}

private fun DrawScope.drawGlossySphere(
    center: Offset,
    radius: Float,
    bubbleColor: BubbleColor
) {
    drawCircle(
        color = bubbleColor.glow.copy(alpha = 0.42f),
        radius = radius + 2.dp.toPx(),
        center = center
    )
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to bubbleColor.highlight,
            0.40f to bubbleColor.primary,
            1.0f to bubbleColor.glow,
            center = Offset(center.x - radius * 0.30f, center.y - radius * 0.30f),
            radius = radius * 1.15f
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.72f),
        radius = radius * 0.24f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
    )
}

private fun DrawScope.drawStoneAsteroid(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFF94A3B8),
            0.55f to Color(0xFF475569),
            1.0f to Color(0xFF1E293B),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
            radius = radius * 1.15f
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color(0xFFCBD5E1).copy(alpha = 0.45f),
        radius = radius * 0.88f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    val d = radius * 0.35f
    drawLine(
        color = Color(0xFF0F172A).copy(alpha = 0.75f),
        start = Offset(center.x - d, center.y - d),
        end = Offset(center.x + d, center.y + d),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF0F172A).copy(alpha = 0.75f),
        start = Offset(center.x + d, center.y - d),
        end = Offset(center.x - d, center.y + d),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawColorblindGlyph(
    center: Offset,
    radius: Float,
    color: BubbleColor
) {
    val gStroke = Stroke(width = (radius * 0.13f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
    val tint = Color.White.copy(alpha = 0.90f)
    val s = radius * 0.42f

    when (color) {
        BubbleColor.CYAN -> {
            drawCircle(color = tint, radius = s * 0.85f, center = center, style = gStroke)
        }
        BubbleColor.ROSE -> {
            val path = Path().apply {
                moveTo(center.x, center.y - s)
                lineTo(center.x + s * 0.9f, center.y + s * 0.75f)
                lineTo(center.x - s * 0.9f, center.y + s * 0.75f)
                close()
            }
            drawPath(path, color = tint, style = gStroke)
        }
        BubbleColor.PURPLE -> {
            val path = Path().apply {
                moveTo(center.x, center.y - s)
                lineTo(center.x + s * 0.85f, center.y)
                lineTo(center.x, center.y + s)
                lineTo(center.x - s * 0.85f, center.y)
                close()
            }
            drawPath(path, color = tint, style = gStroke)
        }
        BubbleColor.GOLD -> {
            drawLine(tint, Offset(center.x - s, center.y), Offset(center.x + s, center.y), gStroke.width, StrokeCap.Round)
            drawLine(tint, Offset(center.x, center.y - s), Offset(center.x, center.y + s), gStroke.width, StrokeCap.Round)
        }
        BubbleColor.GREEN -> {
            val path = Path().apply {
                for (i in 0 until 6) {
                    val a = (PI / 3.0 * i).toFloat()
                    val px = center.x + cos(a) * s
                    val py = center.y + sin(a) * s
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path, color = tint, style = gStroke)
        }
    }
}

fun DrawScope.drawRicochetTrajectory(
    trajectory: AimTrajectory,
    toScreen: (Float, Float) -> Offset,
    radiusPx: Float,
    isFireball: Boolean
) {
    val pts = trajectory.points
    if (pts.size < 2) return
    val lineColor = if (isFireball) Color(0xFFFB923C).copy(alpha = 0.9f) else CyanNeon.copy(alpha = 0.88f)
    val path = Path().apply {
        val first = toScreen(pts[0].x, pts[0].y)
        moveTo(first.x, first.y)
        for (i in 1 until pts.size) {
            val p = toScreen(pts[i].x, pts[i].y)
            lineTo(p.x, p.y)
        }
    }
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(
            width = 2.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx()), 0f),
            cap = StrokeCap.Round
        )
    )
    trajectory.impactPoint?.let { end ->
        val endPx = toScreen(end.x, end.y)
        drawCircle(
            color = lineColor.copy(alpha = 0.45f),
            radius = radiusPx * 0.95f,
            center = endPx,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 4.dp.toPx(),
            center = endPx
        )
    }
}

fun DrawScope.drawLauncherTurret(
    launcherPos: Offset,
    radiusPx: Float,
    aimAngle: Float,
    superCharge: Int,
    activeOrb: OrbType?,
    bulletInFlight: Boolean,
    colorblindEnabled: Boolean
) {
    // Cannon Barrel
    val barrelLength = radiusPx * 2.15f
    val tip = Offset(
        launcherPos.x + cos(aimAngle) * barrelLength,
        launcherPos.y + sin(aimAngle) * barrelLength
    )
    drawLine(
        color = Color(0xFF1E293B),
        start = launcherPos,
        end = tip,
        strokeWidth = radiusPx * 0.9f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = CyanNeon.copy(alpha = 0.7f),
        start = launcherPos,
        end = tip,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Base Pod
    val podRadius = radiusPx * 1.65f
    drawCircle(color = SpaceCenter, radius = podRadius, center = launcherPos)
    drawCircle(
        color = CyanNeon.copy(alpha = 0.35f),
        radius = podRadius,
        center = launcherPos,
        style = Stroke(width = 3.dp.toPx())
    )

    // Super Fireball Charge Arc
    val chargeSweep = (superCharge.coerceIn(0, 5) / 5f) * 360f
    if (chargeSweep > 0f) {
        drawArc(
            color = Color(0xFFFB923C),
            startAngle = -90f,
            sweepAngle = chargeSweep,
            useCenter = false,
            topLeft = Offset(launcherPos.x - podRadius, launcherPos.y - podRadius),
            size = Size(podRadius * 2f, podRadius * 2f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    if (!bulletInFlight && activeOrb != null) {
        drawOrb(launcherPos, radiusPx, activeOrb, colorblindEnabled)
    }
}

fun DrawScope.drawFloatingPopups(
    popups: List<FloatingTextPopup>,
    toScreen: (Float, Float) -> Offset,
    densityScale: Float
) {
    if (popups.isEmpty()) return
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 15f * densityScale
        }
        for (popup in popups) {
            val pos = toScreen(popup.x, popup.y)
            paint.color = popup.color.copy(alpha = popup.life.coerceIn(0f, 1f)).toArgb()
            paint.setShadowLayer(6f, 0f, 2f, Color.Black.copy(alpha = popup.life).toArgb())
            canvas.nativeCanvas.drawText(popup.text, pos.x, pos.y, paint)
        }
    }
}
