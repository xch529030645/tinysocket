package com.socket.serve

import com.socket.serve.mgr.Controllers
import com.socket.serve.mgr.Pools
import com.socket.serve.model.MessageBody
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent

@Sharable
class ServerSocketHandler(val server: CoreServer): SimpleChannelInboundHandler<Any>() {

	override fun channelActive(ctx: ChannelHandlerContext?) {

	}
	
	override fun channelInactive(ctx: ChannelHandlerContext?) {
		ctx ?: return
		Pools.remove(ctx.channel())
	}

	var idleTimes = 0
	override fun userEventTriggered(ctx: ChannelHandlerContext?, obj: Any?) {
		obj?:return
		ctx ?: return
		if (obj is IdleStateEvent) {
			if (IdleState.READER_IDLE == obj.state()) {
				idleTimes++
				println("已等待5秒还没收到客户端发来的消息")
				if (idleTimes >= 3) {
					println("连续3次没有收到消息，关闭")
					Pools.remove(ctx.channel())
					ctx.close()
				}
			}
		} else {
			super.userEventTriggered(ctx, obj)
		}
	}

	override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
		if (ctx != null && msg != null) {
			if (msg is TextWebSocketFrame) {
				val text = msg.text()
				val messageBody = MessageBody()
				messageBody.read(text)
				Controllers.dispatch(messageBody, ctx)
			} else if (msg is BinaryWebSocketFrame) {
				val buf = msg.content()
				val c = buf.capacity()
				if (c == 0) {
					if (idleTimes != 0) {
						idleTimes = 0
					}
				} else {
					val p0 = buf.readInt()
					val p1 = buf.readInt()
					if (p0 == 987123 && p1 == 57132362) {
						val id = buf.readInt()
						val group = buf.readInt()
						val len = buf.readShort().toInt()
						val bytes = ByteArray(len)
						buf.readBytes(bytes)
						val name = String(bytes)
						Pools.add(id, group, name, ctx.channel())
					}
				}
			}
		}

		super.channelRead(ctx, msg)
	}

	override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
//		ctx ?: return
//		msg ?: return
//		val text = msg.text()
//		val messageBody = MessageBody()
//		messageBody.read(text)
//		Controllers.dispatch(messageBody, ctx)
	}

}