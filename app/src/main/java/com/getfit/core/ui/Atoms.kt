package com.getfit.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.theme.SpaceGrotesk
import com.getfit.core.theme.Pill as PillShape

/** Press-to-scale (reproduces the prototype's style-active transform:scale). */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressed: Float = 0.95f,
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val s by animateFloatAsState(if (isPressed) pressed else 1f, label = "pressScale")
    scale(s)
}

/** Rounded chip; selected = lime fill + dark text, else surface + dim text + hairline. */
@Composable
fun Pill(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) GfColor.Lime else GfColor.Surface)
            .then(
                if (selected) Modifier
                else Modifier.border(BorderStroke(1.dp, GfColor.Hairline08), PillShape),
            )
            .padding(horizontal = 15.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = if (selected) GfColor.OnAccent else GfColor.TextDim,
            fontFamily = Manrope,
            fontWeight = FontWeight.W700,
            fontSize = 13.sp,
        )
    }
}

/** Icon + big number + label, in a surface card. */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    iconTint: Color = GfColor.Lime,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(GfColor.Surface)
            .border(BorderStroke(1.dp, GfColor.Hairline06), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(19.dp))
        Text(
            value, color = GfColor.Text, fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.W700, fontSize = 20.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            label, color = GfColor.TextDim, fontFamily = Manrope,
            fontWeight = FontWeight.W600, fontSize = 11.sp,
        )
    }
}

/** Shimmering placeholder box (GIF slot / loading states). */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "shimmerX",
    )
    val brush = Brush.linearGradient(
        colors = listOf(GfColor.SurfaceElevated, GfColor.SurfaceElevatedAlt, GfColor.SurfaceElevated),
        start = Offset(x * 400f - 200f, 0f),
        end = Offset(x * 400f, 400f),
    )
    Box(modifier.background(brush))
}

/** The rounded snackbar toast (light bg, dark text) from the prototype. */
@Composable
fun GfToast(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(GfColor.Text)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = GfColor.OnAccentDim, modifier = Modifier.size(18.dp))
        Text(
            text, color = GfColor.OnAccent, fontFamily = Manrope,
            fontWeight = FontWeight.W700, fontSize = 13.5.sp,
        )
    }
}
