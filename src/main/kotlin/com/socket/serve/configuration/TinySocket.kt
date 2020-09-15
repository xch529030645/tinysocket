package com.socket.serve.configuration

import com.socket.serve.`interface`.ISocketDelegate
import com.socket.serve.annotations.SocketController
import com.socket.serve.mgr.Controllers
import com.socket.serve.mgr.Pools
import javax.annotation.PostConstruct

open class TinySocket {

    @PostConstruct
    fun init() {
        val clz = this::class.java
        val annotation = clz.getAnnotation(SocketController::class.java)
        Controllers.add(annotation.name, clz, this)

        if (this is ISocketDelegate) {
            Pools.addDelegate(this)
        }
    }
}