package com.tangqiu.cloud.ktor.discovery.annotation

enum class DiscoverAuthType(val value: String) {
    AUTH_TYPE_QUERY("query"),
    AUTH_TYPE_HEADER("header")
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Discover(
    val name: String = "",
    val packageName: String = "",
    val url: String = "",
    val vipAddress: String = "",
    val pipe: Boolean = false,
    val host: String = "",
    val port: Int = 80,
    val dateTimeType: String = "",
    val receiveHeader: Boolean = true //是否接收远程服务header
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DiscoverAuth(
    val pattern: String = ".*",
    val type: DiscoverAuthType = DiscoverAuthType.AUTH_TYPE_QUERY,
    val key: String = "access_token",
    val value: String
)
