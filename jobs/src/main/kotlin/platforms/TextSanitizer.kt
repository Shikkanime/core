package fr.shikkanime.jobs.platforms

private const val LINE_BREAK = "\n"
private const val NO_BREAK_SPACE = '\u00A0'

/**
 * Sanitizes an editorial text harvested from a streaming platform.
 *
 * A blank text carries no information and becomes `null`. The remaining text is flattened: its
 * line breaks are dropped and its non-breaking spaces are replaced by regular ones, so a stored
 * localization holds a single readable line.
 *
 * @return the sanitized text, or `null` when the receiver is blank.
 * @see [PlatformAnimeLocalization]
 * @see [PlatformEpisodeLocalization]
 */
internal fun String?.sanitizeText(): String? =
    this?.takeIf(String::isNotBlank)
        ?.replace(LINE_BREAK, "")
        ?.replace(NO_BREAK_SPACE, ' ')
