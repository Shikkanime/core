package fr.shikkanime.converters.rule

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.RuleDto
import fr.shikkanime.entities.Rule
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.RuleService
import java.time.ZonedDateTime

class RuleDtoToRuleConverter : AbstractConverter<RuleDto, Rule>() {
    @Inject
    private lateinit var ruleService: RuleService

    @Converter
    fun convert(from: RuleDto): Rule {
        val findByUuid = ruleService.find(from.uuid)

        if (findByUuid != null)
            return findByUuid

        return Rule(
            platform = Platform.findByName(from.platform.name)!!,
            seriesId = from.seriesId,
            seasonId = from.seasonId,
            action = from.action,
            actionValue = from.actionValue,
            lastUsageDateTime = from.lastUsageDateTime?.let { ZonedDateTime.parse(it) }
        )
    }
}