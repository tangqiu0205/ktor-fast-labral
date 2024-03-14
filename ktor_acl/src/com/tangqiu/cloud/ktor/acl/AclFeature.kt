package com.tangqiu.cloud.ktor.acl

import com.tangqiu.cloud.ktor.acl.model.Role
import com.tangqiu.cloud.ktor.acl.model.verify
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import java.net.URI

class AclFeature private constructor(
    private val configure: Configuration
) {
    val resources: Set<String> get() = configure._resources

    class Configuration {
        internal val _resources = LinkedHashSet<String>()

        /**
         * 判断如果请求参数里带有需要验证的path和method 则验证请求参数内的
         * 否则验证自己
         */
        fun Route.role(provider: suspend PipelineContext<Unit, ApplicationCall>.() -> List<Role>) {
            intercept(ApplicationCallPipeline.Call) {
                val rolePath = call.request.queryParameters["rolePath"]
                val roleMethod = call.request.queryParameters["roleMethod"]?.let { HttpMethod.parse(it) }
                val path = rolePath ?: URI.create(call.request.uri).path
                val method = roleMethod ?: call.request.httpMethod

                val role = provider.invoke(this)
                if (role.any { it.verify(path, method) }) {
                    proceed()
                } else {
                    call.respond(HttpStatusCode.Forbidden, "没有权限访问: path=$path")
                    finish()
                }
            }
        }

        fun pushResources(resources: List<String>) {
            this._resources.addAll(resources)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, AclFeature> {
        /**
         * Unique key that identifies a feature
         */
        override val key: AttributeKey<AclFeature> = AttributeKey("Acl")

        /**
         * Feature installation script
         */
        override fun install(pipeline: Application, configure: Configuration.() -> Unit): AclFeature {
            return AclFeature(Configuration().also(configure))
        }
    }
}