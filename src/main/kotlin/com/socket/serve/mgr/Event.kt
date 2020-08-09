package com.socket.serve.mgr

import com.socket.serve.annotations.Payload
import com.socket.serve.model.MessageBody
import com.socket.serve.model.Response
import io.netty.channel.ChannelHandlerContext
import java.lang.reflect.Method

class Event(val obj: Any, val method: Method) {

    fun invoke(messageBody: MessageBody, ctx: ChannelHandlerContext) {
        if (method.parameterCount > 0) {
            val params = Array<Any?>(method.parameterCount){}
            val isPayloadMap = messageBody.payload is Map<*, *>
            for (p in method.parameters.withIndex()) {
                if (p.value.type == Response::class.java) {
                    params[p.index] = Response(messageBody, ctx)
                } else if (p.value.isAnnotationPresent(Payload::class.java)) {
                    params[p.index] = messageBody.payload
                } else {
                    if (isPayloadMap) {
                        with(messageBody.payload as Map<*,*>) {
                            val v = this[p.value.name]
                            params[p.index] = v
                        }
                    }
                }
            }
            method.invoke(obj, *params)
        } else {
            method.invoke(obj)
        }?.let {
            messageBody.payload = it
            ctx.writeAndFlush(messageBody.frame)
        }
    }
}