package fr.shikkanime.services

import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.utils.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Serializable
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
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
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Media

            if (uuid != other.uuid) return false
            if (type != other.type) return false
            if (url != other.url) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (originalSize != other.originalSize) return false
            if (lastUpdateDateTime != other.lastUpdateDateTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uuid.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + (url?.hashCode() ?: 0)
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + originalSize.hashCode()
            result = 31 * result + (lastUpdateDateTime?.hashCode() ?: 0)
            return result
        }

        companion object {
            const val serialVersionUID: Long = 0
        }
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val nThreads = Runtime.getRuntime().availableProcessors()
    private var threadPool = Executors.newFixedThreadPool(nThreads)
    val cache = mutableMapOf<Pair<UUID, ImageType>, Media>()
    private val change = AtomicBoolean(false)
    private const val CACHE_FILE_NUMBER = 4

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

    fun loadCache() {
        logger.info("Loading images cache...")
        val cachePart = mutableListOf<Media>()

        val take = measureTimeMillis {
            (0..<CACHE_FILE_NUMBER).forEach { index ->
                val part = loadCachePart(File(Constant.dataFolder, "images-cache-part-$index.shikk"))
                cachePart.addAll(part)
                System.gc()
            }
        }

        this.cache.putAll(cachePart.associateBy { it.uuid to it.type })
        logger.info("Loaded images cache in $take ms (${this.cache.size} images)")
    }

    private fun loadCachePart(file: File): List<Media> {
        if (!file.exists()) {
            return emptyList()
        }

        logger.info("Loading images cache part...")
        val cache = mutableListOf<Media>()

        val take = measureTimeMillis {
            val deserializedCache = FileManager.readFile<List<Media>>(file)
            cache.addAll(deserializedCache)
        }

        logger.info("Loaded images cache part in $take ms (${cache.size} images)")
        return cache
    }

    private fun distributeImages(images: Collection<Media>): List<List<Media>> {
        // Sort images by size in descending order for a more balanced distribution
        val sortedImages = images.sortedByDescending { it.bytes.size }

        // Initialize lists to hold the distributed images
        val resultLists = MutableList(CACHE_FILE_NUMBER) { mutableListOf<Media>() }

        // Distribute images using round-robin approach
        sortedImages.forEachIndexed { index, image ->
            resultLists[index % CACHE_FILE_NUMBER].add(image)
        }

        return resultLists
    }

    fun saveCache() {
        if (!change.get()) {
            logger.info("No changes detected in images cache")
            return
        }

        val parts = distributeImages(cache.values)
        logger.info("Saving images cache...")

        val take = measureTimeMillis {
            if (parts.isNotEmpty()) {
                parts.forEach { part ->
                    val index = parts.indexOf(part)
                    saveCachePart(part, File(Constant.dataFolder, "images-cache-part-$index.shikk"))
                    System.gc()
                }
            } else {
                (0..<CACHE_FILE_NUMBER).forEach { index ->
                    saveCachePart(emptyList(), File(Constant.dataFolder, "images-cache-part-$index.shikk"))
                    System.gc()
                }
            }
        }

        logger.info("Saved images cache in $take ms ($originalSize -> $encodedSize)")
        change.set(false)
    }

    private fun saveCachePart(cache: List<Media>, file: File) {
        if (!file.exists()) {
            file.createNewFile()
        }

        logger.info("Saving images cache part...")
        val take = measureTimeMillis { FileManager.writeFile(file, cache) }

        logger.info(
            "Saved images cache part in $take ms (${toHumanReadable(cache.sumOf { it.originalSize.toLong() })} -> ${
                toHumanReadable(
                    cache.sumOf { it.bytes.size.toLong() })
            })"
        )
    }

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

        if (!bypass && (get(uuid, type) != null || isEmpty)) {
            return
        }

        val media = if (!bypass) {
            val media = Media(uuid, type, url)
            cache[uuid to type] = media
            media
        } else {
            get(uuid, type) ?: run {
                val media = Media(uuid, type, url)
                cache[uuid to type] = media
                media
            }
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

                cache[uuid to type] = media
                change.set(true)
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
        change.set(true)
    }

    operator fun get(uuid: UUID, type: ImageType): Media? = cache[uuid to type]

    val originalSize: String
        get() = toHumanReadable(cache.values.sumOf { it.originalSize.toLong() })

    val encodedSize: String
        get() = toHumanReadable(cache.values.sumOf { it.bytes.size.toLong() })

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

            // Calculate the difference between the cache and the UUIDs
            val difference = cache.filter { entry -> entry.key.first !in uuids }.values

            if (difference.isNotEmpty()) {
                logger.warning("Removing ${difference.size} images from cache, not found in database")
                logger.warning("${toHumanReadable(difference.sumOf { img -> img.bytes.size.toLong() })} will be free")

                cache.keys.removeAll { pair -> pair.first !in uuids }
                change.set(true)
            }
        }
    }

    fun clearPool() {
        threadPool.shutdownNow()
        threadPool = Executors.newFixedThreadPool(nThreads)
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