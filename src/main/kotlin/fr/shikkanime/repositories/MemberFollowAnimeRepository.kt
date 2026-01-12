package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.MissedAnime
import fr.shikkanime.entities.miscellaneous.Pageable
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.JoinType
import java.util.*

class MemberFollowAnimeRepository : AbstractRepository<MemberFollowAnime>() {
    override fun getEntityClass() = MemberFollowAnime::class.java

    fun findAllFollowedAnimes(memberUuid: UUID, page: Int, limit: Int): Pageable<Anime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Anime::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowAnime_.anime])

            query.where(
                cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid)
            )

            query.orderBy(cb.desc(root[MemberFollowAnime_.followDateTime]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    fun findAllFollowedAnimesUUID(memberUuid: UUID): List<UUID> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowAnime_.anime][Anime_.uuid])

            query.where(
                cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllFollowedWithGenresAndTags(memberUuid: UUID): List<Tuple> {
        return database.entityManager.use { em ->
            val cb = em.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            // Joins
            val anime = root.join(MemberFollowAnime_.anime)
            val episodes = anime.join(Anime_.mappings, JoinType.LEFT)
            val followedEpisodes = episodes.join(EpisodeMapping_.memberFollowEpisodes, JoinType.LEFT).apply {
                on(cb.equal(this[MemberFollowEpisode_.member][Member_.uuid], memberUuid))
            }
            val genres = anime.join(Anime_.genres, JoinType.LEFT)
            val animeTags = anime.join(Anime_.tags, JoinType.LEFT)
            val tags = animeTags.join(AnimeTag_.tag, JoinType.LEFT)

            // Aggregations & Expressions
            val genresAgg = cb.function("array_agg", Array<String?>::class.java, genres[Genre_.name])
            val tagsAgg = cb.function("array_agg", Array<String?>::class.java, tags[Tag_.name])
            val followRatio = cb.quot(
                cb.prod(cb.countDistinct(followedEpisodes[MemberFollowEpisode_.episode]), 1f),
                cb.countDistinct(episodes[EpisodeMapping_.uuid])
            ).`as`(Float::class.java)

            query.select(
                cb.tuple(
                    anime[Anime_.uuid],
                    anime[Anime_.name],
                    genresAgg,
                    tagsAgg,
                    followRatio
                )
            ).where(
                cb.and(
                    cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid),
                    cb.or(cb.isNotNull(genres), cb.isNotNull(tags))
                )
            ).orderBy(cb.asc(cb.lower(anime[Anime_.name])))
                .groupBy(anime[Anime_.uuid], anime[Anime_.name])

            createReadOnlyQuery(em, query).resultList
        }
    }

    fun findAllMissedAnimes(
        memberUuid: UUID,
        page: Int,
        limit: Int,
    ): Pageable<MissedAnime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(MissedAnime::class.java)
            val root = query.from(getEntityClass())
            val anime = root.join(MemberFollowAnime_.anime)
            val episodeMapping = anime.join(Anime_.mappings, JoinType.LEFT)
            // And episode type is not SUMMARY
            episodeMapping.on(cb.notEqual(episodeMapping[EpisodeMapping_.episodeType], EpisodeType.SUMMARY))
            val memberFollowEpisode = episodeMapping.join(EpisodeMapping_.memberFollowEpisodes, JoinType.LEFT)
            memberFollowEpisode.on(cb.equal(memberFollowEpisode[MemberFollowEpisode_.member][Member_.uuid], memberUuid))

            val memberPredicate = cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid)
            val memberFollowEpisodePredicate = cb.and(cb.isNull(memberFollowEpisode[MemberFollowEpisode_.episode]))

            query.select(
                cb.construct(
                    MissedAnime::class.java,
                    anime,
                    cb.countDistinct(episodeMapping[EpisodeMapping_.uuid]).`as`(Long::class.java)
                )
            ).where(memberPredicate, memberFollowEpisodePredicate)
                .groupBy(anime)
                .having(cb.greaterThan(cb.countDistinct(episodeMapping[EpisodeMapping_.uuid]), 0))
                .orderBy(cb.desc(anime[Anime_.lastReleaseDateTime]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    fun findAllByAnime(anime: Anime): List<MemberFollowAnime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[MemberFollowAnime_.anime], anime)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun existsByMemberUuidAndAnimeUuid(memberUuid: UUID, animeUuid: UUID): Boolean {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(getEntityClass())
            query.select(cb.literal(1))

            query.where(
                cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid),
                cb.equal(root[MemberFollowAnime_.anime][Anime_.uuid], animeUuid)
            )

            createReadOnlyQuery(it.createQuery(query).setMaxResults(1))
                .resultList
                .isNotEmpty()
        }
    }

    fun findByMemberUuidAndAnimeUuid(memberUuid: UUID, animeUuid: UUID): MemberFollowAnime? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid),
                cb.equal(root[MemberFollowAnime_.anime][Anime_.uuid], animeUuid)
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}