package fr.shikkanime.jobs.diagnostics

import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.Platform

/**
 * Compact projection of a platform item, kept in every verdict whatever the outcome.
 *
 * Deliberately not the raw API payload: a diagnostic holds one line per item for a whole run,
 * so it keeps the fields that identify the item and nothing else.
 *
 * @property platform Platform the item came from.
 * @property platformId Identifier of the item on that platform.
 * @property showTitle Title of the show the item belongs to.
 * @property title Title of the item itself.
 * @property releaseDateTime Platform-reported release date, or `null` when it carries none.
 * @property audioLocales Audio locales the item is available in, empty when unreported.
 */
data class PlatformItem(
    val platform: Platform,
    val platformId: String,
    val showTitle: String,
    val title: String,
    val releaseDateTime: String?,
    val audioLocales: List<String>
) {
    /**
     * Renders the item as one readable line: platform, id, show, title, locales and date.
     * Missing values get an explicit marker rather than an empty gap, so a blank field is
     * never confused with a rendering bug.
     */
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
 * The outcome of a single platform item: either ingested, or rejected with a reason and the
 * evidence that produced it.
 *
 * @property item The item this verdict is about, kept on every outcome so a rejected item
 * stays identifiable.
 */
sealed interface IngestionVerdict {
    val item: PlatformItem

    /**
     * The item passed every filter and carries the episode it produced.
     *
     * @property episode The mapped episode, the only thing the ingestion path consumes.
     */
    data class Accepted(
        override val item: PlatformItem,
        val episode: PlatformEpisode
    ) : IngestionVerdict

    /**
     * The item was dropped by a rule.
     *
     * The constructor is private: a rejection without evidence cannot be acted on, so [of]
     * is the only way to build one and it refuses blank evidence.
     *
     * @property reason The rule that rejected the item.
     * @property evidence The value that triggered the rule, e.g. `genres=[Live Action, Drama]`.
     */
    data class Rejected private constructor(
        override val item: PlatformItem,
        val reason: RejectionReason,
        val evidence: String
    ) : IngestionVerdict {
        companion object {
            /**
             * Builds a rejection, refusing an evidence-less one.
             *
             * @throws IllegalArgumentException if [evidence] is blank.
             */
            fun of(item: PlatformItem, reason: RejectionReason, evidence: String): Rejected {
                require(evidence.isNotBlank()) {
                    "A rejection must carry evidence, otherwise it cannot be diagnosed"
                }

                return Rejected(item, reason, evidence)
            }
        }
    }

    companion object {
        /**
         * Records an item that passed every filter.
         */
        fun accepted(item: PlatformItem, episode: PlatformEpisode): IngestionVerdict =
            Accepted(item, episode)

        /**
         * Records an item that a rule dropped.
         *
         * @throws IllegalArgumentException if [evidence] is blank.
         */
        fun rejected(
            item: PlatformItem,
            reason: RejectionReason,
            evidence: String
        ): IngestionVerdict.Rejected = Rejected.of(item, reason, evidence)
    }
}
