package fr.shikkanime.utils

import nu.pattern.OpenCV
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfInt
import org.opencv.imgcodecs.Imgcodecs
import java.io.*
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

    fun File.readBytesBuffered(): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val bytes = ByteArrayOutputStream()

        try {
            FileInputStream(this).use { fis ->
                var bytesRead = fis.read(buffer)

                while (bytesRead != -1) {
                    bytes.write(buffer, 0, bytesRead)
                    bytesRead = fis.read(buffer)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to read file")
        }

        return bytes.toByteArray()
    }

    inline fun <reified T> readFile(file: File): T {
        val value = ObjectInputStream(ByteArrayInputStream(file.readBytesBuffered())).use { it.readObject() }

        require(value is T && (T::class != List::class || typeOf<T>().arguments[0].type == (value as List<*>).firstOrNull()?.let { it::class.starProjectedType })) {
            "Invalid type"
        }

        return value
    }

    private fun File.writeBytesBuffered(bytes: ByteArray) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        try {
            ByteArrayInputStream(bytes).use { bais ->
                var bytesRead = bais.read(buffer)

                FileOutputStream(this).use { fos ->
                    while (bytesRead != -1) {
                        fos.write(buffer, 0, bytesRead)
                        bytesRead = bais.read(buffer)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to write file")
        }
    }

    fun writeFile(file: File, `object`: Any) {
        try {
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos ->
                    oos.writeObject(`object`)
                    file.writeBytesBuffered(baos.toByteArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to write file")
        }
    }
}