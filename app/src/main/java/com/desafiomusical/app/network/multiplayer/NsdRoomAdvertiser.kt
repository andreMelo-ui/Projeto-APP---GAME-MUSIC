package com.desafiomusical.app.network.multiplayer

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Anuncia a sala do host na rede local via NSD/mDNS, para que
 * [NsdRoomDiscoverer] consiga achá-la automaticamente — substitui a digitação
 * manual de IP:porta (etapas 1/2) sem mudar nada no transporte
 * ([KtorHostRoomSession] continua igual). Só testável de verdade em
 * dispositivo/emulador real na mesma rede Wi-Fi.
 */
class NsdRoomAdvertiser(private val nsdManager: NsdManager) {
    private var registrationListener: NsdManager.RegistrationListener? = null

    /** Registra o serviço e suspende até o SO confirmar (ou falhar) o registro. Retorna o nome final do serviço. */
    suspend fun advertise(
        roomCode: String,
        port: Int,
    ): String =
        suspendCancellableCoroutine { cont ->
            check(registrationListener == null) { "Sala já está sendo anunciada." }

            val serviceInfo =
                NsdServiceInfo().apply {
                    serviceName = "DesafioMusical-$roomCode"
                    serviceType = NSD_SERVICE_TYPE
                    this.port = port
                    setAttribute(NSD_TXT_ROOM_CODE, roomCode)
                }

            val listener =
                object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(info: NsdServiceInfo) {
                        if (cont.isActive) cont.resume(info.serviceName)
                    }

                    override fun onRegistrationFailed(
                        info: NsdServiceInfo,
                        errorCode: Int,
                    ) {
                        registrationListener = null
                        if (cont.isActive) {
                            cont.resumeWithException(
                                NsdOperationException("Falha ao registrar serviço NSD (código $errorCode)"),
                            )
                        }
                    }

                    override fun onServiceUnregistered(info: NsdServiceInfo) {
                        registrationListener = null
                    }

                    override fun onUnregistrationFailed(
                        info: NsdServiceInfo,
                        errorCode: Int,
                    ) {
                        registrationListener = null
                    }
                }
            registrationListener = listener

            cont.invokeOnCancellation { stop() }
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        }

    /** Remove o anúncio — idempotente, seguro de chamar mesmo se [advertise] nunca foi chamado ou já falhou. */
    fun stop() {
        val listener = registrationListener ?: return
        registrationListener = null
        runCatching { nsdManager.unregisterService(listener) }
    }
}
