package com.tangqiu.cloud.ktor.discovery

import com.tangqiu.cloud.ktor.cache.cache
import com.tangqiu.cloud.ktor.cache.client.CacheService
import com.tangqiu.cloud.ktor.discovery.serialization.encode
import com.tangqiu.cloud.ktor.discovery.utils.DAYS
import com.tangqiu.cloud.ktor.discovery.utils.HOURS
import com.tangqiu.cloud.ktor.discovery.utils.MINUTES
import com.tangqiu.cloud.ktor.discovery.utils.SECONDS
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import java.util.concurrent.TimeUnit

typealias ChannelHandler = suspend (ByteReadChannel) -> Unit

abstract class DiscoveryClient {
    abstract val app: Application
    abstract val vipAddress: String
    abstract val host: String?
    abstract val port: Int
    protected val client: HttpClient by lazy {
        app.discover(vipAddress, host, port)
    }
    private val cache: CacheService get() = app.cache

    fun encodedPath(path: String, vararg fields: Pair<String, Any?>): String = fields.fold(path) { acc, pair ->
        acc.replace(Regex("\\{*?${pair.first}*?}"), encode(pair.second) ?: "")
    }

    fun formData(vararg data: Pair<String, Any?>): FormDataContent = FormDataContent(Parameters.build {
        data.forEach { encode(it.second)?.let { value -> set(it.first, value) } }
    })

    fun multipart(vararg parts: PartData): MultiPartFormDataContent = MultiPartFormDataContent(parts.toList())

    protected suspend fun <T : Any> getCache(key: String, clazz: Class<T>): T? = cache.get(key, clazz)
    protected suspend fun <T : Any> getCacheList(key: String, clazz: Class<T>): List<T>? = cache.getList(key, clazz)

    protected suspend fun <T : Any> setCache(key: String, value: T, expire: String? = null) {
        if (!expire.isNullOrBlank()) {
            val expirePair = toExpire(expire)
            cache.setWithExpire(key, value, expirePair.first, expirePair.second)
        } else {
            cache.set(key, value)
        }
    }

    private fun toExpire(expire: String): Pair<Long, TimeUnit> {
        val split = expire.trim().split(":")
        val time = split[0].toLong() ?: 0
        var timeUnit = TimeUnit.SECONDS
        when (split[1]) {
            SECONDS -> timeUnit = TimeUnit.SECONDS
            MINUTES -> timeUnit = TimeUnit.MINUTES
            HOURS -> timeUnit = TimeUnit.HOURS
            DAYS -> timeUnit = TimeUnit.DAYS
        }
        return time to timeUnit
    }

    interface Factory<C : DiscoveryClient> {
        val key: AttributeKey<C>
        val cacheKeys: Map<String, String>
        fun create(app: Application): C
    }
}
