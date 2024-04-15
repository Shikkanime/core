package fr.shikkanime.utils

import com.mortennobel.imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun ZonedDateTime.isEqualOrAfter(other: ZonedDateTime): Boolean {
    return this.isEqual(other) || this.isAfter(other)
}

fun ZonedDateTime.withUTC(): ZonedDateTime {
    return this.withZoneSameInstant(Constant.utcZoneId)
}

fun ZonedDateTime.withUTCString(): String {
    return withUTC().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

fun LocalDateTime.withUTC(): ZonedDateTime {
    return this.atZone(Constant.utcZoneId)
}

fun LocalTime.isEqualOrAfter(other: LocalTime): Boolean {
    return this == other || this.isAfter(other)
}

fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
    return ResampleOp(width, height).filter(this, null)
}