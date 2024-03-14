package com.tangqiu.cloud.ktor.config.hosts

import com.tangqiu.cloud.ktor.config.ConfigFeature
import com.tangqiu.cloud.ktor.config.ConfigHost
import com.tangqiu.cloud.ktor.config.loader
import com.uchuhimo.konf.source.Source

object UrlConfigHost : ConfigHost<UrlConfigHost.UrlConfigProfile> {
    override val profile = UrlConfigProfile()

    override fun read(): Source = profile.loader.url(profile.fileURL)

    override fun listen(block: (Source) -> Unit) {}

    class UrlConfigProfile : ConfigHost.ConfigHostProfile {
        override var type: ConfigFeature.ConfigType = ConfigFeature.ConfigType.CONF
        var fileURL: String = "" //通过url获取properties
    }
}