package fr.shikkanime.utils

import java.io.BufferedInputStream
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.math.max

object FileManager {
    private const val WEBP_QUALITY = 75
    private const val MAX_PROCESS_TIME = 30L
    private val logger = Logger.getLogger(FileManager::class.java.name)

    private fun isWebPAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("cwebp", "-version").start()
            val completed = process.waitFor(MAX_PROCESS_TIME, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                false
            } else {
                process.exitValue() == 0
            }
        } catch (_: Exception) {
            false
        }
    }

    fun convertToWebP(image: ByteArray, width: Int, height: Int): ByteArray {
        require(isWebPAvailable()) { "cwebp command not found or not available" }

        try {
            val safeHeight = max(0, height)

            val processBuilder = ProcessBuilder(
                "cwebp",
                "-q", "$WEBP_QUALITY",
                "-resize", width.toString(), safeHeight.toString(),
                "-o", "-",
                "--",
                "-"
            )

            val process = processBuilder.start()

            thread {
                runCatching { process.outputStream.use { it.write(image) } }
            }

            val webpBytes = process.inputStream.readAllBytes()
            val error = process.errorStream.bufferedReader().readText()

            val completed = process.waitFor(MAX_PROCESS_TIME, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                throw Exception("Image conversion timed out")
            }

            if (process.exitValue() != 0) {
                throw Exception("Image conversion failed: $error")
            }

            return webpBytes
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to convert image to WebP", e)
            throw e
        }
    }

    fun getInputStreamFromResource(resource: String): BufferedInputStream {
        return try {
            this.javaClass.classLoader.getResourceAsStream(resource)?.buffered()
                ?: throw Exception("Resource not found")
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to get input stream from resource")
        }
    }
}