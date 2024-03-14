package com.tangqiu.cloud.ktor.openapi

import com.tangqiu.cloud.ktor.openapi.exception.ApiException
import com.tangqiu.cloud.ktor.openapi.serialization.decode
import com.tangqiu.cloud.ktor.openapi.serialization.optDecode
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class Controller<T> {
    abstract val service: T
    abstract val route: Route
    abstract fun install()

    suspend inline fun <reified T : Any> ApplicationCall.exec(noinline block: suspend ApplicationCall.() -> T?) {
        try {
            if (T::class == Unit::class) {
                block.invoke(this)
                respond(HttpStatusCode.OK)
            } else {
                val res = block.invoke(this) ?: return respond(HttpStatusCode.OK)
                respond(res)
            }
        } catch (t: Throwable) {
            application.feature(OpenApiFeature).config.logger?.error(t.message, t)
            val e = if (t is ApiException) t else ApiException(t.message, t)
            respond(e.code, e.message ?: "")
        }
    }

    suspend fun ApplicationCall.execFile(block: suspend ApplicationCall.(ByteWriteChannel) -> Unit) {
        try {
            val fileName = optHeader(FILE_NAME_HEADER) ?: "download"
            respond(NamedChannelWriterContent(fileName) {
                block.invoke(this@execFile, this)
            })
        } catch (t: Throwable) {
            application.feature(OpenApiFeature).config.logger?.error(t.message, t)
            val e = if (t is ApiException) t else ApiException(t.message, t)
            respond(e.code, e.message ?: "")
        }
    }

    inline fun <reified V : Any> ApplicationCall.header(key: String? = null): V {
        val header = request.headers[key ?: ""] ?: throw ApiException(HttpStatusCode.BadRequest)
        return decode(header, V::class.java)
    }

    inline fun <reified V : Any> ApplicationCall.optHeader(key: String? = null): V? {
        val header = request.headers[key ?: ""]
        return header?.let { decode(it, V::class.java) }
    }

    //body数据-可以是文件、表单、、
    suspend inline fun <reified V : Any> ApplicationCall.payload(): V = withContext(Dispatchers.IO) { receive() }

    //文件
    suspend inline fun ApplicationCall.multipart(): MultiPartData = this.receiveMultipart()

    suspend fun <T> ApplicationCall.form(block: suspend Parameters.() -> T): T = block.invoke(receiveParameters())

    //表单数据
    suspend inline fun <reified V : Any> Parameters.formData(key: String? = null): V {
        val receiveParameter = get(key ?: "") ?: throw ApiException(HttpStatusCode.BadRequest)
        return decode(receiveParameter, V::class.java)
    }

    //表单数据
    suspend inline fun <reified V : Any> Parameters.optFromData(key: String? = null): V? {
        val receiveParameter = get(key ?: "")
        return receiveParameter?.let { optDecode(it, V::class.java) }
    }

    //查询参数 www.baidu.com?1b=2&c=3
    inline fun <reified V : Any> ApplicationCall.query(key: String? = null): V {
        val parameter = request.queryParameters[key ?: ""] ?: throw ApiException(HttpStatusCode.BadRequest)
        return decode(parameter, V::class.java)
    }

    //查询参数list www.baidu.com?a=[ "a", "d" ]
    inline fun <reified V : Any> ApplicationCall.queryList(key: String? = null): List<V> {
        val parameterList = request.queryParameters.getAll(key ?: "") ?: throw ApiException(HttpStatusCode.BadRequest)
        val newParam = arrayListOf<V>()
        parameterList.let {
            for (i in it) {
                newParam.add(decode(i, V::class.java))
            }
        }
        return newParam
    }

    //查询参数list www.baidu.com?a=[ "a", "d" ]
    inline fun <reified V : Any> ApplicationCall.optQueryList(key: String? = null): List<V>? {
        val parameterList = request.queryParameters.getAll(key ?: "")
        parameterList?.let {
            val newParam = arrayListOf<V>()
            for (i in it) {
                newParam.add(decode(i, V::class.java))
            }
            return newParam
        }
        return null
    }

    //查询参数 www.baidu.com?1b=2&c=3
    inline fun <reified V : Any> ApplicationCall.optQuery(key: String? = null): V? {
        val parameter = request.queryParameters[key ?: ""]
        return parameter?.let { optDecode(it, V::class.java) }
    }

    //路径参数 www.baidu.com/1
    inline fun <reified V : Any> ApplicationCall.path(key: String? = null): V {
        val pathParameter = parameters[key ?: ""] ?: throw ApiException(HttpStatusCode.BadRequest)
        return decode(pathParameter, V::class.java)
    }

    //路径参数 www.baidu.com/2
    inline fun <reified V : Any> ApplicationCall.optPath(key: String? = null): V? {
        val pathParameter = parameters[key ?: ""]
        return pathParameter?.let { optDecode(it, V::class.java) }
    }

    interface Factory<T, C : Controller<T>> {
        val key: AttributeKey<C>
        val resources: List<String>
        fun create(service: T, route: Route): C
    }

    companion object {
        const val FILE_NAME_HEADER = "File-Name"
    }
}

/**
 * [OutgoingContent] to respond with [ByteWriteChannel].
 * The stream would be automatically closed after [body] finish.
 */
class NamedChannelWriterContent(
    private val name: String,
    private val body: suspend ByteWriteChannel.() -> Unit,
) : OutgoingContent.WriteChannelContent() {
    override val headers = Headers.build {
        set("Content-Disposition", "attachment;filename=$name")
    }
    override var contentType: ContentType = ContentType.Application.OctetStream
    override var status: HttpStatusCode? = null
    override suspend fun writeTo(channel: ByteWriteChannel) {
        try {
            body(channel)
        } catch (e: Throwable) {
            status = HttpStatusCode.InternalServerError
            contentType = ContentType.Text.Plain
            channel.writeStringUtf8(e.message ?: "")
        }
    }
}
