package com.tangqiu.cloud.ktor.discovery

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.tangqiu.cloud.ktor.discovery.auth.header
import com.tangqiu.cloud.ktor.discovery.registry.EurekaDiscoveryRegistry
import com.tangqiu.cloud.ktor.discovery.registry.NacosDiscoveryRegistry
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.util.*
import java.io.Closeable
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 服务注册与发现 服务端
 */
class DiscoveryServerFeature(internal val config: Configuration) : Closeable {
    private val httpClientMap = ConcurrentHashMap<String, HttpClient>()

    private lateinit var registry: DiscoveryRegistry

    internal fun obtainHttpClient(
        vipAddress: String,
        host: String? = null,
        port: Int = 80,
        contentType: String? = "application/json"
    ): HttpClient =
        httpClientMap.getOrPut(vipAddress) {
            HttpClient {
                install(JsonFeature) {
                    serializer = JacksonSerializer {
                        enable(SerializationFeature.INDENT_OUTPUT)
                        registerModule(JavaTimeModule()) //java8 时间
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) //序列化时忽略未声明的属性
                    }
                }
                install(DiscoveryHttpClientFeature) {
                    this.registry = this@DiscoveryServerFeature.registry
                    this.vipAddress = vipAddress
                    this.host = host
                    this.port = port
                }
                //设置默认contentType post put patch需要 ”application/json“ 上传文件时不用设置
                defaultRequest {
                    if (contentType != null) {
                        header("Content-Type", contentType)
                    }
                }
                install(Auth) {
                    header(true)
                }
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
                config.clientConfig?.let { apply(it) }
            }
        }

    class Configuration {
        var properties: Properties = Properties().apply {
            put("type", "")
        }

        var type: String by properties

        private val registryMap = ConcurrentHashMap<String, DiscoveryRegistry.RegistryConfig>()

        internal val registries = mutableListOf<URL>()

        internal var clientConfig: (HttpClientConfig<*>.() -> Unit)? = null

        private val registryConfig: DiscoveryRegistry.RegistryConfig
            get() = registryMap.getOrPut(type) {
                when (type) {
                    DISCOVERY_REGISTRY_ERUREKA -> EurekaDiscoveryRegistry.EurekaConfig()
                    DISCOVERY_REGISTRY_NACOS -> NacosDiscoveryRegistry.NacosConfig()
                    else -> EurekaDiscoveryRegistry.EurekaConfig()
                }
            }

        fun registry(vararg urls: URL) {
            registries.addAll(urls)
            registryConfig.applyRegistries(*urls)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : DiscoveryRegistry.RegistryConfig> configRegistry(block: T.() -> Unit) {
            block.invoke(registryConfig as T)
        }

        fun props(): Properties {
            registryConfig.fillTo(properties)
            return properties
        }

        fun client(block: HttpClientConfig<*>.() -> Unit) {
            clientConfig = block
        }

        inline fun <reified E : Enum<E>> registerResolver(crossinline resolve: (E?) -> Pair<Int, String>?) {
            ApiResult.resolvers[E::class.java] = {
                resolve.invoke(this as? E)
            }
        }
    }


    val healthyNodes: List<ServerNode>
        get() {
            registry = when (config.type) {
                DISCOVERY_REGISTRY_ERUREKA -> EurekaDiscoveryRegistry
                DISCOVERY_REGISTRY_NACOS -> NacosDiscoveryRegistry
                else -> EurekaDiscoveryRegistry
            }
            return registry.healthyNodes
        }

    val instanceIndex: Int
        get() {
            registry = when (config.type) {
                DISCOVERY_REGISTRY_ERUREKA -> EurekaDiscoveryRegistry
                DISCOVERY_REGISTRY_NACOS -> NacosDiscoveryRegistry
                else -> EurekaDiscoveryRegistry
            }
            return registry.instanceIndex
        }

    fun start(): DiscoveryServerFeature {
        registry = when (config.type) {
            DISCOVERY_REGISTRY_ERUREKA -> EurekaDiscoveryRegistry
            DISCOVERY_REGISTRY_NACOS -> NacosDiscoveryRegistry
            else -> EurekaDiscoveryRegistry
        }
        registry.startUp(config.props())
        return this
    }

    fun shutDown(): DiscoveryServerFeature {
        registry = when (config.type) {
            DISCOVERY_REGISTRY_ERUREKA -> EurekaDiscoveryRegistry
            DISCOVERY_REGISTRY_NACOS -> NacosDiscoveryRegistry
            else -> EurekaDiscoveryRegistry
        }
        registry.shutDown()
        return this
    }


    //关闭服务
    override fun close() {
        registry.shutDown()
    }

    companion object Feature : ApplicationFeature<Application, Configuration, DiscoveryServerFeature> {
        override val key = AttributeKey<DiscoveryServerFeature>("DiscoveryServerKey")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): DiscoveryServerFeature {
            return DiscoveryServerFeature(Configuration().apply(configure)).start()
        }
    }
}

fun Application.discover(
    vipAddress: String,
    host: String? = null,
    port: Int = 80,
    contentType: String? = "application/json"
): HttpClient =
    feature(DiscoveryServerFeature).obtainHttpClient(vipAddress, host, port, contentType)

internal fun Map<*, *>.flatInto(output: Properties, key: String? = null) {
    entries.forEach { entry ->
        if (entry.value is Map<*, *>) {
            (entry.value as Map<*, *>).flatInto(output, entry.key as? String)
        } else if (entry.value != null) {
            output.putIfAbsent("${key?.let { "$it." } ?: ""}${entry.key}", entry.value)
        }
    }
}

