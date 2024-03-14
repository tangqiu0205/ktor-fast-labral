package com.tangqiu.cloud.ktor.discovery.utils

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object IpUtils {
    fun getIpAddress(): String {
        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            var ip: InetAddress? = null
            while (allNetInterfaces.hasMoreElements()) {
                val netInterface = allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    val addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip != null && ip is Inet4Address) {
                            return ip.hostAddress
                        }
                    }
                }
            }

        } catch (e: Exception) {
            System.err.println("IP地址获取失败" + e.toString());
        }
        return ""
    }
}
