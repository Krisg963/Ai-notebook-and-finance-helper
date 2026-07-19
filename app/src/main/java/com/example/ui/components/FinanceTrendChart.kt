package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
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
fun ChartStylePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("chart_style_pill_${label.lowercase()}")
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FinanceTrendChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val chartData = rememberChartData(transactions)
    var activeStyle by remember { mutableStateOf("Område") } // "Område", "Linje", "Søyler"
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

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
            // Header with Legend & Style Selector
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
                        text = "Sveip på grafen for å se detaljer",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Recharts Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = IncomeColor, text = "Inntekt")
                    LegendItem(color = ExpenseColor, text = "Utgift")
                }
            }

            // Quick Recharts Chart Style Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visualisering:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                ChartStylePill(label = "Område", isSelected = activeStyle == "Område", onClick = { activeStyle = "Område" })
                ChartStylePill(label = "Linje", isSelected = activeStyle == "Linje", onClick = { activeStyle = "Linje" })
                ChartStylePill(label = "Søyler", isSelected = activeStyle == "Søyler", onClick = { activeStyle = "Søyler" })
            }

            if (chartData.isEmpty()) {
                // Placeholder if empty
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
                            text = "Legg til transaksjoner for å se visualiseringen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                val density = LocalDensity.current
                val paddingLeft = 46.dp
                val paddingRight = 12.dp
                val paddingTop = 12.dp
                val paddingBottom = 24.dp
                val chartHeight = 180.dp - paddingTop - paddingBottom

                val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val highlightColor = MaterialTheme.colorScheme.primary

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val widthDp = maxWidth
                    val chartWidth = widthDp - paddingLeft - paddingRight
                    val groupWidth = chartWidth / chartData.size

                    // Conversion to Pixels for drawing
                    val widthPx = with(density) { widthDp.toPx() }
                    val heightPx = with(density) { 180.dp.toPx() }
                    val paddingLeftPx = with(density) { paddingLeft.toPx() }
                    val paddingRightPx = with(density) { paddingRight.toPx() }
                    val paddingTopPx = with(density) { paddingTop.toPx() }
                    val paddingBottomPx = with(density) { paddingBottom.toPx() }
                    val chartWidthPx = with(density) { chartWidth.toPx() }
                    val chartHeightPx = with(density) { chartHeight.toPx() }
                    val groupWidthPx = with(density) { groupWidth.toPx() }

                    // Scaling math
                    val maxVal = chartData.maxOf { maxOf(it.income, it.expense) }
                    val maxScale = if (maxVal <= 0) 1000f else maxVal * 1.15f

                    // Helper for calculating exact index from touch position
                    fun getIndexFromX(touchX: Float): Int {
                        return ((touchX - paddingLeftPx) / groupWidthPx).toInt().coerceIn(0, chartData.size - 1)
                    }

                    // Map elements to exact screen Offset lists for Area & Line plots
                    val incomePoints = chartData.mapIndexed { index, point ->
                        val x = paddingLeftPx + (index * groupWidthPx) + (groupWidthPx / 2f)
                        val y = paddingTopPx + chartHeightPx - (point.income / maxScale) * chartHeightPx
                        Offset(x, y)
                    }

                    val expensePoints = chartData.mapIndexed { index, point ->
                        val x = paddingLeftPx + (index * groupWidthPx) + (groupWidthPx / 2f)
                        val y = paddingTopPx + chartHeightPx - (point.expense / maxScale) * chartHeightPx
                        Offset(x, y)
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recharts_trend_canvas")
                            .pointerInput(chartData) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        selectedIndex = getIndexFromX(offset.x)
                                        tryAwaitRelease()
                                        selectedIndex = null
                                    }
                                )
                            }
                            .pointerInput(chartData) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        selectedIndex = getIndexFromX(offset.x)
                                    },
                                    onDrag = { change, _ ->
                                        selectedIndex = getIndexFromX(change.position.x)
                                    },
                                    onDragEnd = { selectedIndex = null },
                                    onDragCancel = { selectedIndex = null }
                                )
                            }
                    ) {
                        // 1. Draw horizontal CartesianGrid grid lines
                        val gridLinesCount = 3
                        val textPaint = android.graphics.Paint().apply {
                            color = labelColor
                            textSize = 9.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                            isAntiAlias = true
                        }

                        for (i in 0..gridLinesCount) {
                            val fraction = i.toFloat() / gridLinesCount
                            val y = paddingTopPx + chartHeightPx * (1f - fraction)
                            val value = maxScale * fraction

                            drawLine(
                                color = gridColor,
                                start = Offset(paddingLeftPx, y),
                                end = Offset(widthPx - paddingRightPx, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                formatCompactValue(value),
                                paddingLeftPx - 6.dp.toPx(),
                                y + 3.dp.toPx(),
                                textPaint
                            )
                        }

                        // 2. Draw vertical CartesianGrid lines at center of groups
                        chartData.forEachIndexed { index, _ ->
                            val groupCenterX = paddingLeftPx + (index * groupWidthPx) + (groupWidthPx / 2f)
                            drawLine(
                                color = gridColor,
                                start = Offset(groupCenterX, paddingTopPx),
                                end = Offset(groupCenterX, heightPx - paddingBottomPx),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        // 3. Draw Selected Highlight Column
                        selectedIndex?.let { index ->
                            if (index < chartData.size) {
                                val selectedCenterX = paddingLeftPx + (index * groupWidthPx) + (groupWidthPx / 2f)
                                drawLine(
                                    color = highlightColor.copy(alpha = 0.25f),
                                    start = Offset(selectedCenterX, paddingTopPx),
                                    end = Offset(selectedCenterX, heightPx - paddingBottomPx),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                            }
                        }

                        // 4. Render Chart Data based on selected style
                        when (activeStyle) {
                            "Område" -> {
                                // Area Chart - Income Gradient
                                val incomeAreaPath = Path().apply {
                                    drawSmoothCurve(incomePoints, heightPx - paddingBottomPx)
                                    if (incomePoints.size > 1) {
                                        lineTo(incomePoints.last().x, heightPx - paddingBottomPx)
                                        lineTo(incomePoints.first().x, heightPx - paddingBottomPx)
                                    }
                                    close()
                                }
                                drawPath(
                                    path = incomeAreaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(IncomeColor.copy(alpha = 0.35f), IncomeColor.copy(alpha = 0.0f)),
                                        startY = paddingTopPx,
                                        endY = heightPx - paddingBottomPx
                                    )
                                )
                                // Area Chart - Income Stroke Line
                                val incomeStrokePath = Path().apply {
                                    drawSmoothCurve(incomePoints, heightPx - paddingBottomPx)
                                }
                                drawPath(
                                    path = incomeStrokePath,
                                    color = IncomeColor,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // Area Chart - Expense Gradient
                                val expenseAreaPath = Path().apply {
                                    drawSmoothCurve(expensePoints, heightPx - paddingBottomPx)
                                    if (expensePoints.size > 1) {
                                        lineTo(expensePoints.last().x, heightPx - paddingBottomPx)
                                        lineTo(expensePoints.first().x, heightPx - paddingBottomPx)
                                    }
                                    close()
                                }
                                drawPath(
                                    path = expenseAreaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(ExpenseColor.copy(alpha = 0.35f), ExpenseColor.copy(alpha = 0.0f)),
                                        startY = paddingTopPx,
                                        endY = heightPx - paddingBottomPx
                                    )
                                )
                                // Area Chart - Expense Stroke Line
                                val expenseStrokePath = Path().apply {
                                    drawSmoothCurve(expensePoints, heightPx - paddingBottomPx)
                                }
                                drawPath(
                                    path = expenseStrokePath,
                                    color = ExpenseColor,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // Draw Dots
                                incomePoints.forEach { point ->
                                    drawCircle(color = IncomeColor, radius = 4.dp.toPx(), center = point)
                                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
                                }
                                expensePoints.forEach { point ->
                                    drawCircle(color = ExpenseColor, radius = 4.dp.toPx(), center = point)
                                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
                                }
                            }
                            "Linje" -> {
                                // Line Chart - Income
                                val incomeStrokePath = Path().apply {
                                    drawSmoothCurve(incomePoints, heightPx - paddingBottomPx)
                                }
                                drawPath(
                                    path = incomeStrokePath,
                                    color = IncomeColor,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // Line Chart - Expense
                                val expenseStrokePath = Path().apply {
                                    drawSmoothCurve(expensePoints, heightPx - paddingBottomPx)
                                }
                                drawPath(
                                    path = expenseStrokePath,
                                    color = ExpenseColor,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // Draw Dots
                                incomePoints.forEach { point ->
                                    drawCircle(color = IncomeColor, radius = 4.5.dp.toPx(), center = point)
                                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = point)
                                }
                                expensePoints.forEach { point ->
                                    drawCircle(color = ExpenseColor, radius = 4.5.dp.toPx(), center = point)
                                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = point)
                                }
                            }
                            "Søyler" -> {
                                // Dual Bar Chart
                                val barWidth = (groupWidthPx * 0.28f).coerceIn(4.dp.toPx(), 16.dp.toPx())
                                val barSpacing = 3.dp.toPx()

                                chartData.forEachIndexed { index, point ->
                                    val groupCenterX = paddingLeftPx + (index * groupWidthPx) + (groupWidthPx / 2f)

                                    if (point.income > 0) {
                                        val barHeight = (point.income / maxScale) * chartHeightPx
                                        val left = groupCenterX - barWidth - (barSpacing / 2f)
                                        val top = heightPx - paddingBottomPx - barHeight
                                        drawRoundRect(
                                            color = IncomeColor,
                                            topLeft = Offset(left, top),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    }

                                    if (point.expense > 0) {
                                        val barHeight = (point.expense / maxScale) * chartHeightPx
                                        val left = groupCenterX + (barSpacing / 2f)
                                        val top = heightPx - paddingBottomPx - barHeight
                                        drawRoundRect(
                                            color = ExpenseColor,
                                            topLeft = Offset(left, top),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Draw active dot selection highlight rings
                        selectedIndex?.let { index ->
                            if (index < chartData.size) {
                                if (activeStyle == "Område" || activeStyle == "Linje") {
                                    val incP = incomePoints[index]
                                    val expP = expensePoints[index]

                                    drawCircle(color = IncomeColor.copy(alpha = 0.2f), radius = 10.dp.toPx(), center = incP)
                                    drawCircle(color = IncomeColor, radius = 5.dp.toPx(), center = incP)
                                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = incP)

                                    drawCircle(color = ExpenseColor.copy(alpha = 0.2f), radius = 10.dp.toPx(), center = expP)
                                    drawCircle(color = ExpenseColor, radius = 5.dp.toPx(), center = expP)
                                    drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = expP)
                                }
                            }
                        }

                        // 6. Draw X-axis label texts
                        chartData.forEachIndexed { index, point ->
                            val groupCenterX = paddingLeftPx + (index * groupWidthPx) + (groupWidthPx / 2f)
                            drawContext.canvas.nativeCanvas.drawText(
                                point.label,
                                groupCenterX,
                                heightPx - 6.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = labelColor
                                    textSize = 9.5.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    // 7. Interactive Floating Recharts Tooltip Overlay
                    if (selectedIndex != null) {
                        val idx = selectedIndex!!
                        if (idx < chartData.size) {
                            val point = chartData[idx]
                            val centerX = paddingLeft + (idx * groupWidth) + (groupWidth / 2f)

                            // Align the tooltip to left/right of the pointer to prevent going out of boundaries
                            val tooltipWidth = 135.dp
                            val showOnLeft = centerX > (widthDp / 2)
                            val xOffset = if (showOnLeft) {
                                centerX - tooltipWidth - 8.dp
                            } else {
                                centerX + 8.dp
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                modifier = Modifier
                                    .absoluteOffset(x = xOffset, y = 14.dp)
                                    .width(tooltipWidth)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("chart_tooltip")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Dato: ${point.label}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 0.5.dp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(IncomeColor, RoundedCornerShape(1.dp))
                                        )
                                        Text(
                                            text = "Inntekt: ${formatCurrency(point.income.toDouble())}",
                                            fontSize = 10.sp,
                                            color = IncomeColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(ExpenseColor, RoundedCornerShape(1.dp))
                                        )
                                        Text(
                                            text = "Utgift: ${formatCurrency(point.expense.toDouble())}",
                                            fontSize = 10.sp,
                                            color = ExpenseColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Cubic Bezier curve algorithm to draw perfectly smooth S-lines
fun Path.drawSmoothCurve(points: List<Offset>, baselineY: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        moveTo(points[0].x, points[0].y)
        lineTo(points[0].x, baselineY)
        return
    }
    moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val cp1X = p0.x + (p1.x - p0.x) / 3f
        val cp1Y = p0.y
        val cp2X = p0.x + 2f * (p1.x - p0.x) / 3f
        val cp2Y = p1.y
        cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1.x, p1.y)
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
    return remember(transactions) {
        val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
        val sortedTxs = transactions.sortedBy { it.timestamp }
        val grouped = sortedTxs.groupBy { sdf.format(Date(it.timestamp)) }

        grouped.map { (date, txs) ->
            val income = txs.filter { it.type == "INNTEKT" }.sumOf { it.amount }.toFloat()
            val expense = txs.filter { it.type == "UTGIFT" }.sumOf { it.amount }.toFloat()
            ChartDataPoint(label = date, income = income, expense = expense)
        }.takeLast(7)
    }
}

fun formatCompactValue(value: Float): String {
    return when {
        value >= 1_000_000f -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000f)
        value >= 1_000f -> String.format(Locale.getDefault(), "%.1fk", value / 1_000f)
        else -> value.toInt().toString()
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("no", "NO"))
    return try {
        val formatted = formatter.format(amount)
        formatted.replace("kr", "kr ").replace(",00", ",-")
    } catch (e: Exception) {
        "${amount.toInt()} kr"
    }
}
