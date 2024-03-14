package com.tangqiu.cloud.ktor.mongo

import io.ktor.application.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.commitTransactionAndAwait
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.io.Closeable

/**
 * 数据库功能
 */
class MongoFeature(config: Configuration) : Closeable {
    //数据库配置
    data class Configuration(
        //mongodb://db0.example.com,db1.example.com,db2.example.com/?replicaSet=myRepl&w=majority&wtimeoutMS=5000
//        var connectionString: String = "mongodb://admin:123456@localhost/",
        var userName: String = "xxx",
        var password: String = "xxx",
        var host: String = "xxx",
        var port: Int = 27017,
        var database: String = "xxxx",
        var url: String? = null,
        var installer: suspend CoroutineDatabase.() -> Unit = {}
    ) {
        fun install(block: suspend CoroutineDatabase.() -> Unit) {
            installer = block
        }
    }

    internal val coroutineClient = KMongo.createClient(
        config.url ?: "mongodb://${config.userName}:${config.password}@${config.host}:${config.port}/"
    ).coroutine //use coroutine extension

    val coroutineDatabase = coroutineClient.getDatabase(config.database)

    init {
        runBlocking(Dispatchers.IO) {
            config.installer.invoke(coroutineDatabase)
        }
    }

    override fun close() {
        //关闭连接
        coroutineClient.close()
    }

    companion object Feature : ApplicationFeature<Application, Configuration, MongoFeature> {
        override val key = AttributeKey<MongoFeature>("MongoFeatureKey")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): MongoFeature {
            return MongoFeature(Configuration().apply(configure))
        }
    }
}

inline fun <reified T : Any> Application.mongo() = feature(MongoFeature).coroutineDatabase.getCollection<T>()
suspend fun <T> Application.useTransaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
    feature(MongoFeature).coroutineClient.startSession().use {
        it.startTransaction()
        val res = block.invoke()
        it.commitTransactionAndAwait()
        res
    }
}
