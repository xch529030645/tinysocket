package com.socket.serve.configuration

import com.socket.serve.annotations.*
import com.socket.serve.mgr.FileTransfer
import com.socket.serve.mgr.Pools
import com.socket.serve.mgr.SentEvent
import com.socket.serve.model.MessageBody
import java.io.File
import java.io.Serializable
import java.lang.Exception
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.thread

class ProxySender(val ctrl: String): InvocationHandler {
    lateinit var interfaceClass: Class<*>
    lateinit var proxy: Any

    private val events = HashMap<String, SentEvent>()

    fun bind(clz: Class<*>): Any {
        this.interfaceClass = clz;
        prepareEvents()
        proxy = Proxy.newProxyInstance(clz.classLoader, arrayOf(interfaceClass), this)
        return proxy
    }

    private fun prepareEvents() {
        interfaceClass.declaredMethods.forEach {
            if (it.isAnnotationPresent(EventMapping::class.java)) {
                val anno = it.getAnnotation(EventMapping::class.java)
                val isSync = it.isAnnotationPresent(Sync::class.java)
                val e = anno.name
                events[it.name] = SentEvent(e, isSync)
            }
        }
    }

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        events[method.name]?.let {
            val messageBody = MessageBody()
            messageBody.ctrl = ctrl
            messageBody.event = it.name
            if (args != null) {
                var id = -1
                var group = -1
                var data: Any? = null
                var params: HashMap<String, Any>? = null
                var isTransferFile = false
                var targetFile: File? = null
                for (p in method.parameters.withIndex()) {
                    if (p.value.isAnnotationPresent(SendTo::class.java)) {
                        val anno = p.value.getAnnotation(SendTo::class.java)
                        val v = args[p.index] as Int
                        if (anno.attr == "id") {
                            id = v
                        } else if (anno.attr == "group") {
                            group = v
                        }
                    } else if (p.value.isAnnotationPresent(Payload::class.java)) {
                        if (data == null) {
                            val v = args[p.index]
                            if (v is Serializable) {
                                data = v
                            } else {
                                println("not serializable")
                            }
                        }
                    } else if (p.value.isAnnotationPresent(SendFile::class.java)) {
                        isTransferFile = true
                        val v = args[p.index]
                        targetFile = when (v) {
                            is File -> {
                                v
                            }
                            is String -> {
                                File(v)
                            }
                            else -> {
                                null
                            }
                        }
                    } else {
                        if (params == null) {
                            params = HashMap()
                        }
                        params[p.value.name] = args[p.index]
                    }
                }

                if (isTransferFile) {

                    targetFile?.let {
                        messageBody.payload = targetFile
                        return if (id != -1) {
                            FileTransfer.send(id, 0, messageBody)
                        } else if (group != -1) {
                            FileTransfer.send(group, 1, messageBody)
                        } else {
                            0
                        }
                    }

                    return 0
                } else {
                    if (data == null) {
                        data = params
                    }

                    messageBody.payload = data
                    messageBody.isSync = it.isSync
                    if (id == -1 && group == -1) {
                        id = 0
                    }

                    if (id != -1) {
                        return Pools.sendToId(id, messageBody)
                    } else {
                        Pools.sendToGroup(group, messageBody)
                    }
                }


            } else {
                messageBody.isSync = it.isSync
                return Pools.sendToId(0, messageBody)
            }

        }

        return null
    }
}

//class task: Callable<Any> {
//    val a = Object()
//    var result = 0
//    override fun call(): Any {
//        synchronized(a) {
//            a.wait(5000)
//        }
//
//        return result
//    }
//
//    fun put(v: Int) {
//        synchronized(a) {
//            result = v
//            a.notify()
//        }
//    }
//}
//fun main() {
//    val es = Executors.newFixedThreadPool(1)
//    val t = task()
//
//    thread {
//        Thread.sleep(2000)
//        t.put(2)
//    }
//    val future = es.submit(t)
//    println(future.get())
//
//    es.shutdown()
//}