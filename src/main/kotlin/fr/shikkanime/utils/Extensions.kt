package fr.shikkanime.utils

import com.mortennobel.imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

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

fun <T> Array<T>.toTreeSet(comparator: Comparator<T>): TreeSet<T> = TreeSet(comparator).also { it.addAll(this) }
fun <T> Array<T>.toTreeSet(): TreeSet<T> where T : Comparable<T> = TreeSet<T>().also { it.addAll(this) }
fun <T> Iterable<T>.toTreeSet(comparator: Comparator<T>): TreeSet<T> = TreeSet(comparator).also { it.addAll(this) }
fun <T> Iterable<T>.toTreeSet(): TreeSet<T> where T : Comparable<T> = TreeSet<T>().also { it.addAll(this) }

fun ByteArray?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()

fun Boolean.onTrue(action: () -> Unit) = if (this) action() else Unit

fun <C> C.takeIfNotEmpty(): C? where C : Collection<*> = ifEmpty { null }
fun ByteArray.takeIfNotEmpty() = this.takeIf { it.isNotEmpty() }