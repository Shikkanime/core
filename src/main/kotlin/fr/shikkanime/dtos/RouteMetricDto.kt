package fr.shikkanime.dtos

import java.io.Serializable

data class RouteMetricDto(
    val method: String,
    val path: String,
    val avgDuration: Double,
    val count: Long
) : Serializable
