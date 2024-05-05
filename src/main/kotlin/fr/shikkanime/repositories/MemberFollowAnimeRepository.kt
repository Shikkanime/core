package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberFollowAnime
import fr.shikkanime.entities.MemberFollowAnime_

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

    fun getAllFollowedAnimes(member: Member): List<Anime> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Anime::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowAnime_.anime])

            query.where(
                cb.equal(root[MemberFollowAnime_.member], member)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }
}