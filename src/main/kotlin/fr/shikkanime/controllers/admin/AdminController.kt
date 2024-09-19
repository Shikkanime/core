package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

private const val ADMIN = "/admin"

@Controller(ADMIN)
class AdminController {
    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Inject
    private lateinit var animeService: AnimeService

    @Path
    @Get
    private fun home(@QueryParam("error") error: String?): Response {
        return Response.template(
            "admin/login.ftl",
            "Login",
            if (!error.isNullOrBlank())
                mutableMapOf(
                    "error" to
                            when (error) {
                                "1" -> "Invalid credentials"
                                "2" -> "Token expired"
                                else -> "Unknown error"
                            }
                )
            else
                mutableMapOf()
        )
    }

    @Path("/login")
    @Post
    private fun login(@BodyParam parameters: Parameters): Response {
        val username = parameters["username"] ?: return Response.redirect(ADMIN)
        val password = parameters["password"] ?: return Response.redirect(ADMIN)
        val user = memberService.findByUsernameAndPassword(username, password) ?: return runBlocking {
            delay(1000)
            Response.redirect("$ADMIN?error=1")
        }

        return Response.redirect(Link.DASHBOARD.href, AbstractConverter.convert(user, TokenDto::class.java))
    }

    @Path("/logout")
    @Get
    @AdminSessionAuthenticated
    private fun logout(): Response {
        return Response.redirect(ADMIN, TokenDto.empty)
    }

    @Path("/dashboard")
    @Get
    @AdminSessionAuthenticated
    private fun getDashboard(): Response {
        return Response.template(
            Link.DASHBOARD,
            mapOf(
                "simulcasts" to simulcastCacheService.findAll(),
                "size" to ImageService.size,
                "originalSize" to ImageService.originalSize,
                "compressedSize" to ImageService.compressedSize,
            )
        )
    }

    @Path("/images-save")
    @Get
    @AdminSessionAuthenticated
    private fun saveImages(): Response {
        ImageService.saveCache()
        return Response.redirect(Link.DASHBOARD.href)
    }

    @Path("/images-invalidate")
    @Get
    @AdminSessionAuthenticated
    private fun invalidateImages(): Response {
        ImageService.invalidate()
        return Response.redirect(Link.DASHBOARD.href)
    }

    @Path("/simulcasts-invalidate")
    @Get
    @AdminSessionAuthenticated
    private fun invalidateSimulcasts(): Response {
        animeService.recalculateSimulcasts()
        MapCache.invalidate(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java,
            Simulcast::class.java
        )
        return Response.redirect(Link.DASHBOARD.href)
    }

    @Path("/animes")
    @Get
    @AdminSessionAuthenticated
    private fun getAnimes(): Response {
        return Response.template(Link.ANIMES)
    }

    @Path("/animes/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getAnimeView(): Response {
        return Response.template("admin/animes/edit.ftl", "Edit anime")
    }

    @Path("/episodes")
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodes(): Response {
        return Response.template(Link.EPISODES)
    }

    @Path("/episodes/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodeView(): Response {
        return Response.template("admin/episodes/edit.ftl", "Edit episode")
    }

    @Path("/trace-actions")
    @Get
    @AdminSessionAuthenticated
    private fun getTraceActions(): Response {
        return Response.template(Link.TRACE_ACTIONS)
    }

    @Path("/config")
    @Get
    @AdminSessionAuthenticated
    private fun getConfigs(): Response {
        return Response.template(Link.CONFIG)
    }
}