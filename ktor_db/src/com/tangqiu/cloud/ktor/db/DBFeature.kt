package com.tangqiu.cloud.ktor.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.ktorm.database.Database
import org.ktorm.dsl.desc
import org.ktorm.entity.add
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.sortedBy
import org.ktorm.global.connectGlobally
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.SqlType
import java.io.Closeable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * 数据库功能
 */
class DBFeature(private val config: Configuration) : Closeable {
    //连接池配置
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        driverClassName = config.driverClassName
        username = config.username
        password = config.password
        maximumPoolSize = config.maximumPoolSize
        minimumIdle = config.minIdle
        maxLifetime = TimeUnit.MINUTES.toMillis(config.maxLifetime) //最大生命时间
    }

    //数据库配置
    data class Configuration(
        var version: Int = 1,
        var jdbcUrl: String = "jdbc:mariadb://localhost:3306/myDB?characterEncoding=utf-8",
        var driverClassName: String = "org.mariadb.jdbc.Driver",
        var username: String = "xxx",
        var password: String = "xxx",
        var maximumPoolSize: Int = 10,  //最大连接池
        var minIdle: Int = 10, //空闲连接池
        var maxLifetime: Long = 25, //最大生命时间分钟
        var loggerLevel: String = "INFO" //输出sql日志级别 TRACE DEBUG INFO WARN ERROR
    ) {
        internal val migrators = mutableMapOf<Int, Migrator>()
        internal var versionProvider: suspend (database: Database) -> Int = { database ->
            database.useTransaction {
                it.connection.prepareStatement(
                    """
                    create table if not exists database_version
                    (
                        version int comment '数据库版本',
                        migrate_time datetime comment '升级时间'
                    ) comment '数据库版本表';
                """.trimIndent()
                ).execute()
            }
            database.sequenceOf(DatabaseVersions).sortedBy { it.version.desc() }.firstOrNull()?.version ?: 0
        }

        internal var versionConsumer: suspend (database: Database, version: Int) -> Unit = { database, version ->
            database.sequenceOf(DatabaseVersions).add(DatabaseVersion {
                this.version = version
                this.migrateTime = LocalDateTime.now()
            })
        }

        fun provideVersion(provider: suspend (database: Database) -> Int) {
            versionProvider = provider
        }

        fun consumeVersion(consumer: suspend (database: Database, version: Int) -> Unit) {
            versionConsumer = consumer
        }

        fun migrate(version: Int, block: suspend (database: Database, version: Int) -> Boolean) {
            migrators[version] = object : Migrator {
                override suspend fun migrate(database: Database, version: Int): Boolean {
                    return block.invoke(database, version)
                }
            }
        }

        fun migrate(version: Int, migrator: Migrator) {
            migrators[version] = migrator
        }

        fun setSerializer(serializer: ListSerializer) {
            ListType.setSerializer(serializer)
        }
    }

    //数据库连接source
    private val databaseSource = HikariDataSource(hikariConfig)

    //数据库连接
    val dbConnect = Database.connectGlobally(
        databaseSource,
        logger = ConsoleLogger(threshold = LogLevel.valueOf(config.loggerLevel))
    )
//  val dbConnect =  Database.connect(databaseSource, logger = ConsoleLogger(threshold = LogLevel.valueOf(config.loggerLevel)))

    fun setup(): DBFeature {
        runBlocking(Dispatchers.IO) {
            val currentVersion = config.versionProvider.invoke(dbConnect)
            val migrateVersions = config.migrators.keys.filter {
                it > currentVersion && it <= config.version
            }.sorted()
            for (v in migrateVersions) {
                if (config.migrators[v]?.migrate(dbConnect, v) != true) {
                    throw RuntimeException("Database migrate failed, version: ${config.version}")
                } else {
                    config.versionConsumer.invoke(dbConnect, v)
                }
            }
        }
        return this
    }

    override fun close() {
        //关闭连接
        databaseSource.close()
    }

    companion object Feature : ApplicationFeature<Application, Configuration, DBFeature> {
        override val key = AttributeKey<DBFeature>("DBFeatureKey")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): DBFeature {
            return DBFeature(Configuration().apply(configure)).setup()
        }
    }
}

val Application.DB get() = feature(DBFeature).dbConnect

interface Migrator {
    suspend fun migrate(database: Database, version: Int): Boolean
}

class ListType<T : Any> : SqlType<List<T>>(Types.VARCHAR, "varchar") {
    /**
     * Obtain a result from a given [ResultSet] by [index], the result may be null.
     */
    override fun doGetResult(rs: ResultSet, index: Int): List<T>? {
        val json = rs.getString(index)
        return serializer.decode(json)
    }

    /**
     * Set the [parameter] to a given [PreparedStatement], the parameter can't be null.
     */
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: List<T>) {
        val json = serializer.encode(parameter)
        ps.setString(index, json)
    }

    companion object {
        private val pool = mutableMapOf<KClass<*>, ListType<*>>()

        private lateinit var serializer: ListSerializer

        fun setSerializer(s: ListSerializer) {
            serializer = s
        }

        fun <T : Any> obtain(clazz: KClass<T>): ListType<T> = pool.getOrPut(clazz) {
            ListType<T>()
        } as ListType<T>

        inline fun <reified T : Any> obtain(): ListType<T> = obtain(T::class)
    }
}

interface ListSerializer {
    fun <T : Any> encode(list: List<T>): String
    fun <T : Any> decode(json: String): List<T>?
}
