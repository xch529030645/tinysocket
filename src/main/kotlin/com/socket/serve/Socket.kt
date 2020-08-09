package com.socket.serve

import kotlin.concurrent.thread

object Socket {
    fun start(path: String, port: Int) {
        thread {
            CoreServer(path, port).start()
        }
    }

    fun client(ip: String, path: String, id: Int, group: Int, name: String) {
        thread {
            Client(ip, path, id, group, name).start()
        }
    }
}