package fr.shikkanime.models

/**
 * Returns `true` when the receiver is neither `null` nor blank.
 *
 * Inverse of [isNullOrBlank], for conditions that read better without the negation. Shared by
 * every module through `:models`, the only dependency they all carry.
 *
 * @return `true` when the receiver carries at least one non-whitespace character.
 * @see [kotlin.text.isNullOrBlank]
 */
fun String?.isNotNullOrBlank(): Boolean = !isNullOrBlank()
