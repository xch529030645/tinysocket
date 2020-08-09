package com.socket.serve.model

import io.netty.channel.ChannelHandlerContext

class Response(private val messageBody: MessageBody, private val ctx: ChannelHandlerContext) {

    fun write(obj: Any) {
        messageBody.payload = obj
        ctx.writeAndFlush(messageBody.frame)
    }
}