package fr.shikkanime.dtos

import fr.shikkanime.entities.Rule
import java.util.*

data class RuleDto(
    val uuid: UUID?,
    val creationDateTime: String,
    val platform: PlatformDto,
    val seriesId: String,
    val seasonId: String,
    val action: Rule.Action,
    val actionValue: String
)
