package fr.shikkanime.utils.indexers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.utils.filterNotNull
import fr.shikkanime.utils.sortedWithNotNull
import fr.shikkanime.utils.toLinkedSet
import java.time.ZonedDateTime
import java.util.*

object NewGroupIndexer {
    private data class ReleaseBucket(
        val countryCode: CountryCode,
        val animeUuid: UUID,
        val episodeType: EpisodeType,
        val releaseDateTime: ZonedDateTime,
        val observations: MutableList<Observation> = mutableListOf()
    )

    private data class Observation(
        val countryCode: CountryCode,
        val animeUuid: UUID,
        val animeSlug: String,
        val season: Int,
        val episodeType: EpisodeType,
        val number: Int,
        val variantUuid: UUID,
        val releaseDateTime: ZonedDateTime,
        val audioLocale: String
    )

    data class ElementVariant(
        val uuid: UUID,
        val langType: LangType
    )

    data class Element(
        val countryCode: CountryCode,
        val animeUuid: UUID,
        val animeSlug: String,
        val episodeType: EpisodeType,
        val releaseDateTime: ZonedDateTime,
        val variants: Set<ElementVariant>
    )

    private val buckets = mutableListOf<ReleaseBucket>()
    private val index = mutableListOf<Element>()

    fun addToBucket(
        countryCode: CountryCode,
        animeUuid: UUID,
        animeSlug: String,
        season: Int,
        episodeType: EpisodeType,
        number: Int,
        variantUuid: UUID,
        releaseDateTime: ZonedDateTime,
        audioLocale: String,
    ) {
        val observation = Observation(countryCode, animeUuid, animeSlug, season, episodeType, number, variantUuid, releaseDateTime, audioLocale)
        val fromReleaseDateTime = releaseDateTime.minusHours(2)
        val toReleaseDateTime = releaseDateTime.plusHours(2)

        val bucket = buckets.find { releaseBucket ->
            releaseBucket.countryCode == countryCode
                    && releaseBucket.animeUuid == animeUuid
                    && releaseBucket.episodeType == episodeType
                    && releaseBucket.releaseDateTime in fromReleaseDateTime..toReleaseDateTime
        }

        if (bucket != null) {
            bucket.observations.removeAll { it.variantUuid == variantUuid }
            bucket.observations += observation
        } else {
            buckets += ReleaseBucket(countryCode, animeUuid, episodeType, releaseDateTime, mutableListOf(observation))
        }
    }

    fun buildIndex() {
        index.clear()

        buckets.forEach { bucket ->
            val episodeNumbersByLocale = bucket.observations
                .groupBy { LangType.fromAudioLocale(it.countryCode, it.audioLocale) }
                .mapValues { (_, observations) ->
                    observations.mapTo(sortedSetOf()) { it.season * 1000 + it.number }
                }

            val groups = bucket.observations
                .groupBy { observation -> episodeNumbersByLocale.getValue(LangType.fromAudioLocale(observation.countryCode, observation.audioLocale)) }

            groups.values.forEach { observations ->
                val first = observations.first()
                index += Element(
                    countryCode = first.countryCode,
                    animeUuid = first.animeUuid,
                    animeSlug = first.animeSlug,
                    episodeType = first.episodeType,
                    releaseDateTime = first.releaseDateTime,
                    variants = observations.mapTo(mutableSetOf()) { ElementVariant(it.variantUuid, LangType.fromAudioLocale(it.countryCode, it.audioLocale)) }
                )
            }
        }
    }

    fun getElements(
        predicate: ((Element) -> Boolean)? = null,
        comparator: Comparator<Element>? = null
    ): Set<Element> {
        return index.asSequence()
            .filterNotNull(predicate)
            .sortedWithNotNull(comparator)
            .toLinkedSet()
    }

    fun getPaginatedElements(
        page: Int,
        limit: Int,
        predicate: ((Element) -> Boolean)? = null,
        comparator: Comparator<Element>? = null
    ): Pageable<Element> {
        require(page > 0) { "Page number must be greater than 0" }
        require(limit > 0) { "Limit must be greater than 0" }

        val filteredElements = index.filterNotNull(predicate)

        val paginatedElements = filteredElements.asSequence()
            .sortedWithNotNull(comparator)
            .drop((page - 1) * limit)
            .take(limit)
            .toLinkedSet()

        return Pageable(
            data = paginatedElements,
            page = page,
            limit = limit,
            total = filteredElements.size.toLong()
        )
    }
}