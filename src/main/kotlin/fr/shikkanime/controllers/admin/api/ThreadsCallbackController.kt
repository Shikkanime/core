package fr.shikkanime.controllers.admin.api

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.socialnetworks.ThreadsSocialNetwork
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import fr.shikkanime.wrappers.ThreadsWrapper
import kotlinx.coroutines.runBlocking

@Controller("$ADMIN/api/threads")
@AdminSessionAuthenticated
class ThreadsCallbackController {
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var configService: ConfigService

    @Path
    @Get
    private fun callback(@QueryParam code: String): Response {
        val appId = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_APP_ID))
        val appSecret = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_APP_SECRET))

        return runBlocking {
            try {
                val accessToken = ThreadsWrapper.getAccessToken(
                    appId,
                    appSecret,
                    code,
                )

                val longLivedAccessToken = ThreadsWrapper.getLongLivedAccessToken(
                    appSecret,
                    accessToken,
                )

                configService.findAllByName(ConfigPropertyKey.THREADS_ACCESS_TOKEN.key).firstOrNull()?.let {
                    it.propertyValue = longLivedAccessToken
                    configService.update(it)
                    InvalidationService.invalidate(Config::class.java)
                    Constant.injector.getInstance(ThreadsSocialNetwork::class.java).logout()
                }

                Response.redirect("${Link.THREADS.href}?success=1")
            } catch (e: Exception) {
                e.printStackTrace()
                Response.badRequest(MessageDto.error("Impossible to get token with code $code"))
            }
        }
    }
}