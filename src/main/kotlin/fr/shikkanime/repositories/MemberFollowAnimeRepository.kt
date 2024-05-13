package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import jakarta.persistence.Tuple
import java.util.*

class MemberFollowAnimeRepository : AbstractRepository<MemberFollowAnime>() {
    override fun getEntityClass() = MemberFollowAnime::class.java

    fun findByMemberAndAnime(member: Member, anime: Anime): MemberFollowAnime? {
        return inTransaction {
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

    fun findAllFollowedAnimesUUID(member: Member): List<UUID> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowAnime_.anime][Anime_.UUID])

            query.where(
                cb.equal(root[MemberFollowAnime_.member], member)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllMissedAnimes(
        member: Member,
        watchedEpisodes: List<EpisodeMapping>,
        page: Int,
        limit: Int,
    ): Pageable<Tuple> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createQuery(Tuple::class.java)
            val root = query.from(getEntityClass())

            val subQuery = query.subquery(Long::class.java)
            val subRoot = subQuery.from(EpisodeMapping::class.java)
            subQuery.select(cb.count(subRoot))
            subQuery.where(
                cb.equal(subRoot[EpisodeMapping_.anime], root[MemberFollowAnime_.anime]),
                cb.not(subRoot.get<UUID>(EpisodeMapping_.UUID).`in`(watchedEpisodes.map { it.uuid }))
            )

            query.select(cb.tuple(root[MemberFollowAnime_.anime], subQuery))
            query.where(
                cb.equal(root[MemberFollowAnime_.member], member),
                cb.greaterThan(subQuery, 0)
            )

            query.orderBy(cb.desc(root[MemberFollowAnime_.anime][Anime_.lastReleaseDateTime]))
            buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
        }
    }
}