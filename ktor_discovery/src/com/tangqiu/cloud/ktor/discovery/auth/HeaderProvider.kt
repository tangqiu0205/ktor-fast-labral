package com.tangqiu.cloud.ktor.discovery.auth

/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
import com.tangqiu.cloud.ktor.discovery.pipeCall
import io.ktor.client.features.auth.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.util.*

/**
 * Add [HeaderProvider] to client [Auth] providers.
 */
fun Auth.header(sendWithoutRequest: Boolean = false) {
    providers.add(HeaderProvider(sendWithoutRequest))
}

/**
 * Client basic authentication provider.
 */
open class HeaderProvider(
    override val sendWithoutRequest: Boolean = false
) : AuthProvider {
    override fun isApplicable(auth: HttpAuthHeader): Boolean {
        if (auth !is HttpAuthHeader.Parameterized) return false
        return true
    }

    override suspend fun addRequestHeaders(request: HttpRequestBuilder) {
        val call = request.pipeCall
        call?.request?.headers?.filter { key, _ ->
            !HttpHeaders.isUnsafe(key)
        }?.run {
            request.headers.appendAll(this)
        }
    }
}
