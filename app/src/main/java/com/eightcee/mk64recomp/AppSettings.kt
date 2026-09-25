package com.eightcee.mk64recomp

import android.content.Context

object AppSettings {
    private const val PREFS = "mk64_settings"
    private const val KEY_RESOLUTION = "resolution_scale"
    private const val KEY_TOUCH = "touch_controls"
    private const val KEY_VSYNC = "vsync"

    fun resolutionScale(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_RESOLUTION, 1)

    fun setResolutionScale(context: Context, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_RESOLUTION, value.coerceIn(1, 4)).apply()
    }

    fun touchControls(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_TOUCH, true)

    fun setTouchControls(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_TOUCH, enabled).apply()
    }

    fun vsync(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VSYNC, true)

    fun setVsync(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VSYNC, enabled).apply()
    }
}
