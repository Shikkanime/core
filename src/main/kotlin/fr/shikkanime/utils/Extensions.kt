package fr.shikkanime.utils

import com.mortennobel.imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.time.*
import java.time.format.DateTimeFormatter

fun ZonedDateTime.isEqualOrAfter(other: ZonedDateTime) = this.isEqual(other) || this.isAfter(other)

fun ZonedDateTime.withUTC() = this.withZoneSameInstant(Constant.utcZoneId)

fun ZonedDateTime.withUTCString() = withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

fun LocalTime.isEqualOrAfter(other: LocalTime) = this == other || this.isAfter(other)

fun LocalDate.atStartOfWeek() = this.with(DayOfWeek.MONDAY)

fun LocalDate.atEndOfWeek() = this.with(DayOfWeek.SUNDAY)

fun LocalDate.atEndOfTheDay(zoneId: ZoneId) = this.atTime(LocalTime.MAX).atZone(zoneId)

fun BufferedImage.resize(width: Int, height: Int) = ResampleOp(width, height).filter(this, null)

fun String?.normalize(): String? {
    return this?.replace("(?U)\\s+".toRegex(), " ")
        ?.trim()
        ?.ifBlank { null }
}