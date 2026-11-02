package fr.shikkanime.jobs.platforms.exceptions

import fr.shikkanime.models.Country

/**
 * Thrown when an episode release cannot be mapped due to missing or unsupported audio locales.
 *
 * @param episodeId identifier of the episode that could not be mapped.
 * @param episodeName name of the episode that could not be mapped.
 * @param languages raw language codes the platform returned.
 * @param country country whose audio locales were resolved.
 * @param message Descriptive failure details including the faulty episode identification and context.
 * @param cause Optional underlying cause of the failure.
 * @see [IllegalStateException]
 */
class MissingAudioLocaleException(
    val episodeId: String? = null,
    val episodeName: String? = null,
    val languages: List<String> = emptyList(),
    val country: Country? = null,
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause) {

    private companion object {
        /**
         * Builds the failure message of an exception from the faulty episode context.
         *
         * @param episodeId identifier of the faulty episode.
         * @param episodeName optional name of the episode, dropped from the message when blank.
         * @param languages raw audio languages provided by the platform.
         * @param country target country for which audio locale resolution failed.
         * @return the descriptive failure message.
         * @see [MissingAudioLocaleException]
         */
        private fun buildErrorMessage(
            episodeId: String,
            episodeName: String?,
            languages: List<String>,
            country: Country
        ): String {
            val titlePart = episodeName?.takeIf(String::isNotBlank)
                ?.let { " (name=\"$it\")" }
                .orEmpty()

            return "No audio locale could be resolved for episode id=$episodeId$titlePart " +
                "with languages=$languages in country=$country"
        }
    }

    /**
     * Constructs a [MissingAudioLocaleException] with structured episode context.
     *
     * @param episodeId Identifier of the faulty episode.
     * @param episodeName Optional title or name of the episode.
     * @param languages Raw audio languages provided by the platform.
     * @param country Target country for which audio locale resolution failed.
     */
    constructor(
        episodeId: String,
        episodeName: String?,
        languages: List<String>,
        country: Country
    ) : this(
        episodeId = episodeId,
        episodeName = episodeName,
        languages = languages,
        country = country,
        message = buildErrorMessage(episodeId, episodeName, languages, country)
    )
}
