package com.netflix.appinfo

/*
* Copyright 2012 Netflix, Inc.
*
*    Licensed under the Apache License, Version 2.0 (the "License");
*    you may not use this file except in compliance with the License.
*    You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import com.netflix.config.ConfigurationManager
import com.netflix.config.DynamicPropertyFactory
import com.netflix.discovery.CommonConstants
import org.apache.commons.configuration.Configuration

/**
 * A properties based [InstanceInfo] configuration.
 *
 *
 *
 * The information required for registration with eureka server is provided in a
 * configuration file.The configuration file is searched for in the classpath
 * with the name specified by the property *eureka.client.props* and with
 * the suffix *.properties*. If the property is not specified,
 * *eureka-client.properties* is assumed as the default.The properties
 * that are looked up uses the *namespace* passed on to this class.
 *
 *
 *
 *
 * If the *eureka.environment* property is specified, additionally
 * *eureka-client-<eureka.environment>.properties</eureka.environment>* is loaded in addition
 * to *eureka-client.properties*.
 *
 *
 * @author Karthik Ranganathan
 */
class DiscoveryServerConfig @JvmOverloads constructor(
    private val configInstance: DynamicPropertyFactory,
    namespace: String = CommonConstants.DEFAULT_CONFIG_NAMESPACE,
    info: DataCenterInfo? = DataCenterInfo { DataCenterInfo.Name.MyOwn }
) : AbstractInstanceConfig(info), EurekaInstanceConfig {
    private val namespace: String = if (namespace.endsWith(".")) namespace else "$namespace."
    private val appGrpNameFromEnv: String = ConfigurationManager.getConfigInstance()
        .getString(
            PropertyBasedInstanceConfigConstants.FALLBACK_APP_GROUP_KEY,
            PropertyBasedInstanceConfigConstants.Values.UNKNOWN_APPLICATION
        )

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#isInstanceEnabledOnit()
     */
    override fun isInstanceEnabledOnit(): Boolean = configInstance.getBooleanProperty(
        namespace + PropertyBasedInstanceConfigConstants.TRAFFIC_ENABLED_ON_INIT_KEY,
        super.isInstanceEnabledOnit()
    ).get()

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#getNonSecurePort()
     */
    override fun getNonSecurePort(): Int = configInstance.getIntProperty(
        namespace + PropertyBasedInstanceConfigConstants.PORT_KEY,
        super.getNonSecurePort()
    ).get()

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#getSecurePort()
     */
    override fun getSecurePort(): Int = configInstance.getIntProperty(
        namespace + PropertyBasedInstanceConfigConstants.SECURE_PORT_KEY,
        super.getSecurePort()
    ).get()

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#isNonSecurePortEnabled()
     */
    override fun isNonSecurePortEnabled(): Boolean = configInstance.getBooleanProperty(
        namespace + PropertyBasedInstanceConfigConstants.PORT_ENABLED_KEY,
        super.isNonSecurePortEnabled()
    ).get()

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#getSecurePortEnabled()
     */
    override fun getSecurePortEnabled(): Boolean = configInstance.getBooleanProperty(
        namespace + PropertyBasedInstanceConfigConstants.SECURE_PORT_ENABLED_KEY,
        super.getSecurePortEnabled()
    ).get()

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.appinfo.AbstractInstanceConfig#getLeaseRenewalIntervalInSeconds
     * ()
     */
    override fun getLeaseRenewalIntervalInSeconds(): Int = configInstance.getIntProperty(
        namespace + PropertyBasedInstanceConfigConstants.LEASE_RENEWAL_INTERVAL_KEY,
        super.getLeaseRenewalIntervalInSeconds()
    ).get()

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#
     * getLeaseExpirationDurationInSeconds()
     */
    override fun getLeaseExpirationDurationInSeconds(): Int = configInstance.getIntProperty(
        namespace + PropertyBasedInstanceConfigConstants.LEASE_EXPIRATION_DURATION_KEY,
        super.getLeaseExpirationDurationInSeconds()
    ).get()

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#getVirtualHostName()
     */
    override fun getVirtualHostName(): String? = if (this.isNonSecurePortEnabled) {
        configInstance.getStringProperty(
            namespace + PropertyBasedInstanceConfigConstants.VIRTUAL_HOSTNAME_KEY,
            super.getVirtualHostName()
        ).get()
    } else {
        null
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.netflix.appinfo.AbstractInstanceConfig#getSecureVirtualHostName()
     */
    override fun getSecureVirtualHostName(): String? = if (this.securePortEnabled) {
        configInstance.getStringProperty(
            namespace + PropertyBasedInstanceConfigConstants.SECURE_VIRTUAL_HOSTNAME_KEY,
            super.getSecureVirtualHostName()
        ).get()
    } else {
        null
    }

    /*
     * (non-Javadoc)
     *
     * @see com.netflix.appinfo.AbstractInstanceConfig#getASGName()
     */
    override fun getASGName(): String? = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.ASG_NAME_KEY,
        super.getASGName()
    ).get()

    /**
     * Gets the metadata map associated with the instance. The properties that
     * will be looked up for this will be `namespace + ".metadata"`.
     *
     *
     *
     * For instance, if the given namespace is `eureka.appinfo`, the
     * metadata keys are searched under the namespace
     * `eureka.appinfo.metadata`.
     *
     */
    override fun getMetadataMap(): Map<String, String> {
        val metadataNamespace = namespace + PropertyBasedInstanceConfigConstants.INSTANCE_METADATA_PREFIX + "."
        val metadataMap: MutableMap<String, String> = LinkedHashMap()
        val config = DynamicPropertyFactory.getBackingConfigurationSource() as Configuration
        val subsetPrefix = if (metadataNamespace[metadataNamespace.length - 1] == '.') metadataNamespace.substring(
            0,
            metadataNamespace.length - 1
        ) else metadataNamespace
        val iter = config.subset(subsetPrefix).keys
        while (iter.hasNext()) {
            val key = iter.next()
            val value = config.getString("$subsetPrefix.$key")
            metadataMap[key] = value
        }
        return metadataMap
    }

    override fun getInstanceId(): String? {
        val result =
            configInstance.getStringProperty(namespace + PropertyBasedInstanceConfigConstants.INSTANCE_ID_KEY, null)
                .get()
        return result?.trim { it <= ' ' }
    }

    override fun getAppname(): String = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.APP_NAME_KEY,
        PropertyBasedInstanceConfigConstants.Values.UNKNOWN_APPLICATION
    ).get().trim { it <= ' ' }

    override fun getAppGroupName(): String = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.APP_GROUP_KEY,
        appGrpNameFromEnv
    ).get().trim { it <= ' ' }

    override fun getStatusPageUrlPath(): String = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.STATUS_PAGE_URL_PATH_KEY,
        PropertyBasedInstanceConfigConstants.Values.DEFAULT_STATUSPAGE_URLPATH
    ).get()

    override fun getStatusPageUrl(): String? = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.STATUS_PAGE_URL_KEY,
        null
    ).get()

    override fun getHomePageUrlPath(): String = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.HOME_PAGE_URL_PATH_KEY,
        PropertyBasedInstanceConfigConstants.Values.DEFAULT_HOMEPAGE_URLPATH
    ).get()

    override fun getHomePageUrl(): String? = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.HOME_PAGE_URL_KEY,
        null
    ).get()

    override fun getHealthCheckUrlPath(): String = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.HEALTHCHECK_URL_PATH_KEY,
        PropertyBasedInstanceConfigConstants.Values.DEFAULT_HEALTHCHECK_URLPATH
    ).get()

    override fun getHealthCheckUrl(): String? = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.HEALTHCHECK_URL_KEY,
        null
    ).get()

    override fun getSecureHealthCheckUrl(): String? = configInstance.getStringProperty(
        namespace + PropertyBasedInstanceConfigConstants.SECURE_HEALTHCHECK_URL_KEY,
        null
    ).get()

    override fun getDefaultAddressResolutionOrder(): Array<String?> {
        val result = configInstance.getStringProperty(
            namespace + PropertyBasedInstanceConfigConstants.DEFAULT_ADDRESS_RESOLUTION_ORDER_KEY,
            null
        ).get()
        return result?.split(",".toRegex())?.toTypedArray() ?: arrayOfNulls(0)
    }

    /**
     * Indicates if the public ipv4 address of the instance should be advertised.
     * @return true if the public ipv4 address of the instance should be advertised, false otherwise .
     */
    override fun shouldBroadcastPublicIpv4Addr(): Boolean = configInstance.getBooleanProperty(
        namespace + PropertyBasedInstanceConfigConstants.BROADCAST_PUBLIC_IPV4_ADDR_KEY,
        super.shouldBroadcastPublicIpv4Addr()
    ).get()

    override fun getNamespace(): String = namespace

}