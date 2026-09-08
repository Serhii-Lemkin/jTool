package com.jtool.android.features.tracker.components.tracker_chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jtool.android.core.localization.Loc
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChartSeries(
    val color: Color,
    val points: List<Double?>
)

@Composable
fun TrendifyChart(
    series: List<ChartSeries>,
    timestamps: List<Long>,
    modifier: Modifier = Modifier
) {
    if (series.isEmpty() || series.all { it.points.count { p -> p != null } < 1 }) return

    val labelSize = 9.sp
    val dateFormat = remember(Loc.lang) {
        SimpleDateFormat("dd/MM", Locale.forLanguageTag(Loc.lang))
    }
    
    Canvas(modifier = modifier) {
        val textPaint = Paint().apply {
            color = Color.Gray.toArgb()
            textSize = labelSize.toPx()
            textAlign = Paint.Align.CENTER
        }

        val width = size.width
        val height = size.height
        
        val allPoints = series.flatMap { it.points }.filterNotNull()
        val maxVal = allPoints.maxOrNull() ?: 1.0
        val minVal = allPoints.minOrNull() ?: 0.0
        val range = (maxVal - minVal).coerceAtLeast(1.0).toFloat()
        
        val leftPadding = 30.dp.toPx()
        val rightPadding = 20.dp.toPx()
        val topPadding = 20.dp.toPx()
        val bottomPadding = 30.dp.toPx()
        
        val usableHeight = height - topPadding - bottomPadding
        val usableWidth = width - leftPadding - rightPadding

        // Y-axis labels (Min/Max)
        textPaint.textAlign = Paint.Align.RIGHT
        drawContext.canvas.nativeCanvas.drawText(
            maxVal.toString().removeSuffix(".0"),
            leftPadding - 8.dp.toPx(),
            topPadding + 4.dp.toPx(),
            textPaint
        )
        drawContext.canvas.nativeCanvas.drawText(
            minVal.toString().removeSuffix(".0"),
            leftPadding - 8.dp.toPx(),
            height - bottomPadding + 4.dp.toPx(),
            textPaint
        )

        series.forEach { s ->
            val pointsToDraw = s.points.count { it != null }
            if (pointsToDraw < 1) return@forEach
            
            val path = Path()
            var firstPoint = true
            
            s.points.forEachIndexed { index, value ->
                val x = leftPadding + index * (usableWidth / (s.points.size - 1).coerceAtLeast(1))
                
                // Draw X-axis date labels for first, last and middle points
                if (index == 0 || index == s.points.size - 1 || (s.points.size > 4 && index == s.points.size / 2)) {
                    val dateStr = dateFormat.format(Date(timestamps.getOrNull(index) ?: 0L))
                    textPaint.textAlign = Paint.Align.CENTER
                    drawContext.canvas.nativeCanvas.drawText(
                        dateStr,
                        x,
                        height - 8.dp.toPx(),
                        textPaint
                    )
                }

                if (value == null) return@forEachIndexed
                
                val y = (height - bottomPadding - ((value - minVal) / range * usableHeight)).toFloat()
                
                if (firstPoint) {
                    path.moveTo(x, y)
                    firstPoint = false
                } else {
                    path.lineTo(x, y)
                }
                
                drawCircle(
                    color = s.color,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )

                // Value label near point
                drawContext.canvas.nativeCanvas.drawText(
                    value.toString().removeSuffix(".0"),
                    x,
                    y - 6.dp.toPx(),
                    textPaint.apply { color = s.color.toArgb(); isFakeBoldText = true }
                ).apply { textPaint.isFakeBoldText = false; textPaint.color = Color.Gray.toArgb() }
            }
            
            if (pointsToDraw >= 2) {
                drawPath(
                    path = path,
                    color = s.color,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
