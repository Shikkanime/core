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
    val cache = mutableMapOf<Pair<String, Type>, Image>()
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

        this.cache.putAll(cachePart.associateBy { it.uuid to it.type })
        logger.info("Loaded images cache in $take ms (${this.cache.size} images)")
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

    private fun distributeImages(images: Collection<Image>): List<List<Image>> {
        // Sort images by size in descending order for a more balanced distribution
        val sortedImages = images.sortedByDescending { it.size }

        // Initialize lists to hold the distributed images
        val resultLists = MutableList(CACHE_FILE_NUMBER) { mutableListOf<Image>() }

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

        change.set(false)

        val parts = distributeImages(cache.values)

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
            cache[uuid.toString() to type] = image
            image
        } else {
            get(uuid, type) ?: run {
                val image = Image(uuid.toString(), type, url)
                cache[uuid.toString() to type] = image
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
            cache[uuid.toString() to type] = image
            image
        } else {
            get(uuid, type) ?: run {
                val image = Image(uuid.toString(), type)
                cache[uuid.toString() to type] = image
                image
            }
        }

        encodeImage(bytes, uuid, type, width, height, image)
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

                cache[uuid.toString() to type] = image
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
        cache.remove(uuid.toString() to type)
        change.set(true)
    }

    operator fun get(uuid: UUID, type: Type): Image? = cache[uuid.toString() to type]

    val size: Int
        get() = cache.size

    val originalSize: String
        get() = toHumanReadable(cache.values.sumOf { it.originalSize })

    val compressedSize: String
        get() = toHumanReadable(cache.values.sumOf { it.size })

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

            val uuids = query.resultList.asSequence()
                .filterIsInstance<UUID>()
                .map { uuid -> uuid.toString() }
                .toSet()

            // Calculate the difference between the cache and the UUIDs
            val difference = cache.filter { entry -> entry.key.first !in uuids }.values
            logger.warning("Removing ${difference.size} images from cache, not found in database")
            logger.warning("${toHumanReadable(difference.sumOf { img -> img.size })} will be free")

            cache.keys.removeAll { pair -> pair.first !in uuids }
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

        episodeMappingService.findAllAnimeUuidImageBannerAndUuidImage().groupBy {
            Triple(it[0] as UUID, it[1] as String, it[2] as String)
        }.forEach { (animeGroup, mappings) ->
            animeService.addImage(animeGroup.first, animeGroup.second, bypass)
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