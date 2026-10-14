/*
 * SPDX-FileCopyrightText: 2026 The XPerience Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class ColorSwatchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var topColor: Int = Color.GRAY
        set(value) {
            field = value
            topPaint.color = value
            invalidate()
        }

    var bottomColor: Int = Color.DKGRAY
        set(value) {
            field = value
            bottomPaint.color = value
            invalidate()
        }

    var ringColor: Int = Color.WHITE
        set(value) {
            field = value
            ringPaint.color = value
            invalidate()
        }

    var isSwatchSelected: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val topPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val circleBounds = RectF()

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }

    init {
        isFocusable = true
        isClickable = true
        val outValue = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)) {
            setBackgroundResource(outValue.resourceId)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = dp(56f).toInt()
        val width = resolveSize(defaultSize, widthMeasureSpec)
        val height = resolveSize(defaultSize, heightMeasureSpec)
        val size = minOf(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        val strokeWidth = dp(2.5f)
        ringPaint.strokeWidth = strokeWidth

        val innerRadius = dp(19f)
        val outerRadius = dp(24.5f)

        if (isSwatchSelected) {
            ringPaint.color = ringColor
            canvas.drawCircle(cx, cy, outerRadius, ringPaint)
        }

        circleBounds.set(
            cx - innerRadius,
            cy - innerRadius,
            cx + innerRadius,
            cy + innerRadius
        )

        if (topColor == bottomColor) {
            canvas.drawCircle(cx, cy, innerRadius, topPaint)
        } else {
            // Draw top half: start at 180° (9 o'clock) sweep 180° clockwise to 3 o'clock
            canvas.drawArc(circleBounds, 180f, 180f, true, topPaint)
            // Draw bottom half: start at 0° (3 o'clock) sweep 180° clockwise to 9 o'clock
            canvas.drawArc(circleBounds, 0f, 180f, true, bottomPaint)
        }
    }
}
