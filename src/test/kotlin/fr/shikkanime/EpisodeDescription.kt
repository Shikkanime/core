package fr.shikkanime

import com.google.gson.JsonObject
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper

suspend fun main() {
    val httpRequest = HttpRequest()
    val episodeService = Constant.injector.getInstance(EpisodeService::class.java)
    val crunchyrollEpisodes = episodeService.findAllByPlatform(Platform.CRUN)
    val anonymousAccessToken = CrunchyrollWrapper.getAnonymousAccessToken()
    val cms = CrunchyrollWrapper.getCMS(anonymousAccessToken)
    val adnEpisodes = episodeService.findAllByPlatform(Platform.ANIM)
    val takeSize = 200
    val episodes = (crunchyrollEpisodes + adnEpisodes).shuffled().take(takeSize)
    println("Found ${episodes.size} episodes")

    episodes.forEachIndexed { index, episode ->
        println("Fetching episode description ${index + 1}/${episodes.size}")
        val s = "${episode.anime?.name} - S${episode.season} EP${episode.number}"

        try {
            val content = normalizeContent(episode, httpRequest, anonymousAccessToken, cms) ?: return@forEachIndexed
            val description = normalizeDescription(episode, content)
            println("$s : $description")
            episode.description = description
            episodeService.update(episode)
        } catch (e: Exception) {
            println("Error while fetching episode description for $s")
            e.printStackTrace()
        }
    }

    httpRequest.close()
}

private fun normalizeUrl(episode: Episode): String {
    return when (episode.platform) {
        Platform.CRUN -> {
            val other = "https://www.crunchyroll.com/${episode.anime?.countryCode?.name?.lowercase()}/"
            episode.url!!.replace("https://www.crunchyroll.com/", other)
        }

        else -> episode.url!!
    }
}

private suspend fun normalizeContent(episode: Episode, httpRequest: HttpRequest, accessToken: String, cms: CrunchyrollWrapper.CMS): JsonObject? {
    when (episode.platform) {
        Platform.CRUN -> {
            try {
                httpRequest.getBrowser(normalizeUrl(episode))
            } catch (e: Exception) {
                return null
            }

            val finalUrl = httpRequest.lastPageUrl!!
            val split = finalUrl.split("/")
            val id = split[split.size - 2]
            return CrunchyrollWrapper.getObject(episode.anime!!.countryCode!!.locale, accessToken, cms, id)[0]
        }

        else -> {
            val split = episode.url!!.split("/")
            val animeNameEncoded = split[split.size - 2]
            val animeId = AnimationDigitalNetworkWrapper.getShow(animeNameEncoded).getAsInt("id")!!
            val videoId = split[split.size - 1].split("-")[0].toInt()
            return AnimationDigitalNetworkWrapper.getShowVideos(animeId, videoId)[0]
        }
    }
}

private fun normalizeDescription(episode: Episode, content: JsonObject): String? {
    var description = when (episode.platform) {
        Platform.CRUN -> content.getAsString("description")
        else -> content.getAsString("summary")
    }

    description = description?.replace("\n", "")
    description = description?.replace("\r", "")
    description = description?.trim()
    return description
}