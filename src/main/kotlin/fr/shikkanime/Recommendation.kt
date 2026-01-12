package fr.shikkanime

import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.GenreService
import fr.shikkanime.services.MemberFollowAnimeService
import fr.shikkanime.services.TagService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Matrix
import jakarta.persistence.Tuple
import java.util.*
import kotlin.system.exitProcess

private enum class Type {
    GENRE, TAG
}

private data class Context(
    val animeUuid: UUID,
    val animeName: String,
    val genres: List<Pair<Type, String>>,
    val tags: List<Pair<Type, String>>,
    val followRatio: Float? = null
)

private fun <T> Tuple.getOrNull(index: Int, clazz: Class<T>): T? = try {
    this[index, clazz]
} catch (_: IllegalArgumentException) {
    null
}

fun main() {
    // User Uuid : cf9ff8e0-0aca-4506-b183-9cfd70569e2e
    val memberFollowAnimeService = Constant.injector.getInstance(MemberFollowAnimeService::class.java)
    val genreService = Constant.injector.getInstance(GenreService::class.java)
    val tagService = Constant.injector.getInstance(TagService::class.java)
    val animeService = Constant.injector.getInstance(AnimeService::class.java)

    val genres = genreService.findAll().map { Type.GENRE to it.name!! }
    val tags = tagService.findAll().map { Type.TAG to it.name!! }
    val genresAndTags = genres + tags

    val transformToContext: (Tuple) -> Context = { tuple ->
        Context(
            tuple[0, UUID::class.java],
            tuple[1, String::class.java],
            tuple[2, Array<String?>::class.java].asSequence().filterNotNull().sortedBy(String::lowercase).map { Type.GENRE to it }.toList(),
            tuple[3, Array<String?>::class.java].asSequence().filterNotNull().sortedBy(String::lowercase).map { Type.TAG to it }.toList(),
            tuple.getOrNull(4, Float::class.java)
        )
    }
    val watchlistMember =
        memberFollowAnimeService.findAllFollowedWithGenresAndTags(UUID.fromString("cf9ff8e0-0aca-4506-b183-9cfd70569e2e")).map(transformToContext)
    val watchlistMatrix = Matrix(watchlistMember.size, genresAndTags.size)

    watchlistMember.forEachIndexed { index, context ->
        val animeGenreAndTags = context.genres + context.tags

        animeGenreAndTags.forEach { pair ->
            val pairIndex = genresAndTags.indexOf(pair)
            require(pairIndex >= 0) { "Genre or Tag ${pair.second} not found in master list" }
            watchlistMatrix[index, pairIndex] = .2f + (context.followRatio!! * .8f)
        }
    }

    val memberWatchlistWeights = watchlistMatrix.sumRows().normalize()
    println(memberWatchlistWeights)

    val animes = animeService.findAllWithGenresAndTags().map(transformToContext)
    val animesMatrix = Matrix(animes.size, genresAndTags.size)

    animes.forEachIndexed { index, context ->
        val animeGenreAndTags = context.genres + context.tags

        animeGenreAndTags.forEach { pair ->
            val pairIndex = genresAndTags.indexOf(pair)
            require(pairIndex >= 0) { "Genre or Tag ${pair.second} not found in master list" }
            animesMatrix[index, pairIndex] = 1f
        }
    }

    val nonWatchlistWeights = (memberWatchlistWeights * animesMatrix).sumColumns()

    animes.zip(nonWatchlistWeights.data.toList())
        .asSequence()
        .filterNot { (context, _) -> watchlistMember.any { it.animeUuid == context.animeUuid } }
        .sortedByDescending(Pair<Context, Float>::second)
        .filter { (_, f) -> f > 0 }
        .forEach { (context, f) ->
            println("${context.animeName}: $f")
        }

    exitProcess(0)
}