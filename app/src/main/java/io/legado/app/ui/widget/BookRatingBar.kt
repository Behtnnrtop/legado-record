package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import io.legado.app.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class BookRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var rating: Float = 0f
        set(value) {
            val normalized = normalizeRating(value)
            if (field != normalized) {
                field = normalized
                contentDescription = ratingDescription()
                requestLayout()
                invalidate()
            }
        }

    var onRatingChanged: ((Float) -> Unit)? = null

    private val starSize = 19f.dp()
    private val starTouchWidth = 24f.dp()
    private val minTouchHeight = 36f.dp()
    private val textGap = 8f.dp()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF6B73C.toInt()
        style = Paint.Style.FILL
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFD8D8D8.toInt()
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tv_text_summary)
        textSize = 13f.sp()
    }
    private val starPath = Path()
    private val starBounds = RectF()

    init {
        isClickable = true
        isFocusable = true
        contentDescription = ratingDescription()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val textWidth = textPaint.measureText(ratingText())
        val desiredWidth = (paddingLeft + 5 * starTouchWidth + textGap + textWidth + paddingRight).roundToInt()
        val desiredHeight = (paddingTop + minTouchHeight + paddingBottom).roundToInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = paddingTop + (height - paddingTop - paddingBottom) / 2f
        repeat(5) { index ->
            val centerX = paddingLeft + index * starTouchWidth + starTouchWidth / 2f
            buildStarPath(centerX, centerY, starSize / 2f)
            canvas.drawPath(starPath, emptyPaint)
            val fillRatio = (rating - index).coerceIn(0f, 1f)
            if (fillRatio > 0f) {
                canvas.save()
                canvas.clipRect(
                    starBounds.left,
                    starBounds.top,
                    starBounds.left + starBounds.width() * fillRatio,
                    starBounds.bottom
                )
                canvas.drawPath(starPath, fillPaint)
                canvas.restore()
            }
        }
        val textX = paddingLeft + 5 * starTouchWidth + textGap
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(ratingText(), textX, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> {
                val newRating = ratingFromX(event.x)
                val changedRating = if (newRating == rating) 0f else newRating
                rating = changedRating
                onRatingChanged?.invoke(changedRating)
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun ratingFromX(x: Float): Float {
        val starX = (x - paddingLeft).coerceIn(0f, 5 * starTouchWidth - 1f)
        val index = min((starX / starTouchWidth).toInt(), 4)
        val inSlotX = starX - index * starTouchWidth
        return index + if (inSlotX < starTouchWidth / 2f) 0.5f else 1f
    }

    private fun buildStarPath(centerX: Float, centerY: Float, radius: Float) {
        starPath.reset()
        val innerRadius = radius * 0.5f
        for (i in 0 until 10) {
            val angle = Math.toRadians((i * 36 - 90).toDouble())
            val pointRadius = if (i % 2 == 0) radius else innerRadius
            val x = centerX + (cos(angle) * pointRadius).toFloat()
            val y = centerY + (sin(angle) * pointRadius).toFloat()
            if (i == 0) {
                starPath.moveTo(x, y)
            } else {
                starPath.lineTo(x, y)
            }
        }
        starPath.close()
        starPath.computeBounds(starBounds, true)
    }

    private fun ratingText(): String {
        return if (rating <= 0f) {
            context.getString(R.string.book_rating_unrated)
        } else {
            context.getString(R.string.book_rating_score, rating)
        }
    }

    private fun ratingDescription(): String {
        return if (rating <= 0f) {
            context.getString(R.string.book_rating_unrated)
        } else {
            context.getString(R.string.book_rating_score, rating)
        }
    }

    private fun normalizeRating(value: Float): Float {
        if (value.isNaN() || value.isInfinite() || value <= 0f) return 0f
        return (value.coerceIn(0.5f, 5f) * 2).roundToInt() / 2f
    }

    private fun Float.dp(): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics)
    }

    private fun Float.sp(): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, resources.displayMetrics)
    }
}
