package com.socket.serve.mgr

import com.socket.serve.`interface`.ISocketDelegate
import com.socket.serve.model.Machine
import com.socket.serve.model.MessageBody
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.ChannelMatcher
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.util.concurrent.GlobalEventExecutor
import java.lang.Exception

class IDS(val group: Int) {
    val set = HashSet<Int>()
}

object Pools {
    private val CHANNEL_GROUP: ChannelGroup = DefaultChannelGroup("ChannelGroups", GlobalEventExecutor.INSTANCE)

    private val nameLock = Object()
    private val nameMaps = HashMap<String, IDS>()
    private val onlineMachines = ArrayList<Machine>()

    val delegates = ArrayList<ISocketDelegate>()

    fun addDelegate(delegate: ISocketDelegate) {
        synchronized(nameLock) {
            delegates.add(delegate)
            if (onlineMachines.isNotEmpty()) {
                onlineMachines.forEach {
                    delegate.onMachineEnter(it)
                }
            }
        }
    }

    init {
        ID.get = {
            nameMaps
        }
    }

    fun add(machine: Machine, channel: Channel) {
        var attr = channel.attr(ChannelAttr.idAttr)
        attr.set(machine.machine_id)

        attr = channel.attr(ChannelAttr.groupAttr)
        attr.set(machine.machine_group)

        val nameAttr = channel.attr(ChannelAttr.nameAttr)
        nameAttr.set(machine.machine_name)

        val sync = channel.attr(ChannelAttr.syncAttr)
        sync.setIfAbsent(ArrayList())

        println("client enter id: ${machine.machine_id}, group: ${machine.machine_group}")

        synchronized(nameLock) {
            if (!nameMaps.containsKey(machine.machine_name)) {
                nameMaps[machine.machine_name] = IDS(machine.machine_group)
            }
            nameMaps[machine.machine_name]?.set?.add(machine.machine_id)
            onlineMachines.add(machine)

            if (delegates.isNotEmpty()) {
                delegates.forEach {
                    it.onMachineEnter(machine)
                }
            }
        }

        CHANNEL_GROUP.add(channel)
    }

    fun remove(channel: Channel) {
        CHANNEL_GROUP.remove(channel)
        val nameAttr = channel.attr(ChannelAttr.nameAttr)
        val name = nameAttr.get()
        val idAttr = channel.attr(ChannelAttr.idAttr)
        val id = idAttr.get()
        if (id != null) {
            synchronized(nameLock) {
                nameMaps[name]?.set?.remove(id)
            }

            if (delegates.isNotEmpty()) {
                delegates.forEach {
                    it.onMachineExit(id)
                }
            }
        }

    }


    fun sendToId(id: Int, data: MessageBody): Any? {
        CHANNEL_GROUP.find {
            it.attr(ChannelAttr.idAttr).get() == id
        }?.let {
            it.writeAndFlush(data.frame)
            if (data.isSync) {
                val task = Futures.future(data.ctrl, data.event)
                val syncList = it.attr(ChannelAttr.syncAttr)
                syncList.get().add(task)
                return task.future?.get()
            }
        }
        return null
    }

    fun sendToId(id: Int, buf: ByteBuf) {
        try {
            CHANNEL_GROUP.find {
                it.attr(ChannelAttr.idAttr).get() == id
            }?.let {
                it.writeAndFlush(BinaryWebSocketFrame(buf))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun sendToGroup(group: Int, data: MessageBody) {
        try {
            CHANNEL_GROUP.writeAndFlush(data.frame, ChannelMatcher {
                it.attr(ChannelAttr.groupAttr).get() == group
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun sendToGroup(group: Int, buf: ByteBuf) {
        try {
            CHANNEL_GROUP.writeAndFlush(BinaryWebSocketFrame(buf), ChannelMatcher {
                it.attr(ChannelAttr.groupAttr).get() == group
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun dispatchSync(channel: Channel, messageBody: MessageBody) {
        val sync = channel.attr(ChannelAttr.syncAttr)
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