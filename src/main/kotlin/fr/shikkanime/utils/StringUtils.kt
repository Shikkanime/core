package fr.shikkanime.utils

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.caches.LanguageCacheService
import java.text.Normalizer
import java.util.*
import java.util.regex.Pattern

object StringUtils {
    private val nonLatinPattern: Pattern = Pattern.compile("[^\\w-]")
    private val whitespacePattern: Pattern = Pattern.compile("\\s|:\\b|\\.\\b|/\\b|&\\b")
    private val regex = "( [-|!].*[-|!])|( Saison \\d*)|\\(\\d*\\)".toRegex()
    private val separators = listOf(":", ",", "!", "–", " so ")

    private fun isAllPartsHaveSameAmountOfWords(parts: List<String>, limit: Int): Boolean {
        val words = parts.map { it.trim().split(" ").size }
        return words.all { it <= limit }
    }

    fun getShortName(fullName: String): String {
        var shortName = regex.replace(fullName, "")

        separators.forEach { separator ->
            if (shortName.contains(separator)) {
                val split = shortName.split(separator)
                val firstPart = split[0].trim()
                val lastPart = split.subList(1, split.size).joinToString(" ").trim()

                if (lastPart.count { it == ' ' } >= 2 && firstPart.length > 5 && !isAllPartsHaveSameAmountOfWords(
                        split,
                        2
                    )) {
                    shortName = firstPart
                }
            }
        }

        return shortName.replace(" +".toRegex(), " ").trim()
    }

    fun getHashtag(fullName: String) = getShortName(fullName).lowercase().capitalizeWords()
        .replace(" S ", " s ")
        .filter { it.isLetterOrDigit() }

    fun String.capitalizeWords(): String {
        val delimiters = arrayOf(" ", ",", "-", ":", "/", "'", "\"", "&")

        return this.split(*delimiters).joinToString(" ") {
            it.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(
                    Locale.getDefault()
                ) else char.toString()
            }
        }
    }

    fun toEpisodeString(episode: EpisodeVariantDto): String {
        val etName = when (episode.mapping.episodeType) {
            EpisodeType.EPISODE -> "Épisode"
            EpisodeType.SPECIAL -> "Spécial"
            EpisodeType.FILM -> "Film"
            EpisodeType.SUMMARY -> "Épisode récapitulatif"
        }

        val ltName = when (LangType.fromAudioLocale(episode.mapping.anime.countryCode, episode.audioLocale)) {
            LangType.SUBTITLES -> "VOSTFR"
            LangType.VOICE -> "VF"
        }

        return "Saison ${episode.mapping.season} • $etName ${episode.mapping.number} $ltName"
    }

    fun toSlug(input: String): String {
        val nowhitespace: String = whitespacePattern.matcher(input).replaceAll("-")
        val normalized: String = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
        val slug: String = nonLatinPattern.matcher(normalized).replaceAll("").replace("-+".toRegex(), "-").trim('-')
        return slug.lowercase()
    }

    fun sanitizeXSS(input: String): String = input.replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    fun unSanitizeXSS(input: String): String = input.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")

    fun getIdentifier(
        countryCode: CountryCode,
        platform: Platform,
        id: String,
        audioLocale: String,
        uncensored: Boolean = false
    ) = "${countryCode}-${platform}-$id-${audioLocale.uppercase()}${if (uncensored) "-UNC" else ""}"

    private fun isInvalid(
        image: String?,
        description: String?,
        countryCode: CountryCode,
        languageCacheService: LanguageCacheService
    ) = image.isNullOrBlank() ||
            description.isNullOrBlank() ||
            description.startsWith("(") ||
            (description.isNotBlank() && languageCacheService.detectLanguage(description) != countryCode.name.lowercase())

    fun getStatus(anime: Anime): Status {
        val languageCacheService = Constant.injector.getInstance(LanguageCacheService::class.java)

        return if (
            isInvalid(anime.image, anime.description, anime.countryCode!!, languageCacheService) ||
            anime.banner.isNullOrBlank()
        ) Status.INVALID else Status.VALID
    }

    fun getStatus(episodeMapping: EpisodeMapping): Status {
        val languageCacheService = Constant.injector.getInstance(LanguageCacheService::class.java)

        return if (
            isInvalid(
                episodeMapping.image,
                episodeMapping.description,
                episodeMapping.anime!!.countryCode!!,
                languageCacheService
            ) ||
            episodeMapping.title.isNullOrBlank() ||
            episodeMapping.image == Constant.DEFAULT_IMAGE_PREVIEW
        ) Status.INVALID else Status.VALID
    }

    fun getStatus(episode: AbstractPlatform.Episode): Status {
        val languageCacheService = Constant.injector.getInstance(LanguageCacheService::class.java)

        return if (
            isInvalid(
                episode.image,
                episode.description,
                episode.countryCode,
                languageCacheService
            ) ||
            episode.title.isNullOrBlank() ||
            episode.image == Constant.DEFAULT_IMAGE_PREVIEW
        ) Status.INVALID else Status.VALID
    }

    fun generateRandomString(length: Int): String {
        val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { source.random() }
            .joinToString("")
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$".toRegex()
        return emailRegex.matches(email)
    }
}