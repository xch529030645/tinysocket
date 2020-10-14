package com.socket.serve.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.json.JsonMapper
import com.socket.serve.Json
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import java.util.*

class MessageBody {
    var ctrl = ""
    var event = ""
    var payload: Any? = null
    var isSync = false
    var seq = -1

    fun read(text: String) {
        val json = JSONObject(text)
        ctrl = json.optString("ctrl")
        event = json.optString("event")
        isSync = json.optBoolean("sync")
        seq = json.optInt("seq")
        val data = json.optString("payload")
        try {
            if (data.isNotBlank()) {
                val bytes = Base64.getDecoder().decode(data)
                val ois = ObjectInputStream(ByteArrayInputStream(bytes))
                payload = ois.readObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun frame(): TextWebSocketFrame {
        seq++

//        val data = Json.writeValueAsString(this)
//        return TextWebSocketFrame(data)

            val json = JSONObject()
            json.put("ctrl", ctrl)
            json.put("event", event)
            json.put("sync", isSync)
            json.put("seq", seq)

            if (payload != null) {
                val bos = ByteArrayOutputStream()
                val oos = ObjectOutputStream(bos)
                oos.writeObject(payload)
                val data = Base64.getEncoder().encodeToString(bos.toByteArray())
                json.put("payload", data)
            }
            return TextWebSocketFrame(json.toString())
        }
}