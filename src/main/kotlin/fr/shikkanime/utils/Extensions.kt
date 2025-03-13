package fr.shikkanime.utils

import com.mortennobel.imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.time.*
import java.time.format.DateTimeFormatter

fun ZonedDateTime.withUTC(): ZonedDateTime = this.withZoneSameInstant(Constant.utcZoneId)

fun ZonedDateTime.withUTCString(): String = withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

fun ZonedDateTime.isAfterOrEqual(other: ZonedDateTime): Boolean = this.isAfter(other) || this.isEqual(other)

fun LocalDate.atStartOfWeek(): LocalDate = this.with(DayOfWeek.MONDAY)

fun LocalDate.atEndOfWeek(): LocalDate = this.with(DayOfWeek.SUNDAY)

fun LocalDate.atEndOfTheDay(zoneId: ZoneId): ZonedDateTime = this.atTime(LocalTime.MAX).atZone(zoneId)

fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
    val newHeight = if (height <= 0) (this.height * (width.toDouble() / this.width)).toInt() else height
    return ResampleOp(width, newHeight).filter(this, null)
}

fun String?.normalize(): String? {
    return this?.replace("(?U)\\s+".toRegex(), " ")
        ?.trim()
        ?.ifBlank { null }
}