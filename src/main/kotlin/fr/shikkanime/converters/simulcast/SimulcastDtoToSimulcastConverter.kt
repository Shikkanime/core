package fr.shikkanime.converters.simulcast

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.services.SimulcastService

class SimulcastDtoToSimulcastConverter : AbstractConverter<SimulcastDto, Simulcast>() {
    @Inject
    private lateinit var simulcastService: SimulcastService

    @Converter
    fun convert(from: SimulcastDto): Simulcast {
        val found = simulcastService.find(from.uuid)

        if (found != null)
            return found

        return Simulcast(
            season = from.season,
            year = from.year,
        )
    }
}