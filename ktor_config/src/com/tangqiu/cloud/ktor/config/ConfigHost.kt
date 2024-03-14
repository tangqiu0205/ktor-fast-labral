package com.tangqiu.cloud.ktor.config

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.hocon
import com.uchuhimo.konf.source.xml
import com.uchuhimo.konf.source.yaml

const val CONFIG_HOST_GIT = "git"
const val CONFIG_HOST_URL = "url"
const val CONFIG_HOST_LOCAL = "local"
const val CONFIG_HOST_NACOS = "nacos"

interface ConfigHost<C : ConfigHost.ConfigHostProfile> {
    val profile: C
    fun read(): Source
    fun listen(block: (Source) -> Unit)

    interface ConfigHostProfile {
        var type: ConfigFeature.ConfigType
    }
}

val ConfigHost.ConfigHostProfile.loader
    get() = when (type) {
        ConfigFeature.ConfigType.CONF -> Source.from.hocon
        ConfigFeature.ConfigType.YAML -> Source.from.yaml
        ConfigFeature.ConfigType.JSON -> Source.from.json
        ConfigFeature.ConfigType.PROPERTIES -> Source.from.properties
        ConfigFeature.ConfigType.XML -> Source.from.xml
    }