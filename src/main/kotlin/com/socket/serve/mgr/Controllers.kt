package com.socket.serve.mgr

import com.socket.serve.annotations.SocketController
import com.socket.serve.model.MessageBody
import io.netty.channel.ChannelHandlerContext

object Controllers {
    private val controllers = HashMap<String, Controller>()
    fun add(name: String, clz: Class<*>, obj: Any) {
        controllers[name] = Controller(clz, obj)
    }

    fun dispatch(messageBody: MessageBody, ctx: ChannelHandlerContext) {
        if (messageBody.isSync && messageBody.seq > 0) {
            Pools.dispatchSync(ctx.channel(), messageBody)
            return
        }
        controllers[messageBody.ctrl]?.let {
            it.fireEvent(messageBody, ctx)
        }
    }
}