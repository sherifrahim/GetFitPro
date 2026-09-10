package com.getfit.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.SpaceGrotesk

/** −/+ stepper: minus circle, center content slot, plus circle. */
@Composable
fun Stepper(
    onDec: () -> Unit,
    onInc: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
    tint: Color = GfColor.Accent,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepIcon(ForgeIcons.RemoveCircle, iconSize, tint, onDec)
        content()
        StepIcon(ForgeIcons.AddCircle, iconSize, tint, onInc)
    }
}

@Composable
private fun StepIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Dp,
    tint: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Icon(
        icon, contentDescription = null, tint = tint,
        modifier = Modifier
            .size(size)
            .pressScale(interaction, pressed = 0.86f)
            .clickable(interaction, indication = null, onClick = onClick),
    )
}

/** Equal-width segmented control; selected cell = lime fill + dark text. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    cellHeight: Dp = 44.dp,
    unselectedBg: Color = GfColor.Surface,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val isSel = opt == selected
            SegmentedCell(
                text = label(opt),
                selected = isSel,
                height = cellHeight,
                unselectedBg = unselectedBg,
                onClick = { onSelect(opt) },
            )
        }
    }
}

@Composable
private fun RowScope.SegmentedCell(
    text: String,
    selected: Boolean,
    height: Dp,
    unselectedBg: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GfColor.Accent else unselectedBg)
            .then(
                if (selected) Modifier
                else Modifier.border(BorderStroke(1.dp, GfColor.Hairline08), RoundedCornerShape(12.dp)),
            )
            .pressScale(interaction)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) GfColor.OnAccent else GfColor.TextDim,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.W700,
            fontSize = 13.5.sp,
        )
    }
}
