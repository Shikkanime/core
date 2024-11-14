package fr.shikkanime.modules

import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.LinkObject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.BotDetectorCache
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

private const val ADMIN = "/admin"

fun setGlobalAttributes(
    ipAddress: String,
    userAgent: String,
    modelMap: MutableMap<String, Any?>,
    controller: Any,
    replacedPath: String,
    title: String?
) {
    val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)
    val simulcastCacheService = Constant.injector.getInstance(SimulcastCacheService::class.java)
    val animeCacheService = Constant.injector.getInstance(AnimeCacheService::class.java)
    val botDetectorCache = Constant.injector.getInstance(BotDetectorCache::class.java)

    modelMap["su"] = StringUtils
    modelMap["links"] = getLinks(controller, replacedPath, simulcastCacheService)
    modelMap["footerLinks"] = getFooterLinks(controller)
    modelMap["title"] = getTitle(title)
    modelMap["seoDescription"] = configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION)
    modelMap["googleSiteVerification"] =
        configCacheService.getValueAsString(ConfigPropertyKey.GOOGLE_SITE_VERIFICATION_ID)
    modelMap["currentSimulcast"] = simulcastCacheService.currentSimulcast
    modelMap["baseUrl"] = Constant.baseUrl
    modelMap["apiUrl"] = Constant.apiUrl

    if (!botDetectorCache.isBot(clientIp = ipAddress, userAgent = userAgent)) {
        modelMap["additionalHeadTags"] = configCacheService.getValueAsString(ConfigPropertyKey.ADDITIONAL_HEAD_TAGS)
    }

    var randomAnimeSlug: String? = null

    if (modelMap.containsKey("anime") && modelMap["anime"] is AnimeDto) {
        val anime = modelMap["anime"] as AnimeDto
        randomAnimeSlug = animeCacheService.findAllSlug()?.filter { it != anime.slug }?.random()
    }

    if (modelMap.containsKey("episodeMapping") && modelMap["episodeMapping"] is EpisodeMappingDto) {
        val episodeMapping = modelMap["episodeMapping"] as EpisodeMappingDto
        randomAnimeSlug = animeCacheService.findAllSlug()?.filter { it != episodeMapping.anime.slug }?.random()
    }

    if (randomAnimeSlug == null) {
        randomAnimeSlug = animeCacheService.findAllSlug()?.random()
    }

    modelMap["randomAnimeSlug"] = randomAnimeSlug
}

private fun getLinks(controller: Any, replacedPath: String, simulcastCacheService: SimulcastCacheService) =
    LinkObject.list()
        .filter { it.href.startsWith(ADMIN) == controller.javaClass.simpleName.startsWith("Admin") && !it.footer }
        .map { link ->
            link.href = link.href.replace("{currentSimulcast}", simulcastCacheService.currentSimulcast?.slug ?: "")
            link.active = if (link.href == "/") replacedPath == link.href else replacedPath.startsWith(link.href)
            link
        }

private fun getFooterLinks(controller: Any) = LinkObject.list()
    .filter { it.href.startsWith(ADMIN) == controller.javaClass.simpleName.startsWith("Admin") && it.footer }

private fun getTitle(title: String?): String =
    title?.takeIf { it.contains(Constant.NAME) } ?: "$title - ${Constant.NAME}"