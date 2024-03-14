package com.tangqiu.cloud.ktor.discovery.registry

import com.alibaba.nacos.api.naming.NamingFactory
import com.alibaba.nacos.api.naming.NamingService
import com.alibaba.nacos.api.naming.pojo.Instance
import com.tangqiu.cloud.ktor.discovery.DiscoveryRegistry
import com.tangqiu.cloud.ktor.discovery.ServerNode
import com.tangqiu.cloud.ktor.discovery.utils.IpUtils
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import java.net.URL
import java.util.*

object NacosDiscoveryRegistry : DiscoveryRegistry {
    private lateinit var namingService: NamingService
    private lateinit var config: Properties

    override val healthyNodes: List<ServerNode>
        get() {
            val name: String by config
            val selectInstances = namingService.selectInstances(name, true)
            val list = mutableListOf<ServerNode>()
            selectInstances.forEach { instance ->
                list.add(ServerNode(instance.ip, instance.port))
            }
            return list
        }

    override val instanceIndex: Int
        get() {
            val host: String by config
            val port: Int by config
            return healthyNodes.indexOf(ServerNode(host, port))
        }

    override fun startUp(properties: Properties) {
        val nacos: Properties by properties
        config = nacos
        val name: String by nacos
        val groupName: String? by nacos
        val serverList: String by nacos
        val host: String by nacos
        val port: Int by nacos
        val metadata: Map<String, String>? by nacos
        namingService = NamingFactory.createNamingService(serverList)
        val instance = Instance().apply {
            serviceName = name
            ip = host
            setPort(port)
            this.metadata = metadata
        }
        if (groupName.isNullOrBlank()) {
            namingService.registerInstance(name, instance)
        } else {
            namingService.registerInstance(name, groupName, instance)
        }
    }

    override fun shutDown() {
        val name: String by config
        val groupName: String? by config
        val host: String by config
        val port: Int by config
        val metadata: Map<String, String>? by config
        val instance = Instance().apply {
            serviceName = name
            ip = host
            setPort(port)
            this.metadata = metadata
        }
        if (groupName.isNullOrBlank()) {
            namingService.deregisterInstance(name, instance)
        } else {
            namingService.deregisterInstance(name, groupName, instance)
        }
    }

    override fun selectNode(vipAddress: String): ServerNode? {
        val instance = namingService.selectOneHealthyInstance(vipAddress) ?: return null
        return ServerNode(instance.ip, instance.port)
    }

    class NacosConfig : DiscoveryRegistry.RegistryConfig {
        private val config = HoconApplicationConfig(ConfigFactory.load())
        private val ktorConfig = config.config("ktor")

        //        private val discoveryConfig = config.config("discovery")
        private val map: Properties = Properties().apply {
            put("nacos", Properties().apply {
                put("name", "")
                put("serverList", "")
                put("host", IpUtils.getIpAddress())
                put("port", ktorConfig.propertyOrNull("deployment.port")?.getString()?.toInt() ?: 8080)
//                put("metadata", discoveryConfig.propertyOrNull("metadata") ?: mapOf("kind" to "http"))
                put("metadata", mapOf("kind" to "http"))
            })
        }
        private val nacos: Properties by map
        var name: String by nacos
        var groupName: String? by nacos
        var serverList: String by nacos
        var host: String by nacos
        var port: Int by nacos

        override fun applyRegistries(vararg registries: URL) {
            serverList = registries.joinToString(",") {
                "${it.host}:${it.port}"
            }
        }

        override fun fillTo(properties: Properties) {
            properties.putAll(map)
        }
    }
}