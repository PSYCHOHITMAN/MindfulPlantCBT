package com.mindfulplant.cbt.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.mindfulplant.cbt.R

/**
 * Lightweight custom line chart for mood-over-time, matching the
 * "7-day mood" / "Mood trend" cards in the Planning and Design wireframes
 * (Figure 1). No charting library dependency - just Canvas/Path - since
 * the data here is a handful of points on a fixed 1-5 scale.
 */
class MoodTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Float> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.accent_mint)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.accent_mint)
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_muted)
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    fun setData(newValues: List<Float>) {
        values = newValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingH = 12f
        val paddingV = 16f
        val w = width - paddingH * 2
        val h = height - paddingV * 2

        if (values.size < 2) {
            canvas.drawText(
                if (values.isEmpty()) "No entries yet" else "Log one more entry to see a trend",
                width / 2f,
                height / 2f,
                emptyPaint
            )
            return
        }

        val minScore = 1f
        val maxScore = 5f
        val stepX = w / (values.size - 1)

        fun xFor(i: Int) = paddingH + stepX * i
        fun yFor(v: Float) = paddingV + h - ((v - minScore) / (maxScore - minScore)) * h

        val linePath = Path()
        val fillPath = Path()

        values.forEachIndexed { i, v ->
            val x = xFor(i)
            val y = yFor(v)
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, paddingV + h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(xFor(values.size - 1), paddingV + h)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, paddingV, 0f, paddingV + h,
            ContextCompat.getColor(context, R.color.accent_mint_soft_bg),
            android.graphics.Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        values.forEachIndexed { i, v ->
            canvas.drawCircle(xFor(i), yFor(v), 8f, dotPaint)
        }
    }
}
