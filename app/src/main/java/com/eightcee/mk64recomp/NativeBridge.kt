package com.eightcee.mk64recomp

object NativeBridge {
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
}
