package fr.shikkanime.dtos

data class GroupedMetricDto(
    val date: String,
    val avgCpuLoad: String,
    val avgMemoryUsage: String,
    val avgThreadCount: String,
    val maxPeakThreadCount: String
)