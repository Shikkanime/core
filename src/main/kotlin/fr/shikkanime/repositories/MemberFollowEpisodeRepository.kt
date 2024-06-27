package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import java.util.*

class MemberFollowEpisodeRepository : AbstractRepository<MemberFollowEpisode>() {
    override fun getEntityClass() = MemberFollowEpisode::class.java

    fun findAllFollowedEpisodesUUID(member: Member): List<UUID> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowEpisode_.episode][EpisodeMapping_.UUID])

            query.where(
                cb.equal(root[MemberFollowEpisode_.member], member)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllByEpisode(episode: EpisodeMapping): List<MemberFollowEpisode> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[MemberFollowEpisode_.episode], episode)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun existsByMemberAndEpisode(member: Member, episode: EpisodeMapping): Boolean {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(getEntityClass())
            query.select(cb.literal(1))

            query.where(
                cb.equal(root[MemberFollowEpisode_.member], member),
                cb.equal(root[MemberFollowEpisode_.episode], episode)
            )

            createReadOnlyQuery(it.createQuery(query).setMaxResults(1))
                .resultList
                .isNotEmpty()
        }
    }

    fun findAllFollowedEpisodesByMemberAndEpisodes(member: Member, episodes: List<EpisodeMapping>): List<UUID> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowEpisode_.episode][EpisodeMapping_.uuid])

            query.where(
                cb.equal(root[MemberFollowEpisode_.member], member),
                root[MemberFollowEpisode_.episode].`in`(episodes)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByMemberAndEpisode(member: Member, episode: EpisodeMapping): MemberFollowEpisode? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[MemberFollowEpisode_.member], member),
                cb.equal(root[MemberFollowEpisode_.episode], episode)
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }

    fun getTotalDuration(member: Member): Long {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(getEntityClass())
            query.select(cb.sum(root[MemberFollowEpisode_.episode][EpisodeMapping_.duration]))

            query.where(
                cb.equal(root[MemberFollowEpisode_.member], member)
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull() ?: 0L
        }
    }
}