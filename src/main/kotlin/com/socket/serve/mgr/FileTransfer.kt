package com.socket.serve.mgr

import com.socket.serve.enum.EventType
import com.socket.serve.model.MessageBody
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class FileTransferReceiver(val fid: Int) {
    val filename = UUID.randomUUID().toString()
    private val fos: FileOutputStream
    private val filepath: String

    init {
        val file = File("tmps/$filename").absoluteFile
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        filepath = file.absolutePath
        fos = FileOutputStream(file)
    }
    fun receive(buf: ByteBuf): FileTransferReceiver {
        val t = buf.writerIndex()
        val c = buf.readerIndex()
        val left = t - c
        if (left > 0) {
            val bytes = ByteArray(left)
            buf.readBytes(bytes)
            fos.write(bytes)
        }
        return this
    }

    fun close() {
        fos.close()
    }

    fun remove() {
        try {
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val file = File(filepath)
        if (file.exists()) {
            file.delete()
        }
    }

    fun transferTo(out: File) {
        File(filepath).renameTo(out)
    }

    fun fileExist(): Boolean {
        val file = File(filepath)
        return if (file.exists()) {
            val fis = FileInputStream(file)
            if (fis.available() == 0 ) {
                fis.close()
                file.delete()
                false
            } else {
                true
            }
        } else {
            false
        }
    }
}

object FileTransfer {
    private val lock = Object()
    private val receivers = HashMap<Int, FileTransferReceiver>()
    private val es = Executors.newFixedThreadPool(2)
    private var fid = 0

    private val pool = PooledByteBufAllocator()
    private val cacheLength = 65526

    fun find(fid: Int): FileTransferReceiver? {
        return receivers[fid]
    }

    fun receive(buf: ByteBuf) {
        val fid = buf.readInt()
        val createFlag = buf.readByte().toInt()
        val transFlag = buf.readByte().toInt()
        synchronized(lock) {
            if (createFlag == 0) { //创建文件
                println("receive file $fid")
                receivers.remove(fid)?.remove()
                receivers[fid] = FileTransferReceiver(fid).receive(buf)
            } else {
                if (transFlag == 0) {//传输
                    receivers[fid]?.receive(buf)
                } else if (transFlag == 1) {//完成
                    receivers[fid]?.receive(buf)
                    receivers[fid]?.close()
                } else {
                    println("error transFlag $transFlag")
                }
            }
        }
    }

    fun send(tid: Int, type: Int, messageBody: MessageBody): Int {
        val payload = messageBody.payload
        if (payload is File) {
            fid++
            var createFlag = 0
            var transFlag = 0
            es.submit {
                val fis = FileInputStream(payload)
                while (true) {
                    val buf = pool.buffer()
                    buf.writeInt(EventType.Event_Transfer_File.code)
                    buf.writeInt(fid)

                    val buffer = ByteArray(cacheLength)
                    val len = fis.read(buffer)
                    if (len < cacheLength) {
                        transFlag = 1
                    }
                    if (createFlag == 0) {
                        buf.writeByte(createFlag)
                        createFlag = 1
                    } else {
                        buf.writeByte(createFlag)
                    }
                    buf.writeByte(transFlag)
                    buf.writeBytes(buffer, 0, len)

                    if (type == 0) {
                        Pools.sendToId(tid, buf)
                    } else {
                        Pools.sendToGroup(tid, buf)
                    }

                    if (transFlag != 0) {
                        break
                    }
                }

                messageBody.payload = mapOf("tinyFile" to fid)
                if (type == 0) {
                    Pools.sendToId(tid, messageBody)
                } else {
                    Pools.sendToGroup(tid, messageBody)
                }
            }
            return fid
        } else {
            return 0
        }
    }
}


