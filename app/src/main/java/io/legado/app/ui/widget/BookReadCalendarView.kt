package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.legado.app.R
import io.legado.app.data.entities.BookReadDayRecord
import io.legado.app.utils.dpToPx
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

class BookReadCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val monthWidth = 150.dpToPx()
    private val monthGap = 20.dpToPx()
    private val cellWidth = 20.dpToPx()
    private val cellHeight = 22.dpToPx()
    private val yearHeight = 24.dpToPx()
    private val monthLabelHeight = 28.dpToPx()
    private val weekLabelHeight = 24.dpToPx()
    private val topPadding = 2.dpToPx()
    private val bottomPadding = 6.dpToPx()
    private val labelRadius = 3.dpToPx().toFloat()
    private val dayRadius = 3.dpToPx().toFloat()

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13.dpToPx().toFloat()
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12.dpToPx().toFloat()
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12.dpToPx().toFloat()
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val weeks = arrayOf("一", "二", "三", "四", "五", "六", "日")
    private var months: List<ReadCalendarMonth> = emptyList()

    init {
        setWillNotDraw(false)
    }

    fun setReadDays(records: List<BookReadDayRecord>) {
        months = buildMonths(records)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val monthCount = months.size
        val desiredWidth = if (monthCount == 0) {
            0
        } else {
            monthCount * monthWidth + (monthCount - 1) * monthGap
        } + paddingLeft + paddingRight
        val desiredHeight = topPadding + yearHeight + monthLabelHeight + weekLabelHeight +
            6 * cellHeight + bottomPadding + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (months.isEmpty()) return

        val primaryText = ContextCompat.getColor(context, R.color.tv_text_summary)
        val summaryText = ContextCompat.getColor(context, R.color.tv_text_summary)
        val labelTextColor = ContextCompat.getColor(context, android.R.color.white)
        val calendarAccent = ContextCompat.getColor(context, R.color.read_calendar_accent)
        textPaint.color = primaryText
        smallTextPaint.color = summaryText
        labelTextPaint.color = labelTextColor
        bgPaint.color = calendarAccent

        var x = paddingLeft.toFloat()
        var currentYear: Int? = null
        months.forEach { month ->
            val yearTop = paddingTop + topPadding.toFloat()
            if (currentYear != month.year) {
                drawLabel(
                    canvas,
                    month.year.toString(),
                    x,
                    yearTop,
                    calendarAccent,
                    labelTextColor,
                    58.dpToPx().toFloat()
                )
                currentYear = month.year
            }

            val monthTop = yearTop + yearHeight
            drawLabel(
                canvas = canvas,
                text = "%02d".format(month.month),
                left = x,
                top = monthTop,
                bgColor = calendarAccent,
                textColor = labelTextColor,
                labelWidth = 44.dpToPx().toFloat()
            )

            val weekTop = monthTop + monthLabelHeight
            weeks.forEachIndexed { index, week ->
                canvas.drawText(
                    week,
                    x + index * cellWidth + cellWidth / 2f,
                    weekTop + baseline(smallTextPaint, weekLabelHeight.toFloat()),
                    smallTextPaint
                )
            }

            val daysTop = weekTop + weekLabelHeight
            for (day in 1..month.daysInMonth) {
                val index = month.firstDayOffset + day - 1
                val row = index / 7
                val column = index % 7
                val left = x + column * cellWidth
                val top = daysTop + row * cellHeight
                val read = month.readDays.contains(day)
                if (read) {
                    bgPaint.color = calendarAccent
                    rect.set(
                        left + 2.dpToPx(),
                        top + 1.dpToPx(),
                        left + cellWidth - 2.dpToPx(),
                        top + cellHeight - 1.dpToPx()
                    )
                    canvas.drawRoundRect(rect, dayRadius, dayRadius, bgPaint)
                    textPaint.color = ContextCompat.getColor(context, android.R.color.white)
                } else {
                    textPaint.color = primaryText
                }
                canvas.drawText(
                    day.toString(),
                    left + cellWidth / 2f,
                    top + baseline(textPaint, cellHeight.toFloat()),
                    textPaint
                )
            }
            x += monthWidth + monthGap
        }
    }

    private fun drawLabel(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        bgColor: Int,
        textColor: Int,
        labelWidth: Float
    ) {
        bgPaint.color = bgColor
        labelTextPaint.color = textColor
        rect.set(left, top + 2.dpToPx(), left + labelWidth, top + yearHeight - 3.dpToPx())
        canvas.drawRoundRect(rect, labelRadius, labelRadius, bgPaint)
        canvas.drawText(
            text,
            rect.centerX(),
            rect.top + baseline(labelTextPaint, rect.height()),
            labelTextPaint
        )
    }

    private fun baseline(paint: Paint, height: Float): Float {
        val metrics = paint.fontMetrics
        return height / 2f - (metrics.ascent + metrics.descent) / 2f
    }

    companion object {
        fun buildMonths(records: List<BookReadDayRecord>): List<ReadCalendarMonth> {
            val readDates = records.asSequence()
                .filter { it.readTime > 0L }
                .mapNotNull { record ->
                    try {
                        LocalDate.parse(record.date)
                    } catch (_: DateTimeParseException) {
                        null
                    }
                }
                .toSet()
            if (readDates.isEmpty()) return emptyList()

            val firstMonth = YearMonth.from(readDates.minOrNull())
            val lastMonth = YearMonth.from(readDates.maxOrNull())
            val monthCount = firstMonth.until(lastMonth, java.time.temporal.ChronoUnit.MONTHS).toInt()
            return (0..monthCount).map { offset ->
                val yearMonth = firstMonth.plusMonths(offset.toLong())
                val daysInMonth = yearMonth.lengthOfMonth()
                val firstDayOffset = yearMonth.atDay(1).dayOfWeek.value - 1
                val readDays = readDates
                    .filter { YearMonth.from(it) == yearMonth }
                    .map { it.dayOfMonth }
                    .toSet()
                ReadCalendarMonth(
                    year = yearMonth.year,
                    month = yearMonth.month.value,
                    firstDayOffset = firstDayOffset,
                    daysInMonth = daysInMonth,
                    readDays = readDays
                )
            }
        }
    }
}

data class ReadCalendarMonth(
    val year: Int,
    val month: Int,
    val firstDayOffset: Int,
    val daysInMonth: Int,
    val readDays: Set<Int>
)
