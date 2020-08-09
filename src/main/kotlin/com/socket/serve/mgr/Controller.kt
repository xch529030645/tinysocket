package com.socket.serve.mgr

import com.socket.serve.annotations.EventMapping
import com.socket.serve.model.MessageBody
import io.netty.channel.ChannelHandlerContext

class Controller(val clazz: Class<*>, val obj: Any) {
    private val events = HashMap<String, Event>()

    init {
        clazz.declaredMethods.forEach {
            if (it.isAnnotationPresent(EventMapping::class.java)) {
                val annotation = it.getAnnotation(EventMapping::class.java)
                events[annotation.name] = Event(obj, it)
            }
        }
    }

    fun fireEvent(messageBody: MessageBody, ctx: ChannelHandlerContext) {
        events[messageBody.event]?.invoke(messageBody, ctx)
    }
}