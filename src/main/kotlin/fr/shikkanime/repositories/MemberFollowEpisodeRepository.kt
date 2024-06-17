package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import java.util.*

class MemberFollowEpisodeRepository : AbstractRepository<MemberFollowEpisode>() {
    override fun getEntityClass() = MemberFollowEpisode::class.java

    fun findAllFollowedEpisodesUUID(member: Member): List<UUID> {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(UUID::class.java)
        val root = query.from(getEntityClass())
        query.select(root[MemberFollowEpisode_.episode][EpisodeMapping_.UUID])

        query.where(
            cb.equal(root[MemberFollowEpisode_.member], member)
        )

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
    }

    fun findAllByEpisode(episode: EpisodeMapping): List<MemberFollowEpisode> {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(
            cb.equal(root[MemberFollowEpisode_.episode], episode)
        )

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
    }

    fun findByMemberAndEpisode(member: Member, episode: EpisodeMapping): MemberFollowEpisode? {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(
            cb.equal(root[MemberFollowEpisode_.member], member),
            cb.equal(root[MemberFollowEpisode_.episode], episode)
        )

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
            .firstOrNull()
    }

    fun getTotalDuration(member: Member): Long {
        val cb = database.entityManager.criteriaBuilder
        val query = cb.createQuery(Long::class.java)
        val root = query.from(getEntityClass())
        query.select(cb.sum(root[MemberFollowEpisode_.episode][EpisodeMapping_.duration]))

        query.where(
            cb.equal(root[MemberFollowEpisode_.member], member)
        )

        return createReadOnlyQuery(database.entityManager, query)
            .resultList
            .firstOrNull() ?: 0L
    }
}