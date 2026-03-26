package fr.shikkanime.modules

import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.miscellaneous.LinkObject
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

private fun <T> List<T>.randomIfNotEmpty(): T? = if (isNotEmpty()) random() else null
private fun <T> Array<T>.randomIfNotEmpty(): T? = if (isNotEmpty()) random() else null
private fun <T> Array<T>.randomIfNotEmpty(predicate: (T) -> Boolean): T? = filter(predicate).randomIfNotEmpty()

private val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)
private val simulcastCacheService = Constant.injector.getInstance(SimulcastCacheService::class.java)
private val animeCacheService = Constant.injector.getInstance(AnimeCacheService::class.java)

fun setGlobalAttributes(
    isBot: Boolean,
    modelMap: MutableMap<Any?, Any?>,
    controller: Any,
    replacedPath: String,
    title: String?
) {
    if (!replacedPath.startsWith(ADMIN) && !isSitePath(replacedPath)) {
        return
    }

    val isAdminController = controller.javaClass.simpleName.startsWith("Admin")
    val currentSimulcast = simulcastCacheService.currentSimulcast

    modelMap["su"] = StringUtils
    modelMap["links"] =  (if (isAdminController) LinkObject.adminList else LinkObject.siteList)
        .onEach { link ->
            link.href = link.href.replace("{currentSimulcast}", currentSimulcast?.slug ?: StringUtils.EMPTY_STRING)
            link.active = if (link.href == "/") replacedPath == link.href else replacedPath.startsWith(link.href)
        }
    modelMap["footerLinks"] = if (isAdminController) LinkObject.adminFooterList else LinkObject.siteFooterList
    modelMap["title"] = getTitle(title)
    modelMap["seoDescription"] = configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION)
    modelMap["googleSiteVerification"] = configCacheService.getValueAsString(ConfigPropertyKey.GOOGLE_SITE_VERIFICATION_ID)
    modelMap["currentSimulcast"] = currentSimulcast
    modelMap["baseUrl"] = Constant.baseUrl
    modelMap["apiUrl"] = Constant.apiUrl

    if (!isBot) {
        modelMap["additionalHeadTags"] = configCacheService.getValueAsString(ConfigPropertyKey.ADDITIONAL_HEAD_TAGS)
    }

    var randomAnimeSlug: String? = null

    if (modelMap.containsKey("anime") && modelMap["anime"] is AnimeDto) {
        val anime = modelMap["anime"] as AnimeDto
        randomAnimeSlug = animeCacheService.findAllSlugs().randomIfNotEmpty { it != anime.slug }
    }

    if (modelMap.containsKey("episodeMapping") && modelMap["episodeMapping"] is EpisodeMappingDto) {
        val episodeMapping = modelMap["episodeMapping"] as EpisodeMappingDto
        randomAnimeSlug = animeCacheService.findAllSlugs().randomIfNotEmpty { it != episodeMapping.anime!!.slug }
    }

    if (randomAnimeSlug == null) {
        randomAnimeSlug = animeCacheService.findAllSlugs().randomIfNotEmpty()
    }

    modelMap["randomAnimeSlug"] = randomAnimeSlug
}

private fun getTitle(title: String?): String {
    return when {
        title.isNullOrBlank() -> Constant.NAME
        Constant.NAME in title -> title
        else -> "$title - ${Constant.NAME}"
    }
}