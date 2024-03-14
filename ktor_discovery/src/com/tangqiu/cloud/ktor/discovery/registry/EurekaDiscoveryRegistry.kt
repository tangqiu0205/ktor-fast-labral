package com.tangqiu.cloud.ktor.discovery.registry

import com.alibaba.nacos.api.naming.pojo.Instance
import com.tangqiu.cloud.ktor.discovery.DiscoveryHttpClientFeature
import com.tangqiu.cloud.ktor.discovery.DiscoveryRegistry
import com.tangqiu.cloud.ktor.discovery.ServerNode
import com.tangqiu.cloud.ktor.discovery.flatInto
import com.netflix.appinfo.ApplicationInfoManager
import com.netflix.appinfo.DiscoveryServerConfig
import com.netflix.appinfo.InstanceInfo
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider
import com.netflix.discovery.DiscoveryClient
import com.netflix.discovery.DiscoveryClientConfig
import com.netflix.discovery.EurekaClient
import com.netflix.discovery.StatusChangeEvent
import com.netflix.initConfig
import com.netflix.loadbalancer.BaseLoadBalancer
import com.netflix.loadbalancer.LoadBalancerBuilder
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object EurekaDiscoveryRegistry : DiscoveryRegistry, ApplicationInfoManager.StatusChangeListener {
    private lateinit var eurekaClient: EurekaClient

    //应用程序管理器
    private lateinit var applicationInfoManager: ApplicationInfoManager

    private lateinit var key: UUID

    private val loadBalancerMap by lazy {
        ConcurrentHashMap<String, BaseLoadBalancer>()
    }
    override val healthyNodes: List<ServerNode>
        get() = emptyList()

    override val instanceIndex: Int
        get() = 0

    //负载均衡
    private fun loadBalancer(vipAddress: String): BaseLoadBalancer = loadBalancerMap.getOrPut(vipAddress) {
        val dyList = DiscoveryEnabledNIWSServerList(vipAddress) { eurekaClient }
        LoadBalancerBuilder.newBuilder<DiscoveryEnabledServer>()
            .withDynamicServerList(dyList)
            .buildDynamicServerListLoadBalancer()
    }

    override fun startUp(properties: Properties) {
        key = UUID.randomUUID()
        val dynamicConfig = initConfig(properties)
        val instanceConfig = DiscoveryServerConfig(dynamicConfig)
        val instanceInfo = EurekaConfigBasedInstanceInfoProvider(instanceConfig).get()
        applicationInfoManager = ApplicationInfoManager(instanceConfig, instanceInfo)
        //注册监听状态改变
        applicationInfoManager.registerStatusChangeListener(this)
        //设置状态
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP)
        eurekaClient = DiscoveryClient(applicationInfoManager, DiscoveryClientConfig(dynamicConfig))
    }

    override fun shutDown() {
        eurekaClient.shutdown() //关闭eureka客户端
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN) //服务下线
        applicationInfoManager.unregisterStatusChangeListener(id) //取消注册监听状态改变
    }

    override fun selectNode(vipAddress: String): ServerNode? {
        val server = loadBalancer(vipAddress).chooseServer(key) ?: return null
        return ServerNode(server.host, server.port)
    }


    override fun getId(): String = key.toString()

    override fun notify(statusChangeEvent: StatusChangeEvent?) {
        println("discovery status changed: ${statusChangeEvent?.previousStatus} -> ${statusChangeEvent?.status}")
    }

    class EurekaConfig : DiscoveryRegistry.RegistryConfig {
        private val map: Properties = Properties().apply {
            put("eureka", Properties().apply {
                put("registration", Properties())
                put("serviceUrl", Properties())
            })
        }
        private val eureka: Properties by map

        var region: String by eureka
        var name: String by eureka
        var vipAddress: String by eureka
        var port: Int by eureka
        var preferSameZone: Boolean by eureka
        var shouldUseDns: Boolean by eureka
        var shouldUnregisterOnShutdown: Boolean by eureka
        var shouldEnforceRegistrationAtInit: Boolean by eureka

        val registration: Properties by eureka
        private var enabled: Boolean by registration
        var shouldRegisterAtInit: Boolean
            get() = enabled
            set(value) {
                enabled = value
            }

        val serviceUrl: Properties by eureka
        private var default: Array<String> by serviceUrl

        init {
            region = "default"
            preferSameZone = true
            shouldUseDns = false
            shouldRegisterAtInit = true
            shouldEnforceRegistrationAtInit = true
            shouldUnregisterOnShutdown = true
        }

        override fun applyRegistries(vararg registries: URL) {
            default = registries.map { it.toExternalForm() }.toTypedArray()
        }

        override fun fillTo(properties: Properties) {
            map.flatInto(properties)
        }
    }
}