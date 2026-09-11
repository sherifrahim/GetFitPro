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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
            .background(if (selected) GfColor.Accent else GfColor.Surface)
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
    iconTint: Color = GfColor.Accent,
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
        Icon(icon, contentDescription = null, tint = GfColor.Accent, modifier = Modifier.size(18.dp))
        Text(
            text, color = GfColor.OnLight, fontFamily = Manrope,
            fontWeight = FontWeight.W700, fontSize = 13.5.sp,
        )
    }
}

/**
 * The app's one text field: black well, hairline border, accent cursor. Used for routine names,
 * rep targets, profile fields and API keys alike so every input reads the same.
 */
@Composable
fun GfInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    masked: Boolean = false,
    numeric: Boolean = false,
    textSize: androidx.compose.ui.unit.TextUnit = 13.5.sp,
    fontFamily: androidx.compose.ui.text.font.FontFamily = Manrope,
) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(GfColor.Background)
            .border(1.dp, GfColor.Hairline08, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = GfColor.TextFaint, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = textSize)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = GfColor.Text, fontFamily = fontFamily, fontWeight = FontWeight.W600, fontSize = textSize),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(GfColor.Accent),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (numeric) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text,
            ),
            visualTransformation = if (masked) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A bottom-anchored action sheet over a scrim (Hevy's "⋯" menus). Tap outside to dismiss. */
@Composable
fun GfSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
    ) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clickable(interactionSource = null, indication = null) {}
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)).background(GfColor.Surface)
                .padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 28.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 44.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(GfColor.Hairline12))
            Text(
                title, color = GfColor.Text, fontFamily = SpaceGrotesk, fontWeight = FontWeight.W700, fontSize = 17.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 10.dp),
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            content()
        }
    }
}

/** One row of a [GfSheet]. */
@Composable
fun GfSheetRow(icon: ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) GfColor.Coral else GfColor.Text
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 15.sp)
    }
}
