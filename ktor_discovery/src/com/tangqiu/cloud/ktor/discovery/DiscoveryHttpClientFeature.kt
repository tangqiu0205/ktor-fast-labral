package com.tangqiu.cloud.ktor.discovery

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*

/**
 * 服务注册与发现 客户端功能
 */
internal class DiscoveryHttpClientFeature(private val config: Configuration) {

    //配置类
    class Configuration {
        lateinit var registry: DiscoveryRegistry
        var vipAddress: String = ""
        var host: String? = null
        var port: Int = 80
    }

    companion object Feature : HttpClientFeature<Configuration, DiscoveryHttpClientFeature> {
        override val key = AttributeKey<DiscoveryHttpClientFeature>("DiscoveryClientKey")

        override fun install(feature: DiscoveryHttpClientFeature, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                if (feature.config.host.isNullOrBlank().not()) {
                    context.url {
                        host = feature.config.host!!
                        port = feature.config.port
                    }
                } else {
                    val server = feature.config.run {
                        registry.selectNode(vipAddress)
                    }
                    server?.let {
                        context.url.host = it.host
                        context.url.port = it.port?: 80
                    }
                }
                proceed()
            }
        }

        override fun prepare(block: Configuration.() -> Unit): DiscoveryHttpClientFeature =
            DiscoveryHttpClientFeature(Configuration().apply(block))
    }
}
