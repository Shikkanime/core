package fr.shikkanime.api.images

/**
 * Raised when a picture cannot be read from its source or encoded to WebP.
 *
 * It marks a failure the caller cannot fix by changing the request: an unreachable host, a payload
 * that is not an image, or an encoder that refuses the picture.
 *
 * @param message description of the failure.
 * @param cause failure that triggered it, when there is one.
 */
class WebpConversionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
