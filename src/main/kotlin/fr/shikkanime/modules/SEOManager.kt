package fr.shikkanime.modules

import fr.shikkanime.entities.LinkObject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

private const val ADMIN = "/admin"

fun setGlobalAttributes(
    modelMap: MutableMap<String, Any?>,
    controller: Any,
    replacedPath: String,
    title: String?
) {
    val configCacheService = Constant.injector.getInstance(ConfigCacheService::class.java)
    val simulcastCacheService = Constant.injector.getInstance(SimulcastCacheService::class.java)

    modelMap["su"] = StringUtils
    modelMap["links"] = getLinks(controller, replacedPath, simulcastCacheService)
    modelMap["footerLinks"] = getFooterLinks(controller)
    modelMap["title"] = getTitle(title)
    modelMap["seoDescription"] = configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION)
    modelMap["googleSiteVerification"] =
        configCacheService.getValueAsString(ConfigPropertyKey.GOOGLE_SITE_VERIFICATION_ID)
    modelMap["currentSimulcast"] = simulcastCacheService.currentSimulcast
    modelMap["analyticsDomain"] = configCacheService.getValueAsString(ConfigPropertyKey.ANALYTICS_DOMAIN)
    modelMap["analyticsApi"] = configCacheService.getValueAsString(ConfigPropertyKey.ANALYTICS_API)
    modelMap["analyticsScript"] = configCacheService.getValueAsString(ConfigPropertyKey.ANALYTICS_SCRIPT)
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