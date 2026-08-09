package com.example.gestixtv

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

/**
 * El backend no tiene un evento dedicado a "ticket actualizado", pero SÍ emite
 * 'nueva-notificacion' cada vez que se crea, asigna o resuelve un ticket
 * (siempre trae ticket_id). Usamos esa señal para refrescar el dashboard en
 * tiempo real en vez de hacer polling.
 */
object TicketSocketManager {
    private const val TAG = "TicketSocketManager"
    private const val EVENT_NEW_NOTIFICATION = "nueva-notificacion"

    private var socket: Socket? = null

    fun connect(onTicketChanged: () -> Unit, onConnectionChange: (Boolean) -> Unit) {
        if (socket?.connected() == true) return
        try {
            socket = IO.socket(ApiService.SOCKET_URL).also { socket ->
                socket.on(Socket.EVENT_CONNECT) { onConnectionChange(true) }
                socket.on(Socket.EVENT_DISCONNECT) { onConnectionChange(false) }
                socket.on(Socket.EVENT_CONNECT_ERROR) {
                    Log.w(TAG, "No se pudo conectar al socket")
                    onConnectionChange(false)
                }
                socket.on(EVENT_NEW_NOTIFICATION) { args ->
                    val json = args.getOrNull(0) as? JSONObject ?: return@on
                    if (json.has("ticket_id")) {
                        onTicketChanged()
                    }
                }
                socket.connect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error iniciando socket", e)
            onConnectionChange(false)
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }
}
