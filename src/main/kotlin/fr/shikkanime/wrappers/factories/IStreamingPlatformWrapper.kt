package fr.shikkanime.wrappers.factories

interface IStreamingPlatformWrapper<T, S, E : IStreamingPlatformWrapper.Id<T>> {
    interface Id<T> {
        val id: T
    }

    suspend fun getShow(locale: String, id: T): S
    suspend fun getEpisodesByShowId(locale: String, showId: T): Array<E>

    suspend fun getPreviousEpisode(locale: String, showId: T, episodeId: T): E? {
        val episodes = getEpisodesByShowId(locale, showId)
        val episodeIndex = episodes.indexOfFirst { it.id == episodeId }
        require(episodeIndex != -1) { "Episode not found" }
        return if (episodeIndex == 0) null else episodes[episodeIndex - 1]
    }

    suspend fun getNextEpisode(locale: String, showId: T, episodeId: T): E? {
        val episodes = getEpisodesByShowId(locale, showId)
        val episodeIndex = episodes.indexOfFirst { it.id == episodeId }
        require(episodeIndex != -1) { "Episode not found" }
        return if (episodeIndex == episodes.size - 1) null else episodes[episodeIndex + 1]
    }
}