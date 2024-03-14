package com.tangqiu.cloud.ktor.discovery

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.util.*

class DiscoveryClientFeature private constructor(private val config: Configuration) {

    fun <T : DiscoveryClient> service(sf: DiscoveryClient.Factory<T>): T = config.services[sf.key]

    class Configuration(private val app: Application) {
        internal val services = Attributes(true) //如果为true创建一个ConcurrentHashMap 若为false创建HashMap

        fun <C : DiscoveryClient> register(cf: DiscoveryClient.Factory<C>) {
            services.put(cf.key, cf.create(app))
        }

        fun <C : DiscoveryClient> unregister(cf: DiscoveryClient.Factory<C>) {
            services.remove(cf.key)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, DiscoveryClientFeature> {
        override val key = AttributeKey<DiscoveryClientFeature>("DiscoveryClientKey")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): DiscoveryClientFeature =
            DiscoveryClientFeature(Configuration(pipeline).apply(configure))
    }
}

internal val ApplicationCallKey = AttributeKey<ApplicationCall>("call")

fun <T : DiscoveryClient> Application.discover(sf: DiscoveryClient.Factory<T>): T =
    feature(DiscoveryClientFeature).service(sf)

fun <T : DiscoveryClient> ApplicationCall.discover(sf: DiscoveryClient.Factory<T>): T =
    application.discover(sf)

fun HttpRequestBuilder.pipe(call: ApplicationCall) {
    setAttributes {
        put(ApplicationCallKey, call)
    }
}

val HttpRequestBuilder.pipeCall: ApplicationCall? get() = attributes.getOrNull(ApplicationCallKey)
