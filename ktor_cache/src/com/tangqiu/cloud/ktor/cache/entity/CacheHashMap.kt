package com.tangqiu.cloud.ktor.cache.entity

import com.tangqiu.cloud.ktor.cache.client.CacheServiceImpl
import com.tangqiu.cloud.ktor.cache.exception.CacheException
import com.tangqiu.cloud.ktor.cache.exception.cacheTry
import kotlinx.coroutines.future.asDeferred
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.StringCodec

/**
 * 自定义CacheMap
 */
interface CacheHashMap<T : Any> {
    @Throws(CacheException::class)
    suspend fun count(): Int

    @Throws(CacheException::class)
    suspend fun containsKey(key: String): Boolean

    @Throws(CacheException::class)
    suspend fun get(key: String): T?

    @Throws(CacheException::class)
    suspend fun getAll(keys: Set<String>): Map<String, T?>

    @Throws(CacheException::class)
    suspend fun readAllValuesAsync(): List<T>?

    @Throws(CacheException::class)
    suspend fun readAllEntrySetAsync(): Set<Map.Entry<String, T>>?

    @Throws(CacheException::class)
    suspend fun readAllMapAsync(): Map<String, T>?

    @Throws(CacheException::class)
    suspend fun hIncrBy(k: String, value: Int): T?

    @Throws(CacheException::class)
    suspend fun isEmpty(): Boolean

    @Throws(CacheException::class)
    suspend fun keys(): Set<String>

    @Throws(CacheException::class)
    suspend fun clear()

    @Throws(CacheException::class)
    suspend fun put(key: String, value: T): T?

    @Throws(CacheException::class)
    suspend fun putAll(maps: Map<String, T>)

    @Throws(CacheException::class)
    suspend fun putAll(vararg maps: Pair<String, T>)

    @Throws(CacheException::class)
    suspend fun remove(key: String)

    @Throws(CacheException::class)
    suspend fun removeAll(vararg keys: String)
}

internal class CacheHashMapImpl<T : Any>(
    private val key: String,
    private val codec: BaseCodec? = null,
    private val service: CacheServiceImpl
) : CacheHashMap<T> {
    private val cache
        get() = if (codec != null) service.cache.getMap<String, T>(
            key,
            StringCodec()
        ) else service.cache.getMap<String, T>(key)

    //返回Map大小
    override suspend fun count(): Int = cacheTry(key) {
        cache.sizeAsync().asDeferred().await()
    }
    //返回是否包含此key 包含返回true  不包含返回false
    override suspend fun containsKey(key: String): Boolean = cacheTry(hashedKey(key)) {
        cache.containsKeyAsync(key).asDeferred().await()
    }
    //通过key 获取Value值
    override suspend fun get(key: String): T? = cacheTry(hashedKey(key)) {
        cache.getAsync(key).asDeferred().await()
    }
    //获取全部key的值
    override suspend fun getAll(keys: Set<String>): Map<String, T?> = cacheTry(key) {
        cache.getAllAsync(keys).asDeferred().await()
    }

    //判空
    override suspend fun isEmpty(): Boolean {
        return count() <= 0
    }

    //获取所有key
    override suspend fun keys(): Set<String> = cacheTry(key) {
        cache.readAllKeySetAsync().asDeferred().await()
    }

    //获取所有值List<>
    override suspend fun readAllValuesAsync(): List<T>? = cacheTry(key) {
        cache.readAllValuesAsync().asDeferred().await()?.toList()
    }

    //获取所有值Set<Map>
    override suspend fun readAllEntrySetAsync(): Set<Map.Entry<String, T>>? = cacheTry(key) {
        cache.readAllEntrySetAsync().asDeferred().await()
    }

    //获取所有值Map
    override suspend fun readAllMapAsync(): Map<String, T>? = cacheTry(key) {
        cache.readAllMapAsync().asDeferred().await()
    }

    //hash增值
    override suspend fun hIncrBy(k: String, value: Int): T? = cacheTry(key) {
        cache.addAndGetAsync(k, value).asDeferred().await()
    }

    //异步删除对象
    override suspend fun clear() {
        cacheTry(key) {
            cache.deleteAsync().asDeferred().await()
        }
    }

    //设置成功并返回value值
    override suspend fun put(key: String, value: T): T = cacheTry(hashedKey(key), value) {
        cache.putAsync(key, value).asDeferred().await()
        value
    }
    //批量设置缓存
    override suspend fun putAll(maps: Map<String, T>) {
        cacheTry(key) {
            cache.putAllAsync(maps).asDeferred().await()
        }
    }
    //批量设置缓存
    override suspend fun putAll(vararg maps: Pair<String, T>) {
        return putAll(maps.toMap())
    }
    //删除缓存
    override suspend fun remove(key: String) {
        cacheTry(hashedKey(key)) {
            cache.fastRemoveAsync(key).asDeferred().await()
        }
    }
    //删除全部缓存
    override suspend fun removeAll(vararg keys: String) {
        cacheTry(key) {
            cache.fastRemoveAsync(*keys).asDeferred().await()
        }
    }

    private fun hashedKey(key: String): String = "key: ${this.key}, hashKey: $key"

}