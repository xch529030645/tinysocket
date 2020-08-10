package com.socket.serve.mgr

object ID {

    lateinit var get: () -> Map<String, IDS>

    fun single(name: String): Int {
        with(get()) {
            this[name]?.let {
                return if (it.set.isNotEmpty()) {
                    it.set.first()
                } else {
                    -1
                }
            }
            return -1
        }
    }

    fun random(name: String): Int {
        with(get()) {
            this[name]?.let {
                return if (it.set.isNotEmpty()) {
                    it.set.random()
                } else {
                    -1
                }
            }
            return -1
        }
    }

    fun group(name: String): Int {
        with(get()) {
            this[name]?.let {
                return it.group
            }
            return -1
        }
    }

    fun count(name: String): Int {
        with(get()) {
            return this[name]?.set?.size ?: 0
        }
    }
}