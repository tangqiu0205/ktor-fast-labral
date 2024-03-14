package com.tangqiu.cloud.ktor.cache

import com.tangqiu.cloud.ktor.cache.client.CacheService
import com.tangqiu.cloud.ktor.cache.client.CacheServiceImpl
import io.ktor.application.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec
import org.redisson.config.Config
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

/**
 * 缓存功能
 */
class CacheFeature private constructor(config: Configuration) : Closeable {

    private val cacheConf: Config = Config()

    init {
        cacheConf.threads = config.threads
        cacheConf.nettyThreads = config.nettyThreads
        cacheConf.codec = config.codec

        val singleConfig = cacheConf.useSingleServer()
        singleConfig.idleConnectionTimeout = config.idleConnectionTimeout
        singleConfig.connectTimeout = config.connectTimeout
        singleConfig.timeout = config.timeout
        singleConfig.retryAttempts = config.retryAttempts
        singleConfig.retryInterval = config.retryInterval
        singleConfig.password = config.password
        singleConfig.subscriptionsPerConnection = config.subscriptionsPerConnection
        singleConfig.clientName = config.clientName
        singleConfig.address = config.address
        singleConfig.subscriptionConnectionMinimumIdleSize = config.subscriptionConnectionMinimumIdleSize
        singleConfig.subscriptionConnectionPoolSize = config.subscriptionConnectionPoolSize
        singleConfig.connectionMinimumIdleSize = config.connectionMinimumIdleSize
        singleConfig.connectionPoolSize = config.connectionPoolSize
        singleConfig.database = config.database
        singleConfig.dnsMonitoringInterval = config.dnsMonitoringInterval
    }

    data class Configuration(
        var threads: Int = 16, //默认值: 当前处理核数量 * 2
        var nettyThreads: Int = 32, //默认值: 当前处理核数量 * 2
        var codec: Codec = org.redisson.codec.MarshallingCodec(), //编解码方式

        var idleConnectionTimeout: Int = 10000, //空闲连接超时
        var connectTimeout: Int = 10000, //连接超时
        var timeout: Int = 3000, //超时
        var retryAttempts: Int = 3, //重试次数
        var retryInterval: Int = 1500, //重试间隔
        var password: String? = null, //密码
        var subscriptionsPerConnection: Int = 5, //每个连接的订阅数
        var clientName: String? = null, //客户端名称
        var address: String = "redis://127.0.0.1:6379", //缓存环境连接地址
        var subscriptionConnectionMinimumIdleSize: Int = 1, //最小订阅连接空闲大小
        var subscriptionConnectionPoolSize: Int = 50, //订阅连接池大小
        var connectionMinimumIdleSize: Int = 32, //最小连接空闲大小
        var connectionPoolSize: Int = 64, //连接池大小
        var database: Int = 0, //用于Redis连接的数据库索引
        var dnsMonitoringInterval: Long = 5000, //dns监视间隔
    ) {
        internal val converterMap = ConcurrentHashMap<Class<*>, CacheService.Converter<*>>()

        fun convert(vararg converter: CacheService.Converter<*>) {
            converter.forEach {
                converterMap[it::class.java] = it
            }
        }
    }

    //  private val cacheConf: Config = Config.fromYAML(CacheFeature::class.java.classLoader.getResource("redisson-config.yml"))
    private var redisson: RedissonClient = Redisson.create(cacheConf) //创建连接
    val service = CacheServiceImpl(redisson).apply { setConverters(config.converterMap) }


    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CacheFeature> {
        override val key: AttributeKey<CacheFeature> = AttributeKey("CacheFeatureKey")
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): CacheFeature {
            val config = Configuration().apply(configure)
            return CacheFeature(config)
        }
    }

    override fun close() {
        redisson.shutdown()
    }
}

val Application.cache get() = feature(CacheFeature).service

fun ckey(vararg sections: String, sep: String = ":") = sections.joinToString(sep) { it.trim() }