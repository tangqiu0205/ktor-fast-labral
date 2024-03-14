package com.tangqiu.cloud.ktor.acl.model

import io.ktor.http.*

interface Access {
    val pattern: String
    val methods: Set<String>
}

fun Access.verify(path: String, method: HttpMethod): Boolean =
    path.matches(Regex(pattern)) && (methods.contains("*") || method.value in methods.map { it.toUpperCase() })