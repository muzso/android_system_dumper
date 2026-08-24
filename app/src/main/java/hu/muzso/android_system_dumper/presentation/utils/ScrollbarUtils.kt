package hu.muzso.android_system_dumper.presentation.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extension modifier to draw a vertical scrollbar for a [ScrollState].
 * 
 * @param state The scroll state to track.
 * @param color The color of the scrollbar thumb.
 * @param width The width of the scrollbar thumb.
 */
fun Modifier.drawVerticalScrollbar(
    state: ScrollState,
    color: Color = Color.Gray,
    width: Dp = 4.dp
): Modifier = drawWithContent {
    drawContent()

    val viewPortHeight = size.height
    val totalHeight = state.maxValue + viewPortHeight
    val scrollValue = state.value.toFloat()

    if (totalHeight > viewPortHeight) {
        val scrollbarHeight = (viewPortHeight / totalHeight) * viewPortHeight
        val scrollbarOffset = (scrollValue / totalHeight) * viewPortHeight

        drawRoundRect(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(size.width - width.toPx(), scrollbarOffset),
            size = Size(width.toPx(), scrollbarHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}
