package com.tangqiu.cloud.ktor.mq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import io.ktor.application.*
import io.ktor.util.*
import org.apache.commons.pool2.impl.GenericObjectPoolConfig

class MQFeature(
    val configuration: MQConfiguration
) {

    private val connectionFactory = ConnectionFactory()
    private val poolConfig: GenericObjectPoolConfig<Channel> = GenericObjectPoolConfig()

    init {
        //rabbitMQ配置
        connectionFactory.setUri(configuration.uri)

        //连接池配置
        poolConfig.jmxEnabled = configuration.jmxEnabled
        poolConfig.blockWhenExhausted = configuration.blockWhenExhausted
        poolConfig.maxWaitMillis = configuration.maxWaitMillis
        poolConfig.testOnBorrow = configuration.testOnBorrow
        poolConfig.testOnReturn = configuration.testOnReturn
        poolConfig.maxTotal = configuration.maxTotal
        poolConfig.maxIdle = configuration.maxIdle
        poolConfig.minIdle = configuration.minIdle
        poolConfig.timeBetweenEvictionRunsMillis = configuration.timeBetweenEvictionRunsMillis
        poolConfig.softMinEvictableIdleTimeMillis = configuration.softMinEvictableIdleTimeMillis
    }

    //pool2-factory配置
    private val poolFactory = MQChannelPoolFactory(connectionFactory)

    //channel池配置
    private val channelPool = MQChannelPool(poolFactory, poolConfig)

    //获取通道
    private val channel = channelPool.getChannel()

    private fun initialize() {
        configuration.initializeBlock.invoke(channel)
    }

    fun withChannel(block: Channel.() -> Unit) {
        block.invoke(channel)
    }

    fun <T> publish(exchange: String, routingKey: String, props: AMQP.BasicProperties?, body: T) {
        withChannel {
            val bytes = serialize(body)
            basicPublish(exchange, routingKey, props, bytes)
        }
    }

    inline fun <reified T> deserialize(bytes: ByteArray): T =
        configuration.deserializeBlock.invoke(bytes, T::class) as T

    private fun <T> serialize(body: T): ByteArray = configuration.serializeBlock.invoke(body!!)

    companion object Feature : ApplicationFeature<Application, MQConfiguration, MQFeature> {
        override val key = AttributeKey<MQFeature>("MQFeatureKey")

        override fun install(
            pipeline: Application,
            configure: MQConfiguration.() -> Unit
        ): MQFeature {
            val configuration = MQConfiguration.create()
            configuration.apply(configure)

            val rabbit = MQFeature(configuration).apply {
                initialize()
            }
            pipeline.attributes.put(key, rabbit)
            return rabbit
        }
    }
}

val Application.mq get() = MQConfiguration.create()