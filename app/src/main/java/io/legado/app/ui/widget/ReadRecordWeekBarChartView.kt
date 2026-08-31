package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import io.legado.app.R
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.main.readrecord.ReadRecordDailyBar
import io.legado.app.ui.main.readrecord.ReadRecordWeekBarUiState
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import kotlin.math.max

class ReadRecordWeekBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val contentPaddingStart = 4.dpToPx()
    private val contentPaddingTop = 8.dpToPx()
    private val contentPaddingEnd = 4.dpToPx()
    private val contentPaddingBottom = 8.dpToPx()
    private val xLabelHeight = 22.dpToPx()
    private val barWidth = 18.dpToPx()
    private val barRadius = 5.dpToPx().toFloat()
    private val minVisibleBarHeight = 3.dpToPx().toFloat()

    private val centerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12.dpToPx().toFloat()
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.dpToPx().toFloat()
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var state: ReadRecordWeekBarUiState? = null
    private var barRects: List<Pair<RectF, ReadRecordDailyBar>> = emptyList()
    private var onBarClickListener: ((date: String, readTime: Long) -> Unit)? = null

    init {
        setWillNotDraw(false)
    }

    fun setData(state: ReadRecordWeekBarUiState) {
        this.state = state
        requestLayout()
        invalidate()
    }

    fun setOnBarClickListener(listener: ((date: String, readTime: Long) -> Unit)?) {
        onBarClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 176.dpToPx()
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val state = state ?: return
        val accent = ThemeStore.accentColor(context)
        val cardBackground = ContextCompat.getColor(context, R.color.background_card)
        val summaryText = ContextCompat.getColor(context, R.color.tv_text_summary)
        val axisColor = ColorUtils.blendColors(cardBackground, summaryText, 0.25f)
        val tickLabelColor = ColorUtils.blendColors(cardBackground, summaryText, 0.38f)
        val emptyBarColor = ColorUtils.blendColors(cardBackground, summaryText, 0.16f)
        val gradientTop = ColorUtils.blendColors(cardBackground, accent, 0.80f)
        val gradientBottom = ColorUtils.blendColors(cardBackground, accent, 0.25f)
        val todayGradientTop = accent
        val todayGradientBottom = ColorUtils.blendColors(cardBackground, accent, 0.70f)

        linePaint.color = axisColor
        canvas.drawLine(chartLeft(), chartBottom(), chartRight(), chartBottom(), linePaint)
        drawBars(
            canvas, state, accent,
            tickLabelColor,
            gradientTop, gradientBottom,
            todayGradientTop, todayGradientBottom,
            emptyBarColor
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val bar = barRects.firstOrNull { it.first.contains(event.x, event.y) }?.second ?: return true
        onBarClickListener?.invoke(bar.date.toString(), bar.readTime)
        return true
    }

    private fun drawBars(
        canvas: Canvas,
        state: ReadRecordWeekBarUiState,
        accent: Int,
        tickLabelColor: Int,
        gradientTop: Int,
        gradientBottom: Int,
        todayGradientTop: Int,
        todayGradientBottom: Int,
        emptyBarColor: Int
    ) {
        val bars = state.bars
        if (bars.isEmpty()) {
            barRects = emptyList()
            return
        }
        val chartLeft = chartLeft()
        val chartBottom = chartBottom()
        val slotWidth = chartWidth() / bars.size
        val maxReadTime = max(1L, state.maxReadTime)
        val rects = mutableListOf<Pair<RectF, ReadRecordDailyBar>>()
        bars.forEachIndexed { index, bar ->
            val centerX = chartLeft + slotWidth * index + slotWidth / 2f
            val rawHeight = chartHeight() * bar.readTime / maxReadTime
            val barHeight = when {
                bar.readTime <= 0L -> 0f
                else -> rawHeight.coerceAtLeast(minVisibleBarHeight)
            }
            val left = centerX - barWidth / 2f
            val top = chartBottom - barHeight
            rect.set(left, top, left + barWidth, chartBottom)
            if (bar.readTime <= 0L) {
                val emptyTop = chartBottom - minVisibleBarHeight
                rect.set(left, emptyTop, left + barWidth, chartBottom)
                barPaint.shader = null
                barPaint.color = emptyBarColor
            } else {
                val topColor = if (bar.isToday) todayGradientTop else gradientTop
                val bottomColor = if (bar.isToday) todayGradientBottom else gradientBottom
                barPaint.shader = LinearGradient(
                    0f, rect.top, 0f, rect.bottom,
                    topColor, bottomColor,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, barRadius, barRadius, barPaint)
            barPaint.shader = null
            rects += RectF(rect) to bar

            centerLabelPaint.color = if (bar.isToday) accent else tickLabelColor
            canvas.drawText(
                bar.weekLabel,
                centerX,
                chartBottom + baseline(centerLabelPaint, xLabelHeight.toFloat()),
                centerLabelPaint
            )
        }
        barRects = rects
    }

    private fun chartLeft(): Float = contentPaddingStart.toFloat()

    private fun chartRight(): Float = (measuredWidth - contentPaddingEnd).toFloat()

    private fun chartTop(): Float = contentPaddingTop.toFloat()

    private fun chartBottom(): Float = (measuredHeight - contentPaddingBottom - xLabelHeight).toFloat()

    private fun chartWidth(): Float = (chartRight() - chartLeft()).coerceAtLeast(1f)

    private fun chartHeight(): Float = (chartBottom() - chartTop()).coerceAtLeast(1f)

    private fun baseline(paint: Paint): Float {
        val metrics = paint.fontMetrics
        return -(metrics.ascent + metrics.descent) / 2f
    }

    private fun baseline(paint: Paint, height: Float): Float {
        val metrics = paint.fontMetrics
        return height / 2f - (metrics.ascent + metrics.descent) / 2f
    }

}
