package fr.shikkanime.repositories

import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowEpisode
import fr.shikkanime.entities.MemberFollowEpisode_

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

    fun getAllFollowedEpisodes(member: Member): List<EpisodeMapping> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(EpisodeMapping::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowEpisode_.episode])

            query.where(
                cb.equal(root[MemberFollowEpisode_.member], member)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }
}