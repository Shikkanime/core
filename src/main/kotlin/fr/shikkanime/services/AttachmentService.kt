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
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

private const val FAILED_TO_ENCODE_MESSAGE = "Failed to encode image to WebP"
private const val INVALIDATION_DELAY_MS = 5000L

class AttachmentService : AbstractService<Attachment, AttachmentRepository>() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val nThreads = Runtime.getRuntime().availableProcessors()
    private var threadPool = Executors.newFixedThreadPool(nThreads)
    private var invalidationScheduler = Executors.newSingleThreadScheduledExecutor()
    private val httpRequest = HttpRequest()
    private val imageCache = LRUCache<UUID, ByteArray>(100)
    val inProgressAttachments: MutableSet<UUID> = Collections.synchronizedSet(HashSet())

    @Volatile private var pendingInvalidation: ScheduledFuture<*>? = null

    @Inject private lateinit var attachmentRepository: AttachmentRepository
    @Inject private lateinit var traceActionService: TraceActionService
    @Inject private lateinit var animeService: AnimeService
    @Inject private lateinit var episodeMappingService: EpisodeMappingService
    @Inject private lateinit var memberService: MemberService

    override fun getRepository() = attachmentRepository

    fun findAllByEntityUuidAndType(entityUuid: UUID, type: ImageType) = attachmentRepository.findAllByEntityUuidAndType(entityUuid, type)

    fun findAllNeededUpdate(lastUpdateDateTime: ZonedDateTime) = attachmentRepository.findAllNeededUpdate(lastUpdateDateTime)

    fun findByEntityUuidTypeAndActive(entityUuid: UUID, type: ImageType) = attachmentRepository.findByEntityUuidTypeAndActive(entityUuid, type)

    fun findAllActiveWithUrlAndNotIn(uuids: HashSet<UUID>) = attachmentRepository.findAllActiveWithUrlAndNotIn(uuids)

    fun findAllActive() = attachmentRepository.findAllActive()

    private fun scheduleInvalidation() {
        synchronized(this) {
            try {
                pendingInvalidation?.cancel(false)
            } catch (e: Exception) {
                logger.log(Level.FINE, "No pending invalidation to cancel", e)
            }

            pendingInvalidation = invalidationScheduler.schedule({
                try {
                    logger.info("Performing invalidation for Attachment")
                    InvalidationService.invalidate(Attachment::class.java)
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Failed to perform invalidation", e)
                }
            }, INVALIDATION_DELAY_MS, TimeUnit.MILLISECONDS)
        }
    }

    fun createAttachmentOrMarkAsActive(entityUuid: UUID, type: ImageType, url: String? = null, bytes: ByteArray? = null, async: Boolean = true): Attachment {
        val attachments = findAllByEntityUuidAndType(entityUuid, type)
        val existingAttachment = attachments.find { it.url == url }

        if (existingAttachment?.active == true) {
            if (!bytes.isNullOrEmpty() || !getFile(existingAttachment).exists()) {
                encodeAttachment(existingAttachment, url, bytes, async)
                existingAttachment.lastUpdateDateTime = ZonedDateTime.now()
                update(existingAttachment)
                scheduleInvalidation()
            }

            return existingAttachment
        }

        attachments.forEach { it.active = it == existingAttachment }
        updateAll(attachments)

        if (existingAttachment != null) {
            encodeAttachment(existingAttachment, url, bytes, async)
            scheduleInvalidation()
            return existingAttachment
        }

        val attachment = save(Attachment(entityUuid = entityUuid, type = type, url = url))
        traceActionService.createTraceAction(attachment, TraceAction.Action.CREATE)
        encodeAttachment(attachment, url, bytes, async)
        scheduleInvalidation()
        return attachment
    }

    fun encodeAllActiveWithUrlAndWithoutFile() {
        val now = ZonedDateTime.now()
        val existingFiles = Constant.imagesFolder.list().mapNotNull {
            runCatching { UUID.fromString(it.substringBeforeLast(".")) }.getOrNull()
        }.toHashSet()

        val attachmentsToUpdate = findAllActiveWithUrlAndNotIn(existingFiles)
        if (attachmentsToUpdate.isEmpty()) return
        logger.info("Encoding ${attachmentsToUpdate.size} attachments")

        attachmentsToUpdate.forEach {
            it.lastUpdateDateTime = now
            encodeAttachment(it, it.url, null)
        }

        updateAll(attachmentsToUpdate)
        scheduleInvalidation()
    }

    /**
     * Encodes an attachment by either downloading its content from a URL or using provided byte data.
     * The encoding process can be executed asynchronously or synchronously.
     *
     * @param attachment The `Attachment` object to be encoded.
     * @param url The URL to fetch the attachment content from, if `bytes` is not provided. Can be null.
     * @param bytes The byte array representing the attachment content. Can be null if `url` is provided.
     * @param async A boolean flag indicating whether the encoding should be performed asynchronously.
     *              Defaults to `true`.
     *
     * @throws IllegalArgumentException If both `url` and `bytes` are null.
     */
    fun encodeAttachment(attachment: Attachment, url: String?, bytes: ByteArray?, async: Boolean = true) {
        // Ensure that at least one of `url` or `bytes` is provided
        require(!url.isNullOrBlank() || !bytes.isNullOrEmpty()) { "Either url or bytes must be provided" }

        // Retrieve the UUID of the attachment; return if it is null
        val uuid = attachment.uuid ?: return

        // Add the UUID to the set of in-progress attachments
        inProgressAttachments.add(uuid)

        // Define the encoding task
        val task = {
            try {
                // Perform the encoding process
                taskEncode(attachment, url, bytes)
            } finally {
                // Remove the UUID from the in-progress set after the task is completed
                inProgressAttachments.remove(uuid)
            }
        }

        // Execute the task asynchronously or synchronously based on the `async` flag
        if (async) threadPool.submit(task) else task()
    }

    private fun getFileName(attachment: Attachment) = "${attachment.uuid}.webp"

    fun getFile(attachment: Attachment) = File(Constant.imagesFolder, getFileName(attachment))

    fun getContentFromCache(attachment: Attachment): ByteArray? {
        return imageCache[attachment.uuid!!]
    }

    fun setContentInCache(attachment: Attachment, bytes: ByteArray) {
        imageCache[attachment.uuid!!] = bytes
    }

    private fun removeFile(attachment: Attachment) {
        if (!getFile(attachment).delete())
            logger.warning("Failed to delete file ${getFile(attachment)}")

        imageCache.remove(attachment.uuid!!)
        attachment.active = false
        attachment.lastUpdateDateTime = ZonedDateTime.now()
        update(attachment)
        scheduleInvalidation()
    }

    private fun taskEncode(attachment: Attachment, url: String?, bytes: ByteArray?) {
        val attachmentBytes = if (!url.isNullOrBlank() && bytes.isNullOrEmpty()) {
            val (httpResponse, urlBytes) = runBlocking {
                val response = httpRequest.get(url)
                response to response.readRawBytes()
            }

            if (httpResponse.status != HttpStatusCode.OK || urlBytes.isEmpty() || httpResponse.contentType()?.withoutParameters() !in listOf(ContentType.Image.PNG, ContentType.Image.JPEG)) {
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
                val webp = FileManager.convertToWebP(ByteArrayOutputStream().apply { ImageIO.write(resized, "png", this) }.toByteArray())

                if (webp.isEmpty()) {
                    logger.warning(FAILED_TO_ENCODE_MESSAGE)
                    removeFile(attachment)
                    return@measureTimeMillis
                }

                getFile(attachment).writeBytes(webp)
                setContentInCache(attachment, webp)
            } catch (e: Exception) {
                logger.log(Level.SEVERE, FAILED_TO_ENCODE_MESSAGE, e)
                removeFile(attachment)
            }
        }

        logger.info("Encoded image to WebP in ${take}ms")
    }

    /**
     * Cleans up unused attachments and files from the system.
     *
     * This function performs the following tasks:
     * 1. Identifies valid UUIDs from anime, episode mappings, and members.
     * 2. Removes attachments that are no longer associated with valid UUIDs.
     * 3. Deletes files in the images folder that are not linked to active attachments or are in progress.
     */
    fun cleanUnusedAttachments() {
        val now = System.currentTimeMillis()

        // Retrieve all valid UUIDs from anime, episode mappings, and members
        val validUuids = animeService.findAllUuids() + episodeMappingService.findAllUuids() + memberService.findAllUuids()

        // Map all attachments by their UUIDs for quick lookup
        val attachments = findAll().associateBy { it.uuid!! }.toMutableMap()

        // List all files in the images folder
        val files = Constant.imagesFolder.listFiles()

        // Remove attachments that are not associated with valid UUIDs
        attachments.values.filter { it.entityUuid !in validUuids && it.uuid !in inProgressAttachments }.forEach {
            delete(it) // Delete the attachment from the database
            traceActionService.createTraceAction(it, TraceAction.Action.DELETE) // Log the deletion action
            attachments.remove(it.uuid) // Remove the attachment from the map
            imageCache.remove(it.uuid!!) // Remove the attachment from the cache
        }

        // Delete files that are not linked to active attachments or are in progress
        files?.forEach { file ->
            // Attempt to parse the file name as a UUID
            val uuid = runCatching { UUID.fromString(file.nameWithoutExtension) }.getOrNull()
            val creationDateTime = runCatching { Files.readAttributes(file.toPath(), BasicFileAttributes::class.java).creationTime().toMillis() }.getOrNull()

            // Delete the file if it is not in progress, not active, creation date time is not hourly old, and deletion fails
            if (uuid != null && uuid !in inProgressAttachments && attachments[uuid]?.active != true && creationDateTime != null && creationDateTime < now - 3600000 && !file.delete())
                logger.warning("Failed to delete file $file") // Log a warning if the file deletion fails
        }
    }

    fun clearPool() {
        threadPool.shutdownNow()
        threadPool = Executors.newFixedThreadPool(nThreads)
        Constant.imagesFolder.deleteRecursively()
    }
}