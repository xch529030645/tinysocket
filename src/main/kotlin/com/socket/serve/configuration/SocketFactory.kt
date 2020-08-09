package com.socket.serve.configuration

import org.springframework.beans.factory.FactoryBean

class SocketFactory<T>: FactoryBean<T> {
    var interfaceClass: Class<T>? = null
    lateinit var name: String

    override fun isSingleton(): Boolean {
        return true
    }

    override fun getObject(): T? {
        val o = interfaceClass?.newInstance()
        return o
    }

    override fun getObjectType(): Class<*>? {
        return interfaceClass
    }
}