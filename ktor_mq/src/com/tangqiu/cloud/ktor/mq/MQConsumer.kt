package com.tangqiu.cloud.ktor.mq

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import io.ktor.application.*
import io.ktor.util.pipeline.*

/**
 * 消费消息
 */
@ContextDsl
fun Application.mqConsumer(configuration: MQFeature.() -> Unit): MQFeature = feature(MQFeature).apply(configuration)

@ContextDsl
inline fun <reified T> MQFeature.consume(
    queue: String,
    autoAck: Boolean = true,
    crossinline mqDeliverCallback: (consumerTag: String, body: T) -> Unit
) {
    withChannel {
        basicConsume(queue, autoAck,
            DeliverCallback { consumerTag, message ->
                runCatching {
                    val mappedEntity = deserialize<T>(message.body)
                    mqDeliverCallback.invoke(consumerTag, mappedEntity)
                }.getOrElse {
                    it.printStackTrace()
                }
            },
            CancelCallback {
                println("Consume cancelled: $it")
            }
        )
    }
}
