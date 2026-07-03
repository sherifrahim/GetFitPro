package com.getfit.core.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.math.roundToInt

/**
 * Spring specs that reproduce the prototype's easing. The bottom-tab indicator and steppers use
 * cubic-bezier(.34,1.4,.5,1) — an overshoot — which maps to a low damping ratio.
 */
object GfSpring {
    fun <T> overshoot() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    fun <T> gentle() = spring<T>(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
    fun <T> snappy() = spring<T>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
}

/** Animated integer count-up for stat numbers. */
@Composable
fun countUp(target: Int, durationMillis: Int = 900): Int {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), tween(durationMillis)) }
    return anim.value.roundToInt()
}

/** Per-index entrance delay (ms) for staggered list reveals; capped so long lists stay snappy. */
fun staggerDelay(index: Int, stepMs: Int = 40, cap: Int = 8): Int = minOf(index, cap) * stepMs
