package com.desafiomusical.app.network.multiplayer

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Gera o QR Code do convite de sala do host. Só depende de `com.google.zxing`
 * (core) — sem nada de Android — por isso é testável em JVM puro: dá pra
 * decodificar a [BitMatrix] resultante de volta com o próprio ZXing e
 * verificar o round-trip completo, simulando o que a câmera do cliente faria
 * (ver `QrCodeEncoderTest`). A conversão pra [android.graphics.Bitmap] fica
 * em `QrCodeBitmap.kt`, que aí sim só roda em Android de verdade.
 */
object QrCodeEncoder {
    fun encode(
        payload: String,
        size: Int = 512,
    ): BitMatrix {
        val hints =
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
            )
        return QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
    }
}
