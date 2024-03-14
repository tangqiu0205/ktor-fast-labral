package com.tangqiu.cloud.ktor.openapi.serialization

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * 解码 String->Entity
 */
fun <T> decode(value: String, clazz: Class<T>): T =
    when {
        String::class.java.isAssignableFrom(clazz) -> value as T
        Int::class.java.isAssignableFrom(clazz) -> value.toIntOrNull() as T
        Long::class.java.isAssignableFrom(clazz) -> value.toLongOrNull() as T
        Double::class.java.isAssignableFrom(clazz) -> value.toDoubleOrNull() as T
        Float::class.java.isAssignableFrom(clazz) -> value.toFloatOrNull() as T
        Short::class.java.isAssignableFrom(clazz) -> value.toShortOrNull() as T
        Byte::class.java.isAssignableFrom(clazz) -> value.toByteOrNull() as T
        Boolean::class.java.isAssignableFrom(clazz) -> value.toBoolean() as T
        else -> jacksonObjectMapper().readValue(value, clazz)
    }

/**
 * 解码 可空 String->Entity
 */
fun <T> optDecode(value: String, clazz: Class<T>): T? =
    when {
        String::class.java.isAssignableFrom(clazz) -> value as? T
        Int::class.java.isAssignableFrom(clazz) -> value.toIntOrNull() as? T
        Long::class.java.isAssignableFrom(clazz) -> value.toLongOrNull() as? T
        Double::class.java.isAssignableFrom(clazz) -> value.toDoubleOrNull() as? T
        Float::class.java.isAssignableFrom(clazz) -> value.toFloatOrNull() as? T
        Short::class.java.isAssignableFrom(clazz) -> value.toShortOrNull() as? T
        Byte::class.java.isAssignableFrom(clazz) -> value.toByteOrNull() as? T
        Boolean::class.java.isAssignableFrom(clazz) -> value.toBoolean() as? T
        else -> jacksonObjectMapper().readValue(value, clazz)
    }

/**
 * 解码 String -> List<Entity>
 */
fun <T> decodeToList(value: String): List<T> = jacksonObjectMapper().readValue(value, object : TypeReference<List<T>>() {})

/**
 * 编码 Entity->String
 */
fun <T : Any> encode(value: T): String =
    when {
        value::class.java == String::class.java -> value.toString()
        value::class.javaPrimitiveType?.isPrimitive ?: value::class.java.isPrimitive -> value.toString()
        else -> jacksonObjectMapper().writeValueAsString(value)
    }