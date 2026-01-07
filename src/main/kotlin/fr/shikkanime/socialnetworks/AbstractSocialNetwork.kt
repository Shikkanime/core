package fr.shikkanime.socialnetworks

import com.google.inject.Inject
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

abstract class AbstractSocialNetwork {
    @Inject protected lateinit var configCacheService: ConfigCacheService

    abstract val priority: Int
    abstract fun login()
    abstract fun logout()

    private fun information(groupedEpisode: GroupedEpisode): String {
        val episodeType = groupedEpisode.episodeType
        val minNumber = groupedEpisode.minNumber
        val maxNumber = groupedEpisode.maxNumber
        val mappingsSize = groupedEpisode.mappings.size

        if (mappingsSize == 1) {
            return when (episodeType) {
                EpisodeType.SPECIAL -> "L'épisode spécial"
                EpisodeType.FILM -> "Le film"
                EpisodeType.SUMMARY -> "L'épisode récapitulatif"
                EpisodeType.SPIN_OFF -> "Le spin-off"
                else -> "L'épisode $minNumber"
            }
        }

        val numberLabel = if (minNumber == maxNumber) minNumber.toString() else "$minNumber à $maxNumber"

        return when (episodeType) {
            EpisodeType.SPECIAL -> "Les épisodes spéciaux"
            EpisodeType.FILM -> "Les films"
            EpisodeType.SUMMARY -> "Les épisodes récapitulatifs"
            EpisodeType.SPIN_OFF -> "Les spin-offs"
            else -> "Les épisodes $numberLabel"
        }
    }

    private fun replaceEpisodePlaceholders(template: String, groupedEpisode: GroupedEpisode): String {
        val anime = groupedEpisode.anime
        val variants = groupedEpisode.variants
        val mappingsSize = groupedEpisode.mappings.size
        val shortName = StringUtils.getShortName(anime.name!!)

        val langs = variants.map { it.audioLocale!! }.distinct()
            .sortedBy { LangType.fromAudioLocale(anime.countryCode!!, it) }
            .joinToString(" & ") { StringUtils.toLangTypeString(anime.countryCode!!, it) }

        return template
            .replace("{SHIKKANIME_URL}", getInternalUrl(groupedEpisode))
            .replace("{URL}", variants.first().url!!)
            .replace("{ANIME_HASHTAG}", "#${StringUtils.getHashtag(shortName)}")
            .replace("{ANIME_TITLE}", shortName)
            .replace("{ANIME_DESCRIPTION}", anime.description ?: "")
            .replace("{EPISODE_INFORMATION}", information(groupedEpisode))
            .replace("{EPISODE_TITLE}", groupedEpisode.title ?: "")
            .replace("{EPISODE_NUMBER}", groupedEpisode.minNumber.toString())
            .replace("{SEASON_NUMBER}", groupedEpisode.minSeason.toString())
            .replace("{VOICE}", "en $langs")
            .replace("{LANG}", langs)
            .replace("{BE}", if (mappingsSize <= 1) "est" else "sont")
            .replace("{AVAILABLE}", if (mappingsSize <= 1) "disponible" else "disponibles")
    }

    fun getEpisodeMessage(groupedEpisodes: List<GroupedEpisode>, baseMessage: String): String {
        if (baseMessage.isBlank()) return ""

        var message = baseMessage

        if (groupedEpisodes.size == 1) {
            message = replaceEpisodePlaceholders(message, groupedEpisodes.first())
        } else {
            message = "\\{EPISODES_LIST\\[(.*?)]}".toRegex().replace(message) { matchResult ->
                val itemTemplate = matchResult.groupValues[1]
                groupedEpisodes.joinToString("\n") { replaceEpisodePlaceholders(itemTemplate, it) }
            }

            if ("{EPISODES_LIST}" in message) {
                val defaultTemplate = "• {ANIME_TITLE} : {EPISODE_INFORMATION} ({LANG})"
                val episodesList = groupedEpisodes.joinToString("\n") { replaceEpisodePlaceholders(defaultTemplate, it) }
                message = message.replace("{EPISODES_LIST}", episodesList)
            }

            val animeTitles = groupedEpisodes.map { StringUtils.getShortName(it.anime.name!!) }
            val animeTitlesString = if (animeTitles.size > 1) {
                "${animeTitles.dropLast(1).joinToString(", ")} & ${animeTitles.last()}"
            } else {
                animeTitles.first()
            }

            message = message
                .replace("{SHIKKANIME_URL}", Constant.baseUrl)
                .replace("{ANIME_TITLES}", animeTitlesString)
                .replace("{COUNT}", groupedEpisodes.size.toString())
        }

        return message.replace("\\n", "\n").trim()
    }

    protected fun getInternalUrl(groupedEpisode: GroupedEpisode) =
        "${Constant.baseUrl}/r/${groupedEpisode.variants.first().uuid}"

    abstract suspend fun sendEpisodeRelease(groupedEpisodes: List<GroupedEpisode>, mediaImage: ByteArray?)
}