package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.RuleDto
import fr.shikkanime.entities.Rule
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.services.RuleService
import fr.shikkanime.utils.withUTCString
import java.time.ZonedDateTime

class RuleFactory : IGenericFactory<Rule, RuleDto> {
    @Inject
    private lateinit var ruleService: RuleService

    @Inject
    private lateinit var platformFactory: PlatformFactory

    override fun toDto(entity: Rule) = RuleDto(
        uuid = entity.uuid,
        creationDateTime = entity.creationDateTime.withUTCString(),
        platform = platformFactory.toDto(entity.platform!!),
        seriesId = entity.seriesId!!,
        seasonId = entity.seasonId!!,
        action = entity.action!!,
        actionValue = entity.actionValue!!,
        lastUsageDateTime = entity.lastUsageDateTime?.withUTCString()
    )

    override fun toEntity(dto: RuleDto): Rule {
        val entity = (if (dto.uuid != null) ruleService.find(dto.uuid) else null) ?: Rule()
        entity.platform = platformFactory.toEntity(dto.platform)
        entity.seriesId = dto.seriesId
        entity.seasonId = dto.seasonId
        entity.action = dto.action
        entity.actionValue = dto.actionValue
        entity.lastUsageDateTime = dto.lastUsageDateTime?.let { ZonedDateTime.parse(it) }
        return entity
    }
}