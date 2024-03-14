package com.tangqiu.cloud.ktor.discovery.serialization

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun <T : Any> encode(value: T?): String? =
    when {
        value == null -> null
        value::class.java == String::class.java -> value.toString()
        value::class.javaPrimitiveType?.isPrimitive ?: value::class.java.isPrimitive -> value.toString()
        else -> jacksonObjectMapper().writeValueAsString(value)
    }
