package fr.shikkanime.socialnetworks

import com.google.inject.Inject
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

abstract class AbstractSocialNetwork {
    @Inject
    protected lateinit var configCacheService: ConfigCacheService

    abstract fun utmSource(): String
    abstract fun login()
    abstract fun logout()

    private fun information(episodeDto: EpisodeVariantDto): String {
        return when (episodeDto.mapping.episodeType) {
            EpisodeType.SPECIAL -> "L'épisode spécial"
            EpisodeType.FILM -> "Le film"
            EpisodeType.SUMMARY -> "L'épisode récapitulatif"
            EpisodeType.SPIN_OFF -> "Le spin-off"
            else -> "L'épisode ${episodeDto.mapping.number}"
        }
    }

    fun getEpisodeMessage(episodeDto: EpisodeVariantDto, baseMessage: String): String {
        val uncensored = if (episodeDto.uncensored) " non censuré" else ""
        val isVoice = if (LangType.fromAudioLocale(episodeDto.mapping.anime.countryCode, episodeDto.audioLocale) == LangType.VOICE) " en VF " else " "

        return baseMessage
            .replace("{SHIKKANIME_URL}", getShikkanimeUrl(episodeDto))
            .replace("{URL}", episodeDto.url)
            .replace("{ANIME_HASHTAG}", "#${StringUtils.getHashtag(episodeDto.mapping.anime.shortName)}")
            .replace("{ANIME_TITLE}", episodeDto.mapping.anime.shortName)
            .replace("{EPISODE_INFORMATION}", "${information(episodeDto)}${uncensored}")
            .replace("{VOICE}", isVoice)
            .replace("\\n", "\n")
            .trim()
    }

    protected fun getShikkanimeUrl(episodeDto: EpisodeVariantDto) =
        "${Constant.baseUrl}/r/${episodeDto.uuid}"

    abstract fun sendEpisodeRelease(episodeDto: EpisodeVariantDto, mediaImage: ByteArray?)
}