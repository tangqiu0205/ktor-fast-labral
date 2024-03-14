package com.netflix.discovery

import com.netflix.appinfo.EurekaAccept
import com.netflix.config.DynamicPropertyFactory
import com.netflix.discovery.shared.transport.DefaultEurekaTransportConfig
import com.netflix.discovery.shared.transport.EurekaTransportConfig
import java.util.*

class DiscoveryClientConfig @JvmOverloads
constructor(
    private val configInstance: DynamicPropertyFactory,
    namespace: String = CommonConstants.DEFAULT_CONFIG_NAMESPACE
) :
    EurekaClientConfig {
    private val namespace: String = if (namespace.endsWith(".")) namespace else "$namespace."
    private val transportConfig: EurekaTransportConfig = DefaultEurekaTransportConfig(namespace, configInstance)

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#getRegistryFetchIntervalSeconds
     * ()
     */
    override fun getRegistryFetchIntervalSeconds(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.REGISTRY_REFRESH_INTERVAL_KEY, 30
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#
     * getInstanceInfoReplicationIntervalSeconds()
     */
    override fun getInstanceInfoReplicationIntervalSeconds(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.REGISTRATION_REPLICATION_INTERVAL_KEY, 30
        ).get()
    }

    override fun getInitialInstanceInfoReplicationIntervalSeconds(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.INITIAL_REGISTRATION_REPLICATION_DELAY_KEY, 40
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getDnsPollIntervalSeconds()
     */
    override fun getEurekaServiceUrlPollIntervalSeconds(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_URL_POLL_INTERVAL_KEY, 5 * 60 * 1000
        ).get() / 1000
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getProxyHost()
     */
    override fun getProxyHost(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_PROXY_HOST_KEY, null
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getProxyPort()
     */
    override fun getProxyPort(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_PROXY_PORT_KEY, null
        ).get()
    }

    override fun getProxyUserName(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_PROXY_USERNAME_KEY, null
        ).get()
    }

    override fun getProxyPassword(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_PROXY_PASSWORD_KEY, null
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#shouldGZipContent()
     */
    override fun shouldGZipContent(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_GZIP_CONTENT_KEY, true
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getDSServerReadTimeout()
     */
    override fun getEurekaServerReadTimeoutSeconds(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_READ_TIMEOUT_KEY, 8
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getDSServerConnectTimeout()
     */
    override fun getEurekaServerConnectTimeoutSeconds(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_CONNECT_TIMEOUT_KEY, 5
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getBackupRegistryImpl()
     */
    override fun getBackupRegistryImpl(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.BACKUP_REGISTRY_CLASSNAME_KEY,
            null
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#getDSServerTotalMaxConnections()
     */
    override fun getEurekaServerTotalConnections(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_MAX_CONNECTIONS_KEY, 200
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#getDSServerConnectionsPerHost()
     */
    override fun getEurekaServerTotalConnectionsPerHost(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_MAX_CONNECTIONS_PER_HOST_KEY, 50
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getDSServerURLContext()
     */
    override fun getEurekaServerURLContext(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_URL_CONTEXT_KEY,
            configInstance.getStringProperty(
                namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_FALLBACK_URL_CONTEXT_KEY,
                null
            )
                .get()
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getDSServerPort()
     */
    override fun getEurekaServerPort(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_PORT_KEY,
            configInstance.getStringProperty(
                namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_FALLBACK_PORT_KEY,
                null
            )
                .get()
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getDSServerDomain()
     */
    override fun getEurekaServerDNSName(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_DNS_NAME_KEY,
            configInstance
                .getStringProperty(
                    namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_FALLBACK_DNS_NAME_KEY,
                    null
                )
                .get()
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#shouldUseDns()
     */
    override fun shouldUseDnsForFetchingServiceUrls(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_USE_DNS_KEY,
            false
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#getDiscoveryRegistrationEnabled()
     */
    override fun shouldRegisterWithEureka(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.REGISTRATION_ENABLED_KEY, true
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#shouldUnregisterOnShutdown()
     */
    override fun shouldUnregisterOnShutdown(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_UNREGISTER_ON_SHUTDOWN_KEY, true
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#shouldPreferSameZoneDS()
     */
    override fun shouldPreferSameZoneEureka(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_PREFER_SAME_ZONE_SERVER_KEY,
            true
        ).get()
    }

    override fun allowRedirects(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_ALLOW_REDIRECTS_KEY,
            false
        ).get()
    }

    /*
         * (non-Javadoc)
         *
         * @see com.netflix.discovery.EurekaClientConfig#shouldLogDeltaDiff()
         */
    override fun shouldLogDeltaDiff(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_LOG_DELTA_DIFF_KEY, false
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#shouldDisableDelta()
     */
    override fun shouldDisableDelta(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_DISABLE_DELTA_KEY,
            false
        ).get()
    }

    override fun fetchRegistryForRemoteRegions(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_FETCH_REMOTE_REGION_KEY,
            null
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getRegion()
     */
    override fun getRegion(): String {
        val defaultEurekaRegion = configInstance.getStringProperty(
            PropertyBasedClientConfigConstants.CLIENT_REGION_FALLBACK_KEY,
            PropertyBasedClientConfigConstants.Values.DEFAULT_CLIENT_REGION
        )
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CLIENT_REGION_KEY,
            defaultEurekaRegion.get()
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getAvailabilityZones()
     */
    override fun getAvailabilityZones(region: String): Array<String> {
        return configInstance
            .getStringProperty(
                namespace + region + "." + PropertyBasedClientConfigConstants.CONFIG_AVAILABILITY_ZONE_PREFIX,
                DEFAULT_ZONE
            ).get().split(URL_SEPARATOR.toRegex()).toTypedArray()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#getEurekaServerServiceUrls()
     */
    override fun getEurekaServerServiceUrls(myZone: String): List<String> {
        var serviceUrls = configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CONFIG_EUREKA_SERVER_SERVICE_URL_PREFIX + "." + myZone, null
        ).get()
        if (serviceUrls == null || serviceUrls.isEmpty()) {
            serviceUrls = configInstance.getStringProperty(
                namespace + PropertyBasedClientConfigConstants.CONFIG_EUREKA_SERVER_SERVICE_URL_PREFIX + ".default",
                null
            ).get()
        }
        return if (serviceUrls != null) {
            Arrays.asList(*serviceUrls.split(URL_SEPARATOR.toRegex()).toTypedArray())
        } else ArrayList()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#shouldFilterOnlyUpInstances()
     */
    override fun shouldFilterOnlyUpInstances(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_FILTER_ONLY_UP_INSTANCES_KEY, true
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.discovery.EurekaClientConfig#getEurekaConnectionIdleTimeout()
     */
    override fun getEurekaConnectionIdleTimeoutSeconds(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.EUREKA_SERVER_CONNECTION_IDLE_TIMEOUT_KEY, 45
        )
            .get()
    }

    override fun shouldFetchRegistry(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.FETCH_REGISTRY_ENABLED_KEY, true
        ).get()
    }

    override fun shouldEnforceFetchRegistryAtInit(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_ENFORCE_FETCH_REGISTRY_AT_INIT_KEY, false
        ).get()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig#getRegistryRefreshSingleVipAddress()
     */
    override fun getRegistryRefreshSingleVipAddress(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.FETCH_SINGLE_VIP_ONLY_KEY, null
        ).get()
    }

    /**
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig.getHeartbeatExecutorThreadPoolSize
     */
    override fun getHeartbeatExecutorThreadPoolSize(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.HEARTBEAT_THREADPOOL_SIZE_KEY,
            PropertyBasedClientConfigConstants.Values.DEFAULT_EXECUTOR_THREAD_POOL_SIZE
        ).get()
    }

    override fun getHeartbeatExecutorExponentialBackOffBound(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.HEARTBEAT_BACKOFF_BOUND_KEY,
            PropertyBasedClientConfigConstants.Values.DEFAULT_EXECUTOR_THREAD_POOL_BACKOFF_BOUND
        ).get()
    }

    /**
     * (non-Javadoc)
     *
     * @see com.netflix.discovery.EurekaClientConfig.getCacheRefreshExecutorThreadPoolSize
     */
    override fun getCacheRefreshExecutorThreadPoolSize(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.CACHEREFRESH_THREADPOOL_SIZE_KEY,
            PropertyBasedClientConfigConstants.Values.DEFAULT_EXECUTOR_THREAD_POOL_SIZE
        ).get()
    }

    override fun getCacheRefreshExecutorExponentialBackOffBound(): Int {
        return configInstance.getIntProperty(
            namespace + PropertyBasedClientConfigConstants.CACHEREFRESH_BACKOFF_BOUND_KEY,
            PropertyBasedClientConfigConstants.Values.DEFAULT_EXECUTOR_THREAD_POOL_BACKOFF_BOUND
        ).get()
    }

    override fun getDollarReplacement(): String {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CONFIG_DOLLAR_REPLACEMENT_KEY,
            PropertyBasedClientConfigConstants.Values.CONFIG_DOLLAR_REPLACEMENT
        ).get()
    }

    override fun getEscapeCharReplacement(): String {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CONFIG_ESCAPE_CHAR_REPLACEMENT_KEY,
            PropertyBasedClientConfigConstants.Values.CONFIG_ESCAPE_CHAR_REPLACEMENT
        ).get()
    }

    override fun shouldOnDemandUpdateStatusChange(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_ONDEMAND_UPDATE_STATUS_KEY, true
        ).get()
    }

    override fun shouldEnforceRegistrationAtInit(): Boolean {
        return configInstance.getBooleanProperty(
            namespace + PropertyBasedClientConfigConstants.SHOULD_ENFORCE_REGISTRATION_AT_INIT, false
        ).get()
    }

    override fun getEncoderName(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CLIENT_ENCODER_NAME_KEY, null
        ).get()
    }

    override fun getDecoderName(): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CLIENT_DECODER_NAME_KEY, null
        ).get()
    }

    override fun getClientDataAccept(): String {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CLIENT_DATA_ACCEPT_KEY, EurekaAccept.full.name
        ).get()
    }

    override fun getExperimental(name: String): String? {
        return configInstance.getStringProperty(
            namespace + PropertyBasedClientConfigConstants.CONFIG_EXPERIMENTAL_PREFIX + "." + name,
            null
        ).get()
    }

    override fun getTransportConfig(): EurekaTransportConfig {
        return transportConfig
    }

    companion object {
        const val DEFAULT_ZONE = "defaultZone"
        const val URL_SEPARATOR = "\\s*,\\s*"
    }
}
