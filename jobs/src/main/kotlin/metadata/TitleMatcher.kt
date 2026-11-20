package fr.shikkanime.jobs.metadata

import fr.shikkanime.models.comparableTitle
import kotlin.math.abs

/**
 * Title similarity helpers shared by the metadata clients and the linker.
 *
 * Every comparison runs on [comparableTitle]: normalized, diacritic-free and case-folded, so a
 * French stored title and a romaji external title are compared on their common shape.
 */
object TitleMatcher {
    /**
     * Scores how well [candidate] matches [query], from 0.0 (no relation) to 1.0 (same title).
     *
     * On top of the cheap lexical score, pairs plausible enough are refined by an edit-distance
     * similarity, so spelling variants ("Sergent"/"Sergeant") still match.
     *
     * @param candidate external title to score.
     * @param query stored title to match against.
     * @return the similarity score.
     */
    fun score(candidate: String, query: String): Double {
        val fast = scoreFast(candidate, query)
        if (fast >= LEVENSHTEIN_CEILING) return fast

        val a = comparableTitle(candidate)
        val b = comparableTitle(query)
        if (a.isBlank() || b.isBlank() || abs(a.length - b.length) > maxOf(a.length, b.length) / 2)
            return fast
        return maxOf(fast, levenshteinSimilarity(a, b))
    }

    /**
     * Cheap lexical score without the edit-distance refinement.
     *
     * Used to prefilter mass scans (the AniDB title index holds hundreds of thousands of
     * entries): only pairs surviving it are refined by [score].
     *
     * @param candidate external title to score.
     * @param query stored title to match against.
     * @return the lexical similarity score.
     */
    fun scoreFast(candidate: String, query: String): Double {
        val a = comparableTitle(candidate)
        val b = comparableTitle(query)
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0

        // A single-word title contained in a longer one ("Keroro" in "Sergent Keroro") is too
        // weak to score high: different franchise entries share such words constantly.
        if ((a.contains(b) || b.contains(a)) &&
            minOf(wordCount(a), wordCount(b)) >= MIN_CONTAINMENT_WORDS
        ) {
            return CONTAINMENT_SCORE
        }

        val aWords = a.split(' ').toSet()
        val bWords = b.split(' ').toSet()
        val union = aWords + bWords
        if (union.isEmpty()) return 0.0
        return aWords.intersect(bWords).size.toDouble() / union.size
    }

    /**
     * Best similarity of [titles] against [query].
     *
     * @param titles external titles to score.
     * @param query stored title to match against.
     * @return the best score, 0.0 when [titles] is empty.
     */
    fun bestScore(titles: Set<String>, query: String): Double =
        titles.maxOfOrNull { score(it, query) } ?: 0.0

    /**
     * Best lexical similarity of [titles] against [query], without edit distance.
     *
     * @param titles external titles to score.
     * @param query stored title to match against.
     * @return the best fast score, 0.0 when [titles] is empty.
     */
    fun bestScoreFast(titles: Set<String>, query: String): Double =
        titles.maxOfOrNull { scoreFast(it, query) } ?: 0.0

    /**
     * Counts the words of a title.
     *
     * @param title title to inspect, expected to be normalized already.
     * @return the number of non-blank words.
     */
    private fun wordCount(title: String): Int =
        title.split(' ').count { it.isNotBlank() }

    /**
     * Computes the edit-distance similarity of two titles.
     *
     * The score is `1 - distance / maxLength`, so two identical titles score `1.0` and two
     * unrelated ones approach `0.0`.
     *
     * @param a normalized first title.
     * @param b normalized second title.
     * @return the similarity score between `0.0` and `1.0`.
     */
    private fun levenshteinSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1])
                    0
                else
                    1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return 1.0 - previous[b.length].toDouble() / maxOf(a.length, b.length)
    }

    private const val CONTAINMENT_SCORE = 0.8
    private const val MIN_CONTAINMENT_WORDS = 2
    private const val LEVENSHTEIN_CEILING = 0.99
}
