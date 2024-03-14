package com.tangqiu.cloud.ktor.cache.client

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tangqiu.cloud.ktor.cache.entity.CacheHashMap
import com.tangqiu.cloud.ktor.cache.entity.CacheHashMapImpl
import com.tangqiu.cloud.ktor.cache.exception.CacheException
import com.tangqiu.cloud.ktor.cache.exception.cacheTry
import kotlinx.coroutines.future.asDeferred
import org.redisson.api.RedissonClient
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.StringCodec
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

interface CacheService {
    fun <T> register(converter: Converter<T>)

    @Throws(CacheException::class)
    suspend fun keys(key: String): List<String>?

    @Throws(CacheException::class)
    suspend fun <T : Any> set(key: String, value: T)

    @Throws(CacheException::class)
    suspend fun <T : Any> get(key: String, clazz: Class<T>): T?

    @Throws(CacheException::class)
    suspend fun <T : Any> getList(key: String, clazz: Class<T>): List<T>?

    @Throws(CacheException::class)
    suspend fun delete(key: String)

    @Throws(CacheException::class)
    suspend fun <T : Any> setWithExpire(
        key: String,
        value: T,
        expire: Long,
        expireTimeUnit: TimeUnit = TimeUnit.SECONDS
    )

    @Throws(CacheException::class)
    suspend fun expire(key: String, expire: Long, expireTimeUnit: TimeUnit = TimeUnit.SECONDS): Boolean

    @Throws(CacheException::class)
    fun <T : Any> hash(key: String, codec: BaseCodec? = null): CacheHashMap<T>

    fun getLock(key: String): Lock

    @Throws(CacheException::class)
    suspend fun removeIfExpire(key: String)

    @Throws(CacheException::class)
    suspend fun incr(key: String): Long

    @Throws(CacheException::class)
    suspend fun decr(key: String): Long

    interface Converter<T> {
        fun encode(t: Any): OutputStream
        fun decode(stream: InputStream): T?
    }
}

/**
 * CacheService实现
 */
class CacheServiceImpl(
    private val redissonClient: RedissonClient
) : CacheService {
    internal val cache get() = redissonClient

    private val mConverterMap = ConcurrentHashMap<Class<*>, CacheService.Converter<*>>()

    private val mapper = jacksonObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        registerModule(JavaTimeModule())
    }

    private val codec = StringCodec()

    override fun <T> register(converter: CacheService.Converter<T>) {
        mConverterMap[converter::class.java] = converter
    }

    /**
     * 获取所有key
     */
    override suspend fun keys(key: String): List<String>? {
        return cache.keys.getKeysByPattern(key)?.toList()
    }

    /**
     * 设置缓存
     */
    override suspend fun <T : Any> set(key: String, value: T) {
        cacheTry(key, value) {
            when {
                value::class.java.isPrimitive || value::class.java == String::class.java -> {
                    cache.getBucket<T>(key, codec).setAsync(value).asDeferred().await()
                }
                else -> {
                    val encode = mapper.writeValueAsString(value)
                    cache.getBucket<String>(key, codec).setAsync(encode).asDeferred().await()
                }
            }
        }
    }

    /**
     * 获取缓存
     */
    override suspend fun <T : Any> get(key: String, clazz: Class<T>): T? {
        return cacheTry(key) {
            when {
                clazz.isPrimitive || clazz == String::class.java -> cache.getBucket<T>(key, codec).get()
                else -> {
                    val string = cache.getBucket<String>(key, codec).get()
                    string?.let {
                        mapper.readValue(string, clazz)
                    }
                }
            }
        }
    }

    override suspend fun <T : Any> getList(key: String, clazz: Class<T>): List<T>? {
        return cacheTry(key) {
            val string = cache.getBucket<String>(key, codec).get()
            string?.let {
                val listType = jacksonObjectMapper().typeFactory.constructCollectionType(ArrayList::class.java, clazz)
                mapper.readValue(string, listType)
            }
        }
    }

    /**
     * 删除一个缓存
     */
    override suspend fun delete(key: String) {
        cacheTry(key) {
            cache.getBucket<String>(key).deleteAsync().asDeferred().await()
        }
    }

    /**
     * 计数加一
     */
    override suspend fun incr(key: String): Long {
        return cacheTry(key) {
            cache.getAtomicLong(key).incrementAndGet()
        }
    }

    /**
     * 计数减一
     */
    override suspend fun decr(key: String): Long {
        return cacheTry(key) {
            cache.getAtomicLong(key).decrementAndGet()
        }
    }

    /**
     * 写入缓存并设置时效时间
     */
    override suspend fun <T : Any> setWithExpire(
        key: String,
        value: T,
        expire: Long,
        expireTimeUnit: TimeUnit
    ) {
        cacheTry(key, value) {
            when {
                value::class.java.isPrimitive || value::class.java == String::class.java -> {
                    cache.getBucket<T>(key, codec).setAsync(value, expire, expireTimeUnit).asDeferred().await()
                }
                else -> {
                    val encode = mapper.writeValueAsString(value)
                    cache.getBucket<String>(key, codec).setAsync(encode, expire, expireTimeUnit).asDeferred().await()
                }
            }
        }
    }

    /**
     * 设置缓存时间
     */
    override suspend fun expire(key: String, expire: Long, expireTimeUnit: TimeUnit): Boolean =
        cacheTry(key, expire to expireTimeUnit) {
            cache.getBucket<String>(key, codec).expireAsync(expire, expireTimeUnit).asDeferred().await()
        }

    /**
     * 如果缓存过期则删除
     */
    override suspend fun removeIfExpire(key: String) {
        val ttl = cache.getBucket<String>(key).remainTimeToLiveAsync().asDeferred().await()
        //-1: 未设置过期时间
        if (ttl == -1L) {
            delete(key)
        }
    }

    override fun <T : Any> hash(key: String, codec: BaseCodec?): CacheHashMap<T> {
        return CacheHashMapImpl(key, codec, this)
    }

    /**
     * 获取锁
     */
    override fun getLock(key: String): Lock {
        return cache.getLock(key)
    }

    internal fun setConverters(map: Map<Class<*>, CacheService.Converter<*>>) {
        mConverterMap.putAll(map)
    }
}

/**
 * 获取缓存
 */
suspend inline fun <reified T : Any> CacheService.getValue(key: String): T? {
    return this.get(key, T::class.java)
}

/**
 * 获取缓存List
 */
suspend inline fun <reified T : Any> CacheService.getValueList(key: String): List<T>? {
    return this.getList(key, T::class.java)
}

/**
 * 设置缓存
 */
suspend inline fun <reified T : Any> CacheService.setValue(key: String, value: T) {
    return this.set(key, value)
}
