package fr.shikkanime.utils

import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.caches.LanguageCacheService
import java.text.Normalizer
import java.util.*
import java.util.regex.Pattern

object StringUtils {
    private val nonLatinPattern: Pattern = Pattern.compile("[^\\w-]")
    private val whitespacePattern: Pattern = Pattern.compile("\\s|:\\b|\\.\\b|/\\b|&\\b")
    private val regex = "( [-!~].*[-!~](?: |$))|( Saison \\d*)|(?:: )?\\(\\d*\\)| ([MDCLXVI]+$)".toRegex()
    private val separators = listOf(":", ",", "!", "–", " so ", " - ")
    private val encasedRegex = "<.*> ?.*".toRegex()
    private val duplicateSpaceRegex = " +".toRegex()

    fun removeAnimeNamePart(name: String) = name.replace("Anime ", "").trim()

    private fun isAllPartsHaveSameAmountOfWords(parts: List<String>): Boolean {
        val words = parts.map { it.trim().split(" ", "-").size }.distinct()
        return words.size == 1
    }

    fun getShortName(fullName: String): String {
        var shortName = fullName

        if (encasedRegex.matches(shortName)) {
            shortName = shortName.replace("<", "").replace(">", ": ").trim()
        }

        shortName = regex.replace(removeAnimeNamePart(shortName), " ").trim()

        separators.forEach { separator ->
            if (shortName.contains(separator)) {
                val split = shortName.split(separator)
                val firstPart = split[0].trim()
                val lastPart = split.subList(1, split.size).joinToString(" ").trim()

                if (lastPart.count { it == ' ' } >= 2 && firstPart.length > 5 && (separator == ":" || !isAllPartsHaveSameAmountOfWords(
                        split
                    ))) {
                    shortName = firstPart
                }
            }
        }

        return shortName.replace(duplicateSpaceRegex, " ").trim()
    }

    fun getHashtag(fullName: String) =
        getShortName(normalized(fullName.replace("1/2", ""))).lowercase().capitalizeWords()
        .replace(" S ", " s ")
        .replace(" T ", " t ")
        .filter { it.isLetterOrDigit() }

    fun String.capitalizeWords(): String {
        val pattern = "[ ,\\-:/\"&]|(^')".toPattern()

        return this.split(pattern).joinToString(" ") {
            it.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(
                    Locale.getDefault()
                ) else char.toString()
            }
        }
    }

    fun toSeasonString(countryCode: CountryCode, season: Int): String {
        return when(countryCode) {
            CountryCode.FR -> "Saison $season"
        }
    }

    fun getEpisodeTypeLabel(countryCode: CountryCode, episodeType: EpisodeType): String {
        return when(countryCode) {
            CountryCode.FR -> {
                when (episodeType) {
                    EpisodeType.EPISODE -> "Épisode"
                    EpisodeType.SPECIAL -> "Spécial"
                    EpisodeType.FILM -> "Film"
                    EpisodeType.SUMMARY -> "Épisode récapitulatif"
                    EpisodeType.SPIN_OFF -> "Spin-off"
                }
            }
        }
    }

    fun getEpisodeTypePrefixLabel(countryCode: CountryCode, episodeType: EpisodeType): String {
        return when(countryCode) {
            CountryCode.FR -> {
                when (episodeType) {
                    EpisodeType.EPISODE -> "ÉP"
                    EpisodeType.SPECIAL -> "SP"
                    EpisodeType.FILM -> "FILM "
                    EpisodeType.SUMMARY -> "RÉCAP"
                    EpisodeType.SPIN_OFF -> "SPIN-OFF"
                }
            }
        }
    }

    fun toLangTypeString(countryCode: CountryCode, audioLocale: String): String {
        val langType = LangType.fromAudioLocale(countryCode, audioLocale)

        return when(countryCode) {
            CountryCode.FR -> {
                when(langType) {
                    LangType.SUBTITLES -> "VOSTFR"
                    LangType.VOICE -> "VF"
                }
            }
        }
    }

    fun toEpisodeMappingString(episode: EpisodeMappingDto, showSeason: Boolean = true, separator: Boolean = true): String {
        val countryCode = episode.anime.countryCode

        return buildString {
            append(if (showSeason) toSeasonString(countryCode, episode.season) else "")
            append(if (showSeason && separator) " • " else " ")
            append(getEpisodeTypeLabel(countryCode, episode.episodeType))
            append(" ${episode.number}")
        }.trim()
    }

    fun toEpisodeVariantString(episodes: List<EpisodeVariantDto>): String {
        val mapping = episodes.first().mapping
        val countryCode = mapping.anime.countryCode

        return buildString {
            append(toSeasonString(countryCode, mapping.season))
            append(" • ")
            append(getEpisodeTypeLabel(countryCode, mapping.episodeType))
            append(" ${mapping.number}")
            append(" ${episodes.map { it.audioLocale }.distinct().joinToString(" & ") { toLangTypeString(countryCode, it) }}")
        }
    }

    fun toSlug(input: String): String {
        val nowhitespace: String = whitespacePattern.matcher(normalized(input)).replaceAll("-")
        val normalized: String = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
        val slug: String = nonLatinPattern.matcher(normalized).replaceAll("").replace("-+".toRegex(), "-").trim('-')
        return slug.lowercase()
    }

    fun computeAnimeHashcode(anime: String) = EncryptionManager.toSHA512(toSlug(getShortName(anime).replace(" ", "")))

    private fun normalized(input: String) = input.replace("œ", "oe").replace("@", "a")

    fun sanitizeXSS(input: String): String = input.replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("&", "&amp;")

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

    fun romanToInt(string: String): Int {
        val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var result = 0
        var prev = 0

        string.reversed().forEach { char ->
            val curr = map[char]!!
            result += if (curr < prev) -curr else curr
            prev = curr
        }

        return result
    }
}