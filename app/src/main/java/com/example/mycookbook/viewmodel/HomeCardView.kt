package com.example.mycookbook.viewmodel

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class HomeCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val rectF = RectF()
    private val cornerRadii = floatArrayOf(
        24f * resources.displayMetrics.density,
        24f * resources.displayMetrics.density,
        24f * resources.displayMetrics.density,
        24f * resources.displayMetrics.density,
        0f, 0f, 0f, 0f
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rectF.set(0f, 0f, width.toFloat(), height.toFloat())
        val path = Path().apply {
            addRoundRect(rectF, cornerRadii, Path.Direction.CW)
        }
        canvas.drawPath(path, paint)

        canvas.drawCircle(50f * resources.displayMetrics.density, 50f * resources.displayMetrics.density, 30f * resources.displayMetrics.density, holePaint)
        canvas.drawCircle(width - 70f * resources.displayMetrics.density, 70f * resources.displayMetrics.density, 20f * resources.displayMetrics.density, holePaint)
    }
}
