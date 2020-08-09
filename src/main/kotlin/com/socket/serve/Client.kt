package com.socket.serve

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.timeout.IdleStateHandler
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


open class Client(val ip: String, var path: String, val id: Int, val group: Int, val name: String) {
    var clientHandler: ClientHandler = ClientHandler()

    init {
        if (path.startsWith("/")) {
            path = path.substring(1)
        }
    }

    fun start() {
        thread {
            doStart()
        }
    }

    private fun doStart() {
        val uri = URI("ws://$ip/$path")

        val handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, false, DefaultHttpHeaders())
        clientHandler.handshader = handshaker
        clientHandler.client = this

        val group = NioEventLoopGroup()

        try {
            val b = Bootstrap()
            b.group(group)
                    .channel(NioSocketChannel::class.java)
                    .handler(ClientChannelInitializer(clientHandler))

            val ch = b.connect(uri.host, uri.port).sync().channel()

            ch.closeFuture().sync()
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            doStart()
        } finally {
            group.shutdownGracefully()
            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            doStart()
        }
    }

    class ClientChannelInitializer constructor(ch: ClientHandler): ChannelInitializer<SocketChannel>() {
        private var clientHandler: ClientHandler = ch
        override fun initChannel(p0: SocketChannel?) {
            val p = p0?.pipeline()
            p?.addLast(IdleStateHandler(0, 4, 0, TimeUnit.SECONDS))
            p?.addLast(HttpClientCodec())
            p?.addLast(HttpObjectAggregator(8192))
            p?.addLast(clientHandler)
        }
    }
}