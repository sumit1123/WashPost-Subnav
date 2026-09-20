/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wpds.utils

import android.content.res.Configuration
import android.view.MotionEvent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wpds.theme.wpdsColors

/**
 * Triggers scaling and alpha animations and performs click action when target is pressed
 * @param scale scale to animate target to when pressed. Ex: 0.9f means scale down to 90% of original size
 * @param alpha OPTIONAL alpha value to change target opacity to when pressed
 * @param onClick action to perform when target is pressed
 */
fun Modifier.scaleAndAlphaTap(scale: Float, alpha: Float = 1f, onClick: () -> Unit): Modifier = composed {
    var tapState by remember { mutableStateOf(TapState.IDLE) }
    val scaleAnimation by animateFloatAsState(
        targetValue = if (tapState == TapState.PRESSED) scale else 1f,
        label = "ScalingAnimation"
    )

    this
        .graphicsLayer {
            scaleX = scaleAnimation
            scaleY = scaleAnimation
        }
        .alpha(
            if (tapState == TapState.PRESSED) {
                alpha
            } else {
                1f
            }
        )
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                tapState = TapState.IDLE
                onClick()
            }
        )
        .pointerInput(tapState) {
            awaitPointerEventScope {
                tapState = if (tapState == TapState.PRESSED) {
                    waitForUpOrCancellation()
                    TapState.IDLE
                } else {
                    awaitFirstDown(false)
                    TapState.PRESSED
                }
            }
        }
}

/**
 * Makes the calling composable extend an additional width based on the passed padding
 * @param paddingToOverride the horizontal padding of the composable's parent
 */
fun Modifier.makeFullBleed(paddingToOverride: Dp): Modifier {
    return this.layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                maxWidth = constraints.maxWidth + (2 * paddingToOverride.roundToPx())
            )
        )
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

/**
 * Creates a Shimmer Effect using a Linear Gradient
 * @param durationMillis the duration of the animation. Default is 1500
 */
fun Modifier.shimmerEffect(durationMillis: Int = 1500): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    background(
        brush = getShimmerEffectBrush(
            size.width.toFloat(),
            listOf(wpdsColors.gray400, wpdsColors.gray600, wpdsColors.gray400),
            durationMillis
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun getTextShimmerEffectBrush(
    text: String,
    textStyle: TextStyle,
    colors: List<Color>,
    durationMillis: Int
): Brush {
    val width = measureTextWidthInPixels(text, textStyle)
    return getShimmerEffectBrush(width, colors, durationMillis)
}

@Composable
private fun getShimmerEffectBrush(
    width: Float,
    colors: List<Color>,
    durationMillis: Int = 1500
): Brush {
    val transition = rememberInfiniteTransition(label = "InfiniteTransition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * width,
        targetValue = 2 * width,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis,
                easing = LinearOutSlowInEasing
            )
        ), label = "FloatAnimation"
    )
    return Brush.linearGradient(
        colors = colors,
        start = Offset(startOffsetX, 0f),
        end = Offset(startOffsetX + width, 0f)
    )
}

/**
 * Use this function to add a bounce animation to a composable.
 * To use this function, set it's returned value to either the x or y field in the .offset() modifier
 * (x for horizontal bounce and y for vertical bounce)
 * @param initialValue The leftmost or topmost position of the animation, relative to the composable's starting point
 * @param targetValue The rightmost or bottommost position of the animation, relative to the composable's starting point
 */
@Composable
fun bounceAnimationOffset(initialValue: Float, targetValue: Float): Dp {
    val infiniteTransition = rememberInfiniteTransition()
    val position by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                800,
                easing = FastOutLinearInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    return position.dp
}

/**
 * Creates a pulsing effect around the composable that it is applied to.
 */
@Composable
fun Modifier.pulseEffect(
    targetScale: Float = 1.25f,
    initialScale: Float = 0.5f,
    brush: Brush = SolidColor(wpdsColors.atpPurpleStatic),
    shape: Shape = CircleShape,
    iterationCount: Int = 2,
    duration: Int = 1500,
    delay: Int = 0
): Modifier {
    var initialized by remember { mutableStateOf(false) }
    val pulseScale by animateFloatAsState(
        targetValue = if (initialized) targetScale else initialScale,
        animationSpec = repeatable(
            iterations = iterationCount,
            animation = tween(duration, delay)
        ),
        label = "PulseScale"
    )

    val pulseAlpha by animateFloatAsState(
        targetValue = if (initialized) 0f else 1f,
        animationSpec = repeatable(
            iterations = iterationCount,
            animation = tween(duration, delay)
        ),
        label = "PulseAlpha"
    )
    return this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        scale(pulseScale) {
            drawOutline(outline, brush, pulseAlpha)
        }
        initialized = true
    }
}


/**
 * Uses [pulseEffect] twice, modifying the second one with a shorter duration and adding a delay, to
 * create a double pulse effect
 */
@Composable
fun Modifier.doublePulseEffect(
    targetScale: Float = 1.25f,
    initialScale: Float = 0.5f,
    brush: Brush = SolidColor(wpdsColors.atpPurpleStatic),
    shape: Shape = CircleShape,
    duration: Int = 1500,
): Modifier {
    return this
        .pulseEffect(
            targetScale, initialScale, brush, shape,
            duration = duration
        )
        .pulseEffect(
            targetScale, initialScale, brush, shape,
            duration = (duration * 0.8).toInt(),
            delay = (duration * 0.2).toInt()
        )
}

fun Modifier.fadingEdge(scrollState: ScrollState): Modifier {
    val brush = if (scrollState.canScrollBackward && scrollState.canScrollForward) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.25f to Color.White,
            0.75f to Color.White,
            1f to Color.Transparent
        )
    } else if (scrollState.canScrollBackward) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.25f to Color.White
        )
    } else if (scrollState.canScrollForward) {
        Brush.verticalGradient(
            0.75f to Color.White,
            1f to Color.Transparent
        )
    } else {
        return this
    }
    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(brush = brush, blendMode = BlendMode.DstIn)
        }
}

fun Modifier.disableParentScroll(): Modifier = composed {
    val view = LocalView.current

    this.pointerInteropFilter { motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                view.parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        false
    }
}

fun Modifier.verticalFadingEdge(
    topFadeHeight: Dp = 32.dp,
    bottomFadeHeight: Dp = 32.dp
) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        val contentHeight = size.height

        if (contentHeight <= 0f) {
            drawContent()
            return@drawWithContent
        }

        drawContent()

        val topFadePx = topFadeHeight.toPx()
        val bottomFadePx = bottomFadeHeight.toPx()

        val topStop = (topFadePx / contentHeight).coerceIn(0f, 1f)
        val bottomStop = (1f - (bottomFadePx / contentHeight)).coerceIn(0f, 1f)

        val fadeBrush = Brush.verticalGradient(
            0f to Color.Transparent,
            topStop to Color.Black,
            bottomStop to Color.Black,
            1f to Color.Transparent
        )

        drawRect(
            brush = fadeBrush,
            blendMode = BlendMode.DstIn
        )
    }

@Composable
fun measureTextWidthInPixels(text: String, style: TextStyle): Float {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toFloat() }
}

@Composable
fun isTabletUi(): Boolean {
    return LocalConfiguration.current.smallestScreenWidthDp >= DeviceUtilRepo.TABLET_MIN_WIDTH
}

@Composable
fun isTabletLandscapeUi(): Boolean {
    return LocalConfiguration.current.screenWidthDp >= DeviceUtilRepo.TABLET_LANDSCAPE_MIN_WIDTH
}

enum class TapState {
    PRESSED,
    IDLE
}

@Preview(
    name = "Pixel 5",
    device = "spec:width=1080dp,height=2400dp,dpi=480",
    showSystemUi = true
)
@Preview(
    name = "Pixel 5",
    device = "spec:width=1080dp,height=2400dp,dpi=480",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Samsung Galaxy S21 Ultra",
    device = "spec:width=1440dp,height=3200dp,dpi=515",
    showSystemUi = true
)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showSystemUi = true)
@Preview(
    name = "tablet",
    device = "spec:width=1280dp,height=800dp,dpi=480",
    showSystemUi = true
)
annotation class DevicePreviews
