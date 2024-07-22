package fr.shikkanime.utils

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object FileManager {
    fun encodeToWebP(image: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        ImageIO.write(ImageIO.read(ByteArrayInputStream(image)), "webp", baos)
        return baos.toByteArray()
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