package com.desafiomusical.app.network.multiplayer

/**
 * Dados de convite de uma sala, compactados num payload de texto plano — cabe
 * num QR Code menor e mais rápido de escanear do que JSON. [toQrPayload] e
 * [fromQrPayload] não dependem de Android nem de ZXing, só de String, por
 * isso são testáveis em JVM puro (ao contrário da geração/leitura da imagem
 * em si — ver [QrCodeEncoder] e [QrCodeAnalyzer]).
 */
data class RoomInvite(
    val roomCode: String,
    val hostAddress: String,
    val port: Int
) {
    fun toQrPayload(): String = listOf(PREFIX, roomCode, hostAddress, port.toString()).joinToString(SEPARATOR)

    companion object {
        private const val PREFIX = "DESAFIOMUSICAL1"
        private const val SEPARATOR = "|"

        /** Retorna `null` para qualquer texto que não seja um convite válido — inclui QR Codes de outros apps. */
        fun fromQrPayload(payload: String): RoomInvite? {
            val parts = payload.split(SEPARATOR)
            if (parts.size != 4 || parts[0] != PREFIX) return null
            val roomCode = parts[1].takeIf { it.isNotBlank() } ?: return null
            val hostAddress = parts[2].takeIf { it.isNotBlank() } ?: return null
            val port = parts[3].toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
            return RoomInvite(roomCode, hostAddress, port)
        }
    }
}
