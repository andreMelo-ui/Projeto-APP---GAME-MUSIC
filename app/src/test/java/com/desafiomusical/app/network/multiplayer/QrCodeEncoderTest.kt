package com.desafiomusical.app.network.multiplayer

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa [QrCodeEncoder] decodificando a [BitMatrix] resultante de volta com o
 * próprio ZXing — simula o que a câmera do cliente faria, sem precisar de
 * Android (nem [android.graphics.Bitmap] nem CameraX entram aqui).
 */
class QrCodeEncoderTest {
    /** Adapta a [BitMatrix] (módulos preto/branco) para o formato de luminância que o decoder do ZXing espera. */
    private class BitMatrixLuminanceSource(private val matrix: BitMatrix) :
        LuminanceSource(matrix.width, matrix.height) {
        override fun getRow(
            y: Int,
            row: ByteArray?,
        ): ByteArray {
            val out = row?.takeIf { it.size >= width } ?: ByteArray(width)
            for (x in 0 until width) out[x] = if (matrix.get(x, y)) 0.toByte() else 0xFF.toByte()
            return out
        }

        override fun getMatrix(): ByteArray {
            val out = ByteArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) out[y * width + x] = if (matrix.get(x, y)) 0.toByte() else 0xFF.toByte()
            }
            return out
        }
    }

    private fun decode(matrix: BitMatrix): String =
        MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(BitMatrixLuminanceSource(matrix)))).text

    @Test
    fun `codifica e decodifica o payload do convite corretamente`() {
        val invite = RoomInvite(roomCode = "AB12", hostAddress = "192.168.0.42", port = 54231)
        val matrix = QrCodeEncoder.encode(invite.toQrPayload())

        val decodedPayload = decode(matrix)
        assertEquals(invite.toQrPayload(), decodedPayload)
        assertEquals(invite, RoomInvite.fromQrPayload(decodedPayload))
    }

    @Test
    fun `matriz gerada tem o tamanho pedido e nao esta vazia`() {
        val matrix = QrCodeEncoder.encode(RoomInvite("ABCD", "10.0.0.5", 8080).toQrPayload(), size = 300)
        assertEquals(300, matrix.width)
        assertEquals(300, matrix.height)

        val hasBlackModule = (0 until matrix.width).any { x -> (0 until matrix.height).any { y -> matrix.get(x, y) } }
        assertTrue(hasBlackModule)
    }
}
