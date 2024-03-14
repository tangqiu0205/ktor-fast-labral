package com.tangqiu.cloud.ktor.config.hosts

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.config.listener.Listener
import com.tangqiu.cloud.ktor.config.ConfigFeature
import com.tangqiu.cloud.ktor.config.ConfigHost
import com.tangqiu.cloud.ktor.config.loader
import com.uchuhimo.konf.source.Source
import java.util.concurrent.Executor

object NacosConfigHost : ConfigHost<NacosConfigHost.NacosConfigProfile> {
    override val profile = NacosConfigProfile()

    private val configServer by lazy {
        NacosFactory.createConfigService(profile.nacosConfigServerAttr)
    }

    override fun read(): Source {
        val loader = profile.loader
        val content = runCatching {
            configServer.getConfig(profile.dataId, profile.group, profile.timeout)
        }.getOrThrow()
        return loader.string(content ?: "")
    }

    override fun listen(block: (Source) -> Unit) {
        configServer.addListener(profile.dataId, profile.group, object : Listener {
            override fun getExecutor(): Executor? = null
            override fun receiveConfigInfo(configInfo: String?) {
                block.invoke(profile.loader.string(configInfo ?: ""))

            }
        })
    }


    class NacosConfigProfile : ConfigHost.ConfigHostProfile {
        override var type: ConfigFeature.ConfigType = ConfigFeature.ConfigType.CONF
        lateinit var dataId: String
        lateinit var group: String
        lateinit var nacosConfigServerAttr: String
        var timeout: Long = 3000L
    }

    data class ServerNode(
        val host: String,
        val port: Int?,
        val service: String?
    )
}