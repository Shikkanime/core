package fr.shikkanime.jobs.diagnostics

import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.Platform

/**
 * Compact projection of a platform item, kept in every verdict whatever the outcome.
 *
 * Deliberately not the raw API payload: a diagnostic holds one line per item for a whole run,
 * so it keeps the fields that identify the item and nothing else.
 */
data class PlatformItem(
    val platform: Platform,
    val platformId: String,
    val showTitle: String,
    val title: String,
    val releaseDateTime: String?,
    val audioLocales: List<String>
) {
    fun describe(): String =
        "$platform #$platformId — $showTitle / $title (" +
            "${audioLocales.joinToString(", ").ifEmpty { NO_AUDIO_LOCALE }}" +
            ", ${releaseDateTime ?: NO_RELEASE_DATE})"

    private companion object {
        private const val NO_AUDIO_LOCALE = "no audio locale"
        private const val NO_RELEASE_DATE = "no release date"
    }
}

/**
 * The outcome of one platform item: either ingested, or rejected with a reason and the
 * evidence that produced it.
 */
sealed interface IngestionVerdict {
    val item: PlatformItem

    data class Accepted(
        override val item: PlatformItem,
        val episode: PlatformEpisode
    ) : IngestionVerdict

    data class Rejected private constructor(
        override val item: PlatformItem,
        val reason: RejectionReason,
        val evidence: String
    ) : IngestionVerdict {
        companion object {
            fun of(item: PlatformItem, reason: RejectionReason, evidence: String): Rejected {
                require(evidence.isNotBlank()) { "A rejection must carry evidence, otherwise it cannot be diagnosed" }

                return Rejected(item, reason, evidence)
            }
        }
    }

    companion object {
        fun accepted(item: PlatformItem, episode: PlatformEpisode): IngestionVerdict =
            Accepted(item, episode)

        fun rejected(item: PlatformItem, reason: RejectionReason, evidence: String): IngestionVerdict.Rejected =
            Rejected.of(item, reason, evidence)
    }
}
