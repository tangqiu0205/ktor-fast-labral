package com.tangqiu.cloud.ktor.openapi.compiler

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

class ControllerCodegen(
    packageName: String,
    private val fileName: String
) {
    private val modelPackage = "$packageName.models"
    private val servicePackage = packageName
    private val controllerPackage = packageName

    private val className = fileName.toFirstUpperCase()
    private val serviceType = ClassName(servicePackage, "${className}Service")
    private val controllerType = ClassName(controllerPackage, "${className}Controller")

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
        val serviceSpec = TypeSpec.interfaceBuilder(serviceType)
        //循环paths字段 判断item 请求类型 并调用生成接口方法 传入相应的请求类型
        openAPI.paths?.forEach { path, item ->
            item.get?.run { serviceSpec.addFunction(processServiceApi(this, "get", path)) }
            item.post?.run { serviceSpec.addFunction(processServiceApi(this, "post", path)) }
            item.put?.run { serviceSpec.addFunction(processServiceApi(this, "put", path)) }
            item.patch?.run { serviceSpec.addFunction(processServiceApi(this, "patch", path)) }
            item.delete?.run { serviceSpec.addFunction(processServiceApi(this, "delete", path)) }
        }
        //生成文件
        FileSpec.get(servicePackage, serviceSpec.build()).writeTo(filer)
    }

    //处理生成Controller路由的方法
    fun processController(openAPI: OpenAPI, filer: Filer, logger: Messager?) {
        //生成controller类并设置类名
        val controllerSpec = TypeSpec.classBuilder(controllerType)
            .superclass(baseControllerType.parameterizedBy(serviceType)) //继承父类 添加泛型（参数化类型）
            //主构造函数
            .primaryConstructor(
                //构造函数内添加 ”service“参数 并设置引用类型 修饰符为override
                FunSpec.constructorBuilder()
                    .addParameter("service", serviceType, KModifier.OVERRIDE)
                    .addParameter("route", routeType, KModifier.OVERRIDE)
                    .build()
            )
            //”service“变量 加上 public val
            .addProperty(PropertySpec.builder("service", serviceType).initializer("service").build())
            .addProperty(PropertySpec.builder("route", routeType).initializer("route").build())
            .addFunction(
                FunSpec.builder("install") //添加一个名叫 install 方法
                    .addModifiers(KModifier.OVERRIDE) //添加 override 修饰符
//                    .addParameter("route", routeType) //install方法内添加名为 route的参数 并设置引用类型
                    //install方法体内 开始位置添加 route.run  beginControlFlow：会添加 {}
                    .addCode(CodeBlock.builder().beginControlFlow("route.run").build())
                    .apply {
                        //遍历paths路径 判断请求类型 调用生成Controller路由方法 传入相应请求
                        openAPI.paths?.forEach { path, item ->
                            item.get?.run { processControllerApi(this@apply, this, "get", path) }
                            item.post?.run { processControllerApi(this@apply, this, "post", path) }
                            item.put?.run { processControllerApi(this@apply, this, "put", path) }
                            item.patch?.run { processControllerApi(this@apply, this, "patch", path) }
                            item.delete?.run { processControllerApi(this@apply, this, "delete", path) }
                        }
                    }
                    //结束控制流
                    .addCode(CodeBlock.builder().endControlFlow().build())
                    .build()
            )
            /**
             * public companion object Factory : Controller.Factory<TestService, TestController> {
            public override val key: AttributeKey<TestController> = AttributeKey("Test")
            public override fun create(service: TestService): TestController = TestController(service)
            }
             */
            .addType(
                //添加一个名为 Factory伴生对象
                TypeSpec.companionObjectBuilder("Factory")
                    //继承父接口 并添加参数化类型
                    .addSuperinterface(controllerFactoryType.parameterizedBy(serviceType, controllerType))
                    //添加属性
                    .addProperty(
                        //添加属性 名为key 设置属性类型为attributeKeyType参数化类型 添加override修饰符
                        PropertySpec.builder(
                            "key",
                            attributeKeyType.parameterizedBy(controllerType),
                            KModifier.OVERRIDE
                        )
                            //设置key 初始化值为 = AttributeKey("Test")
                            .initializer("%T(%S)", attributeKeyType, fileName)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder(
                            "resources",
                            List::class.parameterizedBy(String::class),
                            KModifier.OVERRIDE
                        ).initializer(
                            "listOf(${Array(openAPI.paths?.size ?: 0) { "%S" }.joinToString(", ")})",
                            *(openAPI.paths?.keys?.toTypedArray() ?: arrayOf<String>())
                        ).build()
                    )
                    .addFunction(
                        //添加一个create方法 override修饰符 参数为service 参数类型为serviceType 返回值类型为 controllerType
                        FunSpec.builder("create")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("service", serviceType)
                            .addParameter("route", routeType)
                            .returns(controllerType)
                            .addStatement("return %T(service, route)", controllerType) //添加返回语句
                            .build()
                    ).build()
            )
        //生成controller文件
        FileSpec.get(controllerPackage, controllerSpec.build()).writeTo(filer)
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
        fallbackName: String
    ): FunSpec = FunSpec.builder(operation.operationId ?: fallbackName.toFuncName(method)) //生成方法并设置方法名
        .addModifiers(KModifier.SUSPEND, KModifier.ABSTRACT) //添加suspend修饰符 需要添加abstract修饰符否则生成的接口方法有方法体
        .addParameter("call", callType)
        .apply {
            addKdoc(operation.summary) //添加注解
            operation.parameters?.forEachIndexed { index, parameter ->
                //删除#/components/parameters/前缀后 从本地parameterMap中获取数据
                val ref = parameter.`$ref`?.removePrefix("#/components/parameters/")?.let { parameterMap[it] }
                //方法内参数名
                val name = parameter.name ?: ref?.name ?: "p$index"
                //方法内参数类型
                val type = ref?.schema?.typeName(modelPackage) ?: parameter.schema?.typeName(modelPackage)
                ?: Any::class.asTypeName()
                //添加方法内的相关参数 判断参数
                addParameter(name, if (parameter.required == true) type else type.copy(nullable = true))
            }
            operation.responses?.get("200")?.content?.run {
                get("application/text")?.let {
                    returns(String::class.asTypeName().copy(nullable = true)) //添加方法的返回类型
                }
                get("application/json")?.schema?.typeName(modelPackage)?.let {
                    returns(it.copy(nullable = true)) //添加 从引用中获取返回类型
                }
                get("application/octet-stream")?.let {
                    //接口方法内添加 名为"channel"参数 并添加相应的类型引用
                    addParameter("channel", ClassName("io.ktor.utils.io", "ByteWriteChannel"))
                }

            }
            //请求体内的数据
            operation.requestBody?.content?.run {
                get("multipart/form-data")?.let {
                    //在接口方法内 添加名为”multiPartData“的参数，并添加相应类型引用
                    addParameter("multiPartData", ClassName("io.ktor.http.content", "MultiPartData"))
                }
                get("application/x-www-form-urlencoded")?.schema?.let {
                    it.properties?.forEach { (key, ps) ->
                        //添加方法内的相关参数 判断参数
                        addParameter(key, ps.typeName(modelPackage))
                    }
                }
                get("application/json")?.schema?.typeName(modelPackage)?.let {
                    //调用typeName方法 在接口方法内 添加名为”payload“, 并添加相应的引用类型 it为 typeName() 返回的引用类型
                    addParameter("payload", it)
                }
                get("*/*")?.schema?.typeName(modelPackage)?.let {
                    //调用typeName方法 在接口方法内 添加名为”payload“, 并添加相应的引用类型 it为 typeName() 返回的引用类型
                    addParameter("payload", it)
                }
            }
        }.build()

    //生成Controller路由方法
    private fun processControllerApi(
        initSpecBuilder: FunSpec.Builder,
        operation: Operation,
        method: String,
        path: String
    ) {
        /**
         *  /**Add a new pet to the store*/
        post("/pet") {
        call.exec {
        val payload: Pet = payload()
        service.addPet(payload)
        }
        }
         */
        initSpecBuilder.addCode(CodeBlock.builder()
            .addStatement("/**${operation.summary}*/") //添加声明语句 此处为注释
            .beginControlFlow("%T(%S)", ClassName("io.ktor.routing", method), path)
            .apply {
                if (operation.responses?.get("200")?.content?.get("application/octet-stream") != null) {
                    beginControlFlow("%T.execFile", ClassName("io.ktor.application", "call"))
                } else {
                    beginControlFlow("%T.exec", ClassName("io.ktor.application", "call"))
                }
                val params = mutableListOf("call")
                operation.parameters?.forEachIndexed { index, parameter ->
                    //删除前缀后从本地parameterMap查询是否存在引用
                    val ref = parameter.`$ref`?.removePrefix("#/components/parameters/")?.let { parameterMap[it] }
                    val name = parameter.name ?: ref?.name ?: "p$index"
                    val type = ref?.schema?.typeName(modelPackage) ?: parameter.schema?.typeName(modelPackage)
                    ?: Any::class.asTypeName()
                    when (parameter.`in`) {
                        "query" -> if (parameter.required == true) {
                            val pType = type as? ParameterizedTypeName
                            pType?.let {
                                println("pType:::::::::::${pType.rawType}")
                            }
                            if (pType?.rawType == List::class.asTypeName()) {
                                addStatement("val $name: %T = queryList(%S)", type, name)
                            } else {
                                addStatement("val $name: %T = query(%S)", type, name)
                            }
                        } else {
                            val pType = type as? ParameterizedTypeName
                            if (pType?.rawType == List::class.asTypeName()) {
                                addStatement("val $name: %T? = optQueryList(%S)", type, name)
                            } else {
                                addStatement("val $name: %T? = optQuery(%S)", type, name)
                            }
                        }
                        "path" -> if (parameter.required == true) {
                            addStatement("val $name: %T = path(%S)", type, name)
                        } else {
                            addStatement("val $name: %T? = optPath(%S)", type, name)
                        }
                        "body" -> addStatement("val $name: %T = payload()", type)
                        "header" -> if (parameter.required == true) {
                            addStatement("val $name: %T = header(%S)", type, name)
                        } else {
                            addStatement("val $name: %T? = optHeader(%S)", type, name)
                        }
                    }
                    params.add(name)
                }
                operation.responses?.get("200")?.content?.run {
                    get("application/octet-stream")?.let {
                        addStatement("")
                        params.add("it")
                    }
                }
                val isForm = operation.requestBody?.content?.get("application/x-www-form-urlencoded") != null
                operation.requestBody?.content?.run {
                    get("multipart/form-data")?.let {
                        val multiPartData = ClassName("io.ktor.http.content", "MultiPartData")
                        addStatement("val multiPartData: %T = multipart()", multiPartData)
                        params.add("multiPartData")
                    }
                    get("application/x-www-form-urlencoded")?.schema?.let {
                        beginControlFlow("form")
                        it.properties?.forEach { (key, ps) ->
                            addStatement("val $key: %T = formData(%S)", ps.typeName(modelPackage), key)
                            params.add(key)
                        }
                    }
                    get("application/json")?.schema?.typeName(modelPackage)?.let {
                        addStatement("val payload: %T = payload()", it)
                        params.add("payload")
                    }

                    get("*/*")?.schema?.typeName(modelPackage)?.let {
                        addStatement("val payload: %T = payload()", it)
                        params.add("payload")
                    }
                }

                addStatement(
                    "service.${operation.operationId ?: path.toFuncName(method)}(${
                        params.joinToString(", ")
                    })"
                )
                if (isForm) {
                    endControlFlow()
                }
            }
            .endControlFlow()
            .endControlFlow()
            .build()
        )
    }

    private fun <T> Schema<T>.typeName(packageName: String): TypeName {
        val nullable = nullable == true
        if (!`$ref`.isNullOrBlank()) {
            //ref不为空 判断引用是否包含”#/components/schemas/“
            if (`$ref`.contains("#/components/schemas/")) {
                //返回该包的引用类型
                return ClassName(
                    packageName, `$ref`.substringAfter("#/components/schemas/")
                        .toFirstUpperCase()
                ).copy(nullable = nullable)
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
            if (format?.lowercase(Locale.getDefault()) == "int64") String::class.asTypeName() else Int::class.asTypeName()
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
        split(*delimiters).filterNot { it.isBlank() }.mapIndexed { index, s ->
            s.toFirstUpperCase()
        }.joinToString()

    companion object {
        private val baseControllerType = ClassName("com.tangqiu.cloud.ktor.openapi", "Controller")
        private val controllerFactoryType = ClassName("com.tangqiu.cloud.ktor.openapi", "Controller", "Factory")
        private val routeType = ClassName("io.ktor.routing", "Route")
        private val attributeKeyType = ClassName("io.ktor.util", "AttributeKey")
        private val applicationType = ClassName("io.ktor.application", "Application")
        private val callType = ClassName("io.ktor.application", "ApplicationCall")
    }
}
