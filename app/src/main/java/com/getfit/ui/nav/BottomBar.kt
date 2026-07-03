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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
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
import com.getfit.ui.TAB_BUILD
import com.getfit.ui.TAB_EXERCISES
import com.getfit.ui.TAB_HOME
import com.getfit.ui.TAB_PROGRESS

private data class TabDef(val key: String, val icon: ImageVector, val label: String)

private val TABS = listOf(
    TabDef(TAB_HOME, Icons.Filled.Home, "Home"),
    TabDef(TAB_EXERCISES, Icons.Filled.FitnessCenter, "Exercises"),
    TabDef(TAB_BUILD, Icons.Filled.DashboardCustomize, "Build"),
    TabDef(TAB_PROGRESS, Icons.Filled.BarChart, "Progress"),
)

@Composable
fun BottomBar(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val activeIndex = TABS.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GfColor.Background.copy(alpha = 0.92f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            val cell = maxWidth / TABS.size
            val indicatorX by animateDpAsState(
                targetValue = cell * activeIndex + (cell - 60.dp) / 2,
                animationSpec = GfSpring.overshoot(),
                label = "tabIndicator",
            )
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .size(width = 60.dp, height = 34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(GfColor.LimeFill15),
            )
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TABS.forEach { tab ->
                    val active = tab.key == selected
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .width(cell)
                            .clickable(interaction, indication = null) { onSelect(tab.key) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            tab.icon, contentDescription = tab.label,
                            tint = if (active) GfColor.Lime else GfColor.TextFaint,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            tab.label,
                            color = if (active) GfColor.Lime else GfColor.TextFaint,
                            fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 10.5.sp,
                        )
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 120.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(GfColor.Text.copy(alpha = 0.35f)))
        }
    }
}
