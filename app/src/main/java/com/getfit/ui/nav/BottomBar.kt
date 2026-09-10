package com.getfit.ui.nav

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getfit.core.anim.GfSpring
import com.getfit.core.theme.GfColor
import com.getfit.core.theme.Manrope
import com.getfit.core.ui.ForgeIcons
import com.getfit.ui.TAB_BUILD
import com.getfit.ui.TAB_EXERCISES
import com.getfit.ui.TAB_HOME
import com.getfit.ui.TAB_PROGRESS

private data class TabDef(val key: String, val icon: ImageVector, val label: String)

private val TABS = listOf(
    TabDef(TAB_HOME, ForgeIcons.Home, "Home"),
    TabDef(TAB_EXERCISES, ForgeIcons.FitnessCenter, "Exercises"),
    TabDef(TAB_BUILD, ForgeIcons.DashboardCustomize, "Build"),
    TabDef(TAB_PROGRESS, ForgeIcons.Monitoring, "Progress"),
)

@Composable
fun BottomBar(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val activeIndex = TABS.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GfColor.Background.copy(alpha = 0.92f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            val cell = maxWidth / TABS.size
            val pillW = 64.dp
            val pillH = 34.dp
            val indicatorX by animateDpAsState(
                targetValue = cell * activeIndex + (cell - pillW) / 2,
                animationSpec = GfSpring.overshoot(),
                label = "tabIndicator",
            )
            // Sliding pill — sits exactly over the icon box (both top-aligned, same height).
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .size(width = pillW, height = pillH)
                    .clip(RoundedCornerShape(999.dp))
                    .background(GfColor.AccentFill15),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TABS.forEach { tab ->
                    val active = tab.key == selected
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .width(cell)
                            .clickable(interaction, indication = null) { onSelect(tab.key) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Icon centered inside a box the exact size of the pill -> perfect fit.
                        Box(Modifier.height(pillH), contentAlignment = Alignment.Center) {
                            Icon(
                                tab.icon, contentDescription = tab.label,
                                tint = if (active) GfColor.Accent else GfColor.TextFaint,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            tab.label,
                            color = if (active) GfColor.Accent else GfColor.TextFaint,
                            fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 10.5.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
