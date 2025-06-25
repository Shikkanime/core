package fr.shikkanime.repositories

import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.member.AdditionalDataDto
import fr.shikkanime.dtos.member.DetailedMemberDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.withUTCString
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.*
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

            val maxActionDateTimeSubquery = buildMemberQuery(cb, query, root)

            query.orderBy(
                cb.asc(cb.isNull(maxActionDateTimeSubquery)),
                cb.desc(maxActionDateTimeSubquery),
                cb.desc(root[Member_.creationDateTime])
            )

            PageableDto.fromPageable(buildPageableQuery(createReadOnlyQuery(it, query), page, limit)) { tuple ->
                createDetailedMemberDto(tuple)
            }
        }
    }

    fun findDetailedMember(uuid: UUID): DetailedMemberDto? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())

            val additionalDataSubquery = buildAdditionalDataSubquery(cb, query, root)
            buildMemberQuery(cb, query, root, additionalDataSubquery)

            query.where(cb.equal(root[Member_.uuid], uuid))

            val tuple = createReadOnlyQuery(it, query).resultList.firstOrNull() ?: return null
            createDetailedMemberDto(tuple, includeAdditionalData = true)
        }
    }

    override fun find(uuid: UUID): Member? {
        return database.entityManager.use {
            it.find(getEntityClass(), uuid)
                ?.apply { Hibernate.initialize(roles) }
        }
    }

    private fun findBy(vararg pairs: Pair<SingularAttribute<Member, *>, Any>): Member? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            root.fetch(Member_.roles, JoinType.LEFT)

            query.where(*pairs.map { pair ->
                cb.equal(root[pair.first], pair.second)
            }.toTypedArray())

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
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

    private fun buildMemberQuery(
        cb: CriteriaBuilder,
        query: CriteriaQuery<Tuple>,
        root: Root<Member>,
        additionalDataSubquery: Subquery<String>? = null
    ): Subquery<ZonedDateTime> {
        val maxActionDateTimeSubquery = buildLastLoginSubquery(cb, query, root)
        val followedAnimesSubquery = buildFollowedAnimesSubquery(cb, query, root)
        val followedEpisodesSubquery = buildFollowedEpisodesSubquery(cb, query, root)
        val attachmentJoin = buildAttachmentJoin(cb, root)
        
        val selections: MutableList<Selection<*>> = mutableListOf(
            root[Member_.uuid],
            root[Member_.email],
            root[Member_.creationDateTime],
            root[Member_.lastUpdateDateTime],
            attachmentJoin[Attachment_.lastUpdateDateTime],
            maxActionDateTimeSubquery,
            followedAnimesSubquery,
            followedEpisodesSubquery,
            cb.count(attachmentJoin[Attachment_.uuid])
        )
        
        if (additionalDataSubquery != null) {
            selections.add(additionalDataSubquery)
        }
        
        query.select(cb.tuple(selections))
            .groupBy(
                root[Member_.uuid],
                root[Member_.email],
                root[Member_.creationDateTime],
                root[Member_.lastUpdateDateTime],
                attachmentJoin[Attachment_.lastUpdateDateTime]
            )
            
        return maxActionDateTimeSubquery
    }
    
    private fun buildLastLoginSubquery(
        cb: CriteriaBuilder,
        query: CriteriaQuery<Tuple>,
        root: Root<Member>
    ): Subquery<ZonedDateTime> {
        val subquery = query.subquery(ZonedDateTime::class.java)
        val traceActionRoot = subquery.from(TraceAction::class.java)
        
        return subquery.select(cb.greatest(traceActionRoot[TraceAction_.actionDateTime]))
            .where(
                cb.equal(traceActionRoot[TraceAction_.entityUuid], root[Member_.uuid]),
                cb.or(
                    cb.equal(traceActionRoot[TraceAction_.action], TraceAction.Action.LOGIN),
                    cb.isNull(traceActionRoot[TraceAction_.action])
                )
            )
    }
    
    private fun buildAdditionalDataSubquery(
        cb: CriteriaBuilder,
        query: CriteriaQuery<Tuple>,
        root: Root<Member>
    ): Subquery<String> {
        val subquery = query.subquery(String::class.java)
        val traceActionRoot = subquery.from(TraceAction::class.java)
        val maxActionDateTimeSubquery = buildLastLoginSubquery(cb, query, root)
        
        return subquery.select(traceActionRoot[TraceAction_.additionalData])
            .where(
                cb.equal(traceActionRoot[TraceAction_.entityUuid], root[Member_.uuid]),
                cb.equal(traceActionRoot[TraceAction_.action], TraceAction.Action.LOGIN),
                cb.equal(traceActionRoot[TraceAction_.actionDateTime], maxActionDateTimeSubquery)
            )
    }
    
    private fun buildFollowedAnimesSubquery(
        cb: CriteriaBuilder,
        query: CriteriaQuery<Tuple>,
        root: Root<Member>
    ): Subquery<Long> {
        val subquery = query.subquery(Long::class.java)
        val followedAnimesRoot = subquery.from(MemberFollowAnime::class.java)
        
        return subquery.select(cb.count(followedAnimesRoot[MemberFollowAnime_.anime]))
            .where(cb.equal(followedAnimesRoot[MemberFollowAnime_.member], root))
    }
    
    private fun buildFollowedEpisodesSubquery(
        cb: CriteriaBuilder,
        query: CriteriaQuery<Tuple>,
        root: Root<Member>
    ): Subquery<Long> {
        val subquery = query.subquery(Long::class.java)
        val followedEpisodesRoot = subquery.from(MemberFollowEpisode::class.java)
        
        return subquery.select(cb.count(followedEpisodesRoot[MemberFollowEpisode_.episode]))
            .where(cb.equal(followedEpisodesRoot[MemberFollowEpisode_.member], root))
    }
    
    private fun buildAttachmentJoin(
        cb: CriteriaBuilder,
        root: Root<Member>
    ): Join<Member, Attachment> {
        val attachmentJoin = root.join(Member_.attachments, JoinType.LEFT)
        
        return attachmentJoin.on(
            cb.equal(attachmentJoin[Attachment_.type], ImageType.MEMBER_PROFILE),
            cb.isTrue(attachmentJoin[Attachment_.active])
        )
    }

    private fun createDetailedMemberDto(
        tuple: Tuple,
        includeAdditionalData: Boolean = false
    ): DetailedMemberDto {
        return DetailedMemberDto(
            tuple[0, UUID::class.java],
            tuple[1, String::class.java],
            tuple[2, ZonedDateTime::class.java].withUTCString(),
            tuple[3, ZonedDateTime::class.java]?.withUTCString(),
            tuple[4, ZonedDateTime::class.java]?.withUTCString(),
            tuple[5, ZonedDateTime::class.java]?.withUTCString(),
            tuple[6, Long::class.java],
            tuple[7, Long::class.java],
            (runCatching { tuple[8, Long::class.java] }.getOrDefault(0L) ?: 0L) > 0L,
            additionalData = if (includeAdditionalData) tuple[9, String::class.java]?.let {
                ObjectParser.fromJson(
                    it,
                    AdditionalDataDto::class.java
                )
            } else null
        ).apply {
            val creationDate = ZonedDateTime.parse(creationDateTime).toLocalDate()
            val lastUpdateDate = lastUpdateDateTime?.let { ZonedDateTime.parse(it).toLocalDate() }

            isActive = email != null ||
                    hasProfilePicture ||
                    (lastLoginDateTime != null && (followedAnimesCount > 0 || followedEpisodesCount > 0)) ||
                    (lastUpdateDate != null && !lastUpdateDate.isEqual(creationDate))
        }
    }
}