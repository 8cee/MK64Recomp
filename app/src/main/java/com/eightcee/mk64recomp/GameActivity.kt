package com.eightcee.mk64recomp

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
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

    override fun onDestroy() {
        Diagnostics.info("GameActivity.onDestroy")
        super.onDestroy()
    }
}
