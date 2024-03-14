package com.tangqiu.cloud.ktor.mq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.PooledObjectFactory
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import kotlin.reflect.KClass

/**
 * MQ配置
 */
class MQConfiguration private constructor() {
    //rabbitMQ连接配置
    var uri: String = "amqp://guest:guest@localhost:5672"

    //连接池配置
    var jmxEnabled: Boolean = true
    var blockWhenExhausted: Boolean = true
    var maxWaitMillis: Long = 5000
    var testOnBorrow: Boolean = true
    var testOnReturn: Boolean = true
    var maxTotal: Int = 100
    var maxIdle: Int = 20
    var minIdle: Int = 10
    var timeBetweenEvictionRunsMillis: Long = 6000
    var softMinEvictableIdleTimeMillis: Long = 20000

    internal lateinit var initializeBlock: (Channel.() -> Unit) //初始化代码块
    lateinit var serializeBlock: (Any) -> ByteArray  //序列化
    lateinit var deserializeBlock: (ByteArray, KClass<*>) -> Any //反序列化

    var exchange: String = "tangqiu-exchange"  //交换机
    var exchangeType: String = "topic"   //交换机类型
    var queue: String = "tangqiu-queue"  //队列
    var routingKey: String = "tangqiu"  //路由键

    /**
     * 创建交换机、队列
     */
    fun initialize(block: (Channel.() -> Unit)) {
        initializeBlock = block
    }

    /**
     * 消息序列化
     */
    fun serialize(block: (Any) -> ByteArray) {
        serializeBlock = block
    }

    /**
     * 消息反序列化
     */
    fun deserialize(block: (ByteArray, KClass<*>) -> Any) {
        deserializeBlock = block
    }

    companion object {
        fun create(): MQConfiguration {
            return MQConfiguration()
        }
    }
}

//mq通道池工厂
class MQChannelPoolFactory(var factory: ConnectionFactory) : PooledObjectFactory<Channel> {

    override fun makeObject(): PooledObject<Channel> {
        // 池对象创建实例化资源
        return DefaultPooledObject(factory.newConnection().createChannel())
    }

    override fun destroyObject(pool: PooledObject<Channel>?) {
        //连接池对象销毁
        if (pool != null && pool.`object` != null && pool.`object`.isOpen) {
            pool.`object`.close()
        }
    }

    override fun validateObject(p: PooledObject<Channel>?): Boolean {
        //验证资源
        return p?.`object` != null && p.`object`.isOpen
    }

    override fun activateObject(p: PooledObject<Channel>?) {
    }

    override fun passivateObject(p: PooledObject<Channel>?) {
    }
}

//channel池
class MQChannelPool(factory: MQChannelPoolFactory, poolConfig: GenericObjectPoolConfig<Channel>) {
    private var pool: GenericObjectPool<Channel> = GenericObjectPool(factory, poolConfig)

    //获取通道
    fun getChannel(): Channel {
        return pool.borrowObject()
    }

    //返回通道
    fun returnChannel(channel: Channel) {
        var returnObject = pool.returnObject(channel)
    }
}

