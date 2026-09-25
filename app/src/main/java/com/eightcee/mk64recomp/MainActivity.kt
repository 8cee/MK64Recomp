package com.eightcee.mk64recomp

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Diagnostics.init(applicationContext)
        CrashHandler.install(applicationContext)
        Diagnostics.info("MainActivity.onCreate")
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContentView(TextView(this).apply {
            text = "MK64Recomp Android\n\nFoundation ready.\nDiagnostics active."
            textSize = 24f
            gravity = Gravity.CENTER
        })
        Diagnostics.info("UI initialized")
    }
    override fun onResume() { super.onResume(); Diagnostics.info("MainActivity.onResume") }
    override fun onPause() { Diagnostics.info("MainActivity.onPause"); super.onPause() }
}
