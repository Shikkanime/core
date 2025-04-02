package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.repositories.AttachmentRepository
import fr.shikkanime.utils.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

private const val FAILED_TO_ENCODE_MESSAGE = "Failed to encode image to WebP"

class AttachmentService : AbstractService<Attachment, AttachmentRepository>() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val nThreads = Runtime.getRuntime().availableProcessors()
    private var threadPool = Executors.newFixedThreadPool(nThreads)
    private val httpRequest = HttpRequest()
    private val imageCache = LRUCache<UUID, ByteArray>(100)

    @Inject
    private lateinit var attachmentRepository: AttachmentRepository

    @Inject
    private lateinit var traceActionService: TraceActionService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var memberService: MemberService

    override fun getRepository() = attachmentRepository

    fun findAllByEntityUuidAndType(entityUuid: UUID, type: ImageType) = attachmentRepository.findAllByEntityUuidAndType(entityUuid, type)

    fun findAllNeededUpdate(lastUpdateDateTime: ZonedDateTime) = attachmentRepository.findAllNeededUpdate(lastUpdateDateTime)

    fun findByEntityUuidTypeAndActive(entityUuid: UUID, type: ImageType) = attachmentRepository.findByEntityUuidTypeAndActive(entityUuid, type)

    fun findAllActive() = attachmentRepository.findAllActive()

    fun createAttachmentOrMarkAsActive(entityUuid: UUID, type: ImageType, url: String? = null, bytes: ByteArray? = null, async: Boolean = true): Attachment {
        val attachments = findAllByEntityUuidAndType(entityUuid, type)
        val existingAttachment = attachments.find { it.url == url }

        if (existingAttachment?.active == true) {
            if (bytes?.isNotEmpty() == true) {
                encodeAttachment(existingAttachment, url, bytes, async)
                existingAttachment.lastUpdateDateTime = ZonedDateTime.now()
                update(existingAttachment)
                MapCache.invalidate(Attachment::class.java)
            }

            return existingAttachment
        }

        attachments.forEach { it.active = it == existingAttachment }
        updateAll(attachments)

        if (existingAttachment != null) {
            encodeAttachment(existingAttachment, url, bytes, async)
            MapCache.invalidate(Attachment::class.java)
            return existingAttachment
        }

        val attachment = save(Attachment(entityUuid = entityUuid, type = type, url = url))
        traceActionService.createTraceAction(attachment, TraceAction.Action.CREATE)
        encodeAttachment(attachment, url, bytes, async)
        MapCache.invalidate(Attachment::class.java)
        return attachment
    }

    fun encodeAllActive() {
        val now = ZonedDateTime.now()
        val attachments = findAllActive()

        attachments.forEach {
            it.lastUpdateDateTime = now
            encodeAttachment(it, it.url, null)
        }

        updateAll(attachments)
        MapCache.invalidate(Attachment::class.java)
    }

    fun encodeAllActiveWithUrlAndWithoutFile() {
        val now = ZonedDateTime.now()
        val attachments = findAllActive().filter { !getFile(it).exists() && !it.url.isNullOrBlank() }

        attachments.forEach {
            it.lastUpdateDateTime = now
            encodeAttachment(it, it.url, null)
        }

        updateAll(attachments)
        MapCache.invalidate(Attachment::class.java)
    }

    fun encodeAttachment(attachment: Attachment, url: String?, bytes: ByteArray?, async: Boolean = true) {
        if (async)
            threadPool.submit { taskEncode(attachment, url, bytes) }
        else
            taskEncode(attachment, url, bytes)
    }

    fun getFile(attachment: Attachment) = File(Constant.imagesFolder, "${attachment.uuid}.webp")

    fun getContentFromCache(attachment: Attachment): ByteArray? {
        return imageCache[attachment.uuid!!]
    }

    fun setContentInCache(attachment: Attachment, bytes: ByteArray) {
        imageCache[attachment.uuid!!] = bytes
    }

    private fun removeFile(attachment: Attachment) {
        if (!getFile(attachment).delete())
            logger.warning("Failed to delete file ${getFile(attachment)}")
    }

    private fun taskEncode(attachment: Attachment, url: String?, bytes: ByteArray?) {
        val attachmentBytes = if (!url.isNullOrBlank() && (bytes == null || bytes.isEmpty())) {
            val (httpResponse, urlBytes) = runBlocking {
                val response = httpRequest.get(url)
                response to response.readRawBytes()
            }

            if (httpResponse.status != HttpStatusCode.OK || urlBytes.isEmpty()) {
                logger.warning(FAILED_TO_ENCODE_MESSAGE)
                removeFile(attachment)
                return
            }

            urlBytes
        } else {
            bytes ?: return
        }

        encodeImage(attachment, attachmentBytes)
    }
    
    private fun encodeImage(attachment: Attachment, bytes: ByteArray) {
        val take = measureTimeMillis {
            try {
                if (bytes.isEmpty()) {
                    logger.warning(FAILED_TO_ENCODE_MESSAGE)
                    removeFile(attachment)
                    return@measureTimeMillis
                }

                val resized = ImageIO.read(ByteArrayInputStream(bytes)).resize(attachment.type!!.width, attachment.type!!.height)
                val webp = FileManager.encodeToWebP(ByteArrayOutputStream().apply { ImageIO.write(resized, "png", this) }.toByteArray())

                if (webp.isEmpty()) {
                    logger.warning(FAILED_TO_ENCODE_MESSAGE)
                    removeFile(attachment)
                    return@measureTimeMillis
                }

                getFile(attachment).writeBytes(webp)
                if (imageCache.containsKey(attachment.uuid!!)) imageCache.remove(attachment.uuid)
            } catch (e: Exception) {
                when (e) {
                    is InterruptedException, is RuntimeException -> {
                        // Ignore
                    }
                    else -> logger.log(Level.SEVERE, FAILED_TO_ENCODE_MESSAGE, e)
                }

                removeFile(attachment)
            }
        }

        logger.info("Encoded image to WebP in ${take}ms")
    }

    fun cleanUnusedAttachments() {
        val uuids = animeService.findAllUuids() + episodeMappingService.findAllUuids() + memberService.findAllUuids()
        val attachments = findAll().associateBy { it.uuid!! }.toMutableMap()

        attachments.values.filter { it.entityUuid !in uuids }.forEach {
            delete(it)
            attachments.remove(it.uuid)
        }

        Constant.imagesFolder.listFiles()?.filter {
            attachments[UUID.fromString(it.nameWithoutExtension)]?.active != true
        }?.forEach { it.delete() }
    }

    fun clearPool() {
        threadPool.shutdownNow()
        threadPool = Executors.newFixedThreadPool(nThreads)
        Constant.imagesFolder.deleteRecursively()
    }
}