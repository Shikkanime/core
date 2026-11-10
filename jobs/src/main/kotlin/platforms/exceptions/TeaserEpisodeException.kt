package fr.shikkanime.jobs.platforms.exceptions

/**
 * Thrown when an episode release is a teaser or a promotional video rather than a real episode.
 *
 * Teasers, trailers and preview videos are intentionally skipped by the platforms: they carry no
 * episode number and must never be ingested as regular releases.
 *
 * @param episodeId identifier of the teaser episode that was skipped.
 * @param episodeName name of the teaser episode, when the platform provides one.
 * @param reason the platform-specific reason the release was considered a teaser.
 * @param message Descriptive failure details including the faulty episode identification and context.
 * @param cause Optional underlying cause of the failure.
 * @see [IllegalStateException]
 */
class TeaserEpisodeException(
    val episodeId: String? = null,
    val episodeName: String? = null,
    val reason: String? = null,
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause) {

    private companion object {
        /**
         * Builds the failure message of an exception from the teaser episode context.
         *
         * @param episodeId identifier of the teaser episode.
         * @param episodeName optional name of the episode, dropped from the message when blank.
         * @param reason platform-specific reason the release was skipped.
         * @return the descriptive failure message.
         * @see [TeaserEpisodeException]
         */
        fun buildErrorMessage(
            episodeId: String,
            episodeName: String?,
            reason: String?
        ): String {
            val titlePart = episodeName?.takeIf(String::isNotBlank)
                ?.let { " (name=\"$it\")" }
                .orEmpty()
            val reasonPart = reason?.takeIf(String::isNotBlank)
                ?.let { " ($it)" }
                .orEmpty()

            return "Episode id=$episodeId$titlePart is a teaser and was skipped$reasonPart"
        }
    }

    /**
     * Constructs a [TeaserEpisodeException] with structured episode context.
     *
     * @param episodeId Identifier of the teaser episode.
     * @param episodeName Optional title or name of the episode.
     * @param reason Platform-specific reason the release was skipped.
     */
    constructor(
        episodeId: String,
        episodeName: String? = null,
        reason: String? = null
    ) : this(
        episodeId = episodeId,
        episodeName = episodeName,
        reason = reason,
        message = buildErrorMessage(episodeId, episodeName, reason)
    )
}
