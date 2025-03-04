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
    private const val ANIME_STRING = "Anime "
    private const val EMPTY_STRING = ""
    private const val SPACE_STRING = " "
    private const val DASH_STRING = "-"
    const val ROMAN_NUMBERS_CHECK = "VI"

    private val nonLatinPattern: Pattern = Pattern.compile("[^\\w-]")
    private val whitespacePattern: Pattern = Pattern.compile("\\s|:\\b|\\.\\b|/\\b|&\\b")
    private val regex = "( [-!~].*[-!~](?: |$))|( Saison \\d*)|(?:: )?\\(\\d*\\)| ([$ROMAN_NUMBERS_CHECK]+$)".toRegex()
    private val separators = listOf(":", ",", "!", "–", " so ", " - ")
    private val encasedRegex = "<.*> ?.*".toRegex()
    private val duplicateSpaceRegex = " +".toRegex()
    private val wordSeparatorRegex = "[ \\-']".toRegex()

    fun removeAnimeNamePart(name: String) = name.replace(ANIME_STRING, EMPTY_STRING).trim()

    private fun isAllPartsHaveSameAmountOfWords(parts: List<String>) = parts.map { it.trim().split(wordSeparatorRegex).size }.distinct().size == 1

    fun getShortName(fullName: String): String {
        val normalizedName = when {
            fullName.contains(encasedRegex) -> fullName.replace("<", EMPTY_STRING).replace(">", ": ").trim()
            fullName.contains(ANIME_STRING) -> removeAnimeNamePart(fullName)
            else -> regex.replace(fullName, SPACE_STRING).trim()
        }

        for (separator in separators) {
            if (!normalizedName.contains(separator)) {
                continue
            }

            val split = normalizedName.split(separator)
            val firstPart = split[0].trim()
            val lastPart = split.drop(1).joinToString(SPACE_STRING).trim()

            // Check if the last part of the name contains at least two spaces
            // and if the first part meets certain length conditions based on the separator
            // and if the parts do not have the same amount of words when the separator is not a colon
            if (lastPart.count { it == ' ' } >= 2 &&
                firstPart.length > (if (separator == ",") 6 else 5) &&
                (separator == ":" || !isAllPartsHaveSameAmountOfWords(split))) {
                return firstPart.replace(duplicateSpaceRegex, SPACE_STRING).trim()
            }
        }

        return normalizedName.replace(duplicateSpaceRegex, SPACE_STRING).trim()
    }

    fun getHashtag(shortName: String) =
        normalized(shortName.replace("1/2", EMPTY_STRING)).lowercase().capitalizeWords()
        .replace(" S ", " s ")
        .replace(" T ", " t ")
        .filter { it.isLetterOrDigit() }

    fun String.capitalizeWords(): String {
        val pattern = "[ ,\\-:/\"&<]|(^')".toPattern()

        return this.split(pattern).joinToString(SPACE_STRING) {
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

    private fun toEpisodeString(countryCode: CountryCode, season: Int, showSeason: Boolean, showSeparator: Boolean, episodeType: EpisodeType, number: Int): String {
        return buildString {
            append(if (showSeason) toSeasonString(countryCode, season) else EMPTY_STRING)
            append(if (showSeason && showSeparator) " • " else SPACE_STRING)
            append(getEpisodeTypeLabel(countryCode, episodeType))
            append(" $number")
        }.trim()
    }

    fun toEpisodeMappingString(episode: EpisodeMappingDto, showSeason: Boolean = true, separator: Boolean = true) = toEpisodeString(episode.anime.countryCode, episode.season, showSeason, separator, episode.episodeType, episode.number)
    fun toEpisodeMappingString(episode: EpisodeMapping) = toEpisodeString(episode.anime!!.countryCode!!, episode.season!!, true, true, episode.episodeType!!, episode.number!!)

    fun toEpisodeVariantString(episodes: List<EpisodeVariantDto>): String {
        val mapping = episodes.first().mapping

        return buildString {
            append(toEpisodeMappingString(mapping))
            append(" ${episodes.map { it.audioLocale }.distinct().joinToString(" & ") { toLangTypeString(mapping.anime.countryCode, it) }}")
        }
    }

    fun toSlug(input: String): String {
        val nowhitespace: String = whitespacePattern.matcher(normalized(input)).replaceAll(DASH_STRING)
        val normalized: String = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
        val slug: String = nonLatinPattern.matcher(normalized).replaceAll(EMPTY_STRING).replace("-+".toRegex(), DASH_STRING).trim('-')
        return slug.lowercase()
    }

    fun computeAnimeHashcode(slug: String) = EncryptionManager.toSHA512(slug.replace(DASH_STRING, EMPTY_STRING))

    private fun normalized(input: String) = input.replace("œ", "oe").replace("@", "a")

    fun getIdentifier(
        countryCode: CountryCode,
        platform: Platform,
        id: String,
        audioLocale: String,
        uncensored: Boolean = false
    ) = "${countryCode}-${platform}-$id-${audioLocale.uppercase()}${if (uncensored) "-UNC" else EMPTY_STRING}"

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
            .joinToString(EMPTY_STRING)
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