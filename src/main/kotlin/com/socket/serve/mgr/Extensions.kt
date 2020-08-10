package com.socket.serve.mgr

import io.netty.buffer.ByteBuf

fun ByteBuf.readString(): String {
    val len = readShort()
    val bytes = ByteArray(len.toInt())
    return String(bytes)
}