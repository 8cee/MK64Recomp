package com.eightcee.mk64recomp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt

class TouchControlsView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        alpha = 150
    }

    private var stickX = 0f
    private var stickY = 0f
    private var stickPointer = -1

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h * 0.14f
        val baseX = w * 0.16f
        val baseY = h * 0.72f

        canvas.drawCircle(baseX, baseY, radius, paint)
        canvas.drawCircle(
            baseX + stickX * radius * 0.55f,
            baseY + stickY * radius * 0.55f,
            radius * 0.35f,
            paint
        )

        drawButton(canvas, "A", w * 0.86f, h * 0.68f, radius * 0.48f)
        drawButton(canvas, "B", w * 0.74f, h * 0.79f, radius * 0.40f)
        drawButton(canvas, "R", w * 0.88f, h * 0.20f, radius * 0.35f)
        drawButton(canvas, "Z", w * 0.12f, h * 0.22f, radius * 0.35f)
        drawButton(canvas, "START", w * 0.50f, h * 0.86f, radius * 0.35f)
    }

    private fun drawButton(canvas: Canvas, label: String, x: Float, y: Float, radius: Float) {
        canvas.drawCircle(x, y, radius, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = radius * 0.65f
        canvas.drawText(label, x, y + paint.textSize * 0.33f, paint)
        paint.style = Paint.Style.STROKE
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h * 0.14f
        val baseX = w * 0.16f
        val baseY = h * 0.72f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                val x = event.getX(i)
                val y = event.getY(i)
                if (distance(x, y, baseX, baseY) <= radius * 1.35f) {
                    stickPointer = event.getPointerId(i)
                    updateStick(x, y, baseX, baseY, radius)
                } else {
                    updateButton(x, y, true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (stickPointer >= 0) {
                    val i = event.findPointerIndex(stickPointer)
                    if (i >= 0) updateStick(event.getX(i), event.getY(i), baseX, baseY, radius)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = event.actionIndex
                val pointerId = event.getPointerId(i)
                val x = event.getX(i)
                val y = event.getY(i)
                if (pointerId == stickPointer) {
                    stickPointer = -1
                    stickX = 0f
                    stickY = 0f
                    NativeBridge.setStick(0f, 0f)
                    invalidate()
                } else {
                    updateButton(x, y, false)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                stickPointer = -1
                stickX = 0f
                stickY = 0f
                NativeBridge.setStick(0f, 0f)
                NativeBridge.releaseAllButtons()
                invalidate()
            }
        }
        return true
    }

    private fun updateStick(x: Float, y: Float, baseX: Float, baseY: Float, radius: Float) {
        var dx = (x - baseX) / radius
        var dy = (y - baseY) / radius
        val len = sqrt(dx * dx + dy * dy)
        if (len > 1f) {
            dx /= len
            dy /= len
        }
        stickX = dx
        stickY = dy
        NativeBridge.setStick(dx, -dy)
        invalidate()
    }

    private fun updateButton(x: Float, y: Float, pressed: Boolean) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h * 0.14f
        when {
            distance(x, y, w * 0.86f, h * 0.68f) <= r * 0.7f ->
                NativeBridge.setButton(NativeBridge.BUTTON_A, pressed)
            distance(x, y, w * 0.74f, h * 0.79f) <= r * 0.65f ->
                NativeBridge.setButton(NativeBridge.BUTTON_B, pressed)
            distance(x, y, w * 0.88f, h * 0.20f) <= r * 0.6f ->
                NativeBridge.setButton(NativeBridge.BUTTON_R, pressed)
            distance(x, y, w * 0.12f, h * 0.22f) <= r * 0.6f ->
                NativeBridge.setButton(NativeBridge.BUTTON_Z, pressed)
            distance(x, y, w * 0.50f, h * 0.86f) <= r * 0.65f ->
                NativeBridge.setButton(NativeBridge.BUTTON_START, pressed)
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
