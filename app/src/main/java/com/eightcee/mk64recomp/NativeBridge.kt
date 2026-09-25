package com.eightcee.mk64recomp

import android.view.Surface

object NativeBridge {
    const val BUTTON_A = 1
    const val BUTTON_B = 2
    const val BUTTON_Z = 3
    const val BUTTON_START = 4
    const val BUTTON_R = 5

    init {
        runCatching {
            System.loadLibrary("mk64android")
            Diagnostics.info("Native library mk64android loaded")
        }.onFailure {
            Diagnostics.error("Failed to load native library mk64android", it)
        }
    }

    external fun nativeVersion(): String
    external fun initialize(romPath: String): Int
    external fun shutdown()

    external fun attachSurface(surface: Surface): Int
    external fun resizeSurface(width: Int, height: Int)
    external fun detachSurface()

    external fun setStick(x: Float, y: Float)
    external fun setButton(button: Int, pressed: Boolean)
    external fun releaseAllButtons()
}
