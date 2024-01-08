package fr.shikkanime.utils

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import java.util.*

object StringUtils {
    fun getShortName(fullName: String): String {
        val regexs = listOf("-.*-".toRegex(), "Saison \\d*".toRegex(), "\\(\\d*\\)".toRegex())
        val separators = listOf(":", ",")
        var shortName = fullName

        separators.forEach { separator ->
            if (shortName.contains(separator)) {
                val split = shortName.split(separator)
                val firstPart = split[0].trim()
                val lastPart = split.subList(1, split.size).joinToString(" ").trim()

                if (lastPart.count { it == ' ' } >= 2) {
                    shortName = firstPart
                }
            }
        }

        regexs.forEach { regex ->
            shortName = regex.replace(shortName, "")
        }

        shortName = shortName.replace(" +".toRegex(), " ")
        return shortName.trim()
    }

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
}