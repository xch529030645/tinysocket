package com.socket.serve

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.timeout.IdleStateHandler
import java.util.concurrent.TimeUnit


class ChannelDispatcher(val ssl: Ssl?, val websocketPath: String, val serverSocketHandler: ServerSocketHandler): ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel) {
		val p = ch.pipeline()

		ssl?.let {
			p.addLast(it.getHandler(ch))
		}
		p.addLast(IdleStateHandler(5,0,0,TimeUnit.SECONDS))
		p.addLast(HttpServerCodec())
        p.addLast(HttpObjectAggregator(65536))
        p.addLast(WebSocketServerProtocolHandler(websocketPath, null, true))
        p.addLast(serverSocketHandler)
	}
}