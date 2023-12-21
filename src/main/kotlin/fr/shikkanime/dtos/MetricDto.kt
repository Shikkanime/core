package fr.shikkanime.dtos

import java.io.Serializable
import java.util.*

data class MetricDto(
    val uuid: UUID?,
    val cpuLoad: String,
    val averageCpuLoad: String,
    val memoryUsage: String,
    val averageMemoryUsage: String,
    val date: String,
) : Serializable
