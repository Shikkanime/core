package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.jobs.AbstractJob
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.MemberService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.QueryParam
import fr.shikkanime.wrappers.ThreadsWrapper
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

const val ADMIN = "/admin"

@Controller(ADMIN)
class AdminController {
    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var attachmentService: AttachmentService

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

        return Response.redirect(Link.DASHBOARD.href, TokenDto.build(user))
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
                "size" to Constant.imagesFolder.listFiles().size
            )
        )
    }

    @Path("/images-invalidate")
    @Get
    @AdminSessionAuthenticated
    private fun invalidateImages(): Response {
        attachmentService.encodeAllActive()
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

    @Path("/anime-alerts")
    @Get
    @AdminSessionAuthenticated
    private fun getAnimeAlerts(): Response {
        return Response.template(Link.ANIME_ALERTS)
    }

    @Path("/members")
    @Get
    @AdminSessionAuthenticated
    private fun getMembers(): Response {
        return Response.template(Link.MEMBERS)
    }

    @Path("/members/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getMemberView(): Response {
        return Response.template("admin/members/edit.ftl", "Edit member")
    }

    @Path("/rules")
    @Get
    @AdminSessionAuthenticated
    private fun getRules(): Response {
        return Response.template(Link.RULES)
    }

    @Path("/jobs")
    @Get
    @AdminSessionAuthenticated
    private fun getJobs(): Response {
        return Response.template(
            Link.JOBS,
            mapOf(
                "jobs" to Constant.reflections.getSubTypesOf(AbstractJob::class.java)
                    .map { it.simpleName.removeSuffix("Job") }
            ),
        )
    }

    @Path("/threads")
    @Get
    @AdminSessionAuthenticated
    private fun getThreads(
        @QueryParam("success") success: Int?
    ): Response {
        return Response.template(
            Link.THREADS,
            mapOf(
                "askCodeUrl" to ThreadsWrapper.getCode(
                    requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_APP_ID))
                ),
                "success" to success
            )
        )
    }

    @Path("/jobs")
    @Post
    @AdminSessionAuthenticated
    private fun runJob(@BodyParam parameters: Parameters): Response {
        val jobName = parameters["jobName"] ?: return Response.redirect(Link.JOBS.href)
        val jobClass = Constant.reflections.getSubTypesOf(AbstractJob::class.java)
            .firstOrNull { it.simpleName == "${jobName}Job" } ?: return Response.redirect(Link.JOBS.href)
        val job = Constant.injector.getInstance(jobClass)

        // Launch the job in a new thread
        Thread {
            job.run()
        }.start()

        return Response.redirect(Link.JOBS.href)
    }

    @Path("/threads-publish")
    @Get
    @AdminSessionAuthenticated
    private fun threadsPublish(
        @QueryParam("message") message: String,
        @QueryParam("image_url") imageUrl: String?,
    ): Response {
        val hasImage = !imageUrl.isNullOrBlank()

        runBlocking {
            ThreadsWrapper.post(
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_ACCESS_TOKEN)),
                if (hasImage) ThreadsWrapper.PostType.IMAGE else ThreadsWrapper.PostType.TEXT,
                message,
                imageUrl.takeIf { hasImage },
                "An example image".takeIf { hasImage }
            )
        }

        return Response.redirect(Link.THREADS.href)
    }

    @Path("/config")
    @Get
    @AdminSessionAuthenticated
    private fun getConfigs(): Response {
        return Response.template(Link.CONFIG)
    }
}