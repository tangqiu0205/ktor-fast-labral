package com.tangqiu.cloud.ktor.config

import com.tangqiu.cloud.ktor.config.hosts.GitConfigHost
import com.tangqiu.cloud.ktor.config.hosts.LocalConfigHost
import com.tangqiu.cloud.ktor.config.hosts.NacosConfigHost
import com.tangqiu.cloud.ktor.config.hosts.UrlConfigHost
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.*
import com.uchuhimo.konf.toValue
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.collections.*
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KProperty

/**
 * 配置中心功能
 */
class ConfigFeature(configuration: Configuration) {
    //初始化载入
    init {
        configuration.install()
        configuration.listen()
        if (configuration.refreshApi) {
            configuration.route()
        }
    }

    class Configuration(
        private val app: Application,
        var hostType: String = CONFIG_HOST_LOCAL
    ) {
        var refreshApi: Boolean = true  //是否可刷新

        var refreshPath: String = "/config/refresh"

        val callbacks = mutableListOf<ConfigEntity<*, *, *>>()
        val configNames = ConcurrentHashMap<ApplicationFeature<Application, *, *>, String>()

        private lateinit var host: ConfigHost<*>
        private var reinstallChecker: () -> Boolean = { true }

        fun <T : ConfigHost.ConfigHostProfile> hostProfile(block: T.() -> Unit) {
            host = when (hostType) {
                CONFIG_HOST_GIT -> GitConfigHost
                CONFIG_HOST_URL -> UrlConfigHost
                CONFIG_HOST_NACOS -> NacosConfigHost
                else -> LocalConfigHost
            }
            (host.profile as T).apply(block)
        }

        fun checkReinstall(block: () -> Boolean) {
            reinstallChecker = block
        }

        inline fun <A : ApplicationFeature<Application, B, F>, reified B : Any, F : Any> register(
            feature: A,
            configName: String = feature.key.name,
            noinline conf: B.() -> Unit = {},
            noinline callback: B.(Source) -> Unit = { config ->
                mergeFrom(config.toValue())
                apply(conf)
            }
        ) {
            callbacks.add(ConfigEntity(feature, callback))
            configNames.putIfAbsent(feature, configName)
        }

        inline fun <A : ApplicationFeature<Application, B, F>, reified B : Any, F : Any> register(
            feature: A,
            properties: KProperty<Properties>,
            configName: String = feature.key.name,
            noinline conf: B.() -> Unit = {}
        ) {
            val callback: B.(Source) -> Unit = {
                properties.getter.call(this).apply(it.toValue())
                apply(conf)
            }
            callbacks.add(ConfigEntity(feature, callback))
            configNames.putIfAbsent(feature, configName)
        }

        internal fun route() {
            app.routing {
                post(refreshPath) {
                    reinstall()
                }
            }
        }

        internal fun listen() {
            host.listen {
                reinstall(it)
            }
        }


        private fun reinstall(source: Source? = null) {
            ReinstallTask.obtain(reinstallChecker) {
                install(source)
            }
        }

        //更新配置后 重新安装Feature
        internal fun install(source: Source? = null) {

            val config = source ?: host.read()
            //逆向卸载配置
            callbacks.reversed().forEach { entity ->
                val featureInstance = app.runCatching { feature(entity.feature) }.getOrNull()
                if (featureInstance != null) {
                    app.uninstall(entity.feature) //卸载当前feature
                }
            }
            callbacks.forEach { entity ->
                //从configNames获取feature
                val configName = configNames[entity.feature]
                    ?: throw IllegalArgumentException("No available config for Feature: ${entity.feature.key.name}")
                val props = config[configName]
                runCatching {
                    app.install(entity.feature) { //安装当前feature
                        entity.config(this, props)
                    }
                }.onFailure {
                    it.printStackTrace()
//                app.dispose()
                }
            }

        }
    }

    class ConfigEntity<A : ApplicationFeature<Application, B, F>, B : Any, F : Any>(
        internal val feature: A,
        private val callback: B.(Source) -> Unit
    ) {
        fun config(configuration: Any, config: Source) {
            callback.invoke(configuration as B, config)
        }
    }

    companion object Feature : ApplicationFeature<Application, Configuration, ConfigFeature> {
        override val key = AttributeKey<ConfigFeature>("ConfigFeatureKey")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): ConfigFeature {
            val configuration = Configuration(pipeline).apply(configure)
            return ConfigFeature(configuration)
        }
    }

    enum class ConfigType {
        CONF, YAML, JSON, PROPERTIES, XML
    }
}


