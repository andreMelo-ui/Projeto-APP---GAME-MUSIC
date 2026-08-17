@file:Suppress("DEPRECATION") // API 34 traz overloads sem isso, mas minSdk 26 precisa da API clássica de NsdManager.

package com.desafiomusical.app.network.multiplayer

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Uma sala encontrada na rede local, já resolvida (IP:porta reais, prontos para [KtorClientRoomSession]). */
data class DiscoveredRoom(
    val roomCode: String,
    val serviceName: String,
    val hostAddress: String,
    val port: Int
)

/**
 * Descobre salas anunciadas por [NsdRoomAdvertiser] na rede local. Substitui
 * a digitação manual de IP:porta do lado de quem entra na sala (etapa 2
 * continua sendo o transporte real via [KtorClientRoomSession]). Só
 * testável de verdade em dispositivo/emulador real na mesma rede Wi-Fi — em
 * API 33+ o app que chama isso precisa já ter obtido a permissão em tempo de
 * execução `NEARBY_WIFI_DEVICES`, senão o SO nega a descoberta silenciosamente.
 */
class NsdRoomDiscoverer(private val nsdManager: NsdManager) {

    /**
     * Emite o retrato completo das salas conhecidas no momento, atualizado a
     * cada sala encontrada/perdida/resolvida — o coletor não precisa acumular
     * nada, só renderizar a lista mais recente. Fica ativo até o coletor
     * cancelar, quando a descoberta é parada automaticamente.
     */
    fun discoverRooms(): Flow<List<DiscoveredRoom>> = callbackFlow {
        val rooms = LinkedHashMap<String, DiscoveredRoom>() // chave = serviceName
        val resolveQueue = ArrayDeque<NsdServiceInfo>()
        var resolving = false

        fun resolveNext() {
            if (resolving) return
            val next = resolveQueue.removeFirstOrNull() ?: return
            resolving = true
            nsdManager.resolveService(
                next,
                ResolveListener(
                    onDone = { resolving = false; resolveNext() },
                    onResolved = { room -> rooms[room.serviceName] = room; trySend(rooms.values.toList()) }
                )
            )
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType != NSD_SERVICE_TYPE) return
                resolveQueue.addLast(service)
                resolveNext()
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                if (rooms.remove(service.serviceName) != null) trySend(rooms.values.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close(NsdOperationException("Falha ao iniciar descoberta NSD (código $errorCode)"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        nsdManager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        }
    }
}

/** A API clássica do NsdManager só tolera uma resolução por vez — [onDone] libera a próxima da fila independente de sucesso ou falha. */
private class ResolveListener(
    private val onDone: () -> Unit,
    private val onResolved: (DiscoveredRoom) -> Unit
) : NsdManager.ResolveListener {
    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = onDone()

    override fun onServiceResolved(info: NsdServiceInfo) {
        val roomCode = decodeRoomCode(info.attributes)
        val address = info.host?.hostAddress
        if (roomCode != null && address != null) {
            onResolved(DiscoveredRoom(roomCode, info.serviceName, address, info.port))
        }
        onDone()
    }
}
