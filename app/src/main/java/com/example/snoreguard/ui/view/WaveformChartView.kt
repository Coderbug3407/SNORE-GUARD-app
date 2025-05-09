package com.example.snoreguard.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min
import kotlin.math.max

/**
 * A custom view that displays snoring data as a waveform visualization
 * similar to audio waveforms, with intensity represented by amplitude and color.
 */
class WaveformChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        strokeWidth = 1f
        alpha = 70
    }
    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val path = Path()
    private val rect = RectF()

    // Data
    private var data: List<Float> = emptyList()
    private var interventions: List<Boolean> = emptyList()
    private var maxValue = 100f
    private var animatedValues: List<Float> = emptyList()

    // Colors
    private val quietColor = Color.parseColor("#4CAF50")  // Green
    private val lightColor = Color.parseColor("#8BC34A")  // Light Green
    private val mediumColor = Color.parseColor("#FFC107") // Amber
    private val loudColor = Color.parseColor("#FF9800")   // Orange
    private val epicColor = Color.parseColor("#FF5722")   // Deep Orange
    private val interventionColor = Color.parseColor("#00BCD4") // Cyan

    // Animation
    private var animator: ValueAnimator? = null
    private var animationProgress = 0f

    init {
        // Default initialization
        if (isInEditMode) {
            setData(listOf(20f, 40f, 60f, 80f, 100f, 80f, 60f, 40f, 20f), listOf(false, false, true, true, false, false, true, false, false))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(epicColor, loudColor, mediumColor, lightColor, quietColor),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        barPaint.shader = gradient
    }

    fun setData(values: List<Float>, interventions: List<Boolean> = emptyList()) {
        require(interventions.isEmpty() || interventions.size == values.size) {
            "Interventions list size must match values list size"
        }

        this.data = values
        this.interventions = interventions
        this.maxValue = values.maxOrNull() ?: 100f
        this.animatedValues = values.map { 0f }

        // Cancel any existing animation
        animator?.cancel()

        // Start animation
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                animationProgress = valueAnimator.animatedValue as Float
                animatedValues = data.map { it * animationProgress }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty() || width == 0 || height == 0) return

        val barCount = data.size
        val availableWidth = width.toFloat()
        val availableHeight = height.toFloat() * 0.9f  // Leave room for labels
        val maxBarHeight = availableHeight * 0.9f      // Max height for bars
        val barSpacing = 8f                            // Space between bars
        val barWidth = (availableWidth - (barCount * barSpacing)) / barCount

        // Draw horizontal grid lines
        val gridLinesCount = 5
        for (i in 0..gridLinesCount) {
            val y = height - (i * availableHeight / gridLinesCount)
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        // Draw waveform
        var xPosition = barSpacing / 2

        for (i in animatedValues.indices) {
            val value = animatedValues[i]
            val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
            val barHeight = normalizedValue * maxBarHeight

            // Create variable bar colors based on intensity
            var color = when {
                value >= 80 -> epicColor
                value >= 60 -> loudColor
                value >= 40 -> mediumColor
                value >= 20 -> lightColor
                else -> quietColor
            }

            barPaint.color = color

            // Draw the bar with rounded top
            val centerX = xPosition + barWidth / 2
            val topY = height - barHeight - availableHeight * 0.05f
            val bottomY = height - availableHeight * 0.05f

            // If there was an intervention at this point, mark it
            if (interventions.getOrNull(i) == true) {
                val interventionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = interventionColor
                    alpha = 180
                }
                canvas.drawCircle(centerX, topY - 10f, 8f, interventionPaint)
            }

            // Draw the bar with a nice gradient effect
            rect.set(xPosition, topY, xPosition + barWidth, bottomY)
            canvas.drawRoundRect(rect, 8f, 8f, barPaint)

            xPosition += barWidth + barSpacing
        }
    }
}