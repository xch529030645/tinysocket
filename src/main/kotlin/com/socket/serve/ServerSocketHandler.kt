package com.socket.serve

import com.socket.serve.mgr.Controllers
import com.socket.serve.mgr.Pools
import com.socket.serve.model.Machine
import com.socket.serve.model.MessageBody
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelId
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent

@Sharable
class ServerSocketHandler(val server: CoreServer): SimpleChannelInboundHandler<Any>() {

	private val idleStates = HashMap<ChannelId, Int>()

	override fun channelActive(ctx: ChannelHandlerContext?) {

	}
	
	override fun channelInactive(ctx: ChannelHandlerContext?) {
		ctx ?: return
		Pools.remove(ctx.channel())
	}

	override fun userEventTriggered(ctx: ChannelHandlerContext?, obj: Any?) {
		obj?:return
		ctx ?: return
		if (obj is IdleStateEvent) {
			if (IdleState.READER_IDLE == obj.state()) {
				val id = ctx.channel().id()
				if (!idleStates.containsKey(id)) {
					idleStates[id] = 0
				}
				idleStates[id] = idleStates[id]?.plus(1) ?: 0
				println("已等待5秒还没收到客户端$id 发来的消息")
				if (idleStates[id]!! >= 3) {
					idleStates[id] = 0
					println("连续3次没有收到$id 消息，关闭")
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
					val id = ctx.channel().id()
					if (idleStates[id] != 0) {
						idleStates[id] = 0
					}
				} else {
					val p0 = buf.readInt()
					val p1 = buf.readInt()
					if (p0 == 987123 && p1 == 57132362) {
						val id = buf.readInt()
						val group = buf.readInt()
						var len = buf.readShort().toInt()
						val bytes = ByteArray(len)
						buf.readBytes(bytes)
						val name = String(bytes)
						val volume_total = buf.readInt()
						val volume_used = buf.readInt()
						len = buf.readShort().toInt()
						val domainBytes = ByteArray(len)
						buf.readBytes(domainBytes)
						val domain = String(domainBytes)

						var ip = ctx.channel().remoteAddress().toString()
						ip = ip.substring(1, ip.lastIndexOf(":"))
						val machine = Machine()
						machine.machine_id = id
						machine.machine_group = group
						machine.machine_name = name
						machine.ip = ip
						machine.volume_total = volume_total
						machine.volume_free = volume_used
						machine.domain = domain
						machine.isOnline = true

						Pools.add(machine, ctx.channel())
					}
				}
			} else if (msg is FullHttpRequest) {
				ctx.close()
				return
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

	override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
		super.exceptionCaught(ctx, cause)
		cause?.printStackTrace()
	}

}