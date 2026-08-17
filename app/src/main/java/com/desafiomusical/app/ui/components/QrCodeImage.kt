package com.desafiomusical.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.desafiomusical.app.network.multiplayer.QrCodeEncoder
import com.desafiomusical.app.network.multiplayer.toBitmap

/** Exibe o QR Code de um convite de sala — [payload] normalmente vem de [com.desafiomusical.app.network.multiplayer.RoomInvite.toQrPayload]. */
@Composable
fun QrCodeImage(
    payload: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Int = 512
) {
    val imageBitmap = remember(payload, size) {
        QrCodeEncoder.encode(payload, size).toBitmap().asImageBitmap()
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = contentDescription,
        modifier = modifier.size(240.dp)
    )
}
