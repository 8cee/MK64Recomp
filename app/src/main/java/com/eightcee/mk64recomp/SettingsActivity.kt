package com.eightcee.mk64recomp

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scale = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Original / 1x", "2x", "3x", "4x")
            )
            setSelection(AppSettings.resolutionScale(this@SettingsActivity) - 1)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    AppSettings.setResolutionScale(this@SettingsActivity, position + 1)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
        }

        val touch = Switch(this).apply {
            text = "Show touch controls"
            isChecked = AppSettings.touchControls(this@SettingsActivity)
            setOnCheckedChangeListener { _, checked ->
                AppSettings.setTouchControls(this@SettingsActivity, checked)
            }
        }

        val vsync = Switch(this).apply {
            text = "VSync"
            isChecked = AppSettings.vsync(this@SettingsActivity)
            setOnCheckedChangeListener { _, checked ->
                AppSettings.setVsync(this@SettingsActivity, checked)
            }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 36, 48, 36)
                addView(TextView(this@SettingsActivity).apply {
                    text = "MK64 Android Settings"
                    textSize = 24f
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = "Internal resolution"
                    textSize = 16f
                })
                addView(scale)
                addView(touch)
                addView(vsync)
            }
        )
    }
}
