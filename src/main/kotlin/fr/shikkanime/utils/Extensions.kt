package fr.shikkanime.utils

import com.mortennobel.imagescaling.ResampleOp
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO

fun ZonedDateTime.withUTC(): ZonedDateTime = this.withZoneSameInstant(Constant.utcZoneId)

fun ZonedDateTime.withUTCString(): String = withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

fun ZonedDateTime.isAfterOrEqual(other: ZonedDateTime): Boolean = this.isAfter(other) || this.isEqual(other)

fun ZonedDateTime.isBeforeOrEqual(other: ZonedDateTime): Boolean = this.isBefore(other) || this.isEqual(other)

fun LocalDate.atStartOfWeek(): LocalDate = this.with(DayOfWeek.MONDAY)

fun LocalDate.atEndOfWeek(): LocalDate = this.with(DayOfWeek.SUNDAY)

fun LocalDate.atEndOfTheDay(zoneId: ZoneId): ZonedDateTime = this.atTime(LocalTime.MAX).atZone(zoneId)

fun LocalTime.isAfterOrEqual(other: LocalTime) = this >= other

fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
    val newHeight = if (height <= 0) (this.height * (width.toDouble() / this.width)).toInt() else height
    return ResampleOp(width, newHeight).filter(this, null)
}

fun String?.normalize(): String? {
    return this?.replace("(?U)\\s+".toRegex(), StringUtils.SPACE_STRING)
        ?.trim()
        ?.ifBlank { null }
}

fun <T> Array<T>.toTreeSet(): TreeSet<T> where T : Comparable<T> = TreeSet<T>().also { it.addAll(this) }
fun <T> Iterable<T>.toTreeSet(comparator: Comparator<T>): TreeSet<T> = TreeSet(comparator).also { it.addAll(this) }
fun <T> Iterable<T>.toTreeSet(): TreeSet<T> where T : Comparable<T> = TreeSet<T>().also { it.addAll(this) }

fun ByteArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()

fun <T> Boolean.onTrue(block: () -> T): T? = if (this) block() else null
fun Boolean.onFalse(block: () -> Unit) = if (!this) block() else Unit

fun <C> C.takeIfNotEmpty(): C? where C : Collection<*> = ifEmpty { null }
fun ByteArray.takeIfNotEmpty() = this.takeIf { it.isNotEmpty() }
fun Boolean.takeIfFalse() = if (!this) true else null

/**
 * Converts the current `BufferedImage` instance to a byte array in the specified image format.
 *
 * @param format The format of the resulting image (e.g., "jpg", "png"). Defaults to "jpg" if not provided.
 * @return A byte array representing the image data in the specified format.
 */
fun BufferedImage.toByteArray(format: String = "jpg"): ByteArray {
    return ByteArrayOutputStream().use { outputStream ->
        ImageIO.write(this@toByteArray, format, outputStream)
        outputStream.toByteArray()
    }
}

inline fun <R, T : R> T?.ifNull(action: () -> R) = this ?: action()

/**
 * Executes the given action and returns its result if the current nullable `CharSequence` is
 * `null` or blank. Otherwise, returns the current `CharSequence`.
 *
 * @param action A lambda function to be executed and whose result will be returned
 * when the nullable `CharSequence` is either `null` or blank.
 * @return The result of the `action` if the receiver is `null` or blank, otherwise the current `CharSequence`.
 */
inline fun <R : CharSequence, T : R> T?.ifNullOrBlank(action: () -> R) = if (this.isNullOrBlank()) action() else this

/**
 * Reads the contents of a file from a `MultiPartData` object and returns it as a byte array.
 *
 * This function iterates through the parts of the `MultiPartData` object, looking for a file part.
 * When a file part is found, its contents are read and returned as a byte array.
 * Non-file parts are ignored. If no file part is found, the function throws an exception.
 *
 * @return The contents of the file part as a byte array.
 * @throws IllegalArgumentException if no file part is provided in the `MultiPartData`.
 */
suspend fun MultiPartData.readFileAsByteArray(): ByteArray {
    var bytes: ByteArray? = null

    forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                bytes = part.provider().readRemaining().readByteArray()
            }
            else -> {
                // Ignore other parts
            }
        }

        part.dispose()
    }

    requireNotNull(bytes) { "No file provided" }
    return bytes
}
