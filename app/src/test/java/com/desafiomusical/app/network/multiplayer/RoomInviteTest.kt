package com.desafiomusical.app.network.multiplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomInviteTest {

    @Test
    fun `codifica e decodifica corretamente`() {
        val invite = RoomInvite(roomCode = "AB12", hostAddress = "192.168.0.42", port = 54231)
        assertEquals(invite, RoomInvite.fromQrPayload(invite.toQrPayload()))
    }

    @Test
    fun `rejeita payload de outro app ou QR aleatorio`() {
        assertNull(RoomInvite.fromQrPayload("https://example.com/qualquer-coisa"))
        assertNull(RoomInvite.fromQrPayload(""))
    }

    @Test
    fun `rejeita prefixo errado`() {
        assertNull(RoomInvite.fromQrPayload("OUTROAPP1|ABCD|10.0.0.5|8080"))
    }

    @Test
    fun `rejeita numero de partes errado`() {
        assertNull(RoomInvite.fromQrPayload("DESAFIOMUSICAL1|ABCD|10.0.0.5"))
        assertNull(RoomInvite.fromQrPayload("DESAFIOMUSICAL1|ABCD|10.0.0.5|8080|extra"))
    }

    @Test
    fun `rejeita porta invalida`() {
        assertNull(RoomInvite.fromQrPayload("DESAFIOMUSICAL1|ABCD|10.0.0.5|abc"))
        assertNull(RoomInvite.fromQrPayload("DESAFIOMUSICAL1|ABCD|10.0.0.5|0"))
        assertNull(RoomInvite.fromQrPayload("DESAFIOMUSICAL1|ABCD|10.0.0.5|70000"))
    }

    @Test
    fun `rejeita roomCode ou hostAddress em branco`() {
        assertNull(RoomInvite.fromQrPayload("DESAFIOMUSICAL1||10.0.0.5|8080"))
        assertNull(RoomInvite.fromQrPayload("DESAFIOMUSICAL1|ABCD||8080"))
    }
}
