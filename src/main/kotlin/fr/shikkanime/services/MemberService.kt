package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.member.MemberDto
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.Action
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.repositories.MemberRepository
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.RandomManager
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.io.ByteArrayInputStream
import java.time.ZonedDateTime
import java.util.*
import javax.imageio.ImageIO

class MemberService : AbstractService<Member, MemberRepository>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var memberRepository: MemberRepository

    @Inject
    private lateinit var memberActionService: MemberActionService

    @Inject
    private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberRepository

    private fun findAllByRoles(roles: List<Role>) = memberRepository.findAllByRoles(roles)

    fun findAllByAnimeUUID(animeUuid: UUID) = memberRepository.findAllByAnimeUUID(animeUuid)

    fun findByUsernameAndPassword(username: String, password: String) =
        memberRepository.findByUsernameAndPassword(username, EncryptionManager.generate(password))

    fun findByIdentifier(identifier: String) =
        memberRepository.findByIdentifier(EncryptionManager.toSHA512(identifier))

    fun findByEmail(email: String) = memberRepository.findByEmail(email)

    fun initDefaultAdminUser(): String {
        val adminUsers = findAllByRoles(listOf(Role.ADMIN))
        check(adminUsers.isEmpty()) { "Admin user already exists" }
        val password = RandomManager.generateRandomString(32)
        logger.info("Default admin password: $password")
        save(
            Member(
                username = "admin",
                encryptedPassword = EncryptionManager.generate(password),
                roles = mutableSetOf(Role.ADMIN)
            )
        )
        return password
    }

    fun register(identifier: String): Member {
        val saved = save(
            Member(
                isPrivate = true,
                username = EncryptionManager.toSHA512(identifier),
                encryptedPassword = byteArrayOf()
            )
        )

        traceActionService.createTraceAction(saved, TraceAction.Action.CREATE)
        return saved
    }

    fun login(identifier: String): MemberDto? {
        val member = findByIdentifier(identifier) ?: return null
        traceActionService.createTraceAction(member, TraceAction.Action.LOGIN)
        return AbstractConverter.convert(member, MemberDto::class.java)
    }

    fun associateEmail(memberUuid: UUID, email: String): UUID {
        val member = requireNotNull(find(memberUuid))
        // Creation member action
        return memberActionService.save(Action.VALIDATE_EMAIL, member, email)
    }

    fun forgotIdentifier(member: Member): UUID {
        requireNotNull(member.email)
        // Creation member action
        return memberActionService.save(Action.FORGOT_IDENTIFIER, member, member.email!!)
    }

    suspend fun changeProfileImage(member: Member, multiPartData: MultiPartData) {
        var bytes: ByteArray? = null

        multiPartData.forEachPart { part ->
            if (part is PartData.FileItem) {
                bytes = part.provider().readRemaining().readByteArray()
            }

            part.dispose()
        }

        requireNotNull(bytes) { "No file provided" }
        val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
        val imageReaders = ImageIO.getImageReaders(imageInputStream)
        require(imageReaders.hasNext()) { "Invalid file format" }
        val imageReader = imageReaders.next()
        val authorizedFormats = setOf("png", "jpeg", "jpg", "jpe")
        require(imageReader.formatName.lowercase() in authorizedFormats) { "Invalid file format, only png and jpeg are allowed. Received ${imageReader.formatName}" }

        ImageService.add(
            member.uuid!!,
            ImageService.Type.IMAGE,
            bytes,
            128,
            128,
            true
        )

        member.lastUpdateDateTime = ZonedDateTime.now()
        update(member)
        traceActionService.createTraceAction(member, TraceAction.Action.UPDATE)
    }
}