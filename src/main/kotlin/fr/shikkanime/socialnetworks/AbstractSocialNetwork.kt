package fr.shikkanime.socialnetworks

import com.google.inject.Inject
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

abstract class AbstractSocialNetwork {
    @Inject
    protected lateinit var configCacheService: ConfigCacheService

    abstract fun utmSource(): String
    abstract fun login()
    abstract fun logout()

    private fun information(episodeMapping: EpisodeMappingDto): String {
        return when (episodeMapping.episodeType) {
            EpisodeType.SPECIAL -> "L'épisode spécial"
            EpisodeType.FILM -> "Le film"
            EpisodeType.SUMMARY -> "L'épisode récapitulatif"
            EpisodeType.SPIN_OFF -> "Le spin-off"
            else -> "L'épisode ${episodeMapping.number}"
        }
    }

    fun getEpisodeMessage(episodes: List<EpisodeVariantDto>, baseMessage: String): String {
        val mapping = episodes.first().mapping
        val shortName = mapping.anime.shortName
        val isVoice = " en ${episodes.map { it.audioLocale }.distinct().joinToString(" & ") { StringUtils.toLangTypeString(mapping.anime.countryCode, it) }} "

        return baseMessage
            .replace("{SHIKKANIME_URL}", getShikkanimeUrl(episodes))
            .replace("{URL}", episodes.first().url)
            .replace("{ANIME_HASHTAG}", "#${StringUtils.getHashtag(shortName)}")
            .replace("{ANIME_TITLE}", shortName)
            .replace("{EPISODE_INFORMATION}", information(mapping))
            .replace("{VOICE}", isVoice)
            .replace("\\n", "\n")
            .trim()
    }

    protected fun getShikkanimeUrl(episodes: List<EpisodeVariantDto>) =
        "${Constant.baseUrl}/r/${episodes.first().uuid}"

    abstract fun sendEpisodeRelease(episodes: List<EpisodeVariantDto>, mediaImage: ByteArray?)
}