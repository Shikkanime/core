package fr.shikkanime

import fr.shikkanime.services.AnimeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import kotlin.system.exitProcess

data class Song(
    val title: String,
    val artist: String?,
    val type: String,
    val createdAt: ZonedDateTime,
)

suspend fun main() {
    val animeService = Constant.injector.getInstance(AnimeService::class.java)
    val animes = animeService.findAll()

    animes.forEach { anime ->
        println("-- Searching for ${anime.name}")
        // https://api.animethemes.moe/animetheme?page[size]=100&page[number]=1&q=Les carnets de l'apothicaire&filter[has]=song&include=song.artists,anime.images,animethemeentries.videos
        val response = HttpRequest().get(
            "https://api.animethemes.moe/animetheme?page[size]=100&page[number]=1&q=${
                URLEncoder.encode(
                    StringUtils.getShortName(anime.name!!),
                    StandardCharsets.UTF_8
                )
            }&filter[has]=song&include=song.artists,anime.images,animethemeentries.videos"
        )

        if (response.status != HttpStatusCode.OK) {
            println("Error while fetching songs for ${anime.name}")
            return@forEach
        }

        val json = ObjectParser.fromJson(response.bodyAsText())
        val asJsonArray = json.getAsJsonArray("animethemes")

        if (asJsonArray.isEmpty) {
            println("No songs found for ${anime.name}")
            return@forEach
        }

        // Count anime slug
        val slugs = asJsonArray.map {
            it.asJsonObject.getAsJsonObject("anime").getAsString("slug")!!
        }.groupingBy { it }.eachCount().toList().sortedBy { it.first.length }.toMutableList()

        slugs.toList().forEach { slug ->
            slugs.removeIf { it.first.contains(slug.first) && it.first != slug.first }
        }

        if (slugs.size > 1) {
            val normalizedSlug = StringUtils.toSlug(StringUtils.getShortName(anime.name!!)).replace("-", "_")
            slugs.removeIf { it.first != normalizedSlug }
        }

        val slug = slugs.firstOrNull()?.first ?: run {
            println("No slug found for ${anime.name}")
            return@forEach
        }

        println(slugs)

        val songs = asJsonArray.mapNotNull {
            val jsonObject = it.asJsonObject

            if (!jsonObject.getAsJsonObject("anime").getAsString("slug")!!.contains(slug))
                return@mapNotNull null

            if (!(jsonObject.getAsString("group") == null || jsonObject.getAsString("group")?.equals("Original Japanese Version", true) == true))
                return@mapNotNull null

            val song = jsonObject.getAsJsonObject("song")
            val title = song.getAsString("title")!!
            val type = jsonObject.getAsString("type")!!
            val artist = song.getAsJsonArray("artists").joinToString(", ") { it.asJsonObject.getAsString("name") ?: "" }.ifBlank { null }
            val createdAt = ZonedDateTime.parse(jsonObject.getAsString("created_at")!!)
            Song(title, artist, type, createdAt)
        }

        val maxDiff = songs.map { song ->
            val other = songs.filter { it != song }
            val diff = other.map { it.createdAt.toEpochSecond() - song.createdAt.toEpochSecond() }
            song to diff.maxOrNull()?.div(1 * 24 * 60 * 60)
        }

        println(maxDiff)

        println("Found ${songs.size} songs for ${anime.name}")
        println(songs.sortedByDescending { it.createdAt }.joinToString("\n") { "${it.title} - ${it.artist} - ${it.type}" })

    }

    exitProcess(0)
}