package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import java.util.*

class MemberFollowEpisodeRepository : AbstractRepository<MemberFollowEpisode>() {
    override fun getEntityClass() = MemberFollowEpisode::class.java

    fun findByMemberAndEpisode(member: Member, episode: EpisodeMapping): MemberFollowEpisode? {
        return inTransaction {
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

    fun getAllFollowedEpisodesUUID(member: Member): List<UUID> {
        return inTransaction {
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

    fun getTotalDuration(member: Member): Long {
        return inTransaction {
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