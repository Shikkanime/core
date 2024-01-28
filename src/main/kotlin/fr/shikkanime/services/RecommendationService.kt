package fr.shikkanime.services

import fr.shikkanime.entities.Anime

object RecommendationService {
    fun getRecommendations(animes: List<Anime>, buildRecommendationAnime: Anime): Set<Pair<Anime, Double>> {
        return animes
            .asSequence()
            .filter { it.uuid != buildRecommendationAnime.uuid }
            .map { it to getSimilarity(buildRecommendationAnime, it) }
            .sortedByDescending { it.second }
            .toSet()
    }

    private fun getSimilarity(anime1: Anime, anime2: Anime): Double {
        val genres1 = anime1.genres.map { it.name }.toSet()
        val genres2 = anime2.genres.map { it.name }.toSet()
        val intersection = genres1.intersect(genres2)
        val union = genres1.union(genres2)
        return intersection.size.toDouble() / union.size.toDouble()
    }
}