package com.desafiomusical.app.data.mapper

import com.desafiomusical.app.data.room.entity.PlayerEntity
import com.desafiomusical.app.domain.model.Player

fun PlayerEntity.toDomain(): Player = Player(id = id, name = name, createdAt = createdAt)

fun Player.toEntity(): PlayerEntity = PlayerEntity(id = id, name = name, createdAt = createdAt)
