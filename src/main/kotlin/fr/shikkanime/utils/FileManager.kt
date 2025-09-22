package fr.shikkanime.utils

import java.io.BufferedInputStream
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

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

    fun convertToWebP(image: ByteArray): ByteArray {
        if (!isWebPAvailable()) {
            throw Exception("cwebp command not found or not available")
        }

        // Create a temporary file for the input image
        val tempInputFile = File.createTempFile("input_", ".png")
        val tempOutputFile = File.createTempFile("output_", ".webp")
        
        try {
            // Write input image to temporary file
            tempInputFile.writeBytes(image)
            
            // Build the command to convert image using cwebp
            val processBuilder = ProcessBuilder(
                "cwebp",
                "-q", "$WEBP_QUALITY",
                tempInputFile.absolutePath,
                "-o", tempOutputFile.absolutePath
            )
            
            // Start the process
            val process = processBuilder.start()
            
            // Wait for the process to complete with a timeout
            val completed = process.waitFor(MAX_PROCESS_TIME, TimeUnit.SECONDS)
            
            if (!completed) {
                process.destroyForcibly()
                throw Exception("Image conversion timed out")
            }
            
            if (process.exitValue() != 0) {
                val error = process.errorStream.bufferedReader().readText()
                throw Exception("Image conversion failed: $error")
            }
            
            // Read the converted image
            return tempOutputFile.readBytes()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to convert image to WebP", e)
            throw e
        } finally {
            // Clean up temporary files
            if (!tempInputFile.delete())
                logger.warning("Failed to delete temporary input file: ${tempInputFile.absolutePath}")

            if(!tempOutputFile.delete())
                logger.warning("Failed to delete temporary output file: ${tempOutputFile.absolutePath}")
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