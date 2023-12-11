package fr.shikkanime.utils

import nu.pattern.OpenCV
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfInt
import org.opencv.imgcodecs.Imgcodecs
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CompressionManager {
    init {
        OpenCV.loadLocally()
    }

    fun toGzip(bytes: ByteArray): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(byteArrayOutputStream)
        gzip.write(bytes)
        gzip.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun fromGzip(bytes: ByteArray): ByteArray {
        val gzip = GZIPInputStream(ByteArrayInputStream(bytes))
        val compressed = gzip.readBytes()
        gzip.close()
        return compressed
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
}