package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.MissedAnime
import fr.shikkanime.entities.miscellaneous.Pageable
import jakarta.persistence.criteria.JoinType
import java.util.*

class MemberFollowAnimeRepository : AbstractRepository<MemberFollowAnime>() {
    suspend fun findAllByMember(memberUuid: UUID): List<MemberFollowAnime> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(
                cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllFollowedAnimes(memberUuid: UUID, page: Int, limit: Int): Pageable<Anime> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Anime::class.java)
            val root = query.from(entityClass)
            query.select(root[MemberFollowAnime_.anime])

            query.where(
                cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid)
            )

            query.orderBy(cb.desc(root[MemberFollowAnime_.followDateTime]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    suspend fun findAllFollowedAnimesUUID(memberUuid: UUID): List<UUID> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(entityClass)
            query.select(root[MemberFollowAnime_.anime][Anime_.uuid])

            query.where(
                cb.equal(root[MemberFollowAnime_.member][Member_.uuid], memberUuid)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllMissedAnimes(
        memberUuid: UUID,
        page: Int,
        limit: Int,
    ): Pageable<MissedAnime> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(MissedAnime::class.java)
            val root = query.from(entityClass)
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

    suspend fun findAllByAnime(anime: Anime): List<MemberFollowAnime> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(
                cb.equal(root[MemberFollowAnime_.anime], anime)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun existsByMemberUuidAndAnimeUuid(memberUuid: UUID, animeUuid: UUID): Boolean {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(entityClass)
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

    suspend fun findByMemberUuidAndAnimeUuid(memberUuid: UUID, animeUuid: UUID): MemberFollowAnime? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

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