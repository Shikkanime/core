package fr.shikkanime.utils

import com.mortennobel.imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.time.LocalTime
import java.time.ZonedDateTime

fun ZonedDateTime.isEqualOrAfter(other: ZonedDateTime): Boolean {
    return this.isEqual(other) || this.isAfter(other)
}

fun LocalTime.isEqualOrAfter(other: LocalTime): Boolean {
    return this == other || this.isAfter(other)
}

fun BufferedImage.resize(width: Int, height: Int): BufferedImage {
    return ResampleOp(width, height).filter(this, null)
}