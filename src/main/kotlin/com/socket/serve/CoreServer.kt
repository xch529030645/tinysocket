package com.socket.serve

import com.socket.serve.ServerSocketHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOption
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.concurrent.GlobalEventExecutor


class CoreServer(val websocketPath: String, val port: Int) {
	private val serverSocketHandler: ServerSocketHandler = ServerSocketHandler(this)
	private val channelDispatcher: ChannelDispatcher = ChannelDispatcher(null, websocketPath, serverSocketHandler)
	lateinit var channel: Channel

	fun start() {
		val bossGroup = NioEventLoopGroup(1)
		val workerGroup = NioEventLoopGroup()
		val b = ServerBootstrap()
		b.option(ChannelOption.SO_BACKLOG, 1024)
		b.group(bossGroup, workerGroup)
		 .channel(NioServerSocketChannel::class.java)
		 .handler(LoggingHandler(LogLevel.INFO))
		 .childHandler(channelDispatcher)
		
		channel = b.bind(port).sync().channel()
		println("socket server startup successfully! ====> http://127.0.0.1:$port")
		channel.closeFuture().sync()
		
		bossGroup.shutdownGracefully()
		workerGroup.shutdownGracefully()
	}


}