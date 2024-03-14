package com.tangqiu.cloud.ktor.mq

import com.rabbitmq.client.AMQP
import io.ktor.application.*

/**
 * 发送消息
 */
fun <T> Application.publish(exchange: String, routingKey: String, props: AMQP.BasicProperties? = null, body: T) {
    feature(MQFeature).publish(exchange, routingKey, props, body)
}

