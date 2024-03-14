package com.tangqiu.cloud.ktor.config.hosts

import com.tangqiu.cloud.ktor.config.ConfigFeature
import com.tangqiu.cloud.ktor.config.ConfigHost
import com.tangqiu.cloud.ktor.config.loader
import com.uchuhimo.konf.source.Source

object LocalConfigHost : ConfigHost<LocalConfigHost.LocalConfigProfile> {
    override val profile = LocalConfigProfile()

    override fun read(): Source = profile.loader.resource(profile.filePath)

    override fun listen(block: (Source) -> Unit) {}

    class LocalConfigProfile : ConfigHost.ConfigHostProfile {
        override var type: ConfigFeature.ConfigType = ConfigFeature.ConfigType.CONF
        var filePath: String = "" // 本地文件路径 /resources/application.conf
    }
}