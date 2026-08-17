package com.desafiomusical.app.network.multiplayer

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.common.BitMatrix

/** Converte a matriz de módulos do QR Code num [Bitmap] pronto pra exibir na tela do host. */
fun BitMatrix.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}
