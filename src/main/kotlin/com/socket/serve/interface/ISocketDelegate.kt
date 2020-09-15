package com.socket.serve.`interface`

import com.socket.serve.model.Machine

interface ISocketDelegate {
    fun onMachineEnter(machine: Machine)
    fun onMachineExit(id: Int)
}