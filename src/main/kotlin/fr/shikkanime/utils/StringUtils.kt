package fr.shikkanime.utils

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
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

    fun unSanitizeXSS(input: String): String = input.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")

    fun getHash(countryCode: CountryCode, platform: Platform, id: String, langType: LangType) = "${countryCode}-${platform}-$id-$langType"
}