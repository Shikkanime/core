package fr.shikkanime.converters.rule

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.RuleDto
import fr.shikkanime.entities.Rule
import fr.shikkanime.utils.withUTCString

class RuleToRuleDtoConverter : AbstractConverter<Rule, RuleDto>() {
    @Converter
    fun convert(from: Rule): RuleDto {
        return RuleDto(
            uuid = from.uuid,
            creationDateTime = from.creationDateTime.withUTCString(),
            platform = convert(from.platform!!, PlatformDto::class.java),
            seriesId = from.seriesId!!,
            seasonId = from.seasonId!!,
            action = from.action!!,
            actionValue = from.actionValue!!,
            lastUsageDateTime = from.lastUsageDateTime?.withUTCString()
        )
    }
}