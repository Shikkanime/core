package fr.shikkanime.services

import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.utils.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.collections.*
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

private const val FAILED_TO_ENCODE_MESSAGE = "Failed to encode image to WebP"

object ImageService {
    data class Media(
        val uuid: UUID,
        val type: ImageType,
        var url: String? = null,
        var bytes: ByteArray = byteArrayOf(),
        var originalSize: Int = 0,
        var lastUpdateDateTime: Long? = null
    ) : Serializable {
        companion object {
            const val serialVersionUID: Long = 0
        }
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val nThreads = Runtime.getRuntime().availableProcessors()
    private var threadPool = Executors.newFixedThreadPool(nThreads)
    private val cache = ConcurrentMap<Pair<UUID, ImageType>, Pair<Media, Long>>()
    private const val INVALIDATE_DELAY = 10 * 60 * 1000L
    private const val MAX_CACHE_SIZE = 100

    private fun toHumanReadable(bytes: Long): String {
        val kiloByte = 1024L
        val megaByte = kiloByte * 1024L
        val gigaByte = megaByte * 1024L
        val teraByte = gigaByte * 1024L

        return when {
            bytes < kiloByte -> "$bytes B"
            bytes < megaByte -> String.format("%.2f KiB", bytes.toDouble() / kiloByte)
            bytes < gigaByte -> String.format("%.2f MiB", bytes.toDouble() / megaByte)
            bytes < teraByte -> String.format("%.2f GiB", bytes.toDouble() / gigaByte)
            else -> String.format("%.2f TiB", bytes.toDouble() / teraByte)
        }
    }

    private fun getPath(uuid: UUID, type: ImageType) = "${uuid}_${type.name.lowercase()}.shikk"
    private fun getFile(uuid: UUID, type: ImageType) = File(Constant.imagesFolder, getPath(uuid, type))

    private fun taskEncode(uuid: UUID, type: ImageType, url: String?, bytes: ByteArray?, media: Media) {
        val imageBytes = if (!url.isNullOrBlank() && (bytes == null || bytes.isEmpty())) {
            val (httpResponse, urlBytes) = runBlocking {
                val response = HttpRequest().get(url)
                response to response.readRawBytes()
            }

            if (httpResponse.status != HttpStatusCode.OK || urlBytes.isEmpty()) {
                logger.warning("Failed to load image $url")
                remove(uuid, type)
                return
            }

            urlBytes
        } else {
            bytes ?: return
        }

        encodeImage(url, imageBytes, uuid, type, media)
    }

    fun add(
        uuid: UUID,
        type: ImageType,
        url: String?,
        bytes: ByteArray?,
        bypass: Boolean = false,
        async: Boolean = true
    ) {
        val isEmpty = url.isNullOrBlank() && (bytes == null || bytes.isEmpty())

        if (!bypass && (getFile(uuid, type).exists() || isEmpty)) {
            return
        }

        val media = if (!bypass) {
            Media(uuid, type, url)
        } else {
            get(uuid, type) ?: Media(uuid, type, url)
        }

        if (async)
            threadPool.submit { taskEncode(uuid, type, url, bytes, media) }
        else
            taskEncode(uuid, type, url, bytes, media)
    }

    private fun encodeImage(
        url: String?,
        bytes: ByteArray,
        uuid: UUID,
        type: ImageType,
        media: Media
    ) {
        val take = measureTimeMillis {
            try {
                if (bytes.isEmpty()) {
                    logger.warning(FAILED_TO_ENCODE_MESSAGE)
                    remove(uuid, type)
                    return@measureTimeMillis
                }

                val resized = ImageIO.read(ByteArrayInputStream(bytes)).resize(type.width, type.height)
                val webp = FileManager.encodeToWebP(ByteArrayOutputStream().apply { ImageIO.write(resized, "png", this) }.toByteArray())

                if (webp.isEmpty()) {
                    logger.warning(FAILED_TO_ENCODE_MESSAGE)
                    remove(uuid, type)
                    return@measureTimeMillis
                }

                media.url = url
                media.bytes = webp
                media.originalSize = bytes.size
                media.lastUpdateDateTime = System.currentTimeMillis()

                FileManager.writeFile(getFile(uuid, type), media)
                if (cache.containsKey(uuid to type)) cache[uuid to type] = media to System.currentTimeMillis()
            } catch (e: Exception) {
                when (e) {
                    is InterruptedException, is RuntimeException -> {
                        // Ignore
                    }
                    else -> logger.log(Level.SEVERE, FAILED_TO_ENCODE_MESSAGE, e)
                }

                remove(uuid, type)
            }
        }

        logger.info(
            "Encoded image to WebP in ${take}ms (${toHumanReadable(media.originalSize.toLong())} -> ${
                toHumanReadable(
                    media.bytes.size.toLong()
                )
            })"
        )
    }

    fun remove(uuid: UUID, type: ImageType) {
        cache.remove(uuid to type)
        Constant.imagesFolder.listFiles { it.name == getPath(uuid, type) }
            ?.forEach { it.delete() }
    }

    operator fun get(uuid: UUID, type: ImageType, bypass: Boolean = false): Media? {
        if (!bypass) {
            cache[uuid to type]?.let { (media, lastAccessDateTime) ->
                if (System.currentTimeMillis() - lastAccessDateTime < INVALIDATE_DELAY) {
                    return media
                }

                cache.remove(uuid to type)
                return get(uuid, type)
            }
        }

        val file = getFile(uuid, type)
        if (!file.exists()) return null

        val media = FileManager.readFile<Media>(file)

        if (cache.size >= MAX_CACHE_SIZE) {
            cache.entries.minByOrNull { it.value.second }?.key?.let {
                cache.remove(it)
            }
        }

        cache[uuid to type] = media to System.currentTimeMillis()
        return media
    }

    fun getNeededUpdate(delay: Long): List<Media> {
        val now = System.currentTimeMillis()

        return Constant.imagesFolder.walk().asSequence()
            .filter { it.extension == "shikk" }
            .map { FileManager.readFile<Media>(it) }
            .filter { (it.lastUpdateDateTime == null || now - it.lastUpdateDateTime!! >= delay) && !it.url.isNullOrBlank() }
            .toList()
    }

    fun invalidate() {
        removeUnusedImages()
        addAll(true)
    }

    fun removeUnusedImages() {
        Constant.injector.getInstance(Database::class.java).entityManager.use {
            val query = it.createNativeQuery(
                """
                    SELECT uuid
                    FROM anime
                    UNION
                    SELECT uuid
                    FROM episode_mapping
                    UNION
                    SELECT uuid
                    FROM member
                    """,
                UUID::class.java
            )

            val uuids = query.resultList.asSequence()
                .filterIsInstance<UUID>()
                .toSet()

            cache.keys.asSequence()
                .filter { it.first !in uuids }
                .forEach { remove(it.first, it.second) }

            val files = Constant.imagesFolder.listFiles { UUID.fromString(it.name.substringBefore('_')) !in uuids }

            if (files.isNotEmpty()) {
                logger.warning("Removing ${files.size} images from cache, not found in database")
                files.forEach { it.delete() }
            }
        }
    }

    fun clearPool() {
        threadPool.shutdownNow()
        threadPool = Executors.newFixedThreadPool(nThreads)
        Constant.imagesFolder.deleteRecursively()
        cache.clear()
    }

    fun addAll(bypass: Boolean = false) {
        val animeService = Constant.injector.getInstance(AnimeService::class.java)
        val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)

        episodeMappingService.findAllAnimeUuidImageBannerAndUuidImage().asSequence()
            .groupBy { Triple(it[0] as UUID, it[1] as String, it[2] as String) }
            .forEach { (animeGroup, mappings) ->
                animeService.addThumbnail(animeGroup.first, animeGroup.second, bypass)
                animeService.addBanner(animeGroup.first, animeGroup.third, bypass)

                mappings.forEach {
                    episodeMappingService.addImage(
                        it[3] as UUID,
                        (it[4] as String).ifBlank { null } ?: Constant.DEFAULT_IMAGE_PREVIEW,
                        bypass
                    )
                }
            }
    }
}