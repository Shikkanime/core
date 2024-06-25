package fr.shikkanime.repositories

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.services.MemberFollowAnimeService
import jakarta.persistence.Tuple
import java.time.ZonedDateTime

class EpisodeVariantRepository : AbstractRepository<EpisodeVariant>() {
    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    override fun getEntityClass() = EpisodeVariant::class.java

    fun findAllByDateRange(
        member: Member?,
        countryCode: CountryCode,
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): List<EpisodeVariant> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        val countryPredicate =
            cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.countryCode], countryCode)
        val datePredicate = cb.between(root[EpisodeVariant_.releaseDateTime], start, end)
        val predicates = mutableListOf(countryPredicate, datePredicate)

        member?.let {
            val animePredicate = root[EpisodeVariant_.mapping][EpisodeMapping_.anime][Anime_.uuid].`in`(
                memberFollowAnimeService.findAllFollowedAnimesUUID(it)
            )
            predicates.add(animePredicate)
        }

        query.where(cb.and(*predicates.toTypedArray()))

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findAllTypeIdentifier(): List<Tuple> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createTupleQuery()
        val root = query.from(getEntityClass())

        val episodeMappingPath = root[EpisodeVariant_.mapping]
        val animePath = episodeMappingPath[EpisodeMapping_.anime]

        query.multiselect(
            animePath[Anime_.countryCode],
            animePath[Anime_.uuid],
            root[EpisodeVariant_.platform],
            episodeMappingPath[EpisodeMapping_.episodeType],
            episodeMappingPath[EpisodeMapping_.season],
            episodeMappingPath[EpisodeMapping_.number],
            root[EpisodeVariant_.audioLocale],
            root[EpisodeVariant_.identifier]
        )

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findAllAudioLocalesByAnime(anime: Anime): List<String> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(String::class.java)
        val root = query.from(getEntityClass())

        query.select(root[EpisodeVariant_.audioLocale])
            .where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime))
            .distinct(true)

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findAllAudioLocalesByMapping(mapping: EpisodeMapping): List<String> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(String::class.java)
        val root = query.from(getEntityClass())

        query.select(root[EpisodeVariant_.audioLocale])
            .where(cb.equal(root[EpisodeVariant_.mapping], mapping))
            .distinct(true)

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findAllByAnime(anime: Anime): List<EpisodeVariant> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime))

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findAllByMapping(mapping: EpisodeMapping): List<EpisodeVariant> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(cb.equal(root[EpisodeVariant_.mapping], mapping))

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findAllSimulcastedByAnime(anime: Anime): List<EpisodeMapping> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(EpisodeMapping::class.java)
        val root = query.from(getEntityClass())

        query.distinct(true)
            .select(root[EpisodeVariant_.mapping])
            .where(
                cb.and(
                    cb.notEqual(root[EpisodeVariant_.audioLocale], anime.countryCode!!.locale),
                    cb.notEqual(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], EpisodeType.FILM),
                    cb.notEqual(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType], EpisodeType.SUMMARY),
                    cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime)
                )
            )
            .orderBy(
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.releaseDateTime]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.season]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.episodeType]),
                cb.asc(root[EpisodeVariant_.mapping][EpisodeMapping_.number]),
            )

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findByIdentifier(identifier: String): EpisodeVariant? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(cb.equal(root[EpisodeVariant_.identifier], identifier))

        return createReadOnlyQuery(entityManager, query)
            .resultList
            .firstOrNull()
    }

    fun findMinAndMaxReleaseDateTimeByMapping(mapping: EpisodeMapping): Pair<ZonedDateTime, ZonedDateTime> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(Tuple::class.java)
        val root = query.from(getEntityClass())

        query.multiselect(
            cb.least(root[EpisodeVariant_.releaseDateTime]),
            cb.greatest(root[EpisodeVariant_.releaseDateTime]),
        )
            .where(cb.equal(root[EpisodeVariant_.mapping], mapping))

        return createReadOnlyQuery(entityManager, query)
            .singleResult
            .let { it[0] as ZonedDateTime to it[1] as ZonedDateTime }
    }

    fun findMinAndMaxReleaseDateTimeByAnime(anime: Anime): Pair<ZonedDateTime, ZonedDateTime> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(Tuple::class.java)
        val root = query.from(getEntityClass())

        query.multiselect(
            cb.least(root[EpisodeVariant_.releaseDateTime]),
            cb.greatest(root[EpisodeVariant_.releaseDateTime]),
        )
            .where(cb.equal(root[EpisodeVariant_.mapping][EpisodeMapping_.anime], anime))

        return createReadOnlyQuery(entityManager, query)
            .singleResult
            .let { it[0] as ZonedDateTime to it[1] as ZonedDateTime }
    }
}