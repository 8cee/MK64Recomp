package com.eightcee.mk64recomp

import android.view.Surface

object NativeBridge {
    const val BUTTON_A = 0
    const val BUTTON_B = 1
    const val BUTTON_Z = 2
    const val BUTTON_L = 3
    const val BUTTON_R = 4
    const val BUTTON_START = 5
    const val BUTTON_C_UP = 6
    const val BUTTON_C_DOWN = 7
    const val BUTTON_C_LEFT = 8
    const val BUTTON_C_RIGHT = 9
    const val BUTTON_DPAD_UP = 10
    const val BUTTON_DPAD_DOWN = 11
    const val BUTTON_DPAD_LEFT = 12
    const val BUTTON_DPAD_RIGHT = 13

    init {
        runCatching {
            System.loadLibrary("mk64android")
            Diagnostics.info("Native library mk64android loaded")
        }.onFailure {
            Diagnostics.error("Failed to load native library mk64android", it)
        }
    }

    external fun nativeVersion(): String
    external fun graphicsInfo(): String
    external fun initialize(romPath: String): Int
    external fun shutdown()

    external fun attachSurface(surface: Surface): Int
    external fun resizeSurface(width: Int, height: Int)
    external fun detachSurface()

    external fun setStick(x: Float, y: Float)
    external fun setButton(button: Int, pressed: Boolean)
    external fun releaseAllButtons()
}
