package com.socket.serve.configuration

import com.socket.serve.Socket
import com.socket.serve.annotations.EnableTinyClient
import com.socket.serve.annotations.EnableTinyServer
import com.socket.serve.annotations.SocketController
import com.socket.serve.annotations.SocketSender
import com.socket.serve.mgr.Controllers
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.*
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternResolver
import java.io.File
import kotlin.concurrent.thread


@Configuration
class SocketConfiguration: ApplicationContextAware, BeanDefinitionRegistryPostProcessor {
    lateinit var beanFactory: DefaultListableBeanFactory
    lateinit var beanDefinitionRegistry: BeanDefinitionRegistry
    lateinit var context: ApplicationContext

    override fun setApplicationContext(context: ApplicationContext) {
        this.context = context
        beanFactory = context.autowireCapableBeanFactory as DefaultListableBeanFactory
//        val ctrls = beanFactory.getBeansWithAnnotation(SocketController::class.java)
//        for ((k,v) in ctrls) {
//            addCtrl(v)
//        }
        runService()
    }

    override fun postProcessBeanDefinitionRegistry(p0: BeanDefinitionRegistry) {
        beanDefinitionRegistry = p0

        val backPackage = getBasePackage()
        val resolver = PathMatchingResourcePatternResolver(Thread.currentThread().contextClassLoader)
        val searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + backPackage.replace(".", "/") + "/**/*.class"
        val resources = resolver.getResources(searchPath)
        for (resource in resources) {
            val path = resource.file.absolutePath.replace(File.separator, ".").replace(".class", "")
            val clzName = path.substring(path.indexOf(backPackage))
            val clz = Thread.currentThread().contextClassLoader.loadClass(clzName) ?: continue
            process(beanDefinitionRegistry, clz)
        }
    }

    private fun process(registry: BeanDefinitionRegistry, clz: Class<*>) {
        if (clz.isAnnotationPresent(SocketSender::class.java)) {
            val tinyAnno = clz.getAnnotation(SocketSender::class.java)
            val clzName = clz.simpleName
            val name = tinyAnno.name

            val builder: BeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clz)
            val definition = builder.rawBeanDefinition as GenericBeanDefinition
            definition.propertyValues.add("interfaceClass", definition.beanClassName)
            definition.propertyValues.add("ctrl", name)
            definition.setBeanClass(ProxyFactory::class.java)
            definition.autowireMode = GenericBeanDefinition.AUTOWIRE_BY_TYPE
            registry.registerBeanDefinition(clzName, definition)
        } else if (clz.isAnnotationPresent(SocketController::class.java)) {
//            val tinyAnno = clz.getAnnotation(SocketController::class.java)
//            val clzName = clz.simpleName
//            val name = tinyAnno.name
//
//            val builder: BeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clz)
//            val definition = builder.rawBeanDefinition as GenericBeanDefinition
//            definition.propertyValues.add("interfaceClass", definition.beanClassName)
//            definition.propertyValues.add("name", name)
//            definition.setBeanClass(SocketFactory::class.java)
//            definition.autowireMode = GenericBeanDefinition.AUTOWIRE_BY_TYPE
//            registry.registerBeanDefinition(clzName, definition)
        }
    }

    private fun addCtrl(ctrl: Any) {
        val clz = ctrl::class.java
        val annotation = clz.getAnnotation(SocketController::class.java)
        Controllers.add(annotation.name, clz, ctrl)
    }


    override fun postProcessBeanFactory(p0: ConfigurableListableBeanFactory) {
//        val ctrls = beanFactory.getBeansWithAnnotation(SocketController::class.java)
//        for ((k,v) in ctrls) {
//            addCtrl(v)
//        }
    }


    private fun getBasePackage(): String {
        val stackTrace = RuntimeException().stackTrace
        for (stackTraceElement in stackTrace) {
            if ("main" == stackTraceElement.methodName) {
                var name = stackTraceElement.className
                val clz = Thread.currentThread().contextClassLoader.loadClass(name)
                clz.isAnnotationPresent(EnableTinyServer::class.java)
                name = name.substring(0, name.lastIndexOf("."))
                return name
            }
        }
        return ""
    }

    fun runService() {
        val a = beanFactory.getBeansWithAnnotation(EnableTinyServer::class.java)
        val b = beanFactory.getBeansWithAnnotation(EnableTinyClient::class.java)
        if (a.isNotEmpty()) {
            with(context.environment) {
                val port = getProperty("tiny.server.port")?.toInt() ?: return
                val path = getProperty("tiny.server.path") ?: return
                thread {
                    Socket.start(path, port)
                }
            }
        } else if (b.isNotEmpty()) {
            with(context.environment) {
                val url = getProperty("tiny.client.url") ?: return
                val port = getProperty("tiny.client.port")?.toInt() ?: return
                val path = getProperty("tiny.client.path") ?: return
                val id = getProperty("tiny.client.id")?.toInt() ?: return
                val group = getProperty("tiny.client.group")?.toInt() ?: return
                val name = getProperty("tiny.client.name") ?: ""

                thread {
                    Socket.client("$url:$port", path, id, group, name)
                }
            }
        }
    }
}