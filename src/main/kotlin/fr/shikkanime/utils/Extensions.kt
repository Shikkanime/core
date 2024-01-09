package fr.shikkanime.utils

import java.time.ZonedDateTime

fun ZonedDateTime.isEqualOrAfter(other: ZonedDateTime): Boolean {
    return this.isEqual(other) || this.isAfter(other)
}