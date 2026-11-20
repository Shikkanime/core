package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Raw payload of one external API response, kept for a limited time.
 *
 * Metadata platforms ban aggressive clients, so every request is answered from this cache
 * while its entry is fresh: [key] identifies the request (platform, endpoint and parameters)
 * and [body] stores the response exactly as received, leaving the parsing to the caller.
 *
 * @see [createdAt]
 */
data class ApiCacheEntry(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val key: String,
    val body: String
) : ShikkModel(id, createdAt, updatedAt)
