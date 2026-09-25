package com.eightcee.mk64recomp

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.InputDevice
import android.widget.FrameLayout
import android.widget.TextView

class GameActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var surfaceView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Diagnostics.info("GameActivity.onCreate")

        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)

        val root = FrameLayout(this)
        root.addView(
            surfaceView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            TouchControlsView(this),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            TextView(this).apply {
                text = "MK64 Android host"
                textSize = 14f
                alpha = 0.6f
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
        )

        setContentView(root)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Diagnostics.info("Game surface created")
        val result = runCatching {
            NativeBridge.attachSurface(holder.surface)
        }.getOrElse {
            Diagnostics.error("attachSurface failed", it)
            -99
        }
        Diagnostics.info("attachSurface result=" + result)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Diagnostics.info("Game surface changed width=" + width + " height=" + height)
        runCatching { NativeBridge.resizeSurface(width, height) }
            .onFailure { Diagnostics.error("resizeSurface failed", it) }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Diagnostics.info("Game surface destroyed")
        runCatching { NativeBridge.detachSurface() }
            .onFailure { Diagnostics.error("detachSurface failed", it) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            val pressed = event.action == KeyEvent.ACTION_DOWN
            val button = when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> NativeBridge.BUTTON_A
                KeyEvent.KEYCODE_BUTTON_B -> NativeBridge.BUTTON_B
                KeyEvent.KEYCODE_BUTTON_L1 -> NativeBridge.BUTTON_L
                KeyEvent.KEYCODE_BUTTON_R1 -> NativeBridge.BUTTON_R
                KeyEvent.KEYCODE_BUTTON_START -> NativeBridge.BUTTON_START
                KeyEvent.KEYCODE_BUTTON_X -> NativeBridge.BUTTON_C_LEFT
                KeyEvent.KEYCODE_BUTTON_Y -> NativeBridge.BUTTON_C_UP
                else -> -1
            }
            if (button >= 0) {
                NativeBridge.setButton(button, pressed)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE &&
            event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            val x = centeredAxis(event, MotionEvent.AXIS_X)
            val y = centeredAxis(event, MotionEvent.AXIS_Y)
            NativeBridge.setStick(x, -y)

            val lTrigger = axis(event, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
            val rTrigger = axis(event, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS)
            NativeBridge.setButton(NativeBridge.BUTTON_Z, lTrigger > 0.45f)
            NativeBridge.setButton(NativeBridge.BUTTON_R, rTrigger > 0.45f)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun centeredAxis(event: MotionEvent, axis: Int): Float {
        val device = event.device ?: return 0f
        val range = device.getMotionRange(axis, event.source) ?: return 0f
        val value = event.getAxisValue(axis)
        return if (kotlin.math.abs(value) > range.flat) value.coerceIn(-1f, 1f) else 0f
    }

    private fun axis(event: MotionEvent, primary: Int, fallback: Int): Float {
        val p = event.getAxisValue(primary)
        return if (p != 0f) p else event.getAxisValue(fallback)
    }

    override fun onDestroy() {
        NativeBridge.releaseAllButtons()
        NativeBridge.setStick(0f, 0f)
        Diagnostics.info("GameActivity.onDestroy")
        super.onDestroy()
    }
}
