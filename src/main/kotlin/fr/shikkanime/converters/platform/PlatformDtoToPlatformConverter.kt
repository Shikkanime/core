package fr.shikkanime.converters.platform

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.Platform
import fr.shikkanime.services.PlatformService

class PlatformDtoToPlatformConverter : AbstractConverter<PlatformDto, Platform>() {
    @Inject
    private lateinit var platformService: PlatformService

    override fun convert(from: PlatformDto): Platform {
        if (from.uuid != null) {
            val find = platformService.find(from.uuid)

            if (find != null) {
                return find
            }
        }

        return Platform(
            name = from.name,
            url = from.url,
            image = from.image
        )
    }
}