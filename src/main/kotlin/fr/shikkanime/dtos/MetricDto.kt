package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*

data class MetricDto(
    val uuid: UUID?,
    val cpuLoad: String,
    val memoryUsage: String,
    val date: String,
) : Serializable
