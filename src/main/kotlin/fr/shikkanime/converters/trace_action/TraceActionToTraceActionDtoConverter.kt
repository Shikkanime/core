package fr.shikkanime.converters.trace_action

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.TraceActionDto
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.utils.withUTCString

class TraceActionToTraceActionDtoConverter : AbstractConverter<TraceAction, TraceActionDto>() {
    override fun convert(from: TraceAction): TraceActionDto {
        return TraceActionDto(
            uuid = from.uuid!!,
            actionDateTime = from.actionDateTime!!.withUTCString(),
            entityType = from.entityType!!,
            entityUuid = from.entityUuid!!,
            action = from.action!!
        )
    }
}