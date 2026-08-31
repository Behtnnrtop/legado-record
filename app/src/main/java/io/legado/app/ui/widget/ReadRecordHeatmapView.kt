package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import io.legado.app.R
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.main.readrecord.ReadRecordHeatmapUiState
import io.legado.app.ui.widget.readrecord.HeatmapCell
import io.legado.app.ui.widget.readrecord.ReadRecordHeatmapHelper
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class ReadRecordHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val contentPaddingStart = 0.dpToPx()
    private val contentPaddingTop = 10.dpToPx()
    private val contentPaddingEnd = 0.dpToPx()
    private val contentPaddingBottom = 16.dpToPx()
    private val monthLabelHeight = 24.dpToPx()
    private val leftLabelWidth = 28.dpToPx()
    private val cellSize = 12.dpToPx()
    private val cellGap = 5.dpToPx()
    private val legendHeight = 36.dpToPx()
    private val cellRadius = 3.dpToPx().toFloat()

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        textSize = 12.dpToPx().toFloat()
    }
    private val centerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12.dpToPx().toFloat()
    }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private val weekLabels = mapOf(0 to "一", 2 to "三", 4 to "五")
    private var state: ReadRecordHeatmapUiState? = null
    private var cells: List<HeatmapCell> = emptyList()
    private var columns: Int = 0
    private var gridStartDate: LocalDate = LocalDate.now()
    private var colorLevels: IntArray = intArrayOf()
    private var onDayClickListener: ((date: String, readTime: Long) -> Unit)? = null

    init {
        setWillNotDraw(false)
    }

    fun setData(state: ReadRecordHeatmapUiState) {
        this.state = state
        buildCells(state)
        colorLevels = buildColorLevels()
        requestLayout()
        invalidate()
    }

    fun setOnDayClickListener(listener: ((date: String, readTime: Long) -> Unit)?) {
        onDayClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val gridWidth = columns * cellSize + (columns - 1).coerceAtLeast(0) * cellGap
        val desiredWidth = contentPaddingStart + leftLabelWidth + gridWidth + contentPaddingEnd
        val desiredHeight = contentPaddingTop + monthLabelHeight +
            7 * cellSize + 6 * cellGap + legendHeight + contentPaddingBottom
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (state == null) return
        if (colorLevels.isEmpty()) {
            colorLevels = buildColorLevels()
        }
        val summaryText = ContextCompat.getColor(context, R.color.tv_text_summary)
        labelPaint.color = summaryText
        centerLabelPaint.color = summaryText

        drawMonthLabels(canvas)
        drawWeekLabels(canvas)
        drawCells(canvas)
        drawLegend(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val cell = findCell(event.x, event.y) ?: return true
        onDayClickListener?.invoke(cell.date.toString(), cell.readTime)
        return true
    }

    private fun drawMonthLabels(canvas: Canvas) {
        if (columns == 0) return
        var lastMonth: YearMonth? = null
        val y = gridTop() - monthLabelHeight + baseline(labelPaint, monthLabelHeight.toFloat())
        for (column in 0 until columns) {
            val date = gridStartDate.plusWeeks(column.toLong())
            val month = YearMonth.from(date)
            if (month != lastMonth) {
                canvas.drawText(
                    "${month.month.value}月",
                    gridLeft() + column * (cellSize + cellGap),
                    y,
                    labelPaint
                )
                lastMonth = month
            }
        }
    }

    private fun drawWeekLabels(canvas: Canvas) {
        weekLabels.forEach { (row, label) ->
            canvas.drawText(
                label,
                contentPaddingStart.toFloat(),
                gridTop() + row * (cellSize + cellGap) + baseline(labelPaint, cellSize.toFloat()),
                labelPaint
            )
        }
    }

    private fun drawCells(canvas: Canvas) {
        cells.forEach { cell ->
            cellPaint.color = colorLevels[ReadRecordHeatmapHelper.level(cell.readTime)]
            val left = gridLeft() + cell.column * (cellSize + cellGap)
            val top = gridTop() + cell.row * (cellSize + cellGap)
            rect.set(left, top, left + cellSize, top + cellSize)
            canvas.drawRoundRect(rect, cellRadius, cellRadius, cellPaint)
        }
    }

    private fun drawLegend(canvas: Canvas) {
        val legendTop = gridTop() + 7 * cellSize + 6 * cellGap + 13.dpToPx()
        val moreText = "多"
        val lessText = "少"
        val moreWidth = labelPaint.measureText(moreText)
        val lessWidth = labelPaint.measureText(lessText)
        val blockGap = 5.dpToPx()
        val totalWidth = lessWidth + 8.dpToPx() + 5 * cellSize + 4 * blockGap + 8.dpToPx() + moreWidth
        var x = measuredWidth - contentPaddingEnd - totalWidth
        val baseline = legendTop + baseline(labelPaint, cellSize.toFloat())
        canvas.drawText(lessText, x, baseline, labelPaint)
        x += lessWidth + 8.dpToPx()
        for (level in 1..5) {
            cellPaint.color = colorLevels[level]
            rect.set(x, legendTop, x + cellSize, legendTop + cellSize)
            canvas.drawRoundRect(rect, cellRadius, cellRadius, cellPaint)
            x += cellSize + blockGap
        }
        x += 8.dpToPx() - blockGap
        canvas.drawText(moreText, x, baseline, labelPaint)
    }

    private fun buildCells(state: ReadRecordHeatmapUiState) {
        val start = state.startDate
        val end = state.endDate
        gridStartDate = ReadRecordHeatmapHelper.weekStart(start)
        val gridEndDate = ReadRecordHeatmapHelper.weekEnd(end)
        columns = (ChronoUnit.WEEKS.between(gridStartDate, gridEndDate) + 1).toInt()
        cells = (0 until ReadRecordHeatmapHelper.daysBetweenInclusive(start, end)).map { offset ->
            val date = start.plusDays(offset)
            val daysFromGridStart = ChronoUnit.DAYS.between(gridStartDate, date).toInt()
            HeatmapCell(
                date = date,
                column = daysFromGridStart / 7,
                row = date.dayOfWeek.value - 1,
                readTime = state.days[date.toString()] ?: 0L
            )
        }
    }

    private fun buildColorLevels(): IntArray {
        val background = ContextCompat.getColor(context, R.color.background)
        val empty = ContextCompat.getColor(context, R.color.common_gray)
        val accent = ThemeStore.accentColor(context)
        return intArrayOf(
            empty,
            ColorUtils.blendColors(background, accent, 0.18f),
            ColorUtils.blendColors(background, accent, 0.32f),
            ColorUtils.blendColors(background, accent, 0.50f),
            ColorUtils.blendColors(background, accent, 0.70f),
            accent
        )
    }

    private fun findCell(x: Float, y: Float): HeatmapCell? {
        val gridX = x - gridLeft()
        val gridY = y - gridTop()
        if (gridX < 0f || gridY < 0f) return null
        val step = cellSize + cellGap
        val column = (gridX / step).toInt()
        val row = (gridY / step).toInt()
        val cellX = gridX - column * step
        val cellY = gridY - row * step
        if (column !in 0 until columns || row !in 0..6) return null
        if (cellX > cellSize || cellY > cellSize) return null
        return cells.firstOrNull { it.column == column && it.row == row }
    }

    private fun gridLeft(): Float = (contentPaddingStart + leftLabelWidth).toFloat()

    private fun gridTop(): Float = (contentPaddingTop + monthLabelHeight).toFloat()

    private fun baseline(paint: Paint, height: Float): Float {
        val metrics = paint.fontMetrics
        return height / 2f - (metrics.ascent + metrics.descent) / 2f
    }

}
