package com.tangqiu.cloud.ktor.acl.model

import io.ktor.http.*

interface Role {
    val name: String
    val access: List<Access>
    val enabled: Boolean
}

fun Role.verify(path: String, method: HttpMethod): Boolean = enabled && access.any { it.verify(path, method) }