package fr.shikkanime.dtos

import java.util.*

data class ConfigDto(
    val uuid: UUID?,
    val propertyKey: String?,
    var propertyValue: String?,
)
