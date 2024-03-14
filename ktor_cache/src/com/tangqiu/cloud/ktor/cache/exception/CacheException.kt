package com.tangqiu.cloud.ktor.cache.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import org.redisson.client.*

/**
 * 缓存异常类
 */
class CacheException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

internal suspend fun <T> Any.cacheTry(key: String? = null, value: Any? = null, block: suspend () -> T): T {
    return try {
        block.invoke()
    } catch (t: Throwable) {
        throw CacheException(cacheExpMessage(t, key, value), t)
    }
}

/**
 * 异常消息
 */
private fun cacheExpMessage(t: Throwable, key: String? = null, value: Any? = null): String = when (t) {
    is JsonProcessingException -> "Error when encoding key: $key, value: $value"
    is JsonMappingException -> "Error when decoding key: $key, value: $value"
    is RedisNodeNotFoundException -> "Error when cache node cannot be found key: $key, value: $value"
    is RedisOutOfMemoryException -> "Error when cache out of memory key: $key, value: $value"
    is RedisTimeoutException -> "Cache command timeout key: $key, value: $value"
    is RedisAskException -> "Error when cache ask key: $key, value: $value"
    is RedisAuthRequiredException -> "Error when cache authentication key: $key, value: $value"
    is RedisClusterDownException -> "Error when cache cluster is down key: $key, value: $value"
    is RedisConnectionException -> "Error when cache connection failed key: $key, value: $value"
    is RedisRedirectException -> "Error when cache redirect failed key: $key, value: $value"
    is RedisLoadingException -> "Error when cache loading failed key: $key, value: $value"
    is RedisMovedException -> "Error when cache moved failed key: $key, value: $value"
    is RedisResponseTimeoutException -> "Error when cache responseTimeout failed key: $key, value: $value"
    is RedisTryAgainException -> "Error when cache TryAgain failed key: $key, value: $value"
    is WriteRedisConnectionException -> "Error when cache writeConnection failed key: $key, value: $value"
    else -> "${t.message} key: $key, value: $value"
}