package fr.shikkanime.socialnetworks

import com.google.inject.Inject
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils

abstract class AbstractSocialNetwork {
    @Inject
    protected lateinit var configCacheService: ConfigCacheService

    abstract val priority: Int
    abstract fun login()
    abstract fun logout()

    private fun information(mappings: List<EpisodeMapping>): String {
        require(mappings.isNotEmpty()) { "Mappings must not be empty" }
        require(mappings.map { it.episodeType!! }.distinct().size == 1) { "All mappings must have the same episode type" }

        val episodeType = mappings.first().episodeType
        val episodeNumber = mappings.first().number

        if (mappings.size == 1) {
            return when (episodeType) {
                EpisodeType.SPECIAL -> "L'épisode spécial"
                EpisodeType.FILM -> "Le film"
                EpisodeType.SUMMARY -> "L'épisode récapitulatif"
                EpisodeType.SPIN_OFF -> "Le spin-off"
                else -> "L'épisode $episodeNumber"
            }
        }

        val (minNumber, maxNumber) = mappings.map { it.number!! }.let { it.minOrNull()!! to it.maxOrNull()!! }
        val numberLabel = if (minNumber == maxNumber) minNumber.toString() else "$minNumber à $maxNumber"

        return when (episodeType) {
            EpisodeType.SPECIAL -> "Les épisodes spéciaux"
            EpisodeType.FILM -> "Les films"
            EpisodeType.SUMMARY -> "Les épisodes récapitulatifs"
            EpisodeType.SPIN_OFF -> "Les spin-offs"
            else -> "Les épisodes $numberLabel"
        }
    }

    fun getEpisodeMessage(variants: List<EpisodeVariant>, baseMessage: String): String {
        require(variants.isNotEmpty()) { "Variants must not be empty" }
        require(variants.map { it.mapping!!.anime!!.uuid }.distinct().size == 1) { "All variants must be from the same anime" }
        val mappings = variants.map { it.mapping!! }.distinctBy { it.uuid!! }
        require(mappings.map { it.episodeType!! }.distinct().size == 1) { "All mappings must have the same episode type" }
        val anime = mappings.first().anime!!

        val shortName = StringUtils.getShortName(anime.name!!)
        val isVoice = " en ${variants.map { it.audioLocale!! }.distinct().sortedBy { LangType.fromAudioLocale(anime.countryCode!!, it) }.joinToString(" & ") { StringUtils.toLangTypeString(anime.countryCode!!, it) }} "

        return baseMessage
            .replace("{SHIKKANIME_URL}", getInternalUrl(variants))
            .replace("{URL}", variants.first().url!!)
            .replace("{ANIME_HASHTAG}", "#${StringUtils.getHashtag(shortName)}")
            .replace("{ANIME_TITLE}", shortName)
            .replace("{EPISODE_INFORMATION}", information(variants.map { it.mapping!! }.distinctBy { it.uuid!! }))
            .replace("{VOICE}", isVoice)
            .replace("{BE}", if (mappings.size <= 1) "est" else "sont")
            .replace("{AVAILABLE}", if (mappings.size <= 1) "disponible" else "disponibles")
            .replace("\\n", "\n")
            .trim()
    }

    protected fun getInternalUrl(variants: List<EpisodeVariant>) =
        "${Constant.baseUrl}/r/${variants.first().uuid}"

    abstract suspend fun sendEpisodeRelease(variants: List<EpisodeVariant>, mediaImage: ByteArray?)
}