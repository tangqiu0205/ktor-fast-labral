package com.tangqiu.cloud.ktor.openapi.exception

import io.ktor.features.*
import io.ktor.http.*
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.TimeoutException

/**
 * 异常状态码
 */
open class ApiException : RuntimeException {
    private val _code: HttpStatusCode?

    constructor(message: String? = null, cause: Throwable? = null) : super(message ?: cause?.message, cause) {
        _code = null
    }

    constructor(code: HttpStatusCode, message: String? = null, cause: Throwable? = null) : super(
        message ?: cause?.message, cause
    ) {
        _code = code
    }

    val code get() = _code ?: when (cause) {
        is BadRequestException -> HttpStatusCode.BadRequest
        is NotFoundException -> HttpStatusCode.NotFound
        is UnsupportedMediaTypeException -> HttpStatusCode.UnsupportedMediaType
        is TimeoutException, is TimeoutCancellationException -> HttpStatusCode.GatewayTimeout
        else -> HttpStatusCode.InternalServerError
    }
}