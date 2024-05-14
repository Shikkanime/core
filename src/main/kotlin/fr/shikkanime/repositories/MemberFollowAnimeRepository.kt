package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.JoinType
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
        watchedEpisodes: List<UUID>,
        page: Int,
        limit: Int,
    ): Pageable<Tuple> {
        return inTransaction { entityManager ->
            val cb = entityManager.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            val anime = root.join(MemberFollowAnime_.anime)
            val episodeMapping = anime.join(Anime_.mappings, JoinType.LEFT)

            val watchedEpisodesPredicate = episodeMapping[EpisodeMapping_.uuid].`in`(watchedEpisodes)
            val memberPredicate = cb.equal(root[MemberFollowAnime_.member], member)

            query.multiselect(anime, cb.countDistinct(episodeMapping[EpisodeMapping_.uuid]).`as`(Long::class.java))
            query.where(memberPredicate, cb.not(watchedEpisodesPredicate))
            query.groupBy(anime)
            query.having(cb.greaterThan(cb.countDistinct(episodeMapping[EpisodeMapping_.uuid]), 0))
            query.orderBy(cb.desc(anime[Anime_.lastReleaseDateTime]))

            buildPageableQuery(createReadOnlyQuery(entityManager, query), page, limit)
        }
    }
}