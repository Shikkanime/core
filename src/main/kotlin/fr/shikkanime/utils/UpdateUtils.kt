package fr.shikkanime.utils

import java.util.logging.Logger

inline fun <T> hasChangedAndValid(candidate: T?, current: T, isValid: (T) -> Boolean): Boolean =
    candidate != null && candidate != current && isValid(candidate)

inline fun <T> updateIfValidAndChanged(
    logger: Logger,
    identifier: String,
    fieldName: String,
    candidate: T?,
    current: T,
    isValid: (T) -> Boolean,
    apply: (T) -> Unit
): Boolean {
    if (!hasChangedAndValid(candidate, current, isValid)) return false
    apply(candidate!!)
    logger.info("Updating $fieldName for $identifier to $candidate")
    return true
}