package com.tangqiu.cloud.ktor.discovery

import com.tangqiu.cloud.ktor.openapi.exception.ApiException
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.Logger
import java.lang.reflect.Type
import java.util.*

interface ErrorResolver {
    fun resolve(error: Any): Pair<Int, String>
}

data class ApiResult<T>(
    val headers: Headers,
    val statusCode: HttpStatusCode,
    val `data`: T?,
    var cause: Throwable? = null
) {
    companion object {
        val resolvers = mutableMapOf<Type, Any.() -> Pair<Int, String>?>()

        internal var apiExceptionParser: suspend ApiResult<*>.() -> Throwable? = {
            cause
        }

        private fun <E : Enum<E>> resolve(error: E): Pair<Int, String>? = resolvers[error::class.java]?.invoke(error)
    }

    private suspend fun <E : Enum<E>> checkErrorPassThrough(
        logger: Logger,
        error: E?,
        message: String? = null,
        verify: ApiResult<T>.() -> Boolean = { statusCode == HttpStatusCode.OK || statusCode == HttpStatusCode.Found },
        errorResolver: (ApiResult<T>.() -> E?)? = null
    ): ApiResult<T> {
        val cause = apiExceptionParser.invoke(this) ?: this.cause
        val hasError = !verify.invoke(this)
        if (!hasError) return this
        val e = error ?: errorResolver?.invoke(this)
        val pair = e?.run(::resolve) ?: (HttpStatusCode.InternalServerError.value to (cause?.message ?: "未知错误"))

        headers["X-CLOUD-ERROR-BODY"]?.let {
            String(Base64.getDecoder().decode(it))
        }?.let {
            logger.error("business:-->$it")
        }
        logger.error("$message:-->${cause?.message}")
        cause?.stackTraceToString()?.run(logger::error)

        throw ApiException(HttpStatusCode(pair.first, ""), pair.second)
    }

    suspend fun <E : Enum<E>> checkError(
        logger: Logger,
        error: E? = null,
        message: String? = null,
        verify: ApiResult<T>.() -> Boolean = { statusCode == HttpStatusCode.OK || statusCode == HttpStatusCode.Found },
        errorResolver: (ApiResult<T>.() -> E?)? = null
    ): ApiResult<T> = checkErrorPassThrough(logger, error, message, verify, errorResolver)
}

suspend inline fun <reified T> HttpResponse.toApiResult(hasData: Boolean = true): ApiResult<T> {
    val reallyHasData = hasData && content.availableForRead > 0
    return ApiResult(headers, status, if (reallyHasData) receive<T>() else null)
}

inline fun <reified T> T.toApiResult(): ApiResult<T> =
    ApiResult(Headers.Empty, HttpStatusCode.OK, this)

fun <T> ResponseException.toApiResult() = ApiResult<T>(response.headers, response.status, null, this)

class BusinessException : RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )
}

val String.businessException: BusinessException get() = BusinessException(this)
