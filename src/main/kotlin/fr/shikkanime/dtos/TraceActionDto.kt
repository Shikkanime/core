package fr.shikkanime.dtos

import fr.shikkanime.entities.TraceAction
import java.util.*

data class TraceActionDto(
    val uuid: UUID,
    val actionDateTime: String,
    val entityType: String,
    val entityUuid: UUID,
    val action: TraceAction.Action
)
