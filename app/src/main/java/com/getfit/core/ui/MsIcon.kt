package com.getfit.core.ui

import androidx.compose.ui.graphics.vector.ImageVector

/** Resolve a prototype Material-Symbol name to a Forge icon (see ForgeIcons.kt). */
fun msIcon(name: String): ImageVector = when (name) {
    "bolt" -> ForgeIcons.Bolt
    "play_arrow" -> ForgeIcons.PlayArrow
    "local_fire_department" -> ForgeIcons.LocalFireDepartment
    "check" -> ForgeIcons.Check
    "check_circle" -> ForgeIcons.CheckCircle
    "add" -> ForgeIcons.Add
    "add_circle" -> ForgeIcons.AddCircle
    "remove" -> ForgeIcons.Remove
    "remove_circle" -> ForgeIcons.RemoveCircle
    "close" -> ForgeIcons.Close
    "delete" -> ForgeIcons.Delete
    "delete_forever" -> ForgeIcons.DeleteForever
    "warning" -> ForgeIcons.Warning
    "search" -> ForgeIcons.Search
    "search_off" -> ForgeIcons.SearchOff
    "flag" -> ForgeIcons.Flag
    "skip_next" -> ForgeIcons.SkipNext
    "pause" -> ForgeIcons.Pause
    "keyboard_arrow_up" -> ForgeIcons.KeyboardArrowUp
    "keyboard_arrow_down" -> ForgeIcons.KeyboardArrowDown
    "arrow_back" -> ForgeIcons.ArrowBack
    "arrow_forward" -> ForgeIcons.ArrowForward
    "chevron_right" -> ForgeIcons.ChevronRight
    "history" -> ForgeIcons.History
    "emoji_events" -> ForgeIcons.EmojiEvents
    "military_tech" -> ForgeIcons.MilitaryTech
    "monitoring" -> ForgeIcons.Monitoring
    "trending_up" -> ForgeIcons.TrendingUp
    "timer" -> ForgeIcons.Timer
    "self_improvement" -> ForgeIcons.SelfImprovement
    "directions_run", "sprint" -> ForgeIcons.DirectionsRun
    "straighten" -> ForgeIcons.Straighten
    "volume_up" -> ForgeIcons.VolumeUp
    "vibration" -> ForgeIcons.Vibration
    "smart_display" -> ForgeIcons.SmartDisplay
    "dashboard_customize" -> ForgeIcons.DashboardCustomize
    "home" -> ForgeIcons.Home
    "lock" -> ForgeIcons.Lock
    "info" -> ForgeIcons.Info
    "fitness_center", "exercise" -> ForgeIcons.FitnessCenter
    "more_horiz" -> ForgeIcons.MoreHoriz
    "edit" -> ForgeIcons.Edit
    "content_copy" -> ForgeIcons.ContentCopy
    "event_upcoming" -> ForgeIcons.EventUpcoming
    "favorite" -> ForgeIcons.Favorite
    "person" -> ForgeIcons.Person
    "calendar_month" -> ForgeIcons.CalendarMonth
    "playlist_add_check" -> ForgeIcons.PlaylistAddCheck
    "psychology" -> ForgeIcons.Psychology
    else -> ForgeIcons.FitnessCenter
}
