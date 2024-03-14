package com.tangqiu.cloud.ktor.openapi.annotation

enum class OpenApiAuthType(val value: String) {
    AUTH_TYPE_QUERY("query"),
    AUTH_TYPE_HEADER("header")
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OpenApi(
    val name: String = "",
    val packageName: String = "",
    val url: String = ""
)

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class OpenApiAuth(
    val pattern: String = ".*",
    val type: OpenApiAuthType = OpenApiAuthType.AUTH_TYPE_QUERY,
    val key: String = "access_token",
    val value: String
)
