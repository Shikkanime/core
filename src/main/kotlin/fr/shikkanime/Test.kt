package fr.shikkanime

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import kotlin.system.exitProcess

enum class ErrorType {
    INVALID_CHAIN_SEASON,
    INVALID_RELEASE_DATE,
    INVALID_EPISODE_NUMBER,
    INVALID_CHAIN_EPISODE_NUMBER,
}

data class Error(
    val type: ErrorType,
    val reason: String,
)

private fun toString(episodeMapping: EpisodeMapping): String {
    val episodeTypeLabel = when (episodeMapping.episodeType!!) {
        EpisodeType.EPISODE -> "EP"
        EpisodeType.SPECIAL -> "SP"
        EpisodeType.FILM -> "FILM"
        EpisodeType.SUMMARY -> "SUM"
        EpisodeType.SPIN_OFF -> "SPIN-OFF"
    }
    return "S${episodeMapping.season} $episodeTypeLabel${episodeMapping.number}"
}

fun main() {
    val episodeMappingCacheService = Constant.injector.getInstance(EpisodeMappingCacheService::class.java)
    val animeCacheService = Constant.injector.getInstance(AnimeCacheService::class.java)
    MapCache.loadAll()

    val invalidAnimes = mutableMapOf<Anime, MutableSet<Error>>()

    val sortedWith = episodeMappingCacheService.findAll().sortedWith(
        compareBy(
            { it.releaseDateTime },
            { it.season },
            { it.episodeType },
            { it.number }
        )
    )

    sortedWith.mapNotNull { it.anime }
        .distinctBy { it.uuid }
        .forEach { anime ->
            val seasons = sortedWith.filter { it.anime!!.uuid == anime.uuid }
                .mapNotNull { it.season }
                .distinct()
                .sorted()

            seasons.zipWithNext().forEach { (current, next) ->
                if (current + 1 != next) {
                    invalidAnimes.getOrPut(anime) { mutableSetOf() }
                        .add(Error(ErrorType.INVALID_CHAIN_SEASON, "$current -> $next"))
                }
            }
        }

    sortedWith.groupBy { "${it.anime!!.uuid!!}${it.season}${it.episodeType}" }
        .values.forEach { episodes ->
            val anime = episodes.first().anime!!
            val (audioLocales, _) = animeCacheService.findAudioLocalesAndSeasonsByAnimeCache(anime)!!

            if (episodes.first().episodeType == EpisodeType.EPISODE) {
                episodes.groupBy { it.releaseDateTime.toLocalDate() }
                    .values.forEach {
                        if (it.size > 3 && !(audioLocales.size == 1 && LangType.fromAudioLocale(anime.countryCode!!, audioLocales.first()) == LangType.VOICE)) {
                            it.forEach { episodeMapping ->
                                invalidAnimes.getOrPut(episodeMapping.anime!!) { mutableSetOf() }
                                    .add(Error(ErrorType.INVALID_RELEASE_DATE, "S${episodeMapping.season} ${episodeMapping.releaseDateTime.toLocalDate()}[${it.size}]"))
                                return@forEach
                            }
                        }
                    }
            }

            episodes.filter { it.number!! < 0 }
                .forEach { episodeMapping ->
                    invalidAnimes.getOrPut(episodeMapping.anime!!) { mutableSetOf() }
                        .add(Error(ErrorType.INVALID_EPISODE_NUMBER, toString(episodeMapping)))
                }

            episodes.zipWithNext().forEach { (current, next) ->
                if (current.number!! + 1 != next.number!!) {
                    invalidAnimes.getOrPut(current.anime!!) { mutableSetOf() }
                        .add(Error(ErrorType.INVALID_CHAIN_EPISODE_NUMBER, "${toString(current)} -> ${toString(next)}"))
                }
            }
        }

    if (invalidAnimes.isNotEmpty()) {
        println("Invalid animes (${invalidAnimes.size}):")
        invalidAnimes.forEach { (anime, errors) ->
            println("  - ${StringUtils.getShortName(anime.name!!)}:")
            errors.groupBy { it.type }.forEach { (type, errors) ->
                println("    - $type (${errors.size}):")
                errors.forEach { println("      - ${it.reason}") }
            }
        }
    } else {
        println("No invalid animes found.")
    }

    exitProcess(0)
}