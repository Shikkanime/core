package fr.shikkanime

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.utils.Constant
import kotlin.system.exitProcess

fun main() {
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)

    animeService.findAll().forEach { anime ->
        val episodes = episodeMappingService.findAllByAnime(anime)

        episodes.groupBy { it.season to it.episodeType }
            .forEach { (_, seasonTypeEpisodes) ->
                val sortedEpisodes = seasonTypeEpisodes.sortedWith(
                    compareBy({ it.releaseDateTime }, { it.number })
                )

                checkEpisodeNumbering(sortedEpisodes, anime)
            }
    }

    exitProcess(0)
}

private fun checkEpisodeNumbering(episodes: List<EpisodeMapping>, anime: Anime) {
    val episodeThatHaveTheSameReleaseDate = mutableSetOf<EpisodeMapping>()

    episodes.zipWithNext { current, next ->
        if (next.number != current.number?.plus(1)) {
            println("Anomaly detected in anime ${anime.name}:")
            println("Season: ${current.season}, Episode type: ${current.episodeType}")
            println("Current episode number: ${current.number}, Next episode number: ${next.number}")
            return
        }

        if (next.releaseDateTime.toLocalDate() == current.releaseDateTime.toLocalDate()) {
            episodeThatHaveTheSameReleaseDate.add(current)
            episodeThatHaveTheSameReleaseDate.add(next)
        }
    }

    // If 3/4 of the episodes have the same release date, it's probably a mistake
    if (episodes.size > 1 && episodeThatHaveTheSameReleaseDate.size >= episodes.size * 3 / 4) {
        println("Anomaly detected in anime ${anime.name}:")
        println("Season: ${episodes.first().season}, Episode type: ${episodes.first().episodeType}")
        println("More than 3/4 of the episodes have the same release date")
    }
}