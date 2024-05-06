package fr.shikkanime.repositories

import fr.shikkanime.entities.*
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

    fun getAllFollowedAnimesUUID(member: Member): List<UUID> {
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
}