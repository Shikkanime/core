package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.Platform
import fr.shikkanime.services.PlatformService
import fr.shikkanime.utils.routes.BodyParam
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.ziedelth.utils.routes.method.Get
import fr.ziedelth.utils.routes.method.Post
import java.util.*

@Controller("/api/platforms")
class PlatformController {
    @Inject
    private lateinit var platformService: PlatformService

    @Path
    @Get
    private fun getPlatforms(): Response {
        return Response.ok(AbstractConverter.convert(platformService.findAll(), PlatformDto::class.java))
    }

    @Path("/{uuid}")
    @Get
    private fun getPlatform(uuid: UUID): Response {
        return Response.ok(AbstractConverter.convert(platformService.find(uuid), PlatformDto::class.java))
    }

    @Path
    @Post
    private fun createPlatform(@BodyParam platformDto: PlatformDto): Response {
        return Response.ok(
            AbstractConverter.convert(
                platformService.save(AbstractConverter.convert(platformDto, Platform::class.java)),
                PlatformDto::class.java
            )
        )
    }
}