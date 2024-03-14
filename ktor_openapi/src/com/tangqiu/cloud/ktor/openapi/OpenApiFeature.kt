package com.tangqiu.cloud.ktor.openapi

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*

/**
 * 代码生成功能
 */
class OpenApiFeature private constructor(val config: Configuration) {

    @Suppress("UNCHECKED_CAST")
    fun install(): OpenApiFeature {
        config.controllers.allKeys.forEach { key ->
            val controller = config.controllers[key as AttributeKey<Controller<*>>]
            controller.install()
        }
        return this
    }

    class Configuration(internal val app: Application) {
        internal val controllers = Attributes(true) //如果为true创建一个ConcurrentHashMap 若为false创建HashMap
        var logger: Logger? = null

        fun <T, C : Controller<T>> Route.register(cf: Controller.Factory<T, C>, sf: ServiceFactory<T>) {
            controllers.put(cf.key, cf.create(sf.create(app), this))
        }

        fun <T, C : Controller<T>> Route.register(cf: Controller.Factory<T, C>, service: T) {
            controllers.put(cf.key, cf.create(service, this))
        }

        fun <T, C : Controller<T>> unregister(cf: Controller.Factory<T, C>) {
            controllers.remove(cf.key)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, OpenApiFeature> {
        override val key = AttributeKey<OpenApiFeature>("ControllerKey")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): OpenApiFeature =
            OpenApiFeature(Configuration(pipeline).apply(configure)).install()
    }
}
