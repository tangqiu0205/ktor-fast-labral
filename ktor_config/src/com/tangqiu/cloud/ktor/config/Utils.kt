package com.tangqiu.cloud.ktor.config

import com.uchuhimo.konf.*
import java.util.*

fun Config.toProperties(): Properties = Properties().apply {
    (this@toProperties as? BaseConfig)?.let { config ->
        putAll(config.source.tree.paths.mapNotNull { path ->
            config.source[path].tree.getValue()?.let { path to it }
        })
    }
}

internal fun TreeNode.getValue(): Any? = when (this) {
    is ValueNode -> value
    is ListNode -> list.mapNotNull { it.getValue() }
    else -> null
}

fun Properties.apply(config: Config) {
    putAll(config.toProperties())
}

fun <T : Any> T.mergeFrom(other: T) {
    this::class.java.declaredFields.forEach {
        kotlin.runCatching {
            it.isAccessible = true
            it.set(this, it.get(other))
        }.onFailure {
            println(it)
        }
    }
}