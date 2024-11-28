package fr.shikkanime.utils

import com.mortennobel.imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.time.*
import java.time.format.DateTimeFormatter

fun ZonedDateTime.isEqualOrBefore(other: ZonedDateTime) = this.isEqual(other) || this.isBefore(other)

fun ZonedDateTime.isEqualOrAfter(other: ZonedDateTime) = this.isEqual(other) || this.isAfter(other)

fun ZonedDateTime.isBetween(z0: ZonedDateTime, z1: ZonedDateTime): Boolean {
    val min = minOf(z0, z1)
    val max = maxOf(z0, z1)
    return this.isEqualOrAfter(min) && this.isEqualOrBefore(max)
}

fun ZonedDateTime.withUTC(): ZonedDateTime = this.withZoneSameInstant(Constant.utcZoneId)

fun ZonedDateTime.withUTCString(): String = withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

fun LocalTime.isEqualOrAfter(other: LocalTime) = this == other || this.isAfter(other)

fun LocalDate.atStartOfWeek(): LocalDate = this.with(DayOfWeek.MONDAY)

fun LocalDate.atEndOfWeek(): LocalDate = this.with(DayOfWeek.SUNDAY)

fun LocalDate.atEndOfTheDay(zoneId: ZoneId): ZonedDateTime = this.atTime(LocalTime.MAX).atZone(zoneId)

fun BufferedImage.resize(width: Int, height: Int): BufferedImage = ResampleOp(width, height).filter(this, null)

fun String?.normalize(): String? {
    return this?.replace("(?U)\\s+".toRegex(), " ")
        ?.trim()
        ?.ifBlank { null }
}