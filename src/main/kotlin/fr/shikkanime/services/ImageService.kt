package fr.shikkanime.services

import fr.shikkanime.utils.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

private const val FAILED_TO_ENCODE_MESSAGE = "Failed to encode image to WebP"

object ImageService {
    enum class Type {
        IMAGE,
        BANNER,
    }

    data class Image(
        val uuid: String,
        val type: Type,
        val url: String? = null,
        var bytes: ByteArray = byteArrayOf(),
        var originalSize: Long = 0,
        var size: Long = 0,
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Image

            if (uuid != other.uuid) return false
            if (type != other.type) return false
            if (url != other.url) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (originalSize != other.originalSize) return false
            if (size != other.size) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uuid.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + (url?.hashCode() ?: 0)
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + originalSize.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val nThreads = Runtime.getRuntime().availableProcessors()
    private var threadPool = Executors.newFixedThreadPool(nThreads)
    val cache = mutableListOf<Image>()
    private val searchCacheIndex = mutableMapOf<String, Int>()
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
        val cachePart = mutableListOf<Image>()

        val take = measureTimeMillis {
            (0..<CACHE_FILE_NUMBER).toList().parallelStream().forEach { index ->
                val part = loadCachePart(File(Constant.dataFolder, "images-cache-part-$index.shikk"))
                cachePart.addAll(part)
            }
        }

        this.cache.addAll(cachePart)
        logger.info("Loaded images cache in $take ms (${this.cache.size} images)")
        createImageIndex()
    }

    private fun createImageIndex() {
        logger.info("Creating search index...")

        val take = measureTimeMillis {
            this.searchCacheIndex.clear()

            this.searchCacheIndex.putAll(
                cache.asSequence()
                    .mapIndexed { index, image -> image.uuid + image.type to index }
                    .toMap()
            )
        }

        logger.info("Created search index in $take ms")
    }

    private fun loadCachePart(file: File): List<Image> {
        if (!file.exists()) {
            return emptyList()
        }

        logger.info("Loading images cache part...")
        val cache = mutableListOf<Image>()

        val take = measureTimeMillis {
            val deserializedCache = FileManager.readFile<List<Image>>(file)
            cache.addAll(deserializedCache)
        }

        logger.info("Loaded images cache part in $take ms (${cache.size} images)")
        return cache
    }

    private fun distributeImages(images: List<Image>, numberOfLists: Int): List<List<Image>> {
        // Sort images by size in descending order for a more balanced distribution
        val sortedImages = images.sortedByDescending { it.size }

        // Initialize lists to hold the distributed images
        val resultLists = MutableList(numberOfLists) { mutableListOf<Image>() }

        // Distribute images using round-robin approach
        sortedImages.forEachIndexed { index, image ->
            resultLists[index % numberOfLists].add(image)
        }

        return resultLists
    }

    fun saveCache() {
        if (!change.get()) {
            logger.info("No changes detected in images cache")
            return
        }

        change.set(false)

        val parts = distributeImages(cache, CACHE_FILE_NUMBER)

        logger.info("Saving images cache...")

        val take = measureTimeMillis {
            if (parts.isNotEmpty()) {
                parts.parallelStream().forEach { part ->
                    val index = parts.indexOf(part)
                    saveCachePart(part, File(Constant.dataFolder, "images-cache-part-$index.shikk"))
                }
            } else {
                (0..<CACHE_FILE_NUMBER).toList().parallelStream().forEach { index ->
                    saveCachePart(emptyList(), File(Constant.dataFolder, "images-cache-part-$index.shikk"))
                }
            }
        }

        logger.info("Saved images cache in $take ms ($originalSize -> $compressedSize)")
    }

    private fun saveCachePart(cache: List<Image>, file: File) {
        if (!file.exists()) {
            file.createNewFile()
        }

        logger.info("Saving images cache part...")
        val take = measureTimeMillis { FileManager.writeFile(file, cache) }

        logger.info(
            "Saved images cache part in $take ms (${toHumanReadable(cache.sumOf { it.originalSize })} -> ${
                toHumanReadable(
                    cache.sumOf { it.size })
            })"
        )
    }

    fun add(uuid: UUID, type: Type, url: String, width: Int, height: Int, bypass: Boolean = false) {
        if (!bypass && (get(uuid, type) != null || url.isBlank())) {
            return
        }

        val image = if (!bypass) {
            val image = Image(uuid.toString(), type, url)
            val futureIndex = cache.size
            cache.add(image)
            searchCacheIndex[uuid.toString() + type] = futureIndex
            image
        } else {
            get(uuid, type) ?: run {
                val image = Image(uuid.toString(), type, url)
                val futureIndex = cache.size
                cache.add(image)
                searchCacheIndex[uuid.toString() + type] = futureIndex
                image
            }
        }

        threadPool.submit { encodeImage(url, uuid, type, width, height, image) }
    }

    fun add(uuid: UUID, type: Type, bytes: ByteArray, width: Int, height: Int, bypass: Boolean = false) {
        if (!bypass && (get(uuid, type) != null || bytes.isEmpty())) {
            return
        }

        val image = if (!bypass) {
            val image = Image(uuid.toString(), type)
            val futureIndex = cache.size
            cache.add(image)
            searchCacheIndex[uuid.toString() + type] = futureIndex
            image
        } else {
            get(uuid, type) ?: run {
                val image = Image(uuid.toString(), type)
                val futureIndex = cache.size
                cache.add(image)
                searchCacheIndex[uuid.toString() + type] = futureIndex
                image
            }
        }

        threadPool.submit { encodeImage(bytes, uuid, type, width, height, image) }
    }

    private fun encodeImage(
        url: String,
        uuid: UUID,
        type: Type,
        width: Int,
        height: Int,
        image: Image
    ) {
        val (httpResponse, bytes) = runBlocking {
            val response = HttpRequest().get(url)
            response to response.readRawBytes()
        }

        if (httpResponse.status != HttpStatusCode.OK || bytes.isEmpty()) {
            logger.warning("Failed to load image $url")
            remove(uuid, type)
            return
        }

        encodeImage(
            bytes,
            uuid,
            type,
            width,
            height,
            image
        )
    }

    private fun encodeImage(
        bytes: ByteArray,
        uuid: UUID,
        type: Type,
        width: Int,
        height: Int,
        image: Image
    ) {
        val take = measureTimeMillis {
            try {
                if (bytes.isEmpty()) {
                    logger.warning(FAILED_TO_ENCODE_MESSAGE)
                    remove(uuid, type)
                    return@measureTimeMillis
                }

                val resized = ImageIO.read(ByteArrayInputStream(bytes)).resize(width, height)
                val webp =
                    FileManager.encodeToWebP(ByteArrayOutputStream().apply { ImageIO.write(resized, "png", this) }
                        .toByteArray())

                if (webp.isEmpty()) {
                    logger.warning(FAILED_TO_ENCODE_MESSAGE)
                    remove(uuid, type)
                    return@measureTimeMillis
                }

                image.bytes = webp
                image.originalSize = bytes.size.toLong()
                image.size = webp.size.toLong()

                val indexOf = searchCacheIndex[uuid.toString() + type] ?: -1

                if (indexOf == -1) {
                    logger.warning("Failed to find image in cache")
                    return@measureTimeMillis
                }

                cache[indexOf] = image
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
            "Encoded image to WebP in ${take}ms (${toHumanReadable(image.originalSize)} -> ${
                toHumanReadable(
                    image.size
                )
            })"
        )
    }

    fun remove(uuid: UUID, type: Type) {
        cache.removeIf { it.uuid == uuid.toString() && it.type == type }
        searchCacheIndex.remove(uuid.toString() + type)
        change.set(true)
    }

    operator fun get(uuid: UUID, type: Type): Image? = searchCacheIndex[uuid.toString() + type]?.let { cache[it] }

    val size: Int
        get() = cache.toList().size

    val originalSize: String
        get() = toHumanReadable(cache.toList().sumOf { it.originalSize })

    val compressedSize: String
        get() = toHumanReadable(cache.toList().sumOf { it.size })

    fun invalidate() {
        removeUnusedImages()
        addAll(true)
    }

    private fun removeUnusedImages() {
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

            val uuids = (query.resultList as List<UUID>) // NOSONAR
                .map { uuid -> uuid.toString() }
                .toSet()

            // Calculate the difference between the cache and the UUIDs
            val difference = cache.filter { img -> img.uuid !in uuids }
            logger.warning("Removing ${difference.size} images from cache, not found in database")
            logger.warning("${toHumanReadable(difference.sumOf { img -> img.size })} will be free")

            cache.removeIf { img -> img.uuid !in uuids }
            createImageIndex()
        }
    }

    fun clearPool() {
        threadPool.shutdownNow()
        threadPool = Executors.newFixedThreadPool(nThreads)
        cache.clear()
        searchCacheIndex.clear()
    }

    fun addAll(bypass: Boolean = false) {
        val animeService = Constant.injector.getInstance(AnimeService::class.java)
        val episodeMappingService = Constant.injector.getInstance(EpisodeMappingService::class.java)

        animeService.findAllUuidImageAndBanner().forEach {
            val uuid = it[0] as UUID
            val image = it[1] as String
            val banner = it[2] as String

            animeService.addImage(uuid, image, bypass)
            animeService.addBanner(uuid, banner, bypass)
        }

        episodeMappingService.findAllUuidAndImage().forEach {
            val uuid = it[0] as UUID
            val image = it[1] as String

            episodeMappingService.addImage(
                uuid,
                image.ifBlank { null } ?: Constant.DEFAULT_IMAGE_PREVIEW,
                bypass
            )
        }
    }
}