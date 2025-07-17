package com.rdwatch.androidtv.network.interceptors

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom DNS resolver that prioritizes IPv4 addresses
 * This helps avoid IPv6 connectivity issues on some Android TV devices
 */
@Singleton
class IPv4DnsResolver
    @Inject
    constructor() : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                // Get all addresses for the hostname
                val allAddresses = InetAddress.getAllByName(hostname)

                // Separate IPv4 and IPv6 addresses
                val ipv4Addresses = allAddresses.filter { it is Inet4Address }
                val ipv6Addresses = allAddresses.filter { it !is Inet4Address }

                // Return IPv4 addresses first, then IPv6 as fallback
                when {
                    ipv4Addresses.isNotEmpty() -> ipv4Addresses
                    ipv6Addresses.isNotEmpty() -> ipv6Addresses
                    else -> listOf(InetAddress.getByName(hostname))
                }
            } catch (e: Exception) {
                // Fallback to default DNS behavior
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }
