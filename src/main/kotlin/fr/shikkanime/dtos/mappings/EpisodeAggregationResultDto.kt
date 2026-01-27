package fr.shikkanime.dtos.mappings

import java.util.*

data class EpisodeAggregationResultDto(
    val episodeUuid: UUID,
    val animeName: String,
    val episodeName: String,
    val releaseDate: String,
    val aggregations: List<Aggregation>,
) {
    data class Aggregation(
        val titles: Set<String>,
        val airings: List<AiringDate>,
        val sources: List<Source>,
    )

    data class Source(
        val platform: String,
        val url: String?,
    )

    data class AiringDate(
        val date: String,
        val occurrenceCount: Int,
    )
}
