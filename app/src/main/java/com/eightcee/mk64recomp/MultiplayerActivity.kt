package com.eightcee.mk64recomp

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MultiplayerActivity : Activity() {
    private val client = MultiplayerClient()
    private lateinit var status: TextView
    private lateinit var host: EditText
    private lateinit var room: EditText
    private lateinit var name: EditText

    private val refresh = object : Runnable {
        override fun run() {
            updateStatus()
            status.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("multiplayer", MODE_PRIVATE)

        host = EditText(this).apply {
            hint = "Server host or IP"
            setSingleLine()
            setText(prefs.getString("host", ""))
        }
        room = EditText(this).apply {
            hint = "Room code (blank = new room)"
            setSingleLine()
            setText(prefs.getString("room", ""))
        }
        name = EditText(this).apply {
            hint = "Player name"
            setSingleLine()
            setText(prefs.getString("name", "Player"))
        }
        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(16, 24, 16, 24)
        }

        val connect = Button(this).apply {
            text = "Connect / Join Room"
            setOnClickListener {
                val h = host.text.toString().trim()
                val r = room.text.toString().trim()
                val n = name.text.toString().trim().ifEmpty { "Player" }

                if (h.isEmpty()) {
                    Toast.makeText(
                        this@MultiplayerActivity,
                        "Enter the multiplayer server host/IP",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                prefs.edit()
                    .putString("host", h)
                    .putString("room", r)
                    .putString("name", n)
                    .apply()

                client.connect(h, 6464, r, n)
                Diagnostics.info("Multiplayer connect requested")
                updateStatus()
            }
        }

        val disconnect = Button(this).apply {
            text = "Disconnect"
            setOnClickListener {
                client.disconnect()
                updateStatus()
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 36, 48, 36)
            addView(TextView(this@MultiplayerActivity).apply {
                text = "MK64 Online Multiplayer"
                textSize = 24f
                gravity = Gravity.CENTER
            })
            addView(host)
            addView(room)
            addView(name)
            addView(connect)
            addView(disconnect)
            addView(status)
        }

        setContentView(root)
        status.post(refresh)
    }

    private fun updateStatus() {
        status.text = buildString {
            appendLine("Connection: " + if (client.connected) "connected" else "disconnected")
            if (client.roomCode.isNotEmpty()) appendLine("Room: " + client.roomCode)
            if (client.lastPingMs >= 0) appendLine("Ping: " + client.lastPingMs + " ms")
            append("Port: 6464 / UDP")
        }
    }

    override fun onDestroy() {
        status.removeCallbacks(refresh)
        client.disconnect()
        super.onDestroy()
    }
}
