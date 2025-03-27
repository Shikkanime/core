package fr.shikkanime.utils

import nu.pattern.OpenCV
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfInt
import org.opencv.imgcodecs.Imgcodecs
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

object FileManager {
    init {
        OpenCV.loadLocally()
    }

    fun encodeToWebP(image: ByteArray): ByteArray {
        val matImage = Imgcodecs.imdecode(MatOfByte(*image), Imgcodecs.IMREAD_UNCHANGED)
        val parameters = MatOfInt(Imgcodecs.IMWRITE_WEBP_QUALITY, 75)
        val output = MatOfByte()

        if (Imgcodecs.imencode(".webp", matImage, output, parameters)) {
            return output.toArray()
        } else {
            throw Exception("Failed to encode image to WebP")
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

    inline fun <reified T> readFile(file: File): T {
        val value = ObjectInputStream(FileInputStream(file)).use { it.readObject() }

        require(value is T && (T::class != List::class || typeOf<T>().arguments[0].type == (value as List<*>).firstOrNull()?.let { it::class.starProjectedType })) {
            "Invalid type"
        }

        return value
    }
}