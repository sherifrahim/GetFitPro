package com.getfit.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Forge's own icon set: a bold, chamfered/geometric glyph language (flat-cut
 * facets, octagons in place of true circles) that replaces the stock Material
 * Icons Extended set used throughout the app. Every glyph here is drawn from
 * scratch on a 24x24 grid so it drops straight into existing `Icon(...)` call
 * sites with no size/layout changes. See msIcon() in MsIcon.kt for the
 * name -> icon mapping.
 *
 * Each icon is built once via `by lazy` (ImageVector construction is not
 * free) and cached for the process lifetime, mirroring how
 * androidx.compose.material.icons.Icons.Filled.* vectors are themselves
 * lazily-built singletons.
 */
object ForgeIcons {
    val Bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Bolt",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(14.0f, 2.0f)
            lineTo(7.0f, 13.0f)
            lineTo(11.0f, 13.0f)
            lineTo(9.0f, 22.0f)
            lineTo(17.0f, 10.0f)
            lineTo(13.0f, 10.0f)
            close()
        }.build()
    }

    val PlayArrow: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.PlayArrow",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(7.0f, 4.0f)
            lineTo(19.0f, 12.0f)
            lineTo(7.0f, 20.0f)
            close()
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Pause",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 4.0f)
            lineTo(10.0f, 4.0f)
            lineTo(10.0f, 20.0f)
            lineTo(6.0f, 20.0f)
            close()
            moveTo(14.0f, 4.0f)
            lineTo(18.0f, 4.0f)
            lineTo(18.0f, 20.0f)
            lineTo(14.0f, 20.0f)
            close()
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Check",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 12.5f)
            lineTo(9.0f, 17.5f)
            lineTo(20.0f, 6.0f)
            lineTo(18.0f, 4.0f)
            lineTo(9.0f, 13.0f)
            lineTo(6.0f, 10.0f)
            close()
        }.build()
    }

    val CheckCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.CheckCircle",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.22f, 3.0f)
            lineTo(15.78f, 3.0f)
            lineTo(21.0f, 8.22f)
            lineTo(21.0f, 15.78f)
            lineTo(15.78f, 21.0f)
            lineTo(8.22f, 21.0f)
            lineTo(3.0f, 15.78f)
            lineTo(3.0f, 8.22f)
            close()
            moveTo(7.5f, 12.3f)
            lineTo(10.6f, 15.4f)
            lineTo(17.0f, 9.0f)
            lineTo(15.6f, 7.6f)
            lineTo(10.6f, 12.6f)
            lineTo(8.9f, 10.9f)
            close()
        }.build()
    }

    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Add",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(10.5f, 4.0f)
            lineTo(13.5f, 4.0f)
            lineTo(13.5f, 10.5f)
            lineTo(20.0f, 10.5f)
            lineTo(20.0f, 13.5f)
            lineTo(13.5f, 13.5f)
            lineTo(13.5f, 20.0f)
            lineTo(10.5f, 20.0f)
            lineTo(10.5f, 13.5f)
            lineTo(4.0f, 13.5f)
            lineTo(4.0f, 10.5f)
            lineTo(10.5f, 10.5f)
            close()
        }.build()
    }

    val AddCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.AddCircle",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(8.22f, 3.0f)
            lineTo(15.78f, 3.0f)
            lineTo(21.0f, 8.22f)
            lineTo(21.0f, 15.78f)
            lineTo(15.78f, 21.0f)
            lineTo(8.22f, 21.0f)
            lineTo(3.0f, 15.78f)
            lineTo(3.0f, 8.22f)
            close()
            moveTo(10.8f, 7.0f)
            lineTo(13.2f, 7.0f)
            lineTo(13.2f, 10.8f)
            lineTo(17.0f, 10.8f)
            lineTo(17.0f, 13.2f)
            lineTo(13.2f, 13.2f)
            lineTo(13.2f, 17.0f)
            lineTo(10.8f, 17.0f)
            lineTo(10.8f, 13.2f)
            lineTo(7.0f, 13.2f)
            lineTo(7.0f, 10.8f)
            lineTo(10.8f, 10.8f)
            close()
        }.build()
    }

    val Remove: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Remove",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 10.5f)
            lineTo(20.0f, 10.5f)
            lineTo(20.0f, 13.5f)
            lineTo(4.0f, 13.5f)
            close()
        }.build()
    }

    val RemoveCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.RemoveCircle",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(8.22f, 3.0f)
            lineTo(15.78f, 3.0f)
            lineTo(21.0f, 8.22f)
            lineTo(21.0f, 15.78f)
            lineTo(15.78f, 21.0f)
            lineTo(8.22f, 21.0f)
            lineTo(3.0f, 15.78f)
            lineTo(3.0f, 8.22f)
            close()
            moveTo(7.0f, 10.8f)
            lineTo(17.0f, 10.8f)
            lineTo(17.0f, 13.2f)
            lineTo(7.0f, 13.2f)
            close()
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Close",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 6.1f)
            lineTo(6.1f, 5.0f)
            lineTo(12.0f, 10.9f)
            lineTo(17.9f, 5.0f)
            lineTo(19.0f, 6.1f)
            lineTo(13.1f, 12.0f)
            lineTo(19.0f, 17.9f)
            lineTo(17.9f, 19.0f)
            lineTo(12.0f, 13.1f)
            lineTo(6.1f, 19.0f)
            lineTo(5.0f, 17.9f)
            lineTo(10.9f, 12.0f)
            close()
        }.build()
    }

    val Delete: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Delete",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 7.0f)
            lineTo(18.0f, 7.0f)
            lineTo(17.0f, 21.0f)
            lineTo(7.0f, 21.0f)
            close()
            moveTo(9.0f, 4.0f)
            lineTo(15.0f, 4.0f)
            lineTo(15.0f, 6.0f)
            lineTo(9.0f, 6.0f)
            close()
            moveTo(4.0f, 7.0f)
            lineTo(20.0f, 7.0f)
            lineTo(20.0f, 9.0f)
            lineTo(4.0f, 9.0f)
            close()
        }.build()
    }

    val DeleteForever: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.DeleteForever",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 7.0f)
            lineTo(18.0f, 7.0f)
            lineTo(17.0f, 21.0f)
            lineTo(7.0f, 21.0f)
            close()
            moveTo(9.0f, 4.0f)
            lineTo(15.0f, 4.0f)
            lineTo(15.0f, 6.0f)
            lineTo(9.0f, 6.0f)
            close()
            moveTo(4.0f, 7.0f)
            lineTo(20.0f, 7.0f)
            lineTo(20.0f, 9.0f)
            lineTo(4.0f, 9.0f)
            close()
            moveTo(9.3f, 12.0f)
            lineTo(10.6f, 12.0f)
            lineTo(12.0f, 14.0f)
            lineTo(13.4f, 12.0f)
            lineTo(14.7f, 12.0f)
            lineTo(12.9f, 15.5f)
            lineTo(14.7f, 19.0f)
            lineTo(13.4f, 19.0f)
            lineTo(12.0f, 17.0f)
            lineTo(10.6f, 19.0f)
            lineTo(9.3f, 19.0f)
            lineTo(11.1f, 15.5f)
            close()
        }.build()
    }

    val Warning: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Warning",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 3.0f)
            lineTo(22.0f, 20.0f)
            lineTo(2.0f, 20.0f)
            close()
            moveTo(11.0f, 9.0f)
            lineTo(13.0f, 9.0f)
            lineTo(12.7f, 15.0f)
            lineTo(11.3f, 15.0f)
            close()
            moveTo(11.3f, 16.3f)
            lineTo(12.7f, 16.3f)
            lineTo(12.7f, 17.7f)
            lineTo(11.3f, 17.7f)
            close()
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Search",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(7.77f, 4.0f)
            lineTo(13.23f, 4.0f)
            lineTo(17.0f, 7.77f)
            lineTo(17.0f, 13.23f)
            lineTo(13.23f, 17.0f)
            lineTo(7.77f, 17.0f)
            lineTo(4.0f, 13.23f)
            lineTo(4.0f, 7.77f)
            close()
            moveTo(6.2f, 8.69f)
            lineTo(6.2f, 12.31f)
            lineTo(8.69f, 14.8f)
            lineTo(12.31f, 14.8f)
            lineTo(14.8f, 12.31f)
            lineTo(14.8f, 8.69f)
            lineTo(12.31f, 6.2f)
            lineTo(8.69f, 6.2f)
            close()
            moveTo(15.5f, 15.5f)
            lineTo(17.0f, 14.0f)
            lineTo(21.5f, 18.5f)
            lineTo(20.0f, 20.0f)
            close()
        }.build()
    }

    val SearchOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.SearchOff",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(7.77f, 4.0f)
            lineTo(13.23f, 4.0f)
            lineTo(17.0f, 7.77f)
            lineTo(17.0f, 13.23f)
            lineTo(13.23f, 17.0f)
            lineTo(7.77f, 17.0f)
            lineTo(4.0f, 13.23f)
            lineTo(4.0f, 7.77f)
            close()
            moveTo(6.2f, 8.69f)
            lineTo(6.2f, 12.31f)
            lineTo(8.69f, 14.8f)
            lineTo(12.31f, 14.8f)
            lineTo(14.8f, 12.31f)
            lineTo(14.8f, 8.69f)
            lineTo(12.31f, 6.2f)
            lineTo(8.69f, 6.2f)
            close()
            moveTo(15.5f, 15.5f)
            lineTo(17.0f, 14.0f)
            lineTo(21.5f, 18.5f)
            lineTo(20.0f, 20.0f)
            close()
            moveTo(4.5f, 3.5f)
            lineTo(20.5f, 19.5f)
            lineTo(19.1f, 20.9f)
            lineTo(3.1f, 4.9f)
            close()
        }.build()
    }

    val Flag: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Flag",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 3.0f)
            lineTo(6.0f, 21.0f)
            lineTo(4.0f, 21.0f)
            lineTo(4.0f, 3.0f)
            close()
            moveTo(6.0f, 4.0f)
            lineTo(19.0f, 4.0f)
            lineTo(15.5f, 8.5f)
            lineTo(19.0f, 13.0f)
            lineTo(6.0f, 13.0f)
            close()
        }.build()
    }

    val SkipNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.SkipNext",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 5.0f)
            lineTo(13.0f, 12.0f)
            lineTo(5.0f, 19.0f)
            close()
            moveTo(15.0f, 5.0f)
            lineTo(18.0f, 5.0f)
            lineTo(18.0f, 19.0f)
            lineTo(15.0f, 19.0f)
            close()
        }.build()
    }

    val KeyboardArrowUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.KeyboardArrowUp",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 8.0f)
            lineTo(19.0f, 15.5f)
            lineTo(16.8f, 17.5f)
            lineTo(12.0f, 12.2f)
            lineTo(7.2f, 17.5f)
            lineTo(5.0f, 15.5f)
            close()
        }.build()
    }

    val KeyboardArrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.KeyboardArrowDown",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 16.0f)
            lineTo(5.0f, 8.5f)
            lineTo(7.2f, 6.5f)
            lineTo(12.0f, 11.8f)
            lineTo(16.8f, 6.5f)
            lineTo(19.0f, 8.5f)
            close()
        }.build()
    }

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.ArrowBack",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(11.0f, 4.5f)
            lineTo(13.0f, 6.5f)
            lineTo(8.5f, 11.0f)
            lineTo(21.0f, 11.0f)
            lineTo(21.0f, 13.5f)
            lineTo(8.5f, 13.5f)
            lineTo(13.0f, 18.0f)
            lineTo(11.0f, 20.0f)
            lineTo(3.0f, 12.0f)
            close()
        }.build()
    }

    val ArrowForward: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.ArrowForward",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(13.0f, 4.5f)
            lineTo(11.0f, 6.5f)
            lineTo(15.5f, 11.0f)
            lineTo(3.0f, 11.0f)
            lineTo(3.0f, 13.5f)
            lineTo(15.5f, 13.5f)
            lineTo(11.0f, 18.0f)
            lineTo(13.0f, 20.0f)
            lineTo(21.0f, 12.0f)
            close()
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.ChevronRight",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.5f, 5.0f)
            lineTo(10.5f, 5.0f)
            lineTo(16.5f, 12.0f)
            lineTo(10.5f, 19.0f)
            lineTo(8.5f, 19.0f)
            lineTo(14.5f, 12.0f)
            close()
        }.build()
    }

    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.History",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.43f, 4.0f)
            lineTo(15.57f, 4.0f)
            lineTo(20.5f, 8.93f)
            lineTo(20.5f, 16.07f)
            lineTo(15.57f, 21.0f)
            lineTo(8.43f, 21.0f)
            lineTo(3.5f, 16.07f)
            lineTo(3.5f, 8.93f)
            close()
            moveTo(11.2f, 7.0f)
            lineTo(12.8f, 7.0f)
            lineTo(12.8f, 12.9f)
            lineTo(17.0f, 15.3f)
            lineTo(16.2f, 16.7f)
            lineTo(11.2f, 13.8f)
            close()
        }.build()
    }

    val EmojiEvents: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.EmojiEvents",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.0f, 4.0f)
            lineTo(16.0f, 4.0f)
            lineTo(16.0f, 11.0f)
            curveTo(16.0f, 14.3f, 14.2f, 16.6f, 12.0f, 16.6f)
            curveTo(9.8f, 16.6f, 8.0f, 14.3f, 8.0f, 11.0f)
            close()
            moveTo(8.0f, 6.0f)
            lineTo(4.5f, 6.0f)
            lineTo(4.5f, 9.0f)
            curveTo(4.5f, 10.9f, 6.0f, 12.4f, 8.0f, 12.7f)
            lineTo(8.0f, 10.2f)
            curveTo(7.2f, 9.9f, 6.7f, 9.2f, 6.7f, 8.3f)
            lineTo(6.7f, 7.5f)
            lineTo(8.0f, 7.5f)
            close()
            moveTo(16.0f, 6.0f)
            lineTo(19.5f, 6.0f)
            lineTo(19.5f, 9.0f)
            curveTo(19.5f, 10.9f, 18.0f, 12.4f, 16.0f, 12.7f)
            lineTo(16.0f, 10.2f)
            curveTo(16.8f, 9.9f, 17.3f, 9.2f, 17.3f, 8.3f)
            lineTo(17.3f, 7.5f)
            lineTo(16.0f, 7.5f)
            close()
            moveTo(10.5f, 17.0f)
            lineTo(13.5f, 17.0f)
            lineTo(13.5f, 19.0f)
            lineTo(10.5f, 19.0f)
            close()
            moveTo(8.5f, 19.0f)
            lineTo(15.5f, 19.0f)
            lineTo(15.5f, 21.0f)
            lineTo(8.5f, 21.0f)
            close()
        }.build()
    }

    val MilitaryTech: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.MilitaryTech",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.27f, 3.5f)
            lineTo(14.73f, 3.5f)
            lineTo(18.5f, 7.27f)
            lineTo(18.5f, 12.73f)
            lineTo(14.73f, 16.5f)
            lineTo(9.27f, 16.5f)
            lineTo(5.5f, 12.73f)
            lineTo(5.5f, 7.27f)
            close()
            moveTo(9.0f, 4.0f)
            lineTo(9.0f, 9.0f)
            lineTo(12.0f, 12.0f)
            lineTo(15.0f, 9.0f)
            lineTo(15.0f, 4.0f)
            lineTo(12.0f, 6.5f)
            close()
            moveTo(9.5f, 14.5f)
            lineTo(11.3f, 14.5f)
            lineTo(9.8f, 20.5f)
            lineTo(8.0f, 19.5f)
            close()
            moveTo(14.5f, 14.5f)
            lineTo(16.3f, 14.5f)
            lineTo(16.0f, 19.5f)
            lineTo(14.2f, 20.5f)
            close()
        }.build()
    }

    val Monitoring: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Monitoring",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 15.0f)
            lineTo(8.0f, 15.0f)
            lineTo(8.0f, 20.0f)
            lineTo(4.0f, 20.0f)
            close()
            moveTo(10.0f, 10.0f)
            lineTo(14.0f, 10.0f)
            lineTo(14.0f, 20.0f)
            lineTo(10.0f, 20.0f)
            close()
            moveTo(16.0f, 4.0f)
            lineTo(20.0f, 4.0f)
            lineTo(20.0f, 20.0f)
            lineTo(16.0f, 20.0f)
            close()
        }.build()
    }

    val TrendingUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.TrendingUp",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(3.0f, 17.0f)
            lineTo(9.0f, 11.0f)
            lineTo(13.0f, 15.0f)
            lineTo(19.5f, 7.5f)
            lineTo(21.0f, 9.0f)
            lineTo(13.0f, 18.5f)
            lineTo(9.0f, 14.5f)
            lineTo(4.5f, 19.0f)
            close()
            moveTo(16.0f, 7.0f)
            lineTo(21.0f, 7.0f)
            lineTo(21.0f, 12.0f)
            lineTo(18.5f, 9.5f)
            lineTo(16.5f, 11.5f)
            close()
        }.build()
    }

    val Timer: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Timer",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.43f, 4.5f)
            lineTo(15.57f, 4.5f)
            lineTo(20.5f, 9.43f)
            lineTo(20.5f, 16.57f)
            lineTo(15.57f, 21.5f)
            lineTo(8.43f, 21.5f)
            lineTo(3.5f, 16.57f)
            lineTo(3.5f, 9.43f)
            close()
            moveTo(9.0f, 2.0f)
            lineTo(15.0f, 2.0f)
            lineTo(15.0f, 4.2f)
            lineTo(9.0f, 4.2f)
            close()
            moveTo(11.2f, 7.5f)
            lineTo(12.8f, 7.5f)
            lineTo(12.8f, 13.4f)
            lineTo(17.0f, 15.8f)
            lineTo(16.2f, 17.2f)
            lineTo(11.2f, 14.3f)
            close()
        }.build()
    }

    val SelfImprovement: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.SelfImprovement",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(8.22f, 3.0f)
            lineTo(15.78f, 3.0f)
            lineTo(21.0f, 8.22f)
            lineTo(21.0f, 15.78f)
            lineTo(15.78f, 21.0f)
            lineTo(8.22f, 21.0f)
            lineTo(3.0f, 15.78f)
            lineTo(3.0f, 8.22f)
            close()
            moveTo(6.5f, 9.69f)
            lineTo(6.5f, 14.31f)
            lineTo(9.69f, 17.5f)
            lineTo(14.31f, 17.5f)
            lineTo(17.5f, 14.31f)
            lineTo(17.5f, 9.69f)
            lineTo(14.31f, 6.5f)
            lineTo(9.69f, 6.5f)
            close()
            moveTo(10.8f, 10.8f)
            lineTo(13.2f, 10.8f)
            lineTo(13.2f, 13.2f)
            lineTo(10.8f, 13.2f)
            close()
        }.build()
    }

    val DirectionsRun: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.DirectionsRun",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(3.0f, 7.0f)
            lineTo(8.0f, 12.0f)
            lineTo(3.0f, 17.0f)
            lineTo(5.5f, 17.0f)
            lineTo(10.5f, 12.0f)
            lineTo(5.5f, 7.0f)
            close()
            moveTo(11.0f, 7.0f)
            lineTo(16.0f, 12.0f)
            lineTo(11.0f, 17.0f)
            lineTo(13.5f, 17.0f)
            lineTo(18.5f, 12.0f)
            lineTo(13.5f, 7.0f)
            close()
        }.build()
    }

    val Straighten: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Straighten",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(3.0f, 8.0f)
            lineTo(21.0f, 8.0f)
            lineTo(21.0f, 16.0f)
            lineTo(3.0f, 16.0f)
            close()
            moveTo(6.0f, 8.0f)
            lineTo(6.0f, 11.5f)
            lineTo(7.2f, 11.5f)
            lineTo(7.2f, 8.0f)
            close()
            moveTo(9.4f, 8.0f)
            lineTo(9.4f, 13.0f)
            lineTo(10.6f, 13.0f)
            lineTo(10.6f, 8.0f)
            close()
            moveTo(12.8f, 8.0f)
            lineTo(12.8f, 11.5f)
            lineTo(14.0f, 11.5f)
            lineTo(14.0f, 8.0f)
            close()
            moveTo(16.2f, 8.0f)
            lineTo(16.2f, 13.0f)
            lineTo(17.4f, 13.0f)
            lineTo(17.4f, 8.0f)
            close()
        }.build()
    }

    val VolumeUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.VolumeUp",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 9.0f)
            lineTo(8.0f, 9.0f)
            lineTo(13.0f, 4.5f)
            lineTo(13.0f, 19.5f)
            lineTo(8.0f, 15.0f)
            lineTo(4.0f, 15.0f)
            close()
            moveTo(15.5f, 7.5f)
            lineTo(17.0f, 6.3f)
            curveTo(19.0f, 8.0f, 20.2f, 9.9f, 20.2f, 12.0f)
            curveTo(20.2f, 14.1f, 19.0f, 16.0f, 17.0f, 17.7f)
            lineTo(15.5f, 16.5f)
            curveTo(17.1f, 15.1f, 18.0f, 13.6f, 18.0f, 12.0f)
            curveTo(18.0f, 10.4f, 17.1f, 8.9f, 15.5f, 7.5f)
            close()
        }.build()
    }

    val Vibration: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Vibration",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.5f, 4.0f)
            lineTo(15.5f, 4.0f)
            lineTo(15.5f, 20.0f)
            lineTo(8.5f, 20.0f)
            close()
            moveTo(0.8f, 10.0f)
            lineTo(2.3f, 10.0f)
            lineTo(2.3f, 14.0f)
            lineTo(0.8f, 14.0f)
            close()
            moveTo(3.8f, 7.5f)
            lineTo(5.3f, 7.5f)
            lineTo(5.3f, 16.5f)
            lineTo(3.8f, 16.5f)
            close()
            moveTo(18.7f, 7.5f)
            lineTo(20.2f, 7.5f)
            lineTo(20.2f, 16.5f)
            lineTo(18.7f, 16.5f)
            close()
            moveTo(21.7f, 10.0f)
            lineTo(23.2f, 10.0f)
            lineTo(23.2f, 14.0f)
            lineTo(21.7f, 14.0f)
            close()
        }.build()
    }

    val SmartDisplay: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.SmartDisplay",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.22f, 3.0f)
            lineTo(15.78f, 3.0f)
            lineTo(21.0f, 8.22f)
            lineTo(21.0f, 15.78f)
            lineTo(15.78f, 21.0f)
            lineTo(8.22f, 21.0f)
            lineTo(3.0f, 15.78f)
            lineTo(3.0f, 8.22f)
            close()
            moveTo(10.0f, 8.0f)
            lineTo(17.0f, 12.0f)
            lineTo(10.0f, 16.0f)
            close()
        }.build()
    }

    val DashboardCustomize: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.DashboardCustomize",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 4.0f)
            lineTo(10.5f, 4.0f)
            lineTo(10.5f, 10.5f)
            lineTo(4.0f, 10.5f)
            close()
            moveTo(13.5f, 4.0f)
            lineTo(20.0f, 4.0f)
            lineTo(20.0f, 10.5f)
            lineTo(13.5f, 10.5f)
            close()
            moveTo(4.0f, 13.5f)
            lineTo(10.5f, 13.5f)
            lineTo(10.5f, 20.0f)
            lineTo(4.0f, 20.0f)
            close()
            moveTo(13.5f, 13.5f)
            lineTo(20.0f, 13.5f)
            lineTo(20.0f, 20.0f)
            lineTo(13.5f, 20.0f)
            close()
        }.build()
    }

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Home",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 3.0f)
            lineTo(21.0f, 11.0f)
            lineTo(18.5f, 11.0f)
            lineTo(18.5f, 20.0f)
            lineTo(13.5f, 20.0f)
            lineTo(13.5f, 14.0f)
            lineTo(10.5f, 14.0f)
            lineTo(10.5f, 20.0f)
            lineTo(5.5f, 20.0f)
            lineTo(5.5f, 11.0f)
            lineTo(3.0f, 11.0f)
            close()
        }.build()
    }

    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Lock",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 10.0f)
            lineTo(18.0f, 10.0f)
            lineTo(18.0f, 21.0f)
            lineTo(6.0f, 21.0f)
            close()
            moveTo(8.0f, 10.0f)
            lineTo(8.0f, 7.5f)
            curveTo(8.0f, 4.5f, 9.8f, 2.5f, 12.0f, 2.5f)
            curveTo(14.2f, 2.5f, 16.0f, 4.5f, 16.0f, 7.5f)
            lineTo(16.0f, 10.0f)
            lineTo(13.8f, 10.0f)
            lineTo(13.8f, 7.5f)
            curveTo(13.8f, 5.6f, 13.0f, 4.5f, 12.0f, 4.5f)
            curveTo(11.0f, 4.5f, 10.2f, 5.6f, 10.2f, 7.5f)
            lineTo(10.2f, 10.0f)
            close()
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.Info",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.22f, 3.0f)
            lineTo(15.78f, 3.0f)
            lineTo(21.0f, 8.22f)
            lineTo(21.0f, 15.78f)
            lineTo(15.78f, 21.0f)
            lineTo(8.22f, 21.0f)
            lineTo(3.0f, 15.78f)
            lineTo(3.0f, 8.22f)
            close()
            moveTo(10.8f, 10.0f)
            lineTo(13.2f, 10.0f)
            lineTo(13.2f, 17.0f)
            lineTo(10.8f, 17.0f)
            close()
            moveTo(10.8f, 6.3f)
            lineTo(13.2f, 6.3f)
            lineTo(13.2f, 8.7f)
            lineTo(10.8f, 8.7f)
            close()
        }.build()
    }

    val FitnessCenter: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.FitnessCenter",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(2.0f, 10.5f)
            lineTo(4.5f, 10.5f)
            lineTo(4.5f, 13.5f)
            lineTo(2.0f, 13.5f)
            close()
            moveTo(5.5f, 8.0f)
            lineTo(7.5f, 8.0f)
            lineTo(7.5f, 16.0f)
            lineTo(5.5f, 16.0f)
            close()
            moveTo(8.3f, 10.8f)
            lineTo(15.7f, 10.8f)
            lineTo(15.7f, 13.2f)
            lineTo(8.3f, 13.2f)
            close()
            moveTo(16.5f, 8.0f)
            lineTo(18.5f, 8.0f)
            lineTo(18.5f, 16.0f)
            lineTo(16.5f, 16.0f)
            close()
            moveTo(19.5f, 10.5f)
            lineTo(22.0f, 10.5f)
            lineTo(22.0f, 13.5f)
            lineTo(19.5f, 13.5f)
            close()
        }.build()
    }

    val LocalFireDepartment: ImageVector by lazy {
        ImageVector.Builder(
            name = "Forge.LocalFireDepartment",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f, strokeAlpha = 1f,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 2.0f)
            curveTo(16.0f, 7.0f, 18.0f, 10.5f, 17.0f, 15.0f)
            curveTo(16.3f, 18.5f, 14.0f, 21.0f, 11.5f, 22.5f)
            curveTo(12.5f, 19.5f, 11.8f, 17.0f, 9.7f, 15.8f)
            curveTo(8.3f, 17.0f, 8.3f, 19.5f, 10.0f, 21.5f)
            curveTo(6.5f, 19.5f, 4.8f, 16.3f, 5.3f, 12.8f)
            curveTo(5.8f, 9.0f, 8.5f, 5.0f, 12.0f, 2.0f)
            close()
        }.build()
    }

    // --- added with routines / profile / history ---

    private fun icon(name: String, body: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "Forge.$name",
            defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).path(fill = SolidColor(Color.Black), fillAlpha = 1f, strokeAlpha = 1f, pathFillType = PathFillType.NonZero, pathBuilder = body).build()

    /** Three dots — the "⋯" menu affordance. */
    val MoreHoriz: ImageVector by lazy {
        icon("MoreHoriz") {
            for (cx in listOf(5.5f, 12f, 18.5f)) {
                moveTo(cx - 2f, 12f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + 2f, y1 = 12f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - 2f, y1 = 12f)
                close()
            }
        }
    }

    /** Pencil. */
    val Edit: ImageVector by lazy {
        icon("Edit") {
            moveTo(3f, 17.25f); lineTo(3f, 21f); lineTo(6.75f, 21f); lineTo(17.81f, 9.94f); lineTo(14.06f, 6.19f); close()
            moveTo(20.71f, 7.04f)
            curveTo(21.1f, 6.65f, 21.1f, 6.02f, 20.71f, 5.63f)
            lineTo(18.37f, 3.29f)
            curveTo(17.98f, 2.9f, 17.35f, 2.9f, 16.96f, 3.29f)
            lineTo(15.13f, 5.12f); lineTo(18.88f, 8.87f); close()
        }
    }

    /** Two overlapping sheets — duplicate. */
    val ContentCopy: ImageVector by lazy {
        icon("ContentCopy") {
            moveTo(16f, 1f); lineTo(4f, 1f); lineTo(4f, 15f); lineTo(6f, 15f); lineTo(6f, 3f); lineTo(16f, 3f); close()
            moveTo(19f, 5f); lineTo(8f, 5f); lineTo(8f, 23f); lineTo(19f, 23f); close()
            moveTo(17f, 21f); lineTo(10f, 21f); lineTo(10f, 7f); lineTo(17f, 7f); close()
        }
    }

    /** Arrow into a bar — "do this next". */
    val EventUpcoming: ImageVector by lazy {
        icon("EventUpcoming") {
            moveTo(4f, 11f); lineTo(13.17f, 11f); lineTo(9.59f, 7.41f); lineTo(11f, 6f); lineTo(17f, 12f); lineTo(11f, 18f); lineTo(9.59f, 16.59f); lineTo(13.17f, 13f); lineTo(4f, 13f); close()
            moveTo(19f, 5f); lineTo(21f, 5f); lineTo(21f, 19f); lineTo(19f, 19f); close()
        }
    }

    /** Heart — heart rate. */
    val Favorite: ImageVector by lazy {
        icon("Favorite") {
            moveTo(12f, 21.35f)
            lineTo(10.55f, 20.03f)
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
            close()
        }
    }

    /** Person silhouette — profile. */
    val Person: ImageVector by lazy {
        icon("Person") {
            moveTo(12f, 12f)
            curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
            curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
            curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
            curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
            close()
            moveTo(12f, 14f)
            curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
            lineTo(4f, 20f); lineTo(20f, 20f); lineTo(20f, 18f)
            curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
            close()
        }
    }

    /** Calendar grid. */
    val CalendarMonth: ImageVector by lazy {
        icon("CalendarMonth") {
            moveTo(19f, 4f); lineTo(18f, 4f); lineTo(18f, 2f); lineTo(16f, 2f); lineTo(16f, 4f); lineTo(8f, 4f); lineTo(8f, 2f); lineTo(6f, 2f); lineTo(6f, 4f); lineTo(5f, 4f)
            curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
            lineTo(3f, 20f)
            curveTo(3f, 21.1f, 3.9f, 22f, 5f, 22f)
            lineTo(19f, 22f)
            curveTo(20.1f, 22f, 21f, 21.1f, 21f, 20f)
            lineTo(21f, 6f)
            curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
            close()
            moveTo(19f, 20f); lineTo(5f, 20f); lineTo(5f, 9f); lineTo(19f, 9f); close()
        }
    }

    /** Clipboard with a tick — "saved as routine". */
    val PlaylistAddCheck: ImageVector by lazy {
        icon("PlaylistAddCheck") {
            moveTo(3f, 10f); lineTo(14f, 10f); lineTo(14f, 12f); lineTo(3f, 12f); close()
            moveTo(3f, 6f); lineTo(14f, 6f); lineTo(14f, 8f); lineTo(3f, 8f); close()
            moveTo(3f, 14f); lineTo(10f, 14f); lineTo(10f, 16f); lineTo(3f, 16f); close()
            moveTo(20.59f, 11.93f); lineTo(22f, 13.34f); lineTo(15.01f, 20.34f); lineTo(10.77f, 16.1f); lineTo(12.18f, 14.69f); lineTo(15.01f, 17.52f); close()
        }
    }

    /** Psychology — the AI coach. */
    val Psychology: ImageVector by lazy {
        icon("Psychology") {
            moveTo(13f, 2f)
            curveTo(9.13f, 2f, 6f, 5.13f, 6f, 9f)
            lineTo(6f, 11.2f); lineTo(4.2f, 14f); lineTo(6f, 14f); lineTo(6f, 17f)
            curveTo(6f, 18.1f, 6.9f, 19f, 8f, 19f)
            lineTo(10f, 19f); lineTo(10f, 22f); lineTo(17f, 22f); lineTo(17f, 17.3f)
            curveTo(18.86f, 16.07f, 20f, 13.99f, 20f, 11.7f)
            lineTo(20f, 9f)
            curveTo(20f, 5.13f, 16.87f, 2f, 13f, 2f)
            close()
            moveTo(13f, 6f)
            curveTo(14.66f, 6f, 16f, 7.34f, 16f, 9f)
            curveTo(16f, 10.66f, 14.66f, 12f, 13f, 12f)
            curveTo(11.34f, 12f, 10f, 10.66f, 10f, 9f)
            curveTo(10f, 7.34f, 11.34f, 6f, 13f, 6f)
            close()
        }
    }

}
