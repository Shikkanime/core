package fr.shikkanime.models

import java.text.Normalizer
import java.util.Locale.ROOT

private const val ANIME_STRING = "Anime "
private const val SPACE_STRING = " "
private const val COMMA_STRING = ","

private val romansRegex = "( [-!~].*[-!~](?: |$))|( Saison \\d*)|(?:: )?\\(\\d*\\)| (VI+$)".toRegex()
private val separators = listOf(":", COMMA_STRING, "!", "–", " so ", " - ")
private val secondarySeparators = listOf("?")
private val secondarySeparatorsArray = secondarySeparators.toTypedArray()
private val encasedRegex = "(<.*>|«.*») ?.*".toRegex()
private val specialCharRegex = "[<«»]".toRegex()
private val duplicateSpaceRegex = " +".toRegex()
private val wordSeparatorRegex = "[ \\-']".toRegex()

// Ties between locales are broken in this order: the first locale present here wins the title.
val TITLE_LOCALE_PRIORITY: List<Locale> = listOf(Locale.EN_US, Locale.FR_FR)

/**
 * Tells whether the tail following a separator is an alternate title to drop.
 *
 * A multi-word tail is an alternate title when the head is substantial and the two sides do not
 * share the same word shape; a head made of question-marked parts is never split.
 *
 * @param separator separator the title was split on.
 * @param firstPart head of the title, before the separator.
 * @param lastPart tail of the title, after the separator.
 * @param parts every part of the split title.
 * @return true when only the head must be kept.
 * @see [normalizeTitle]
 */
private fun shouldDropAlternateTitle(
    separator: String,
    firstPart: String,
    lastPart: String,
    parts: List<String>
): Boolean =
    !(secondarySeparators.any { it in firstPart }
        && isAllPartsHaveSameAmountOfWords(firstPart.split(*secondarySeparatorsArray)))
        && lastPart.count { it == ' ' } >= 2
        && (separator != COMMA_STRING
            && firstPart.length > 5
            || firstPart.count { it == ' ' } >= 2)
        && (separator == ":"
            || !isAllPartsHaveSameAmountOfWords(parts))

/**
 * Normalizes a title into the identity title of an anime.
 *
 * The rules are the historical ones (`getShortName` on master): volatile parts (season markers,
 * years in parentheses, encased subtitles) are removed and the subtitle after a separator is
 * dropped when it looks like an alternate title. Two animes of the same franchise keep distinct
 * titles as long as their real titles differ, and cosmetic changes (case, spaces, casing of the
 * separators) never move the result.
 *
 * @param fullTitle title as harvested or stored.
 * @return the normalized title.
 * @see [canonicalTitle]
 */
fun normalizeTitle(fullTitle: String): String {
    var normalizedTitle = removeSpecialCharacters(fullTitle)

    if (ANIME_STRING in fullTitle)
        normalizedTitle = normalizedTitle.replace(ANIME_STRING, "")
            .trim()

    normalizedTitle = romansRegex.replace(normalizedTitle, SPACE_STRING)
        .trim()

    for (separator in separators) {
        if (separator !in normalizedTitle)
            continue

        val parts = normalizedTitle.split(separator)
        val firstPart = parts[0].trim()
        val lastPart = parts.drop(1)
            .joinToString(SPACE_STRING)
            .trim()

        if (shouldDropAlternateTitle(separator, firstPart, lastPart, parts))
            normalizedTitle = firstPart.replace(duplicateSpaceRegex, SPACE_STRING)
                .trim()
    }

    return normalizedTitle.replace(" $COMMA_STRING", COMMA_STRING)
        .replace(duplicateSpaceRegex, SPACE_STRING)
        .trim()
}

/**
 * Normalizes a title and folds its diacritics and case.
 *
 * The comparison key of two normalized titles: two titles equal here are the same identity.
 *
 * @param fullTitle title as harvested or stored.
 * @return the comparable form of [normalizeTitle].
 * @see [canonicalTitle]
 */
fun comparableTitle(fullTitle: String): String =
    Normalizer.normalize(normalizeTitle(fullTitle), Normalizer.Form.NFD)
        .lowercase(ROOT)
        .filter { it.isLetterOrDigit() || it == ' ' }
        .replace(duplicateSpaceRegex, SPACE_STRING)
        .trim()

/**
 * Computes the identity title of an anime from its localized titles.
 *
 * Every localization is normalized first; the title shared by the most localizations is kept, and
 * an equal split is broken by [TITLE_LOCALE_PRIORITY]. The rules are the authority: a title they
 * rewrite is compared on the rewritten value, whatever its locale.
 *
 * @param titles localized titles of one anime, keyed by locale.
 * @return the normalized title to store and to look the anime up with.
 * @see [normalizeTitle]
 */
fun canonicalTitle(titles: Map<Locale, String>): String {
    val winner = titles.entries
        .groupBy({ (_, title) -> comparableTitle(title) }, { it })
        .maxWithOrNull(
            compareBy<Map.Entry<String, List<Map.Entry<Locale, String>>>> { it.value.size }
                .thenBy { entry -> entry.value.minOfOrNull { localeRank(it.key) } ?: Int.MAX_VALUE }
        )
        ?.value
        ?.minByOrNull { localeRank(it.key) }
        ?: return ""

    // The stored value is the rules' output on the winning title, not the comparison key
    return normalizeTitle(winner.value)
}

/**
 * Ranks a locale in [TITLE_LOCALE_PRIORITY].
 *
 * @param locale locale to rank.
 * @return its index, or the size of the priority list when the locale is not listed.
 * @see [canonicalTitle]
 */
private fun localeRank(locale: Locale): Int =
    TITLE_LOCALE_PRIORITY.indexOf(locale)
        .takeIf { it >= 0 }
        ?: TITLE_LOCALE_PRIORITY.size

/**
 * Drops the encased markers around a name.
 *
 * @param name name to clean.
 * @return the name without its encased markers.
 * @see [normalizeTitle]
 */
private fun removeSpecialCharacters(name: String): String {
    if (encasedRegex in name)
        return name.replace(specialCharRegex, "")
            .replace(">", ": ")
            .trim()

    return name
}

/**
 * Tells whether every part holds the same amount of words.
 *
 * @param parts parts to compare.
 * @return true when one word count fits them all.
 * @see [normalizeTitle]
 */
private fun isAllPartsHaveSameAmountOfWords(parts: List<String>): Boolean {
    val first = parts.first()
        .trim()
        .split(wordSeparatorRegex)
        .size

    return parts.all {
        it.trim()
            .split(wordSeparatorRegex)
            .size == first
    }
}
