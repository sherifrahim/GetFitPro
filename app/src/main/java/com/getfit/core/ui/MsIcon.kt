package com.getfit.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/** Resolve a prototype Material-Symbol name to a Material Icons Extended vector. */
fun msIcon(name: String): ImageVector = when (name) {
    "bolt" -> Icons.Filled.Bolt
    "play_arrow" -> Icons.Filled.PlayArrow
    "local_fire_department" -> Icons.Filled.LocalFireDepartment
    "check" -> Icons.Filled.Check
    "check_circle" -> Icons.Filled.CheckCircle
    "add" -> Icons.Filled.Add
    "add_circle" -> Icons.Filled.AddCircle
    "remove" -> Icons.Filled.Remove
    "remove_circle" -> Icons.Filled.RemoveCircle
    "close" -> Icons.Filled.Close
    "delete" -> Icons.Filled.Delete
    "delete_forever" -> Icons.Filled.DeleteForever
    "warning" -> Icons.Filled.Warning
    "search" -> Icons.Filled.Search
    "search_off" -> Icons.Filled.SearchOff
    "flag" -> Icons.Filled.Flag
    "skip_next" -> Icons.Filled.SkipNext
    "pause" -> Icons.Filled.Pause
    "keyboard_arrow_up" -> Icons.Filled.KeyboardArrowUp
    "keyboard_arrow_down" -> Icons.Filled.KeyboardArrowDown
    "arrow_back" -> Icons.AutoMirrored.Filled.ArrowBack
    "arrow_forward" -> Icons.AutoMirrored.Filled.ArrowForward
    "chevron_right" -> Icons.Filled.ChevronRight
    "history" -> Icons.Filled.History
    "emoji_events" -> Icons.Filled.EmojiEvents
    "military_tech" -> Icons.Filled.MilitaryTech
    "monitoring" -> Icons.Filled.BarChart
    "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
    "timer" -> Icons.Filled.Timer
    "self_improvement" -> Icons.Filled.SelfImprovement
    "directions_run", "sprint" -> Icons.AutoMirrored.Filled.DirectionsRun
    "straighten" -> Icons.Filled.Straighten
    "volume_up" -> Icons.AutoMirrored.Filled.VolumeUp
    "vibration" -> Icons.Filled.Vibration
    "smart_display" -> Icons.Filled.SmartDisplay
    "dashboard_customize" -> Icons.Filled.DashboardCustomize
    "home" -> Icons.Filled.Home
    "lock" -> Icons.Filled.Lock
    "info" -> Icons.Filled.Info
    "fitness_center", "exercise" -> Icons.Filled.FitnessCenter
    else -> Icons.Filled.FitnessCenter
}
