package com.desafiomusical.app.network.multiplayer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * Analisador de quadros de câmera (CameraX) que tenta decodificar um QR Code
 * de convite a cada frame. Quando acha um payload válido de [RoomInvite],
 * chama [onInviteFound] uma única vez (dedupe por payload) e para de tentar
 * decodificar até [reset] — evita reprocessar o mesmo QR dezenas de vezes por
 * segundo enquanto a câmera continua apontada pra ele. Não precisa corrigir
 * [ImageProxy.imageInfo]'s `rotationDegrees`: o ZXing já detecta o padrão do
 * QR Code em qualquer rotação dentro do plano da imagem. Só verificável em
 * dispositivo/emulador real — não roda em teste de unidade.
 */
class QrCodeAnalyzer(
    private val onInviteFound: (RoomInvite) -> Unit,
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader()

    @Volatile
    private var lastPayload: String? = null

    override fun analyze(image: ImageProxy) {
        try {
            if (lastPayload != null) return

            val yPlane = image.planes[0]
            val source =
                PlanarYUVLuminanceSource(
                    yPlane.buffer.toByteArray(),
                    yPlane.rowStride,
                    image.height,
                    0,
                    0,
                    image.width,
                    image.height,
                    false,
                )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val payload = runCatching { reader.decode(binaryBitmap).text }.getOrNull() ?: return
            val invite = RoomInvite.fromQrPayload(payload) ?: return

            lastPayload = payload
            onInviteFound(invite)
        } finally {
            reader.reset()
            image.close()
        }
    }

    /** Permite tentar decodificar de novo (ex.: usuário quer escanear outra sala depois de um erro de conexão). */
    fun reset() {
        lastPayload = null
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val bytes = ByteArray(remaining())
        get(bytes)
        return bytes
    }
}
