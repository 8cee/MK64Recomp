package com.eightcee.mk64recomp

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

class MultiplayerClient {
    private var socket: DatagramSocket? = null
    private var thread: Thread? = null
    private val running = AtomicBoolean(false)

    @Volatile var roomCode: String = ""
        private set
    @Volatile var connected: Boolean = false
        private set
    @Volatile var lastPingMs: Long = -1
        private set

    fun connect(host: String, port: Int, room: String, playerName: String) {
        disconnect()
        roomCode = room.trim().uppercase()
        running.set(true)

        thread = Thread({
            try {
                val address = InetAddress.getByName(host)
                val s = DatagramSocket().also {
                    it.soTimeout = 1000
                    socket = it
                }

                Diagnostics.info("MP connect host=" + host + " port=" + port + " room=" + roomCode)
                sendJson(
                    s,
                    address,
                    port,
                    JSONObject()
                        .put("type", "join")
                        .put("room", roomCode)
                        .put("name", playerName)
                )

                val buffer = ByteArray(4096)
                var lastHeartbeat = 0L

                while (running.get()) {
                    val now = System.currentTimeMillis()
                    if (now - lastHeartbeat >= 2000L) {
                        sendJson(
                            s,
                            address,
                            port,
                            JSONObject()
                                .put("type", "ping")
                                .put("room", roomCode)
                                .put("clientTime", now)
                        )
                        lastHeartbeat = now
                    }

                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        s.receive(packet)
                        val text = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                        handlePacket(JSONObject(text))
                    } catch (_: java.net.SocketTimeoutException) {
                    }
                }
            } catch (t: Throwable) {
                Diagnostics.error("Multiplayer client failed", t)
            } finally {
                connected = false
                socket?.close()
                socket = null
                Diagnostics.info("MP disconnected")
            }
        }, "MK64-Multiplayer")
        thread?.start()
    }

    fun disconnect() {
        running.set(false)
        socket?.close()
        thread?.interrupt()
        thread = null
        connected = false
    }

    private fun handlePacket(obj: JSONObject) {
        when (obj.optString("type")) {
            "joined" -> {
                connected = true
                Diagnostics.info(
                    "MP joined room=" + obj.optString("room") +
                        " players=" + obj.optInt("players")
                )
            }
            "pong" -> {
                val sent = obj.optLong("clientTime", 0L)
                if (sent > 0L) {
                    lastPingMs = (System.currentTimeMillis() - sent).coerceAtLeast(0L)
                }
            }
            "peer_joined" -> Diagnostics.info("MP peer joined")
            "peer_left" -> Diagnostics.info("MP peer left")
            "error" -> Diagnostics.warn("MP server error: " + obj.optString("message"))
        }
    }

    private fun sendJson(
        socket: DatagramSocket,
        address: InetAddress,
        port: Int,
        obj: JSONObject
    ) {
        val bytes = obj.toString().toByteArray(Charsets.UTF_8)
        socket.send(DatagramPacket(bytes, bytes.size, address, port))
    }
}
