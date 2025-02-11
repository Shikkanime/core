package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.MissedAnime
import fr.shikkanime.entities.miscellaneous.Pageable
import jakarta.persistence.criteria.JoinType
import java.util.*

class MemberFollowAnimeRepository : AbstractRepository<MemberFollowAnime>() {
    override fun getEntityClass() = MemberFollowAnime::class.java

    fun findAllFollowedAnimes(member: Member, page: Int, limit: Int): Pageable<Anime> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Anime::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowAnime_.anime])

            query.where(
                cb.equal(root[MemberFollowAnime_.member], member)
            )

            query.orderBy(cb.desc(root[MemberFollowAnime_.followDateTime]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    fun findAllFollowedAnimesUUID(member: Member): List<UUID> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowAnime_.anime][Anime_.uuid])

            query.where(
                cb.equal(root[MemberFollowAnime_.member], member)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllMissedAnimes(
        member: Member,
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
            memberFollowEpisode.on(cb.equal(memberFollowEpisode[MemberFollowEpisode_.member], member))

            val memberPredicate = cb.equal(root[MemberFollowAnime_.member], member)
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

    fun existsByMemberAndAnime(member: Member, anime: Anime): Boolean {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(getEntityClass())
            query.select(cb.literal(1))

            query.where(
                cb.equal(root[MemberFollowAnime_.member], member),
                cb.equal(root[MemberFollowAnime_.anime], anime)
            )

            createReadOnlyQuery(it.createQuery(query).setMaxResults(1))
                .resultList
                .isNotEmpty()
        }
    }

    fun findByMemberAndAnime(member: Member, anime: Anime): MemberFollowAnime? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[MemberFollowAnime_.member], member),
                cb.equal(root[MemberFollowAnime_.anime], anime)
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}