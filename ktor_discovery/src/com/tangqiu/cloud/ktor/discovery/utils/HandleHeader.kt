package com.tangqiu.cloud.ktor.discovery.utils

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*

fun handleHeader(call: ApplicationCall, headers: Headers) {
    headers.filter { i, _ -> i.startsWith("X-") }.forEach { k, v ->
        v.forEach {
            call.response.headers.append(k, it, false)
        }
    }
}