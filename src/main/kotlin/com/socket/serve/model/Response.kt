package com.socket.serve.model

import com.socket.serve.mgr.ChannelAttr
import io.netty.channel.ChannelHandlerContext

class Response(private val messageBody: MessageBody, private val ctx: ChannelHandlerContext) {
    val id = ctx.channel().attr(ChannelAttr.idAttr).get()

    fun write(obj: Any) {
        messageBody.payload = obj
        ctx.writeAndFlush(messageBody.frame)
    }
}