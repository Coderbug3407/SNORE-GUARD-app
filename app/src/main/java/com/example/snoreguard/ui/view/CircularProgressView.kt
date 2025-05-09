package com.example.snoreguard.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#333333")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }

    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }

    private val rectF = RectF()
    private var strokeWidth = 20f
    private var progress = 0
    private var animatedProgress = 0f
    private var gradient: SweepGradient? = null

    init {
        progressPaint.strokeWidth = strokeWidth
        backgroundPaint.strokeWidth = strokeWidth

        val colors = intArrayOf(
            Color.parseColor("#6200EA"), // Deep Purple
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FF5722")  // Deep Orange
        )

        setProgress(0)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        strokeWidth = min(width, height) / 12f
        progressPaint.strokeWidth = strokeWidth
        backgroundPaint.strokeWidth = strokeWidth

        textPaint.textSize = min(width, height) * 0.2f
        percentPaint.textSize = min(width, height) * 0.08f

        updateGradient()

        val diameter = min(width, height) - strokeWidth * 2
        val left = (width - diameter) / 2
        val top = (height - diameter) / 2
        rectF.set(left, top, left + diameter, top + diameter)
    }

    private fun updateGradient() {
        val colors = intArrayOf(
            Color.parseColor("#6200EA"), // Deep Purple
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#FFC107"), // Amber
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FF5722")  // Deep Orange
        )
        gradient = SweepGradient(width / 2f, height / 2f, colors, null)
        progressPaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawArc(rectF, 135f, 270f, false, backgroundPaint)

        val sweepAngle = 270f * (animatedProgress / 100f)
        canvas.drawArc(rectF, 135f, sweepAngle, false, progressPaint)

        val centerX = width / 2f
        val centerY = height / 2f

        canvas.drawText("$progress", centerX, centerY + textPaint.textSize / 3, textPaint)

        canvas.drawText("%", centerX, centerY + textPaint.textSize / 3 + percentPaint.textSize, percentPaint)
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        this.animatedProgress = progress.toFloat()
        invalidate()
    }
}