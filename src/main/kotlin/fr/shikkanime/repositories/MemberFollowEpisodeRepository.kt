package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.Pageable
import jakarta.persistence.criteria.JoinType
import java.util.*

class MemberFollowEpisodeRepository : AbstractRepository<MemberFollowEpisode>() {
    override fun getEntityClass() = MemberFollowEpisode::class.java

    fun findAllFollowedEpisodes(member: Member, page: Int, limit: Int): Pageable<EpisodeMapping> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(EpisodeMapping::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowEpisode_.episode])

            query.where(
                cb.equal(root[MemberFollowEpisode_.member], member)
            )

            query.orderBy(
                cb.desc(root[MemberFollowEpisode_.followDateTime]),
                cb.desc(root[MemberFollowEpisode_.episode][EpisodeMapping_.anime]),
                cb.desc(root[MemberFollowEpisode_.episode][EpisodeMapping_.season]),
                cb.desc(root[MemberFollowEpisode_.episode][EpisodeMapping_.episodeType]),
                cb.desc(root[MemberFollowEpisode_.episode][EpisodeMapping_.number]),
            )

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    fun findAllFollowedEpisodesUUID(memberUuid: UUID): List<UUID> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            query.select(root[MemberFollowEpisode_.episode][EpisodeMapping_.UUID])

            query.where(
                cb.equal(root[MemberFollowEpisode_.member][Member_.uuid], memberUuid)
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

    fun existsByMemberAndEpisode(memberUuid: UUID, episodeUuid: UUID): Boolean {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(Long::class.java)
            val root = query.from(getEntityClass())
            query.select(cb.literal(1))

            query.where(
                cb.equal(root[MemberFollowEpisode_.member][Member_.uuid], memberUuid),
                cb.equal(root[MemberFollowEpisode_.episode][EpisodeMapping_.uuid], episodeUuid)
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

    fun getSeenAndUnseenDuration(member: Member): Pair<Long, Long> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(MemberFollowAnime::class.java)
            val anime = root.join(MemberFollowAnime_.anime)
            val episodeMapping = anime.join(Anime_.mappings, JoinType.LEFT)
            val memberFollowEpisode = episodeMapping.join(EpisodeMapping_.memberFollowEpisodes, JoinType.LEFT)
            memberFollowEpisode.on(cb.equal(memberFollowEpisode[MemberFollowEpisode_.member], member))

            val seenDuration = cb.sum(
                cb.selectCase<Number?>()
                    .`when`(cb.isNotNull(memberFollowEpisode[MemberFollowEpisode_.episode]), episodeMapping[EpisodeMapping_.duration])
                    .otherwise(0)
            )

            val unseenDuration = cb.sum(
                cb.selectCase<Number?>()
                    .`when`(
                        cb.and(
                            cb.isNull(memberFollowEpisode[MemberFollowEpisode_.episode]),
                            cb.notEqual(episodeMapping[EpisodeMapping_.episodeType], EpisodeType.SUMMARY)
                        ),
                        episodeMapping[EpisodeMapping_.duration]
                    )
                    .otherwise(0)
            )

            query.select(
                cb.tuple(
                    cb.coalesce(seenDuration, 0L),
                    cb.coalesce(unseenDuration, 0L)
                )
            )

            query.where(cb.equal(root[MemberFollowAnime_.member], member))

            createReadOnlyQuery(it, query)
                .singleResult
                .let { pair -> pair[0] as Long to pair[1] as Long }
        }
    }
}