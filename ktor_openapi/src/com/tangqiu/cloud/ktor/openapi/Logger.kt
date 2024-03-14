package com.tangqiu.cloud.ktor.openapi

interface Logger {
    fun debug(message: String?)
    fun error(message: String?, e: Throwable?)
}