# ktor框架 消息队列
ktor框架 消息队列封装库

## 安装

### Gradle

1.项目目录下build.gradle，添加:

```groovy
implementation 'com.tangqiu.cloud:ktor_mq:1.0.0-SNAPSHOT'
```

2.在项目Application.kt文件内添加

```kotlin
//MQ
install(MQFeature) {
    exchange = "tangqiu-exchange"  //交换机
    exchangeType = "topic"   //交换机类型
    queue = "tangqiu-queue"  //队列
    routingKey = "tangqiu"  //路由键

    //rabbit连接配置与连接池配置
    uri = "amqp://guest:guest@localhost:5672"
    //序列化
    serialize { jacksonObjectMapper().writeValueAsBytes(it) }
    //反序列化
    deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }
    //初始化 交换机队列
    initialize {
        exchangeDeclare(exchange, exchangeType, true)
        queueDeclare(queue, true, false, false, emptyMap())
        queueBind(queue, exchange, "$routingKey.#")
    }
}
```

## 使用
```kotlin
配合实体类使用
data class Messages(val messages: String) : Serializable
```
```kotlin
routing {
    //发送消息
    get("/sendMessage") {
        val messages = Messages("发送MQ消息!")
        publish(mq.exchange, "$mq.routingKey.mq", body = messages)
        call.respond(mapOf("发送" to "成功"))
    }
    //消费消息
    mqConsumer {
        consume<Messages>(mq.queue) { consumerTag, body ->
            println("消费消息：--> 内容：$body, 消费Tag: $consumerTag")
        }
    }
}
```
