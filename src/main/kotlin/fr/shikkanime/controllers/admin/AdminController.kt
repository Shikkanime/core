package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.dtos.member.TokenDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.jobs.AbstractJob
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.AttachmentService
import fr.shikkanime.services.MailService
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
    @Inject private lateinit var memberService: MemberService
    @Inject private lateinit var simulcastCacheService: SimulcastCacheService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var attachmentService: AttachmentService
    @Inject private lateinit var mailService: MailService

    @Path
    @Get
    private fun home(@QueryParam error: String?) = Response.template(
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
    private fun logout() = Response.redirect(ADMIN, TokenDto.empty)

    @Path("/dashboard")
    @Get
    @AdminSessionAuthenticated
    private fun getDashboard() = Response.template(
        Link.DASHBOARD,
        mapOf(
            "simulcasts" to simulcastCacheService.findAll(),
            "size" to Constant.imagesFolder.listFiles().size
        )
    )

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
    private fun getAnimes() = Response.template(Link.ANIMES)

    @Path("/animes/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getAnimeView() = Response.template("admin/animes/edit.ftl", "Edit anime")

    @Path("/episodes")
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodes() = Response.template(Link.EPISODES)

    @Path("/episodes/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodeView() = Response.template("admin/episodes/edit.ftl", "Edit episode")

    @Path("/trace-actions")
    @Get
    @AdminSessionAuthenticated
    private fun getTraceActions() = Response.template(Link.TRACE_ACTIONS)

    @Path("/anime-alerts")
    @Get
    @AdminSessionAuthenticated
    private fun getAnimeAlerts() = Response.template(Link.ANIME_ALERTS)

    @Path("/members")
    @Get
    @AdminSessionAuthenticated
    private fun getMembers(): Response {
        return Response.template(Link.MEMBERS)
    }

    @Path("/members/{uuid}")
    @Get
    @AdminSessionAuthenticated
    private fun getMemberView() = Response.template("admin/members/edit.ftl", "Edit member")

    @Path("/rules")
    @Get
    @AdminSessionAuthenticated
    private fun getRules() = Response.template(Link.RULES)

    @Path("/jobs")
    @Get
    @AdminSessionAuthenticated
    private fun getJobs() = Response.template(
        Link.JOBS,
        mapOf(
            "jobs" to Constant.reflections.getSubTypesOf(AbstractJob::class.java)
                .map { it.simpleName.removeSuffix("Job") }
        ),
    )

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

    @Path("/emails")
    @Get
    @AdminSessionAuthenticated
    private fun getEmails() = Response.template(Link.EMAILS)

    @Path("/emails")
    @Post
    @AdminSessionAuthenticated
    private fun sendEmails(@BodyParam parameters: Parameters): Response {
        val subject = parameters["subject"] ?: return Response.redirect(Link.EMAILS.href)
        val body = parameters["body"] ?: return Response.redirect(Link.EMAILS.href)

        // Send the email to all members
        val members = memberService.findAll().filterNot { it.email.isNullOrBlank() }

        val bodyEmail = mailService.getFreemarkerContent("/mail/custom-message.ftl", model = mapOf(
            "description" to body,
        )).toString()

        members.forEach { member ->
            mailService.save(
                Mail(
                    recipient = member.email,
                    title = "${Constant.NAME} - $subject",
                    body = bodyEmail
                )
            )
        }

        mailService.save(
            Mail(
                recipient = configCacheService.getValueAsString(ConfigPropertyKey.ADMIN_EMAIL),
                title = "${Constant.NAME} - $subject",
                body = bodyEmail
            )
        )

        return Response.redirect(Link.EMAILS.href)
    }


    @Path("/threads")
    @Get
    @AdminSessionAuthenticated
    private fun getThreads(@QueryParam success: Int?) = Response.template(
        Link.THREADS,
        mapOf(
            "askCodeUrl" to ThreadsWrapper.getCode(requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_APP_ID))),
            "success" to success
        )
    )

    @Path("/threads-publish")
    @Get
    @AdminSessionAuthenticated
    private fun threadsPublish(
        @QueryParam message: String,
        @QueryParam("image_url") imageUrl: String?
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
    private fun getConfigs() = Response.template(Link.CONFIG)
}