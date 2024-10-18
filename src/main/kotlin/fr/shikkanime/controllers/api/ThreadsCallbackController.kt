package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.param.QueryParam
import fr.shikkanime.wrappers.ThreadsWrapper
import kotlinx.coroutines.runBlocking

@Controller("/api/threads")
class ThreadsCallbackController {
    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var configService: ConfigService

    @Path
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun callback(
        @QueryParam("code") code: String,
    ): Response {
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

                configService.findByName(ConfigPropertyKey.THREADS_ACCESS_TOKEN.key)?.let {
                    it.propertyValue = longLivedAccessToken
                    configService.update(it)
                    MapCache.invalidate(Config::class.java)
                }

                Response.redirect("${Link.THREADS.href}?success=1")
            } catch (e: Exception) {
                e.printStackTrace()
                Response.badRequest("Impossible to get token with code $code")
            }
        }
    }
}