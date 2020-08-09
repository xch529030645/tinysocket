package com.socket.serve

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory

class Ssl(val jks: String, val password: String) {
    fun getHandler(ch: io.netty.channel.socket.SocketChannel): SslHandler {
        var keyManagerFactory: KeyManagerFactory? = null
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(FileInputStream(jks), password.toCharArray())
        keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
        keyManagerFactory!!.init(keyStore, password.toCharArray())
        val sslContext = SslContextBuilder.forServer(keyManagerFactory).build()
        val engine = sslContext.newEngine(ch.alloc())
        engine.useClientMode = false
        return SslHandler(engine)
    }
}