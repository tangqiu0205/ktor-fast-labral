package com.tangqiu.cloud.ktor.discovery

import com.alibaba.nacos.api.naming.pojo.Instance
import com.tangqiu.cloud.ktor.discovery.utils.IpUtils
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import java.net.URL
import java.util.*

data class ServerNode(
    val host: String,
    val port: Int?
)

const val DISCOVERY_REGISTRY_ERUREKA = "eureka"
const val DISCOVERY_REGISTRY_NACOS = "nacos"

interface DiscoveryRegistry {
    //获取健康的节点
    val healthyNodes: List<ServerNode>

    //拿到节点在list中的位置
    val instanceIndex: Int

    //启动
    fun startUp(properties: Properties)
    fun shutDown()

    //选择一个服务节点
    fun selectNode(vipAddress: String): ServerNode?

    interface RegistryConfig {
        fun applyRegistries(vararg registries: URL)
        fun fillTo(properties: Properties)
    }
}