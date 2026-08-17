package com.desafiomusical.app.network.multiplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [decodeRoomCode] é a única lógica de descoberta NSD que não depende de
 * `android.net.nsd.*` — o registro/descoberta de verdade só é verificável em
 * dois aparelhos reais na mesma rede Wi-Fi (ver [NsdRoomAdvertiser] e
 * [NsdRoomDiscoverer]).
 */
class NsdRoomConstantsTest {

    @Test
    fun `decodifica roomCode de um TXT record valido`() {
        val attributes = mapOf(NSD_TXT_ROOM_CODE to "ABCD".toByteArray(Charsets.UTF_8))
        assertEquals("ABCD", decodeRoomCode(attributes))
    }

    @Test
    fun `retorna null quando a chave roomCode nao existe`() {
        val attributes = mapOf("outraChave" to "valor".toByteArray(Charsets.UTF_8))
        assertNull(decodeRoomCode(attributes))
    }

    @Test
    fun `retorna null quando o valor e nulo ou em branco`() {
        assertNull(decodeRoomCode(mapOf(NSD_TXT_ROOM_CODE to null)))
        assertNull(decodeRoomCode(mapOf(NSD_TXT_ROOM_CODE to "   ".toByteArray(Charsets.UTF_8))))
        assertNull(decodeRoomCode(mapOf(NSD_TXT_ROOM_CODE to ByteArray(0))))
    }

    @Test
    fun `decodifica corretamente caracteres UTF-8 fora do ASCII`() {
        val attributes = mapOf(NSD_TXT_ROOM_CODE to "SÃO-1".toByteArray(Charsets.UTF_8))
        assertEquals("SÃO-1", decodeRoomCode(attributes))
    }
}
