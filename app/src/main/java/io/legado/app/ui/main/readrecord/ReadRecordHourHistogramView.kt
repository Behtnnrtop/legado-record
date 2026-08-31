package io.legado.app.ui.main.readrecord

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.legado.app.R
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dpToPx
import kotlin.math.max

class ReadRecordHourHistogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val contentPaddingStart = 4.dpToPx()
    private val contentPaddingTop = 8.dpToPx()
    private val contentPaddingEnd = 4.dpToPx()
    private val contentPaddingBottom = 8.dpToPx()
    private val xLabelHeight = 22.dpToPx()
    private val barRadius = 3.dpToPx().toFloat()
    private val minVisibleBarHeight = 3.dpToPx().toFloat()

    private val centerLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11.dpToPx().toFloat()
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13.dpToPx().toFloat()
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.dpToPx().toFloat()
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var counts: IntArray = IntArray(24)

    init {
        setWillNotDraw(false)
    }

    fun setCounts(counts: IntArray) {
        this.counts = counts.copyOf(24)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 180.dpToPx()
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val accent = ThemeStore.accentColor(context)
        val cardBackground = ContextCompat.getColor(context, R.color.background_card)
        val summaryText = ContextCompat.getColor(context, R.color.tv_text_summary)
        val primaryText = ContextCompat.getColor(context, R.color.primaryText)
        val axisColor = ColorUtils.blendColors(cardBackground, summaryText, 0.25f)
        val tickLabelColor = ColorUtils.blendColors(cardBackground, summaryText, 0.38f)
        val regularGradientTop = ColorUtils.blendColors(cardBackground, accent, 0.35f)
        val regularGradientBottom = ColorUtils.blendColors(cardBackground, accent, 0.10f)
        val peakGradientTop = accent
        val peakGradientBottom = ColorUtils.blendColors(cardBackground, accent, 0.30f)

        linePaint.color = axisColor
        centerLabelPaint.color = tickLabelColor
        emptyPaint.color = summaryText

        val maxCount = counts.maxOrNull() ?: 0
        canvas.drawLine(chartLeft(), chartBottom(), chartRight(), chartBottom(), linePaint)
        drawBars(
            canvas, maxCount,
            regularGradientTop, regularGradientBottom,
            peakGradientTop, peakGradientBottom
        )
        drawHourLabels(canvas)
        if (maxCount <= 0) {
            emptyPaint.color = ColorUtils.blendColors(cardBackground, primaryText, 0.62f)
            canvas.drawText(
                context.getString(R.string.read_record_hour_histogram_empty),
                chartLeft() + chartWidth() / 2f,
                chartTop() + chartHeight() / 2f + baseline(emptyPaint),
                emptyPaint
            )
        }
    }

    private fun drawBars(
        canvas: Canvas,
        maxCount: Int,
        regularGradientTop: Int,
        regularGradientBottom: Int,
        peakGradientTop: Int,
        peakGradientBottom: Int
    ) {
        val scaleMax = max(1, maxCount)
        val slotWidth = chartWidth() / 24f
        val barWidth = (slotWidth * 0.56f).coerceAtLeast(2.dpToPx().toFloat())
        counts.forEachIndexed { hour, count ->
            if (count <= 0) return@forEachIndexed
            val centerX = chartLeft() + slotWidth * hour + slotWidth / 2f
            val barHeight = (chartHeight() * count / scaleMax).coerceAtLeast(minVisibleBarHeight)
            rect.set(
                centerX - barWidth / 2f,
                chartBottom() - barHeight,
                centerX + barWidth / 2f,
                chartBottom()
            )
            val isPeak = maxCount > 0 && count == maxCount
            barPaint.shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                if (isPeak) peakGradientTop else regularGradientTop,
                if (isPeak) peakGradientBottom else regularGradientBottom,
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, barRadius, barRadius, barPaint)
            barPaint.shader = null

        }
    }

    private fun drawHourLabels(canvas: Canvas) {
        listOf(0, 6, 12, 18, 24).forEach { hour ->
            val x = chartLeft() + chartWidth() * hour / 24f
            centerLabelPaint.textAlign = when (hour) {
                0 -> Paint.Align.LEFT
                24 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            canvas.drawText(
                hour.toString(),
                x,
                chartBottom() + baseline(centerLabelPaint, xLabelHeight.toFloat()),
                centerLabelPaint
            )
        }
        centerLabelPaint.textAlign = Paint.Align.CENTER
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
