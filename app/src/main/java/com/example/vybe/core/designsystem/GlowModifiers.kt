package com.example.vybe.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.vybeGlassCard(
    cornerRadius: Dp = 16.dp,
    borderColor: Color = Color(0x22FFFFFF),
    backgroundColor: Color = Color(0xBB181A24)
): Modifier = this
    .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
    .background(backgroundColor, RoundedCornerShape(cornerRadius))

fun Modifier.vybeNeonGlow(
    color: Color = VybeEmerald,
    radius: Dp = 20.dp,
    alpha: Float = 0.35f
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = color.copy(alpha = alpha).toArgb()
        frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
            radius.toPx(),
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
        canvas.drawCircle(
            center = Offset(size.width / 2, size.height / 2),
            radius = size.width / 2,
            paint = paint
        )
    }
}

fun Modifier.vybePulsingGlow(
    color: Color = VybeEmerald
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    this.vybeNeonGlow(color = color, alpha = alpha)
}
