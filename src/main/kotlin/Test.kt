import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.StringUtils
import java.time.ZonedDateTime
import kotlin.system.exitProcess

data class AnimeRelease(
    val anime: Anime,
    val platforms: Set<Platform>,
    val releaseDateTime: ZonedDateTime,
    val minSeason: Int,
    val maxSeason: Int,
    val episodeType: EpisodeType,
    val minNumber: Int,
    val maxNumber: Int,
    val langTypes: Set<LangType>,
    val mappings: Set<EpisodeMapping>
) {
    override fun toString(): String {
        val seasonString = if (minSeason == maxSeason) "S$minSeason" else "S$minSeason-$maxSeason"
        val numberString = if (minNumber == maxNumber) "$minNumber" else "$minNumber-$maxNumber"

        return "${StringUtils.getShortName(anime.name!!)} [${platforms.joinToString()}]"
            .plus("\n$seasonString $episodeType $numberString")
            .plus("\n${langTypes.joinToString("\n") { "  - ${it.name}" }}")
    }
}

fun findAllAnimeReleases(
    page: Int,
    limit: Int,
): List<AnimeRelease> {
    val groupedVariants = mutableMapOf<Triple<ClosedRange<ZonedDateTime>, Anime, EpisodeType>, List<EpisodeVariant>>()
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)

    val variants = episodeVariantService.findAll().asSequence()
        .sortedWith(compareByDescending<EpisodeVariant> { it.releaseDateTime }
            .thenByDescending { it.mapping!!.anime!!.name }
            .thenByDescending { it.mapping!!.season!! }
            .thenByDescending { it.mapping!!.episodeType!! }
            .thenByDescending { it.mapping!!.number!! })

    for (variant in variants) {
        val animeUuid = variant.mapping!!.anime!!.uuid
        val validRange = groupedVariants.keys.firstOrNull { variant.releaseDateTime in it.first && it.second.uuid == animeUuid && it.third == variant.mapping!!.episodeType }

        if (validRange != null) {
            groupedVariants[validRange] = groupedVariants[validRange]!! + variant
        } else {
            val newRange = variant.releaseDateTime.minusHours(1)..variant.releaseDateTime.plusHours(1)
            groupedVariants[Triple(newRange, variant.mapping!!.anime!!, variant.mapping!!.episodeType!!)] = listOf(variant)
        }
    }

    return groupedVariants.map { (key, variants) ->
        AnimeRelease(
            anime = key.second,
            platforms = variants.map { it.platform!! }.toSet(),
            releaseDateTime = variants.minOf { it.releaseDateTime },
            minSeason = variants.minOf { it.mapping!!.season!! },
            maxSeason = variants.maxOf { it.mapping!!.season!! },
            episodeType = key.third,
            minNumber = variants.minOf { it.mapping!!.number!! },
            maxNumber = variants.maxOf { it.mapping!!.number!! },
            langTypes = variants.map { LangType.fromAudioLocale(it.mapping!!.anime!!.countryCode!!, it.audioLocale!!) }.toSet(),
            mappings = variants.map { it.mapping!! }.toSet()
        )
    }.subList((page - 1) * limit, page * limit)
}

fun main() {
    println(findAllAnimeReleases(2, 9).joinToString("\n\n"))
    exitProcess(0)
}