package com.socket.serve.file

import com.socket.serve.mgr.FileTransferReceiver
import java.io.File

class TinyFile(private val receiver: FileTransferReceiver) {
    val originalName = receiver.filename
    val what = receiver.fid

    fun check(): Boolean {
        return receiver.fileExist()
    }

    fun transferTo(out: String) {
        val f = File(out)
        if (!f.parentFile.exists()) {
            f.parentFile.mkdirs()
        }
        receiver.transferTo(f)
    }
}