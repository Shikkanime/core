package fr.shikkanime.factories.impl

import fr.shikkanime.dtos.TraceActionDto
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.factories.IGenericFactory
import fr.shikkanime.utils.withUTCString

class TraceActionFactory : IGenericFactory<TraceAction, TraceActionDto> {
    override fun toDto(entity: TraceAction) = TraceActionDto(
        uuid = entity.uuid!!,
        actionDateTime = entity.actionDateTime!!.withUTCString(),
        entityType = entity.entityType!!,
        entityUuid = entity.entityUuid!!,
        action = entity.action!!
    )
}