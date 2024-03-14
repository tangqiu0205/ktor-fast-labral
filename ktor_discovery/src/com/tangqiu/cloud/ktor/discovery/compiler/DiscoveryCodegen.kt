package com.tangqiu.cloud.ktor.discovery.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.*
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager

class DiscoveryCodegen(
    packageName: String,
    private val fileName: String,
    private val vipAddress: String,
    private val pipe: Boolean,
    private val host: String,
    private val port: Int,
    private val dateTimeType: TypeName = LocalDateTime::class.asTypeName(),
    private val receiveHeader: Boolean = true //是否接收远程服务header
) {
    private val modelPackage = "$packageName.models"
    private val servicePackage = packageName

    private val className = fileName.toFirstUpperCase()
    private val serviceType = ClassName(servicePackage, "${className}Service")

    private val parameterMap = mutableMapOf<String, Parameter>()

    //生成实体类
    fun processAllModels(openAPI: OpenAPI, filer: Filer, logger: Messager?) {
        //获取content字段下的数据
        openAPI.components?.requestBodies?.forEach { (t, u) ->
            u.content?.get("application/json")?.schema?.let {
                processModel(it, filer, logger, "Request$t")
            }
        }
        //schemas字段下的数据
        openAPI.components?.schemas?.forEach { (t, u) ->
            processModel(u, filer, logger, t)
        }
        //遍历parameters 逐个添加到本地parameterMap中
        openAPI.components?.parameters?.forEach { (t, u) ->
            parameterMap[t] = u
        }
    }

    //处理生成接口方法
    fun processService(openAPI: OpenAPI, filer: Filer, logger: Messager?) {
        //生成接口类并设置类名
        val serviceSpec = TypeSpec.classBuilder(serviceType).superclass(baseServiceType)
            //主构造函数
            .primaryConstructor(
                //构造函数内添加 ”service“参数 并设置引用类型 修饰符为override
                FunSpec.constructorBuilder().addParameter("app", applicationType, KModifier.OVERRIDE).build()
            )
            .addProperty(PropertySpec.builder("app", applicationType).initializer("app").build())
            .addProperty(
                PropertySpec.builder("vipAddress", String::class).initializer("%S", vipAddress)
                    .addModifiers(KModifier.OVERRIDE).build()
            )
            .addProperty(
                PropertySpec.builder("host", String::class.asTypeName().copy(true)).initializer("%S", host)
                    .addModifiers(KModifier.OVERRIDE).build()
            )
            .addProperty(
                PropertySpec.builder("port", Int::class).initializer("%L", port)
                    .addModifiers(KModifier.OVERRIDE).build()
            )

        val cacheKeys = mutableMapOf<String, String>()
        //循环paths字段 判断item 请求类型 并调用生成接口方法 传入相应的请求类型
        openAPI.paths?.forEach { path, item ->
            item.get?.run { serviceSpec.addFunction(processServiceApi(this, "get", path, cacheKeys)) }
            item.post?.run { serviceSpec.addFunction(processServiceApi(this, "post", path, cacheKeys)) }
            item.put?.run { serviceSpec.addFunction(processServiceApi(this, "put", path, cacheKeys)) }
            item.patch?.run { serviceSpec.addFunction(processServiceApi(this, "patch", path, cacheKeys)) }
            item.delete?.run { serviceSpec.addFunction(processServiceApi(this, "delete", path, cacheKeys)) }
        }
        serviceSpec.addType(
            //添加一个名为 Factory伴生对象
            TypeSpec.companionObjectBuilder("Factory")
                //继承父接口 并添加参数化类型
                .addSuperinterface(serviceFactoryType.parameterizedBy(serviceType))
                //添加属性
                .addProperty(
                    //添加属性 名为key 设置属性类型为attributeKeyType参数化类型 添加override修饰符
                    PropertySpec.builder(
                        "key",
                        attributeKeyType.parameterizedBy(serviceType),
                        KModifier.OVERRIDE
                    )
                        //设置key 初始化值为 = AttributeKey("Test")
                        .initializer("%T(%S)", attributeKeyType, fileName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder(
                        "cacheKeys",
                        Map::class.parameterizedBy(String::class, String::class),
                        KModifier.OVERRIDE
                    ).initializer(
                        "mapOf(${Array(cacheKeys.size) { "%S to %S" }.joinToString(", ")})",
                        *(cacheKeys.entries.flatMap { listOf(it.key, it.value) }.toTypedArray())
                    ).build()
                )
                .addFunction(
                    //添加一个create方法 override修饰符 参数为service 参数类型为serviceType 返回值类型为 controllerType
                    FunSpec.builder("create")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("app", applicationType)
                        .returns(serviceType)
                        .addStatement("return %T(app)", serviceType) //添加返回语句
                        .build()
                ).build()
        )
        //生成文件
        FileSpec.get(servicePackage, serviceSpec.build()).writeTo(filer)
    }

    private fun <T> processModel(
        schema: Schema<T>,
        filer: Filer,
        logger: Messager?,
        fileName: String
    ) {
        val classBuilder = TypeSpec.classBuilder(fileName) //生成类并添加类名
            .addModifiers(KModifier.DATA) //类类型 为数据类
            .apply { schema.description?.let { addKdoc(it) } } //添加注释
        val constructorBuilder = FunSpec.constructorBuilder() //生成构造函数

        val nullables = (schema.extensions ?: emptyMap())["x-nullable"] as? List<*> ?: emptyList<Any>()
        schema.properties?.forEach { (name, pSchema) ->
            val shouldBeNullable = nullables.contains(name)
            val typeName = pSchema.typeName(modelPackage).run {
                if (shouldBeNullable) copy(nullable = true) else this
            }
            val enum = pSchema.enum ?: emptyList()
            constructorBuilder.addParameter(
                ParameterSpec.builder(name, typeName).apply {
                    val default = pSchema.default
                    val nullable = typeName.isNullable
                    if (nullable && default == null) {
                        defaultValue("null") //设置默认值
                    }
                    default?.let {
                        defaultValue(default.toString()) //设置默认值
                    }

                    pSchema.description?.let { addKdoc(it) }
                    if (enum.isNotEmpty()) {
                        addKdoc("\n")
                        addKdoc(
                            "value must be in: [${
                                Array(enum.size) { if (pSchema.isString) "%S" else "%L" }.joinToString(", ")
                            }]", *enum.toTypedArray()
                        )
                    }
                }.build()
            )
            classBuilder.addProperty(PropertySpec.builder(name, typeName).initializer(name).build())
        }
        //主构造函数加入到class中
        classBuilder.primaryConstructor(constructorBuilder.build())
        try {
            FileSpec.get(modelPackage, classBuilder.build()).writeTo(filer)
        } catch (t: Throwable) {
            println(t.message)
        }
    }

    /**
     * 生成方法
     */
    private fun processServiceApi(
        operation: Operation,
        method: String,
        path: String,
        cacheKeys: MutableMap<String, String>
    ): FunSpec = FunSpec.builder(operation.operationId ?: path.toFuncName(method)) //生成方法并设置方法名
        .addModifiers(KModifier.SUSPEND) //添加suspend修饰符 需要添加abstract修饰符否则生成的接口方法有方法体
        .apply {
            var returnType: TypeName = Unit::class.asTypeName()
//            val queryParams = mutableListOf<String>()
            val queryParams = mutableMapOf<String, TypeName>()
            val pathParams = mutableListOf<String>()
            val headerParams = mutableListOf<String>()
            var body: CodeBlock? = null
            var isStream = false
            addKdoc(operation.summary) //添加注解

            beginControlFlow("try")

            if (pipe) {
                addParameter("call", callType)
            }
            operation.parameters?.forEachIndexed { index, parameter ->
                //删除#/components/parameters/前缀后 从本地parameterMap中获取数据
                val ref = parameter.`$ref`?.removePrefix("#/components/parameters/")?.let { parameterMap[it] }
                //方法内参数名
                val name = parameter.name ?: ref?.name ?: "p$index"
                //方法内参数类型
                val type = ref?.schema?.typeName(modelPackage) ?: parameter.schema?.typeName(modelPackage)
                ?: Any::class.asTypeName()
                //添加方法内的相关参数 判断参数
                addParameter(
                    ParameterSpec.builder(name, if (parameter.required == true) type else type.copy(nullable = true))
                        .apply {
                            if (parameter.required != true) defaultValue("null")
                        }.build()
                )

                when (parameter.`in`) {
//                    "query" -> queryParams.add(name)
                    "query" -> queryParams[name] = type
                    "path" -> pathParams.add(name)
                    "header" -> headerParams.add(name)
                }
            }
            operation.responses?.get("200")?.content?.run {
                get("application/text")?.let {
                    //添加方法的返回类型
                    returnType = String::class.asTypeName()
                }
                get("application/json")?.schema?.typeName(modelPackage)?.let {
                    returnType = it //添加 从引用中获取返回类型
                }
                get("application/octet-stream")?.let {
                    //接口方法内添加 名为"channel"参数 并添加相应的类型引用
                    addParameter("block", channelHandlerType)
                    isStream = true
                }
            }
            //请求体内的数据
            operation.requestBody?.content?.run {
                get("multipart/form-data")?.let {
                    //在接口方法内 添加名为”parts“的参数，并添加相应类型引用
                    addParameter("parts", ClassName("io.ktor.http.content", "PartData"), KModifier.VARARG)
                    body = CodeBlock.of("multipart(*parts)")
                }
                get("application/x-www-form-urlencoded")?.schema?.let { schema ->
                    schema.properties?.forEach { (key, ps) ->
                        //添加方法内的相关参数 判断参数
                        addParameter(key, ps.typeName(modelPackage))
                    }
                    schema.properties?.run {
                        if (keys.isNotEmpty()) {
                            body = CodeBlock.of("formData(${keys.joinToString(", ") { "%S to %L" }})", *keys.flatMap {
                                listOf(it, it)
                            }.toTypedArray())
                        }
                    }
                }
                get("application/json")?.schema?.typeName(modelPackage)?.let {
                    //调用typeName方法 在接口方法内 添加名为”payload“, 并添加相应的引用类型 it为 typeName() 返回的引用类型
                    addParameter("payload", it)
                    body = CodeBlock.of("this.body = payload")
                }
                get("*/*")?.schema?.typeName(modelPackage)?.let {
                    //调用typeName方法 在接口方法内 添加名为”payload“, 并添加相应的引用类型 it为 typeName() 返回的引用类型
                    addParameter("payload", it)
                    body = CodeBlock.of("this.body = payload")
                }
            }
            returns(apiResultType.parameterizedBy(returnType))

            // 缓存Key模板
            val cacheKeyPattern = operation.extensions?.get("x-cache") as? String
            if (!cacheKeyPattern.isNullOrBlank()) {
                val cacheKeyDesc = operation.extensions?.get("x-cache-description") as? String ?: cacheKeyPattern
                cacheKeys[cacheKeyPattern] = cacheKeyDesc
                val paramPattern =
                    listOf(
                        *pathParams.toTypedArray(),
                        *queryParams.keys.toTypedArray()
                    ).joinToString(", ") { "%S to %L" }
                addStatement(
                    "val cacheKey = encodedPath(%S, $paramPattern)",
                    cacheKeyPattern,
                    *pathParams.flatMap {
                        listOf(it, it)
                    }.toTypedArray(),
                    *queryParams.flatMap {
                        listOf(it, it)
                    }.toTypedArray()
                )
                addStatement("val cached = getCache(cacheKey, %T::class.java)", returnType)
                beginControlFlow("if (cached != null)")
                addStatement("return cached.%M<%T>()", toApiResultType, returnType)
                endControlFlow()
            }

            val memberName = MemberName("io.ktor.client.request", method.lowercase(Locale.getDefault()))
            beginControlFlow("val httpResponse = client.%M<%T>()", memberName, httpResponseType)

            if (pipe) {
                val pipeMember = MemberName("com.tangqiu.cloud.ktor.discovery", "pipe")
                addStatement("%M(call)", pipeMember)
            }
            beginControlFlow("url")
            val pathPattern = pathParams.joinToString(", ") { "%S to %L" }
            if (pathPattern.isNotBlank()) {
                addStatement(
                    "encodedPath = encodedPath(%S, $pathPattern)",
                    path,
                    *pathParams.flatMap {
                        listOf(it, it)
                    }.toTypedArray()
                )
            } else {
                addStatement("encodedPath = %S", path)
            }

            queryParams.forEach { (k, v) ->
                if (v.toString().startsWith(List::class.qualifiedName!!)) {
                    val parameterAll = MemberName("com.tangqiu.cloud.ktor.discovery.utils", "parameterAll")
                    addStatement("%M(%S, %L)", parameterAll, k, k)
                } else {
                    val parameter = MemberName("io.ktor.client.request", "parameter")
                    addStatement("%M(%S, %L)", parameter, k, k)
                }
            }
            endControlFlow()
            headerParams.forEach {
                addStatement("%M(%S, %L)", headerType, it, it)
            }
            body?.let { addCode(it) }
            endControlFlow()
            if (isStream) {
                beginControlFlow(".execute")
                addStatement("block.invoke(it.content)")
                endControlFlow()
            }

//            val receive = MemberName("io.ktor.client.call", "receive")
            addStatement("val res = httpResponse.%M<%T>()", toApiResultType, returnType)

            if (!cacheKeyPattern.isNullOrBlank()) {
                addStatement("val data = res.data ?: return res")
                // 缓存Key模板
                val cacheKeyExpire = operation.extensions?.get("x-cache-expire") as? String
                addStatement("setCache(cacheKey, data, %S)", cacheKeyExpire ?: "")
            }
            //不接收远程服务header (和operationId平级)
            val noHeader = operation.extensions?.get("x-no-header") as? Boolean
            if (receiveHeader && (noHeader == null || noHeader == false)) {
                //是否接收远程服务header
                addStatement("%M(call, res.headers)", handleHeader)
            }

            addStatement("return res")
            endControlFlow()
            beginControlFlow("catch (e: %T)", ResponseExceptionType)
            addStatement("return e.%M<%T>()", toApiResultType, returnType)
            endControlFlow()
        }.build()

    private fun <T> Schema<T>.typeName(packageName: String): TypeName {
        val nullable = nullable == true
        if (!`$ref`.isNullOrBlank()) {
            //ref不为空 判断引用是否包含”#/components/schemas/“
            if (`$ref`.contains("#/components/schemas/")) {
                //返回该包的引用类型
                return ClassName(packageName, `$ref`.substringAfter("#/components/schemas/").toFirstUpperCase()).copy(
                    nullable = nullable
                )
                //判断引用是否包含 ”#/components/parameters/“
            } else if (`$ref`.contains("#/components/parameters/")) {
                //查看本地缓存中是否有当前引用的类型 若无 则从schema中 递归调用自己 获取引用类型 否则返回Any
                return parameterMap[`$ref`.substringAfter("#/components/parameters/")]?.schema?.typeName(packageName)
                    ?.copy(nullable = nullable)
                    ?: Any::class.asTypeName().copy(nullable = nullable)
            }
        }

        return when (type.lowercase(Locale.getDefault())) {
            //返回List<>
            "array" -> (this as? ArraySchema)?.items?.let {
                List::class.asTypeName().parameterizedBy(it.typeName(packageName)).copy(nullable = nullable)
            } ?: List::class.asTypeName().parameterizedBy(Any::class.asTypeName()).copy(nullable = nullable)
            //返回Map<*,*>
            "object" -> (additionalProperties as? Schema<*>)?.let {
                Map::class.asTypeName().parameterizedBy(String::class.asTypeName(), it.typeName(modelPackage))
                    .copy(nullable = nullable)
            } ?: Map::class.asTypeName().parameterizedBy(String::class.asTypeName(), Any::class.asTypeName())
                .copy(nullable = nullable)

            else -> typeName(type, format).copy(nullable = nullable)
        }
    }

    private fun typeName(type: String, format: String?): TypeName = when (type.lowercase(Locale.getDefault())) {
        "integer" -> {
            if (format?.lowercase(Locale.getDefault()) == "int64") Long::class.asTypeName() else Int::class.asTypeName()
        }
        "string" -> {
            when (format?.lowercase(Locale.getDefault())) {
                "byte" -> Byte::class.asTypeName()
                "binary" -> InputStream::class.asTypeName()
                "date" -> LocalDate::class.asTypeName()
                "time" -> LocalTime::class.asTypeName()
                "date-time" -> LocalDateTime::class.asTypeName()
                "zone-time" -> ZonedDateTime::class.asTypeName()
                else -> String::class.asTypeName()
            }
        }
        "number" -> {
            if (format?.lowercase(Locale.getDefault()) == "float") Float::class.asTypeName() else Double::class.asTypeName()
        }
        "boolean" -> Boolean::class.asTypeName()

        else -> Any::class.asTypeName()
    }

    private val <T> Schema<T>.isString: Boolean get() = type.lowercase(Locale.getDefault()) == "string"

    //生成方法名
    private fun String.toFuncName(method: String): String =
        split("/") //去除 ”/“
            .filterNot { it.isNotBlank() } //过滤掉不符合条件的元素
            .joinToString {
                it.toFirstUpperCase().replace(Regex("\\{(.*?)}")) { res ->
                    "By${res.groupValues[1].toFirstUpperCase()}"
                }
            }.let { "$method$it" }

    //首字母大写
    private fun String.toFirstUpperCase(): String =
        if (isBlank()) "" else replaceRange(0, 1, get(0).uppercaseChar().toString())

    //驼峰
    private fun String.toCamelCase(vararg delimiters: String): String =
        split(*delimiters).filterNot { it.isBlank() }.mapIndexed { _, s ->
            s.toFirstUpperCase()
        }.joinToString()

    companion object {
        private val baseServiceType = ClassName("com.tangqiu.cloud.ktor.discovery", "DiscoveryClient")
        private val serviceFactoryType = ClassName("com.tangqiu.cloud.ktor.discovery", "DiscoveryClient", "Factory")
        private val channelHandlerType = ClassName("com.tangqiu.cloud.ktor.discovery", "ChannelHandler")
        private val attributeKeyType = ClassName("io.ktor.util", "AttributeKey")
        private val applicationType = ClassName("io.ktor.application", "Application")
        private val callType = ClassName("io.ktor.application", "ApplicationCall")
        private val partDataType = ClassName("io.ktor.http.content", "PartData")
        private val httpResponseType = ClassName("io.ktor.client.statement", "HttpResponse")
        private val httpStatusCodeType = ClassName("io.ktor.http", "HttpStatusCode")
        private val resultDataType = ClassName("", "ResultData")
        private val apiResultType = ClassName("com.tangqiu.cloud.ktor.discovery", "ApiResult")
        private val apiExceptionType = ClassName("com.tangqiu.cloud.ktor.openapi.exception", "ApiException")
        private val ResponseExceptionType = ClassName("io.ktor.client.features", "ResponseException")

        private val headerType = MemberName("io.ktor.client.request", "header")
        private val toApiResultType = MemberName("com.tangqiu.cloud.ktor.discovery", "toApiResult")
        private val handleHeader = MemberName("com.tangqiu.cloud.ktor.discovery.utils", "handleHeader")
    }
}
