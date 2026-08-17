package com.desafiomusical.app.network.multiplayer

/** Tipo de serviço mDNS/DNS-SD usado para anunciar e descobrir salas na rede local. */
internal const val NSD_SERVICE_TYPE = "_desafiomusical._tcp."

/** Chave do registro TXT que carrega o código da sala. */
internal const val NSD_TXT_ROOM_CODE = "roomCode"

/** Erro de registro/descoberta NSD — sempre carrega o `errorCode` bruto do `NsdManager` na mensagem. */
class NsdOperationException(message: String) : Exception(message)

/**
 * Extrai o `roomCode` dos atributos TXT de um serviço resolvido. É a única
 * parte da descoberta NSD que não depende de `android.net.nsd.*` — por isso é
 * a única coberta por teste de unidade puro; o resto (registro/descoberta de
 * verdade) só é verificável com dois aparelhos na mesma rede Wi-Fi.
 */
internal fun decodeRoomCode(attributes: Map<String, ByteArray?>): String? =
    attributes[NSD_TXT_ROOM_CODE]?.toString(Charsets.UTF_8)?.takeIf { it.isNotBlank() }
