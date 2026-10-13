package fr.shikkanime.jobs.platforms.exceptions

import fr.shikkanime.models.Country

/**
 * Thrown when an episode release cannot be mapped due to missing or unsupported audio locales.
 *
 * @property episodeId Identifier of the faulty episode, if available.
 * @property episodeName Name or title of the faulty episode, if available.
 * @property languages Raw audio language tags provided by the platform.
 * @property country Target country for which audio locale resolution failed, if available.
 * @param message Descriptive failure details including the faulty episode identification and context.
 * @param cause Optional underlying cause of the failure.
 */
class MissingAudioLocaleException(
    val episodeId: String? = null,
    val episodeName: String? = null,
    val languages: List<String> = emptyList(),
    val country: Country? = null,
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause) {

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

    private companion object {
        private fun buildErrorMessage(
            episodeId: String,
            episodeName: String?,
            languages: List<String>,
            country: Country
        ): String {
            val titlePart = episodeName?.takeIf { it.isNotBlank() }
                ?.let { " (name=\"$it\")" }
                .orEmpty()

            return "No audio locale could be resolved for episode id=$episodeId$titlePart " +
                "with languages=$languages in country=$country"
        }
    }
}
