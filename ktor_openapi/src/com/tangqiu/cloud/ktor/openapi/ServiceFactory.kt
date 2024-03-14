package com.tangqiu.cloud.ktor.openapi

import io.ktor.application.*

interface ServiceFactory<T> {
    fun create(app: Application): T
}