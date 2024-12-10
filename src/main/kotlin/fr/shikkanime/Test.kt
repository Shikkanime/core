package fr.shikkanime

import fr.shikkanime.entities.Anime
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

fun main() {
    val episodeMappingCacheService = Constant.injector.getInstance(EpisodeMappingCacheService::class.java)
    MapCache.loadAll()

    val invalidAnimes = mutableMapOf<Anime, MutableSet<ErrorType>>()

    val sortedWith = episodeMappingCacheService.findAll()
        .sortedWith(
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
            val seasons = sortedWith.filter { it.anime!!.uuid == anime.uuid }.mapNotNull { it.season }.distinct().sorted()

            seasons.zipWithNext().forEach { (current, next) ->
                if (current + 1 != next) {
                    invalidAnimes.getOrPut(anime) { mutableSetOf() }.add(ErrorType.INVALID_CHAIN_SEASON)
                }
            }
        }

    sortedWith.groupBy { "${it.anime!!.uuid!!}${it.season}${it.episodeType}" }
        .values.forEach { episodes ->
            episodes.groupBy { it.releaseDateTime.toLocalDate() }.values.forEach {
                if (it.size > 3) {
                    it.forEach {
                        invalidAnimes.getOrPut(it.anime!!) { mutableSetOf() }.add(ErrorType.INVALID_RELEASE_DATE)
                    }
                }
            }

            episodes.filter { it.number!! < 0 }.forEach {
                invalidAnimes.getOrPut(it.anime!!) { mutableSetOf() }.add(ErrorType.INVALID_EPISODE_NUMBER)
            }

            episodes.zipWithNext().forEachIndexed { _, (current, next) ->
                if (current.number!! + 1 != next.number!!) {
                    invalidAnimes.getOrPut(current.anime!!) { mutableSetOf() }.add(ErrorType.INVALID_CHAIN_EPISODE_NUMBER)
                }
            }
        }

    if (invalidAnimes.isNotEmpty()) {
        println("Invalid animes (${invalidAnimes.size}):")
        invalidAnimes.forEach { (anime, errors) ->
            println("  - ${StringUtils.getShortName(anime.name!!)}:")
            errors.forEach {
                println("    - ${it.name}")
            }
        }
    } else {
        println("No invalid animes found.")
    }

    exitProcess(0)
}