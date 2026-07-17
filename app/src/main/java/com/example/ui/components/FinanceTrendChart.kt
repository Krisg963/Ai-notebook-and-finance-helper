package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.IncomeColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class ChartDataPoint(
    val label: String,
    val income: Float,
    val expense: Float
)

@Composable
fun FinanceTrendChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    // Process transactions into daily aggregates
    val chartData = rememberChartData(transactions)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("finance_trend_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Økonomisk Utvikling",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Viser siste dager med transaksjoner",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = IncomeColor, text = "Inntekt")
                    LegendItem(color = ExpenseColor, text = "Utgift")
                }
            }

            if (chartData.isEmpty()) {
                // Placeholder with sample design if no data exists
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Legg til transaksjoner for å se grafen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Display a subtle placeholder bar chart
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(60.dp)
                        ) {
                            PlaceholderBar(heightFraction = 0.4f, color = IncomeColor.copy(alpha = 0.3f))
                            PlaceholderBar(heightFraction = 0.2f, color = ExpenseColor.copy(alpha = 0.3f))
                            PlaceholderBar(heightFraction = 0.7f, color = IncomeColor.copy(alpha = 0.3f))
                            PlaceholderBar(heightFraction = 0.5f, color = ExpenseColor.copy(alpha = 0.3f))
                        }
                    }
                }
            } else {
                // Draw the custom Canvas chart
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("trend_canvas")
                ) {
                    val width = size.width
                    val height = size.height

                    // Leave padding for X-axis labels and Y-axis scale values
                    val paddingLeft = 40.dp.toPx()
                    val paddingBottom = 24.dp.toPx()
                    val paddingTop = 12.dp.toPx()
                    val paddingRight = 12.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // Calculate max value for scaling
                    val maxVal = chartData.maxOf { maxOf(it.income, it.expense) }
                    val maxScale = if (maxVal <= 0) 1000f else maxVal * 1.15f

                    // Draw horizontal grid lines and Y-axis scale values
                    val gridLinesCount = 3
                    val paint = android.graphics.Paint().apply {
                        color = labelColor
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                        isAntiAlias = true
                    }

                    for (i in 0..gridLinesCount) {
                        val fraction = i.toFloat() / gridLinesCount
                        val y = paddingTop + chartHeight * (1f - fraction)
                        val value = maxScale * fraction

                        // Draw dashed line
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Draw value text
                        drawContext.canvas.nativeCanvas.drawText(
                            formatCompactValue(value),
                            paddingLeft - 8.dp.toPx(),
                            y + 4.dp.toPx(),
                            paint
                        )
                    }

                    // Draw data bars
                    val barGroupCount = chartData.size
                    val groupWidth = chartWidth / barGroupCount
                    val barWidth = (groupWidth * 0.3f).coerceIn(4.dp.toPx(), 16.dp.toPx())
                    val barSpacing = 4.dp.toPx()

                    chartData.forEachIndexed { index, point ->
                        val groupCenterX = paddingLeft + (index * groupWidth) + (groupWidth / 2f)

                        // 1. Draw Income Bar
                        if (point.income > 0) {
                            val barHeightFraction = point.income / maxScale
                            val barHeight = chartHeight * barHeightFraction
                            val left = groupCenterX - barWidth - (barSpacing / 2f)
                            val top = paddingTop + chartHeight - barHeight

                            drawRoundRect(
                                color = IncomeColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // 2. Draw Expense Bar
                        if (point.expense > 0) {
                            val barHeightFraction = point.expense / maxScale
                            val barHeight = chartHeight * barHeightFraction
                            val left = groupCenterX + (barSpacing / 2f)
                            val top = paddingTop + chartHeight - barHeight

                            drawRoundRect(
                                color = ExpenseColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // 3. Draw X-axis label (Date)
                        drawContext.canvas.nativeCanvas.drawText(
                            point.label,
                            groupCenterX,
                            height - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = labelColor
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PlaceholderBar(heightFraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight(heightFraction)
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    )
}

@Composable
fun rememberChartData(transactions: List<Transaction>): List<ChartDataPoint> {
    return androidx.compose.runtime.remember(transactions) {
        val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
        // Sort ascending chronologically
        val sortedTxs = transactions.sortedBy { it.timestamp }
        
        // Group by day label
        val grouped = sortedTxs.groupBy { sdf.format(Date(it.timestamp)) }
        
        grouped.map { (date, txs) ->
            val income = txs.filter { it.type == "INNTEKT" }.sumOf { it.amount }.toFloat()
            val expense = txs.filter { it.type == "UTGIFT" }.sumOf { it.amount }.toFloat()
            ChartDataPoint(label = date, income = income, expense = expense)
        }.takeLast(7) // limit to the last 7 distinct active days
    }
}

fun formatCompactValue(value: Float): String {
    return when {
        value >= 1_000_000f -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000f)
        value >= 1_000f -> String.format(Locale.getDefault(), "%.1fk", value / 1_000f)
        else -> value.toInt().toString()
    }
}
