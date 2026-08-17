package com.desafiomusical.app.network.multiplayer

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Primeiro endereço IPv4 não-loopback da máquina — o IP da rede Wi-Fi local
 * que o host anuncia pra quem for entrar na sala (QR Code / digitação manual).
 * Não pede nenhuma permissão especial (ao contrário de `WifiManager`).
 */
fun findLocalIpv4Address(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces().asSequence()
        .flatMap { it.inetAddresses.asSequence() }
        .filterIsInstance<Inet4Address>()
        .firstOrNull { !it.isLoopbackAddress }
        ?.hostAddress
}.getOrNull()
