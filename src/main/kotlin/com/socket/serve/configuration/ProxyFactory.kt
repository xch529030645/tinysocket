package com.socket.serve.configuration

import org.springframework.beans.factory.FactoryBean

class ProxyFactory<T>: FactoryBean<T> {
    var ctrl = ""
    var interfaceClass: Class<T>? = null

    override fun isSingleton(): Boolean {
        return true
    }

    override fun getObject(): T? {
        return ProxySender(ctrl).bind(interfaceClass!!) as T
    }

    override fun getObjectType(): Class<*>? {
        return interfaceClass
    }
}