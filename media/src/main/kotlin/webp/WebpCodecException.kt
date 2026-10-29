package fr.shikkanime.media.webp

/**
 * Raised when the WebP codec cannot do the work it was asked for.
 *
 * The message names the missing library and the command that installs it, rather than letting a
 * native failure surface as an obscure link error.
 *
 * @param message description of the failure.
 * @param cause native or decoding failure that triggered it, when there is one.
 */
class WebpCodecException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
