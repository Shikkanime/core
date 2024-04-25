package fr.shikkanime

import com.google.gson.JsonObject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.Genre
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsInt
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import java.util.*
import kotlin.system.exitProcess

suspend fun main() {
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val memberService = Constant.injector.getInstance(MemberService::class.java)

    val animes = animeService.findAll().sortedBy { it.name!!.lowercase() }.take(200)
    val unknownGenres = mutableSetOf<String>()
    val usedGenres = mutableSetOf<Genre>()
    val invalidAnimes = mutableSetOf<Anime>()

    val httpRequest = HttpRequest()
    extractGenresFromAnilist(animeService, animes, httpRequest, usedGenres, invalidAnimes, unknownGenres)
    httpRequest.close()

    println("Unknown genres: $unknownGenres")
    println("Unused genres: ${Genre.entries.toTypedArray().filter { genre -> genre !in usedGenres }}")
    println("Invalid animes: ${invalidAnimes.joinToString { StringUtils.getShortName(it.name!!) }}")

    // cf9ff8e0-0aca-4506-b183-9cfd70569e2e
    val member = memberService.find(UUID.fromString("cf9ff8e0-0aca-4506-b183-9cfd70569e2e")) ?: exitProcess(1)
    val followedAnimes = Constant.injector.getInstance(MemberFollowAnimeService::class.java).findAllFollowedAnimes(member).mapNotNull { it.anime }
    val genres = Genre.entries
    val userFeatureVector = getUserFeatureVector(followedAnimes, genres)
    println(userFeatureVector)

    val uuids = followedAnimes.map { anime -> anime.uuid }

    val nonFollowedAnimesFeatureVector = animes.filter { it.uuid !in uuids && it.genres.isNotEmpty() }.map { anime ->
        val featureVector = getAnimeFeatureVector(anime, genres, 1.0)
        // Multiply userFeatureVector by featureVector
        anime to userFeatureVector.mapValues { it.value * featureVector.second[it.key]!! }.values.sum()
    }

    val recommendedAnimes = nonFollowedAnimesFeatureVector.sortedByDescending { it.second }.take(20)

    recommendedAnimes.forEach {
        println("${StringUtils.getShortName(it.first.name!!)} -> ${it.second}")
    }

    exitProcess(0)
}

private fun getUserFeatureVector(followedAnimes: List<Anime>, genres: List<Genre>): Map<Genre, Double> {
    val evaluation = 5.0
    val animesFeatureVector = followedAnimes.map { anime -> getAnimeFeatureVector(anime, genres, evaluation) }
    val userFeatureVector = genres.associateWith { genre -> animesFeatureVector.sumOf { it.second[genre]!! } }
    val sum = userFeatureVector.values.sum()
    return userFeatureVector.mapValues { it.value / sum }
}

private fun getAnimeFeatureVector(
    anime: Anime,
    genres: List<Genre>,
    evaluation: Double
) = anime to genres.associateWith { genre -> if (genre in anime.genres) evaluation else 0.0 }

private suspend fun extractGenresFromAnilist(
    animeService: AnimeService,
    animes: List<Anime>,
    httpRequest: HttpRequest,
    totalGenres: MutableSet<Genre>,
    invalidAnimes: MutableSet<Anime>,
    unknownGenres: MutableSet<String>
) {
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)
    val accessToken = CrunchyrollWrapper.getAnonymousAccessToken()

    animes.forEach {
        println(StringUtils.getShortName(it.name!!))
        val variants = episodeVariantService.findAllByAnime(it)
        val platforms = variants.map { variant -> variant.platform }.toList()

        var name = if (platforms.contains(Platform.CRUN)) {
            val episodeVariant = variants.first { variant -> variant.platform == Platform.CRUN }
            val crunchyrollId = "[A-Z]{2}-CRUN-([A-Z0-9]{9})-[A-Z]{2}-[A-Z]{2}".toRegex()
                .find(episodeVariant.identifier!!)?.groupValues?.get(1)
            CrunchyrollWrapper.getEpisode("en-US", accessToken, crunchyrollId!!).seriesTitle
        } else if (platforms.contains(Platform.ANIM)) {
            val episodeVariant = variants.first { variant -> variant.platform == Platform.ANIM }
            val adnId = "[A-Z]{2}-ANIM-([0-9]{1,5})-[A-Z]{2}-[A-Z]{2}(?:-UNC)?".toRegex()
                .find(episodeVariant.identifier!!)?.groupValues?.get(1)
            val showVideo = AnimationDigitalNetworkWrapper.getShowVideo(adnId!!)

            if (showVideo.show.genres.none { genre -> genre == "Animation japonaise" }) {
                println("Warning: not a Japanese animation")
                invalidAnimes.add(it)
                return@forEach
            }

            val originalTitle = showVideo.show.originalTitle

            if (originalTitle.contains("??")) {
                it.name
            } else {
                val kitaganaTitle = "\\((.*)\\)".toRegex().find(originalTitle)?.groupValues?.get(1)
                println(kitaganaTitle)
                kitaganaTitle ?: originalTitle
            }
        } else {
            it.name
        }

        var response = getAnilistMedia(httpRequest, name)

        while (response.status.value == 429) {
            val retryAfter = response.headers["Retry-After"]?.toIntOrNull() ?: 0
            println("Rate limited, waiting $retryAfter seconds")
            delay(retryAfter * 1000L)
            response = getAnilistMedia(httpRequest, name)
        }

        if (response.status.value == 404 && platforms.contains(Platform.ANIM)) {
            name = it.name
            response = getAnilistMedia(httpRequest, name)
        }

        val json = response.bodyAsText()
        println(json)

        if (response.status.value != 200) {
            println("Failed to get media list")
            invalidAnimes.add(it)
            return@forEach
        }

        val mediaObject = ObjectParser.fromJson(json, JsonObject::class.java).getAsJsonObject("data").getAsJsonObject("Media")
        val malId = mediaObject.getAsInt("idMal")

        if (malId == null) {
            println("Warning: MAL ID not found")
            invalidAnimes.add(it)
            return@forEach
        }

        var myAnimeListResponse = httpRequest.get("https://api.jikan.moe/v4/anime/$malId")

        while (myAnimeListResponse.status.value == 429) {
            println("Rate limited, waiting 1 minute")
            delay(60000)
            myAnimeListResponse = httpRequest.get("https://api.jikan.moe/v4/anime/$malId")
        }

        val myAnimeListJson =
            ObjectParser.fromJson(myAnimeListResponse.bodyAsText(), JsonObject::class.java).getAsJsonObject("data")
        val malGenres =
            myAnimeListJson.getAsJsonArray("genres").map { a -> a.asJsonObject.getAsJsonPrimitive("name").asString }
        val malThemes =
            myAnimeListJson.getAsJsonArray("themes").map { a -> a.asJsonObject.getAsJsonPrimitive("name").asString }

        val genresS = (malGenres + malThemes).toMutableSet()
        val genres = genresS.mapNotNull { genre -> Genre.from(genre) }.toMutableSet()
        totalGenres.addAll(genres)
        unknownGenres.addAll(genresS.filter { genre -> Genre.from(genre) == null })

        if (genres.isEmpty()) {
            invalidAnimes.add(it)
            return@forEach
        }

        it.genres.clear()
        it.genres.addAll(genres)
        animeService.update(it)
        delay(1000L)
    }
}

private suspend fun getAnilistMedia(
    httpRequest: HttpRequest,
    name: String?
) = httpRequest.post(
    "https://graphql.anilist.co",
    headers = mapOf("Content-Type" to "application/json"),
    body = ObjectParser.toJson(
        mapOf(
            "query" to "query { Media(search: \"${
                name?.replace(
                    "\"",
                    "\\\""
                )
            }\", type: ANIME) { id, idMal, title { romaji, english } } }"
        )
    ),
)
