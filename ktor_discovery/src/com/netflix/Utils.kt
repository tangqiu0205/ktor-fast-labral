package com.netflix

import com.netflix.config.AggregatedConfiguration
import com.netflix.config.ConcurrentMapConfiguration
import com.netflix.config.ConfigurationManager
import com.netflix.config.DynamicPropertyFactory
import com.netflix.config.util.ConfigurationUtils
import java.util.*

internal fun initConfig(properties: Properties): DynamicPropertyFactory = ConfigurationManager.getConfigInstance().run {
    if (this is AggregatedConfiguration) {
        val config = ConcurrentMapConfiguration()
        config.loadProperties(properties)
        addConfiguration(config, "discovery")
    } else {
        ConfigurationUtils.loadProperties(properties, this)
    }
    println(this.getString("eureka.name"))
    DynamicPropertyFactory.initWithConfigurationSource(this)
}