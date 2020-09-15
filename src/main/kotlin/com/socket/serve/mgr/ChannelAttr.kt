package com.socket.serve.mgr

import io.netty.util.AttributeKey

object ChannelAttr {
    val idAttr = AttributeKey.valueOf<Int>("id")
    val groupAttr = AttributeKey.valueOf<Int>("group")
    val nameAttr = AttributeKey.valueOf<String>("name")
    val syncAttr = AttributeKey.valueOf<ArrayList<SyncTask>>("sync")
}