package com.tangqiu.cloud.ktor.discovery.utils

import com.uchuhimo.konf.source.base.EmptyStringNode.value
import io.ktor.client.request.*

/**
 * Sets a single URL query parameter of [key] with a specific [value] if the value is not null. Can not be used to set
 * form parameters in the body.
 */
fun HttpRequestBuilder.parameterAll(key: String, values: Iterable<Any>?): Unit =
    values?.let { v -> url.parameters.appendAll(key, v.map { it.toString() }) } ?: Unit