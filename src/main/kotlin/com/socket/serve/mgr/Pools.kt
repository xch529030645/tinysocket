package com.socket.serve.mgr

import com.socket.serve.model.MessageBody
import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.ChannelMatcher
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.AttributeKey
import io.netty.util.concurrent.GlobalEventExecutor

class IDS(val group: Int) {
    val set = HashSet<Int>()
}

object Pools {
    private val CHANNEL_GROUP: ChannelGroup = DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE)
    private val idAttr = AttributeKey.valueOf<Int>("id")
    private val groupAttr = AttributeKey.valueOf<Int>("group")
    private val nameAttr = AttributeKey.valueOf<String>("name")
    private val syncAttr = AttributeKey.valueOf<ArrayList<SyncTask>>("sync")

    private val nameLock = Object()
    private val nameMaps = HashMap<String, IDS>()

    init {
        ID.get = {
            nameMaps
        }
    }

    fun add(id: Int, group: Int, name: String, channel: Channel) {
        var attr = channel.attr(idAttr)
        attr.set(id)

        attr = channel.attr(groupAttr)
        attr.set(group)

        val nameAttr = channel.attr(nameAttr)
        nameAttr.set(name)

        val sync = channel.attr(syncAttr)
        sync.setIfAbsent(ArrayList())

        println("client enter id: $id, group: $group")

        synchronized(nameLock) {
            if (!nameMaps.containsKey(name)) {
                nameMaps[name] = IDS(group)
            }
            nameMaps[name]?.set?.add(id)
        }

        CHANNEL_GROUP.add(channel)
    }

    fun remove(channel: Channel) {
        CHANNEL_GROUP.remove(channel)
        val nameAttr = channel.attr(nameAttr)
        val name = nameAttr.get()
        val idAttr = channel.attr(idAttr)
        val id = idAttr.get()
        synchronized(nameLock) {
            nameMaps[name]?.set?.remove(id)
        }
    }


    fun sendToId(id: Int, data: MessageBody): Any? {
        CHANNEL_GROUP.find {
            it.attr(idAttr).get() == id
        }?.let {
            it.writeAndFlush(data.frame)
            if (data.isSync) {
                val task = Futures.future(data.ctrl, data.event)
                val syncList = it.attr(syncAttr)
                syncList.get().add(task)
                return task.future?.get()
            }
        }
        return null
    }

    fun sendToGroup(group: Int, data: MessageBody) {
        CHANNEL_GROUP.writeAndFlush(data, ChannelMatcher {
            it.attr(groupAttr).get() == group
        })
    }

    fun dispatchSync(channel: Channel, messageBody: MessageBody) {
        val sync = channel.attr(syncAttr)
        sync.get().removeIf {
            if (it.ctrl == messageBody.ctrl && it.event == messageBody.event) {
                it.put(messageBody.payload)
                true
            } else {
                false
            }
        }
    }
}