package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.EpisodeType
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.JoinType
import java.util.*

class MemberFollowAnimeRepository : AbstractRepository<MemberFollowAnime>() {
    override fun getEntityClass() = MemberFollowAnime::class.java

    fun findAllFollowedAnimesUUID(member: Member): List<UUID> {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(UUID::class.java)
        val root = query.from(getEntityClass())
        query.select(root[MemberFollowAnime_.anime][Anime_.UUID])

        query.where(
            cb.equal(root[MemberFollowAnime_.member], member)
        )

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
    }

    fun findAllMissedAnimes(
        member: Member,
        page: Int,
        limit: Int,
    ): Pageable<Tuple> {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createTupleQuery()
        val root = query.from(getEntityClass())
        val anime = root.join(MemberFollowAnime_.anime)
        val episodeMapping = anime.join(Anime_.mappings, JoinType.LEFT)
        // And episode type is not SUMMARY
        episodeMapping.on(cb.notEqual(episodeMapping[EpisodeMapping_.episodeType], EpisodeType.SUMMARY))
        val memberFollowEpisode = episodeMapping.join(EpisodeMapping_.memberFollowEpisodes, JoinType.LEFT)
        memberFollowEpisode.on(cb.equal(memberFollowEpisode[MemberFollowEpisode_.member], member))

        val memberPredicate = cb.equal(root[MemberFollowAnime_.member], member)
        val memberFollowEpisodePredicate = cb.and(cb.isNull(memberFollowEpisode[MemberFollowEpisode_.episode]))

        query.multiselect(anime, cb.countDistinct(episodeMapping[EpisodeMapping_.uuid]).`as`(Long::class.java))
        query.where(memberPredicate, memberFollowEpisodePredicate)
        query.groupBy(anime)
        query.having(cb.greaterThan(cb.countDistinct(episodeMapping[EpisodeMapping_.uuid]), 0))
        query.orderBy(cb.desc(anime[Anime_.lastReleaseDateTime]))

        return buildPageableQuery(createReadOnlyQuery(database.entityManager, query), page, limit)
    }

    fun findAllByAnime(anime: Anime): List<MemberFollowAnime> {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(
            cb.equal(root[MemberFollowAnime_.anime], anime)
        )

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
    }

    fun findByMemberAndAnime(member: Member, anime: Anime): MemberFollowAnime? {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(
            cb.equal(root[MemberFollowAnime_.member], member),
            cb.equal(root[MemberFollowAnime_.anime], anime)
        )

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
            .firstOrNull()
    }
}