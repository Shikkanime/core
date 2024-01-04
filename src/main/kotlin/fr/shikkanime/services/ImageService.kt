package fr.shikkanime.services

import com.mortennobel.imagescaling.ResampleOp
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis

object ImageService {
    data class Image(
        val uuid: String,
        val url: String,
        var bytes: ByteArray = byteArrayOf(),
        var originalSize: Long = 0,
        var size: Long = 0,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false

            if (uuid != other.uuid) return false
            if (url != other.url) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (originalSize != other.originalSize) return false
            if (size != other.size) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uuid.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + originalSize.hashCode()
            result = 31 * result + size.hashCode()
            return result
        }
    }

    private val threadPool = Executors.newFixedThreadPool(2)
    private val file = File(Constant.dataFolder, "images-cache.shikk")
    private var cache = mutableListOf<Image>()
    private val change = AtomicBoolean(false)

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
        if (!file.exists()) {
            change.set(true)
            saveCache()
            return
        }

        println("Loading images cache...")

        val take = measureTimeMillis {
            val json = String(FileManager.fromGzip(file.readBytes()))
            val map = ObjectParser.fromJson(json, Array<Image>::class.java).toMutableList()
            cache = map
        }

        println("Loaded images cache in $take ms (${cache.size} images)")
    }

    fun saveCache() {
        if (!change.get()) {
            println("No changes detected in images cache")
            return
        }

        if (!file.exists()) {
            file.createNewFile()
        }

        println("Saving images cache...")

        val take = measureTimeMillis {
            file.writeBytes(FileManager.toGzip(ObjectParser.toJson(cache).toByteArray()))
        }

        println(
            "Saved images cache in $take ms (${toHumanReadable(cache.sumOf { it.originalSize })} -> ${
                toHumanReadable(
                    cache.sumOf { it.size })
            })"
        )
        change.set(false)
    }

    fun add(uuid: UUID, url: String, width: Int, height: Int) {
        if (get(uuid) != null) {
            return
        }

        val image = Image(uuid.toString(), url)
        cache.add(image)

        threadPool.submit {
            val take = measureTimeMillis {
                try {
                    println("Loading image $url...")
                    val httpResponse = runBlocking {
                        HttpRequest().get(url)
                    }

                    if (httpResponse.status != HttpStatusCode.OK) {
                        println("Failed to load image $url")
                        remove(uuid)
                        return@measureTimeMillis
                    }

                    val bytes = runBlocking {
                        httpResponse.readBytes()
                    }

                    if (bytes.isEmpty()) {
                        println("Failed to load image $url")
                        remove(uuid)
                        return@measureTimeMillis
                    }

                    println("Encoding image to WebP...")
                    val resized = ResampleOp(width, height).filter(ImageIO.read(ByteArrayInputStream(bytes)), null)
                    val tmpFile = File.createTempFile("shikk", ".png")
                        .apply {
                            writeBytes(ByteArrayOutputStream().apply { ImageIO.write(resized, "png", this) }
                                .toByteArray())
                        }
                    val readBytesResized = tmpFile.readBytes()
                    val webp = FileManager.encodeToWebP(readBytesResized)
                    tmpFile.delete()

                    if (webp.isEmpty()) {
                        println("Failed to encode image to WebP")
                        remove(uuid)
                        return@measureTimeMillis
                    }

                    image.bytes = webp
                    image.originalSize = bytes.size.toLong()
                    image.size = webp.size.toLong()
                    cache[cache.indexOf(image)] = image
                    change.set(true)
                } catch (e: Exception) {
                    println("Failed to load image $url: ${e.message}")
                    e.printStackTrace()
                    remove(uuid)
                }
            }

            println(
                "Encoded image to WebP in ${take}ms (${toHumanReadable(image.originalSize)} -> ${
                    toHumanReadable(
                        image.size
                    )
                })"
            )
        }
    }

    fun remove(uuid: UUID) {
        cache.removeIf { it.uuid == uuid.toString() }
    }

    operator fun get(uuid: UUID): Image? = cache.find { it.uuid == uuid.toString() }
}