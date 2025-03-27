package fr.shikkanime.repositories

import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.member.DetailedMemberDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.withUTCString
import jakarta.persistence.metamodel.SingularAttribute
import org.hibernate.Hibernate
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class MemberRepository : AbstractRepository<Member>() {
    override fun getEntityClass() = Member::class.java

    fun findAllByRoles(roles: List<Role>): List<Member> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(root.join(Member_.roles).`in`(roles))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllByAnimeUUID(animeUuid: UUID): List<Member> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            val followedAnimesJoin = root.join(Member_.followedAnimes)

            query.distinct(true)
                .where(cb.equal(followedAnimesJoin[MemberFollowAnime_.anime][Anime_.uuid], animeUuid))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllWithLastLogin(page: Int, limit: Int): PageableDto<DetailedMemberDto> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            val maxActionDateTimeSubquery = query.subquery(ZonedDateTime::class.java)
            val traceActionRoot = maxActionDateTimeSubquery.from(TraceAction::class.java)

            maxActionDateTimeSubquery.select(cb.greatest(traceActionRoot[TraceAction_.actionDateTime]))
                .where(
                    cb.equal(traceActionRoot[TraceAction_.entityUuid], root[Member_.uuid]),
                    cb.or(
                        cb.equal(traceActionRoot[TraceAction_.action], TraceAction.Action.LOGIN),
                        cb.isNull(traceActionRoot[TraceAction_.action])
                    )
                )

            val followedAnimesSubquery = query.subquery(Long::class.java)
            val followedAnimesRoot = followedAnimesSubquery.from(MemberFollowAnime::class.java)

            followedAnimesSubquery.select(cb.count(followedAnimesRoot[MemberFollowAnime_.anime]))
                .where(cb.equal(followedAnimesRoot[MemberFollowAnime_.member], root))

            val followedEpisodesSubquery = query.subquery(Long::class.java)
            val followedEpisodesRoot = followedEpisodesSubquery.from(MemberFollowEpisode::class.java)

            followedEpisodesSubquery.select(cb.count(followedEpisodesRoot[MemberFollowEpisode_.episode]))
                .where(cb.equal(followedEpisodesRoot[MemberFollowEpisode_.member], root))

            val hasMemberImageSubquery = query.subquery(Boolean::class.java)
            val attachmentRoot = hasMemberImageSubquery.from(Attachment::class.java)

            hasMemberImageSubquery.select(cb.literal(true))
                .where(
                    cb.equal(attachmentRoot[Attachment_.entityUuid], root[Member_.uuid]),
                    cb.equal(attachmentRoot[Attachment_.type], ImageType.MEMBER_PROFILE),
                    cb.isTrue(attachmentRoot[Attachment_.active])
                )

            query.multiselect(
                root[Member_.uuid],
                root[Member_.email],
                root[Member_.creationDateTime],
                root[Member_.lastUpdateDateTime],
                maxActionDateTimeSubquery,
                followedAnimesSubquery,
                followedEpisodesSubquery,
                hasMemberImageSubquery
            ).groupBy(
                root[Member_.uuid],
                root[Member_.email],
                root[Member_.creationDateTime],
                root[Member_.lastUpdateDateTime],
            ).orderBy(
                cb.asc(cb.isNull(maxActionDateTimeSubquery)),
                cb.desc(maxActionDateTimeSubquery),
                cb.desc(root[Member_.creationDateTime])
            )

            PageableDto.fromPageable(buildPageableQuery(createReadOnlyQuery(it, query), page, limit)) { tuple ->
                DetailedMemberDto(
                    tuple[0, UUID::class.java],
                    tuple[1, String::class.java],
                    tuple[2, ZonedDateTime::class.java].withUTCString(),
                    tuple[3, ZonedDateTime::class.java]?.withUTCString(),
                    tuple[4, ZonedDateTime::class.java]?.withUTCString(),
                    tuple[5, Long::class.java],
                    tuple[6, Long::class.java],
                    runCatching { tuple[7, Boolean::class.java] }.getOrDefault(false) ?: false
                ).apply {
                    isActive = email != null || hasProfilePicture || (lastLoginDateTime != null && (followedAnimesCount > 0 || followedEpisodesCount > 0)) || (lastUpdateDateTime != null && ZonedDateTime.parse(lastUpdateDateTime).toLocalDate() != ZonedDateTime.parse(creationDateTime).toLocalDate())
                }
            }
        }
    }

    override fun find(uuid: UUID): Member? {
        return database.entityManager.use {
            it.find(getEntityClass(), uuid)
                ?.apply { Hibernate.initialize(roles) }
        }
    }

    fun findDetailedMember(uuid: UUID): DetailedMemberDto? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            val maxActionDateTimeSubquery = query.subquery(ZonedDateTime::class.java).apply {
                val traceActionRoot = from(TraceAction::class.java)
                select(cb.greatest(traceActionRoot[TraceAction_.actionDateTime]))
                    .where(
                        cb.equal(traceActionRoot[TraceAction_.entityUuid], root[Member_.uuid]),
                        cb.or(
                            cb.equal(traceActionRoot[TraceAction_.action], TraceAction.Action.LOGIN),
                            cb.isNull(traceActionRoot[TraceAction_.action])
                        )
                    )
            }

            val followedAnimesSubquery = query.subquery(Long::class.java).apply {
                val followedAnimesRoot = from(MemberFollowAnime::class.java)
                select(cb.count(followedAnimesRoot[MemberFollowAnime_.anime]))
                    .where(cb.equal(followedAnimesRoot[MemberFollowAnime_.member], root))
            }

            val followedEpisodesSubquery = query.subquery(Long::class.java).apply {
                val followedEpisodesRoot = from(MemberFollowEpisode::class.java)
                select(cb.count(followedEpisodesRoot[MemberFollowEpisode_.episode]))
                    .where(cb.equal(followedEpisodesRoot[MemberFollowEpisode_.member], root))
            }

            val hasMemberImageSubquery = query.subquery(Boolean::class.java)
            val attachmentRoot = hasMemberImageSubquery.from(Attachment::class.java)

            hasMemberImageSubquery.select(cb.literal(true))
                .where(
                    cb.equal(attachmentRoot[Attachment_.entityUuid], root[Member_.uuid]),
                    cb.equal(attachmentRoot[Attachment_.type], ImageType.MEMBER_PROFILE),
                    cb.isTrue(attachmentRoot[Attachment_.active])
                )

            query.multiselect(
                root[Member_.uuid],
                root[Member_.email],
                root[Member_.creationDateTime],
                root[Member_.lastUpdateDateTime],
                maxActionDateTimeSubquery,
                followedAnimesSubquery,
                followedEpisodesSubquery,
                hasMemberImageSubquery
            ).where(cb.equal(root[Member_.uuid], uuid))

            val tuple = createReadOnlyQuery(it, query).resultList.firstOrNull() ?: return null

            return DetailedMemberDto(
                tuple[0, UUID::class.java],
                tuple[1, String::class.java],
                tuple[2, ZonedDateTime::class.java].withUTCString(),
                tuple[3, ZonedDateTime::class.java]?.withUTCString(),
                tuple[4, ZonedDateTime::class.java]?.withUTCString(),
                tuple[5, Long::class.java],
                tuple[6, Long::class.java],
                runCatching { tuple[7, Boolean::class.java] }.getOrDefault(false) ?: false
            ).apply {
                isActive = email != null || hasProfilePicture || (lastLoginDateTime != null && (followedAnimesCount > 0 || followedEpisodesCount > 0)) || (lastUpdateDateTime != null && ZonedDateTime.parse(lastUpdateDateTime).toLocalDate() != ZonedDateTime.parse(creationDateTime).toLocalDate())
            }
        }
    }

    private fun findBy(vararg pairs: Pair<SingularAttribute<Member, *>, Any>): Member? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(*pairs.map { pair ->
                cb.equal(root[pair.first], pair.second)
            }.toTypedArray())

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
                .apply { Hibernate.initialize(this?.roles) }
        }
    }

    fun findByUsernameAndPassword(username: String, password: ByteArray) = findBy(
        Member_.username to username,
        Member_.encryptedPassword to password
    )

    fun findByIdentifier(identifier: String) = findBy(Member_.username to identifier)

    fun findByEmail(email: String) = findBy(Member_.email to email)

    fun findMemberLoginActivities(memberUUID: UUID, after: LocalDate): List<TraceAction> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(TraceAction::class.java)
            val root = query.from(TraceAction::class.java)

            query.where(
                cb.equal(root[TraceAction_.entityUuid], memberUUID),
                cb.equal(root[TraceAction_.action], TraceAction.Action.LOGIN),
                cb.greaterThanOrEqualTo(root[TraceAction_.actionDateTime], after.atStartOfDay(Constant.utcZoneId))
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findMemberFollowAnimeActivities(memberUUID: UUID, after: LocalDate): List<TraceAction> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(TraceAction::class.java)
            val root = query.from(TraceAction::class.java)

            val memberFollowAnimeSubquery = query.subquery(UUID::class.java)
            val memberFollowAnimeRoot = memberFollowAnimeSubquery.from(MemberFollowAnime::class.java)

            memberFollowAnimeSubquery.select(memberFollowAnimeRoot[MemberFollowAnime_.uuid])
                .where(
                    cb.equal(memberFollowAnimeRoot[MemberFollowAnime_.member][Member_.uuid], memberUUID)
                )

            query.where(
                root[TraceAction_.entityUuid].`in`(memberFollowAnimeSubquery),
                cb.equal(root[TraceAction_.action], TraceAction.Action.CREATE),
                cb.equal(root[TraceAction_.entityType], MemberFollowAnime::class.java.simpleName),
                cb.greaterThanOrEqualTo(root[TraceAction_.actionDateTime], after.atStartOfDay(Constant.utcZoneId))
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findMemberFollowEpisodeActivities(memberUUID: UUID, after: LocalDate): List<TraceAction> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(TraceAction::class.java)
            val root = query.from(TraceAction::class.java)

            val memberFollowEpisodeSubquery = query.subquery(UUID::class.java)
            val memberFollowEpisodeRoot = memberFollowEpisodeSubquery.from(MemberFollowEpisode::class.java)

            memberFollowEpisodeSubquery.select(memberFollowEpisodeRoot[MemberFollowEpisode_.uuid])
                .where(
                    cb.equal(memberFollowEpisodeRoot[MemberFollowEpisode_.member][Member_.uuid], memberUUID)
                )

            query.where(
                root[TraceAction_.entityUuid].`in`(memberFollowEpisodeSubquery),
                cb.equal(root[TraceAction_.action], TraceAction.Action.CREATE),
                cb.equal(root[TraceAction_.entityType], MemberFollowEpisode::class.java.simpleName),
                cb.greaterThanOrEqualTo(root[TraceAction_.actionDateTime], after.atStartOfDay(Constant.utcZoneId))
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }
}