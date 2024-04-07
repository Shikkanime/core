package fr.shikkanime.socialnetworks

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.PlatformDto
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

    abstract fun sendMessage(message: String)

    open fun platformAccount(platform: PlatformDto): String {
        return platform.name
    }

    private fun information(episodeDto: EpisodeDto): String {
        return when (episodeDto.episodeType) {
            EpisodeType.SPECIAL -> "L'épisode spécial"
            EpisodeType.FILM -> "Le film"
            else -> "L'épisode ${episodeDto.number}"
        }
    }

    fun getEpisodeMessage(episodeDto: EpisodeDto, baseMessage: String): String {
        val uncensored = if (episodeDto.uncensored) " non censuré" else ""
        val isVoice = if (episodeDto.langType == LangType.VOICE) " en VF " else " "

        var configMessage = baseMessage
        configMessage = configMessage.replace("{SHIKKANIME_URL}", getShikkanimeUrl(episodeDto))
        configMessage = configMessage.replace("{URL}", episodeDto.url)
        configMessage = configMessage.replace("{PLATFORM_ACCOUNT}", platformAccount(episodeDto.platform))
        configMessage =
            configMessage.replace("{ANIME_HASHTAG}", "#${StringUtils.getHashtag(episodeDto.anime.shortName)}")
        configMessage = configMessage.replace("{ANIME_TITLE}", episodeDto.anime.shortName)
        configMessage = configMessage.replace("{EPISODE_INFORMATION}", "${information(episodeDto)}${uncensored}")
        configMessage = configMessage.replace("{VOICE}", isVoice)
        configMessage = configMessage.replace("\\n", "\n")
        configMessage = configMessage.trim()
        return configMessage
    }

    protected fun getShikkanimeUrl(episodeDto: EpisodeDto) =
        "${Constant.BASE_URL}/animes/${episodeDto.anime.slug}?utm_campaign=episode_post&utm_medium=social&utm_source=${utmSource()}&utm_content=${episodeDto.uuid}"

    abstract fun sendEpisodeRelease(episodeDto: EpisodeDto, mediaImage: ByteArray)

    open fun sendCalendar(message: String, calendarImage: ByteArray) {
        // Default implementation
    }
}