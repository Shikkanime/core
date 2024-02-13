package fr.shikkanime.converters.simulcast

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.simulcasts.UpdatedSimulcastDto
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.caches.AnimeCacheService

class SimulcastToUpdatedSimulcastDtoConverter : AbstractConverter<Simulcast, UpdatedSimulcastDto>() {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    override fun convert(from: Simulcast): UpdatedSimulcastDto {
        val lastReleaseDateTime = animeCacheService.findAllBy(
            CountryCode.FR,
            from.uuid,
            listOf(SortParameter("releaseDateTime", SortParameter.Order.DESC)),
            1,
            1
        )?.data?.firstOrNull()?.releaseDateTime

        return UpdatedSimulcastDto(
            uuid = from.uuid,
            season = from.season!!,
            year = from.year!!,
            slug = "${from.season.lowercase()}-${from.year}",
            label = when (from.season) {
                "WINTER" -> "Hiver"
                "SPRING" -> "Printemps"
                "SUMMER" -> "Été"
                "AUTUMN" -> "Automne"
                else -> "Inconnu"
            } + " ${from.year}",
            lastReleaseDateTime = lastReleaseDateTime
        )
    }
}