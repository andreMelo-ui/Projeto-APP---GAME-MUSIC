package com.desafiomusical.app.network.payloads

import kotlinx.serialization.json.Json

/** Instância única de [Json] usada por host e cliente para (de)serializar [GameEvent]. */
val GameEventJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
