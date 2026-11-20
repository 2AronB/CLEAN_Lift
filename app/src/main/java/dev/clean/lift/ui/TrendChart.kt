package dev.clean.lift.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.clean.lift.R
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

data class TrendValue(val time: Long, val value: Double, val recordId: Long = 0)

internal fun displayNumber(value: Double): String = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(value)
internal fun displayDate(time: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))

@Composable
internal fun TrendChart(values: List<TrendValue>, unit: String, label: String,
                        window: LongRange? = null, referenceMarks: List<Double> = emptyList(), onPoint: ((TrendValue) -> Unit)? = null,
                        dateLabel: (Long) -> String = ::displayDate) {
    if (values.isEmpty()) { Text(stringResource(R.string.no_chart_data)); return }
    val ordered = values.sortedBy { it.time }
    val low = (ordered.map { it.value } + referenceMarks).min()
    val high = (ordered.map { it.value } + referenceMarks).max()
    val range = (high - low).takeIf { it > 0 } ?: maxOf(1.0, high * 0.1)
    val bottom = low - range * 0.1
    val top = high + range * 0.1
    val first = window?.first ?: ordered.first().time
    val last = window?.last ?: ordered.last().time
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(R.string.chart_accessibility, label, ordered.size, displayNumber(low), displayNumber(high), unit)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${displayNumber(top)} $unit", style = MaterialTheme.typography.labelSmall)
        Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = description }
            .pointerInput(ordered, first, last, bottom, top, onPoint) {
                if (onPoint != null) detectTapGestures { tap ->
                    val inset = 6.dp.toPx()
                    val width = (size.width - inset * 2).coerceAtLeast(1f)
                    val height = (size.height - inset * 2).coerceAtLeast(1f)
                    ordered.minByOrNull { value ->
                        val x = inset + width * (if (last == first) 0.5 else (value.time - first).toDouble() / (last - first))
                        val y = inset + height * (1 - (value.value - bottom) / (top - bottom))
                        (tap.x - x) * (tap.x - x) + (tap.y - y) * (tap.y - y)
                    }?.let(onPoint)
                }
            }) {
            val inset = 6.dp.toPx()
            val width = (size.width - inset * 2).coerceAtLeast(1f)
            val height = (size.height - inset * 2).coerceAtLeast(1f)
            repeat(3) { index ->
                val y = inset + height * index / 2
                drawLine(grid, Offset(inset, y), Offset(inset + width, y), strokeWidth = 1.dp.toPx())
            }
            referenceMarks.forEach { value ->
                val y = inset + height * (1 - (value - bottom) / (top - bottom)).toFloat()
                drawLine(grid, Offset(inset, y), Offset(inset + width, y), strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            }
            val path = Path()
            ordered.forEachIndexed { index, value ->
                val x = inset + width * (if (last == first) 0.5 else (value.time - first).toDouble() / (last - first)).toFloat()
                val y = inset + height * (1 - (value.value - bottom) / (top - bottom)).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color, radius = 3.dp.toPx(), center = Offset(x, y))
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        }
        Text("${displayNumber(bottom)} $unit", style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(dateLabel(first), style = MaterialTheme.typography.labelSmall)
            if (last != first) Text(dateLabel(last), style = MaterialTheme.typography.labelSmall)
        }
    }
}
