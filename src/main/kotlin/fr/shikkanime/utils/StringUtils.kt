package fr.shikkanime.utils

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.*
import fr.shikkanime.services.caches.LanguageCacheService
import java.text.Normalizer
import java.util.*
import java.util.regex.Pattern

object StringUtils {
    private val NONLATIN: Pattern = Pattern.compile("[^\\w-]")
    private val WHITESPACE: Pattern = Pattern.compile("\\s")
    private val regex = "([-|!].*[-|!])|(Saison \\d*)|\\(\\d*\\)".toRegex()
    private val separators = listOf(":", ",", "!", "–", " so ")

    fun getShortName(fullName: String): String {
        var shortName = regex.replace(fullName, "")

        separators.forEach { separator ->
            if (shortName.contains(separator)) {
                val split = shortName.split(separator)
                val firstPart = split[0].trim()
                val lastPart = split.subList(1, split.size).joinToString(" ").trim()

                if (lastPart.count { it == ' ' } >= 2 && firstPart.length > 5) {
                    shortName = firstPart
                }
            }
        }

        return shortName.replace(" +".toRegex(), " ").trim()
    }

    fun getHashtag(fullName: String) = getShortName(fullName).capitalizeWords().filter { it.isLetterOrDigit() }

    fun String.capitalizeWords(): String {
        val delimiters = arrayOf(" ", ",")

        return this.split(*delimiters).joinToString(" ") {
            it.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(
                    Locale.getDefault()
                ) else char.toString()
            }
        }
    }

    fun toEpisodeString(episode: EpisodeDto): String {
        val etName = when (episode.episodeType) {
            EpisodeType.EPISODE -> "Épisode"
            EpisodeType.SPECIAL -> "Spécial"
            EpisodeType.FILM -> "Film"
        }

        val ltName = when (episode.langType) {
            LangType.SUBTITLES -> "VOSTFR"
            LangType.VOICE -> "VF"
        }

        return "Saison ${episode.season} • $etName ${episode.number} $ltName"
    }

    fun toSlug(input: String): String {
        val nowhitespace: String = WHITESPACE.matcher(input).replaceAll("-")
        val normalized: String = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
        val slug: String = NONLATIN.matcher(normalized).replaceAll("").replace("-+".toRegex(), "-")
        return slug.lowercase()
    }

    fun sanitizeXSS(input: String): String = input.replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    fun unsanitizeXSS(input: String): String = input.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")

    private fun isInvalid(
        image: String?,
        description: String?,
        countryCode: CountryCode,
        languageCacheService: LanguageCacheService
    ) = image.isNullOrBlank() ||
            description.isNullOrBlank() ||
            description.startsWith("(") ||
            languageCacheService.detectLanguage(description) != countryCode.name.lowercase()

    fun getStatus(anime: Anime): Status {
        val languageCacheService = Constant.injector.getInstance(LanguageCacheService::class.java)

        return if (
            isInvalid(anime.image, anime.description, anime.countryCode!!, languageCacheService) ||
            anime.banner.isNullOrBlank()
        ) Status.INVALID else Status.VALID
    }

    fun getStatus(episode: Episode): Status {
        val languageCacheService = Constant.injector.getInstance(LanguageCacheService::class.java)

        return if (
            isInvalid(episode.image, episode.description, episode.anime!!.countryCode!!, languageCacheService) ||
            episode.url?.contains("media-", true) == true ||
            episode.image == Constant.DEFAULT_IMAGE_PREVIEW ||
            episode.audioLocale.isNullOrBlank()
        ) Status.INVALID else Status.VALID
    }

    fun getDeprecatedHash(countryCode: CountryCode, platform: Platform, id: String, langType: LangType) = "${countryCode}-${platform}-$id-$langType"
    fun getHash(countryCode: CountryCode, platform: Platform, id: String, audioLocale: String) = "${countryCode}-${platform}-$id-$audioLocale"
}