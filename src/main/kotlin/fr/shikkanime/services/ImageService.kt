package fr.shikkanime.services

import fr.shikkanime.utils.CompressionManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

object ImageService {
    data class Image(
        val url: String,
        var bytes: ByteArray = byteArrayOf(),
        var ratio: Double = 0.0,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Image

            if (url != other.url) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (ratio != other.ratio) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + ratio.hashCode()
            return result
        }
    }

    private val threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1)
    private val file = File("images-cache.shikk")
    private var cache = mutableMapOf<UUID, Image>()

    fun loadCache() {
        if (!file.exists()) {
            saveCache()
            return
        }

        println("Loading images cache...")

        val take = measureTimeMillis {
            val json = String(CompressionManager.fromGzip(file.readBytes()))
            val map = ObjectParser.fromJson(json, mutableMapOf<UUID, Image>().javaClass)
            cache = map
        }

        println("Loaded images cache in $take ms")
    }

    fun saveCache() {
        if (!file.exists()) {
            file.createNewFile()
        }

        println("Saving images cache...")

        val take = measureTimeMillis {
            file.writeBytes(CompressionManager.toGzip(ObjectParser.toJson(cache).toByteArray()))
        }

        println("Saved images cache in $take ms")
    }

    fun contains(uuid: UUID) = cache.containsKey(uuid)

    fun add(uuid: UUID, url: String) {
        val image = Image(url)
        cache[uuid] = image

        threadPool.submit {
            val take = measureTimeMillis {
                try {
                    println("Loading image $url...")
                    val httpResponse = runBlocking {
                        HttpRequest().get(url)
                    }

                    if (httpResponse.status != HttpStatusCode.OK) {
                        println("Failed to load image $url")
                        cache.remove(uuid)
                        return@measureTimeMillis
                    }

                    val bytes = runBlocking {
                        httpResponse.readBytes()
                    }

                    if (bytes.isEmpty()) {
                        println("Failed to load image $url")
                        cache.remove(uuid)
                        return@measureTimeMillis
                    }

                    println("Encoding image to WebP...")
                    val webp = CompressionManager.encodeToWebP(bytes)
                    image.bytes = webp
                    image.ratio = bytes.size.toDouble() / webp.size.toDouble()
                    cache[uuid] = image
                } catch (e: Exception) {
                    println("Failed to load image $url: ${e.message}")
                    cache.remove(uuid)
                }
            }

            println("Ratio for $url: ${String.format("%.2f", image.ratio * 100)}%, took $take ms")
            println("Encoded image to WebP in ${take}ms")
        }
    }
}