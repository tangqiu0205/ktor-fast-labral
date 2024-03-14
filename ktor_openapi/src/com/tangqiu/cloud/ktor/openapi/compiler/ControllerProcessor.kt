package com.tangqiu.cloud.ktor.openapi.compiler

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auto.service.AutoService
import com.tangqiu.cloud.ktor.openapi.annotation.OpenApi
import com.tangqiu.cloud.ktor.openapi.annotation.OpenApiAuth
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.AuthorizationValue
import java.net.URI
import java.nio.file.Paths
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes(
    "com.tangqiu.cloud.ktor.openapi.annotation.OpenApi",
    "com.tangqiu.cloud.ktor.openapi.annotation.OpenApiAuth"
)
class ControllerProcessor : AbstractProcessor() {
    //注解处理器
    private var logger: Messager? = null //日志
    private var filer: Filer? = null //用来创建新源、类或辅助文件的 Filer
    private var options: Map<String, String>? = null
    private var objectMapper: ObjectMapper? = null //ObjectMapper提供了读取和写入JSON的功能

    private val root: String get() = options?.get(OPT_ROOTPATH) ?: "."

    override fun getSupportedOptions(): MutableSet<String> = mutableSetOf(OPT_PROXY, OPT_ROOTPATH)

    //执行一些初始化逻辑
    override fun init(processingEnv: ProcessingEnvironment?) {
        logger = processingEnv?.messager
        filer = processingEnv?.filer
        options = processingEnv?.options

        objectMapper = ObjectMapper()
        objectMapper!!.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    }

    //扫描、评估和处理注解的代码，以及生成Java文件
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val auths = roundEnv?.getElementsAnnotatedWith(OpenApiAuth::class.java)?.mapNotNull {
            it.runCatching { getAnnotation(OpenApiAuth::class.java) }.getOrNull()
        }?.map { openApiAuth ->
            AuthorizationValue(openApiAuth.key, openApiAuth.value, openApiAuth.type.value) {
                it.toString().matches(Regex(openApiAuth.pattern))
            }
        } ?: emptyList()
        roundEnv?.getElementsAnnotatedWith(OpenApi::class.java)?.mapNotNull {
            it.runCatching { getAnnotation(OpenApi::class.java) }.getOrNull()
        }?.distinctBy { it.name }?.forEach {
            val codegen = ControllerCodegen(it.packageName, it.name) //new KtorControllerCodegen对象
            try {
                val url = if (!it.url.startsWith("http")) {
                    val adjustedLocation: String = it.url.replace("\\\\".toRegex(), "/")
                    val fileScheme = "file:"
                    val path = if (adjustedLocation.toLowerCase().startsWith(fileScheme)) Paths.get(
                        URI.create(
                            adjustedLocation
                        )
                    ) else Paths.get(root, adjustedLocation)
                    logger?.printMessage(Diagnostic.Kind.NOTE, path.toString())
                    path.toFile().absolutePath
                } else it.url
                val openAPI = OpenAPIV3Parser().read(url, auths, null)
                if (openAPI != null) {
                    //openAPI解析传入的JsonNode
                    codegen.processAllModels(openAPI, filer!!, logger) //调用生成数据类方法
                    codegen.processService(openAPI, filer!!, logger) //调用生成接口方法
                    codegen.processController(openAPI, filer!!, logger) //调用生成路由方法
                }
            } catch (e: Throwable) {
                logger?.printMessage(Diagnostic.Kind.ERROR, e.message)
            }
        }

        return true
    }

    //伴生对象 静态类型
    companion object {
        const val OPT_PROXY = "processor_proxy"
        const val OPT_ROOTPATH = "processor_root"
    }
}
