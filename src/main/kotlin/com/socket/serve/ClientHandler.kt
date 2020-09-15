package com.socket.serve

import com.socket.serve.enum.EventType
import com.socket.serve.mgr.Controllers
import com.socket.serve.mgr.FileTransfer
import com.socket.serve.mgr.Pools
import com.socket.serve.model.Machine
import com.socket.serve.model.MessageBody
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import java.io.File
import java.lang.Exception


@ChannelHandler.Sharable
open class ClientHandler: SimpleChannelInboundHandler<Any>() {
    lateinit var handshader: WebSocketClientHandshaker
    lateinit var client: Client

    private val HEARTBEAT_SEQUENCE = BinaryWebSocketFrame(Unpooled.unreleasableBuffer(Unpooled.buffer(0)))

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (ctx != null) {
            val ch = ctx.channel()
            if (!handshader.isHandshakeComplete) {
                handshader.finishHandshake(ch, msg as FullHttpResponse)
                println("connected!!!")

                var volume_total = 0
                var volume_free = 0
                try {
                    val f = File("/")
                    volume_total = (f.totalSpace / 1024).toInt()
                    volume_free = (f.usableSpace / 1024).toInt()
                } catch (e: Exception) {

                }

                val buf = Unpooled.buffer(16)
                buf.writeInt(987123)
                buf.writeInt(57132362)
                buf.writeInt(client.id)
                buf.writeInt(client.group)
                val bytes = client.name.toByteArray()
                buf.writeShort(bytes.size)
                buf.writeBytes(bytes)
                buf.writeInt(volume_total)
                buf.writeInt(volume_free)
                val domainBytes = client.domain.toByteArray()
                buf.writeShort(domainBytes.size)
                buf.writeBytes(domainBytes)
                ctx.writeAndFlush(BinaryWebSocketFrame(buf))

                var ip = ctx.channel().remoteAddress().toString()
                ip = ip.substring(1, ip.lastIndexOf(":"))
                val machine = Machine()
                machine.machine_id = 0
                machine.machine_group = 0
                machine.machine_name = "server"
                machine.ip = ip
                machine.volume_total = 0
                machine.volume_free = 0
                machine.isOnline = true

                Pools.add(machine, ctx.channel())

                return
            }

            if (msg is TextWebSocketFrame) {
                val text = msg.text()
                val messageBody = MessageBody()
                messageBody.read(text)
                Controllers.dispatch(messageBody, ctx)
            } else if (msg is BinaryWebSocketFrame) {
                val buf = msg.content()
                val msgType = buf.readInt()
                if (msgType == EventType.Event_Transfer_File.code) {
                    FileTransfer.receive(buf)
                }
            }

        }

    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (ctx != null) {
            handshader.handshake(ctx.channel())
            val incoming = ctx.channel()
            println("connected to server: $incoming")

        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        super.channelInactive(ctx)
        if (ctx != null) {
            Pools.remove(ctx.channel())
            val incoming = ctx.channel()
            println("disconnected to server: $incoming")
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
//        super.exceptionCaught(ctx, cause)
        cause?.printStackTrace()
        println("load server close!!!!!!!!")
        ctx?.close()
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext?, obj: Any?) {
//        super.userEventTriggered(ctx, evt)
        obj ?: return
        if (obj is IdleStateEvent) {
            if (IdleState.WRITER_IDLE == obj.state()) {
//                println("发送心跳")
                ctx!!.channel().writeAndFlush(HEARTBEAT_SEQUENCE.duplicate())
            }
        } else {
            super.userEventTriggered(ctx, obj)
        }
    }

}