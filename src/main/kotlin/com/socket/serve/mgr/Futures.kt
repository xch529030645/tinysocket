package com.socket.serve.mgr

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class SyncTask(val ctrl: String, val event: String): Callable<Any> {
    var future: Future<Any>? = null
    private val lock = Object()
    var value: Any? = null
    override fun call(): Any? {
        synchronized(lock) {
            lock.wait(2000)
            return value
        }
    }
    fun put(v: Any?) {
        synchronized(lock) {
            value = v
            lock.notify()
        }
    }
}
object Futures {
    val es = Executors.newFixedThreadPool(8)
    fun future(ctrl: String, event: String): SyncTask {
        val task = SyncTask(ctrl, event)
        task.future = es.submit(task)
        return task
    }
}