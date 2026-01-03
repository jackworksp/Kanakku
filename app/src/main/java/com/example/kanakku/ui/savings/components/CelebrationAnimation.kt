package com.example.kanakku.ui.savings.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti particle data class
 *
 * @param x Horizontal position (0.0 - 1.0)
 * @param y Vertical position (0.0 - 1.0)
 * @param rotation Rotation angle in degrees
 * @param color Color of the confetti particle
 * @param size Size of the particle
 * @param speed Fall speed multiplier
 * @param swayAmplitude Horizontal sway amplitude
 * @param swayFrequency Horizontal sway frequency
 */
private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val rotation: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float
)

/**
 * Celebration animation with confetti particles
 *
 * @param show Whether to show the celebration animation
 * @param modifier Modifier for this composable
 * @param onAnimationComplete Callback when animation completes
 * @param message Celebration message to display
 * @param particleCount Number of confetti particles to generate
 * @param durationMillis Duration of the animation in milliseconds
 */
@Composable
fun CelebrationAnimation(
    show: Boolean,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {},
    message: String = "Goal Completed! 🎉",
    particleCount: Int = 80,
    durationMillis: Int = 3000
) {
    if (!show) return

    // Generate random confetti particles
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.3f, // Start above screen
                rotation = Random.nextFloat() * 360f,
                color = confettiColors.random(),
                size = Random.nextFloat() * 8f + 6f, // 6-14 dp
                speed = Random.nextFloat() * 0.5f + 0.5f, // 0.5-1.0x
                swayAmplitude = Random.nextFloat() * 40f + 20f, // 20-60 px
                swayFrequency = Random.nextFloat() * 2f + 1f // 1-3 cycles
            )
        }
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
        label = "confetti_animation"
    )

    // Trigger animation and callback when complete
    LaunchedEffect(show) {
        animationPlayed = true
        delay(durationMillis.toLong())
        onAnimationComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            particles.forEach { particle ->
                // Calculate particle position with fall and sway
                val progress = animatedProgress * particle.speed
                val yPos = particle.y * canvasHeight + (progress * canvasHeight * 1.3f)

                // Only draw if particle is visible on screen
                if (yPos < canvasHeight + 50f) {
                    val swayOffset = sin(progress * particle.swayFrequency * Math.PI * 2) * particle.swayAmplitude
                    val xPos = particle.x * canvasWidth + swayOffset.toFloat()

                    // Calculate rotation (particles spin as they fall)
                    val rotationAngle = particle.rotation + (progress * 720f) // 2 full rotations

                    rotate(degrees = rotationAngle, pivot = Offset(xPos, yPos)) {
                        // Draw confetti as rectangles
                        val particleSize = particle.size
                        drawRect(
                            color = particle.color,
                            topLeft = Offset(xPos - particleSize / 2, yPos - particleSize),
                            size = Size(particleSize, particleSize * 2)
                        )
                    }
                }
            }
        }

        // Celebration message with fade-in animation
        val messageAlpha by animateFloatAsState(
            targetValue = if (animationPlayed) 1f else 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "message_fade_in"
        )

        val messageScale by animateFloatAsState(
            targetValue = if (animationPlayed) 1f else 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "message_scale"
        )

        Box(
            modifier = Modifier
                .padding(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = messageAlpha * 0.95f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = messageAlpha),
                fontSize = (24 * messageScale).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Compact celebration animation for inline use
 *
 * @param show Whether to show the celebration animation
 * @param modifier Modifier for this composable
 * @param onAnimationComplete Callback when animation completes
 * @param particleCount Number of confetti particles to generate
 * @param durationMillis Duration of the animation in milliseconds
 */
@Composable
fun CompactCelebrationAnimation(
    show: Boolean,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {},
    particleCount: Int = 40,
    durationMillis: Int = 2000
) {
    if (!show) return

    // Generate random confetti particles
    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.2f,
                rotation = Random.nextFloat() * 360f,
                color = confettiColors.random(),
                size = Random.nextFloat() * 6f + 4f, // 4-10 dp
                speed = Random.nextFloat() * 0.4f + 0.6f,
                swayAmplitude = Random.nextFloat() * 30f + 15f,
                swayFrequency = Random.nextFloat() * 2f + 1f
            )
        }
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing),
        label = "compact_confetti_animation"
    )

    LaunchedEffect(show) {
        animationPlayed = true
        delay(durationMillis.toLong())
        onAnimationComplete()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val progress = animatedProgress * particle.speed
            val yPos = particle.y * canvasHeight + (progress * canvasHeight * 1.2f)

            if (yPos < canvasHeight + 30f) {
                val swayOffset = sin(progress * particle.swayFrequency * Math.PI * 2) * particle.swayAmplitude
                val xPos = particle.x * canvasWidth + swayOffset.toFloat()
                val rotationAngle = particle.rotation + (progress * 540f)

                rotate(degrees = rotationAngle, pivot = Offset(xPos, yPos)) {
                    val particleSize = particle.size
                    drawRect(
                        color = particle.color,
                        topLeft = Offset(xPos - particleSize / 2, yPos - particleSize),
                        size = Size(particleSize, particleSize * 2)
                    )
                }
            }
        }
    }
}

/**
 * Star burst celebration animation for milestone achievements
 *
 * @param show Whether to show the celebration animation
 * @param modifier Modifier for this composable
 * @param onAnimationComplete Callback when animation completes
 * @param starCount Number of star bursts to generate
 * @param durationMillis Duration of the animation in milliseconds
 */
@Composable
fun StarBurstAnimation(
    show: Boolean,
    modifier: Modifier = Modifier,
    onAnimationComplete: () -> Unit = {},
    starCount: Int = 12,
    durationMillis: Int = 1500
) {
    if (!show) return

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        label = "star_burst_animation"
    )

    LaunchedEffect(show) {
        animationPlayed = true
        delay(durationMillis.toLong())
        onAnimationComplete()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2

        repeat(starCount) { i ->
            val angle = (i * 360f / starCount) * Math.PI / 180
            val radius = maxRadius * animatedProgress
            val x = centerX + (radius * kotlin.math.cos(angle)).toFloat()
            val y = centerY + (radius * kotlin.math.sin(angle)).toFloat()

            val starSize = 20f * (1f - animatedProgress * 0.5f) // Stars shrink as they expand out
            val alpha = 1f - animatedProgress

            // Draw star
            val starPath = Path().apply {
                val points = 5
                val outerRadius = starSize
                val innerRadius = starSize * 0.4f

                for (j in 0 until points * 2) {
                    val currentRadius = if (j % 2 == 0) outerRadius else innerRadius
                    val currentAngle = j * Math.PI / points - Math.PI / 2
                    val px = x + (currentRadius * kotlin.math.cos(currentAngle)).toFloat()
                    val py = y + (currentRadius * kotlin.math.sin(currentAngle)).toFloat()

                    if (j == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }

            drawPath(
                path = starPath,
                color = Color(0xFFFFD700).copy(alpha = alpha) // Gold color
            )
        }
    }
}

/**
 * Predefined colors for confetti particles
 */
private val confettiColors = listOf(
    Color(0xFFFF6B6B), // Red
    Color(0xFF4ECDC4), // Teal
    Color(0xFFFFE66D), // Yellow
    Color(0xFF95E1D3), // Mint
    Color(0xFFF38181), // Pink
    Color(0xFFAA96DA), // Purple
    Color(0xFFFCACA6), // Peach
    Color(0xFF6BCB77), // Green
    Color(0xFF4D96FF), // Blue
    Color(0xFFFFB347)  // Orange
)
