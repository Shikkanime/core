package fr.shikkanime.jobs.diagnostics

import fr.shikkanime.models.Platform
import kotlinx.datetime.LocalDateTime

/**
 * One ingestion pass over a platform: everything the API returned, and what happened to it.
 *
 * [error] is set when the platform call itself failed, so a broken fetch is reported instead
 * of leaving the registry empty.
 */
data class IngestionRun(
    val platform: Platform,
    val verdicts: List<IngestionVerdict>,
    val fetchedAt: LocalDateTime,
    val error: String? = null
) {
    val acceptedCount: Int = verdicts.count { it is IngestionVerdict.Accepted }

    val rejectedCount: Int = verdicts.count { it is IngestionVerdict.Rejected }

    val totalCount: Int = verdicts.size

    /**
     * Rejections grouped by reason, most frequent first, so a spike stands out.
     */
    fun rejectionBreakdown(): List<Pair<RejectionReason, Int>> =
        verdicts.asSequence()
            .filterIsInstance<IngestionVerdict.Rejected>()
            .groupingBy { it.reason }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<RejectionReason, Int>> { it.value }.thenBy { it.key.name })
            .map { it.key to it.value }

    fun rejectionsFor(reason: RejectionReason): List<IngestionVerdict.Rejected> =
        verdicts.filterIsInstance<IngestionVerdict.Rejected>()
            .filter { it.reason == reason }

    /**
     * One-screen report: how many items came back, how many survived, and why the others did not.
     *
     * The accepted items are summarised by count while the rejections are listed one by one, each
     * with its identity: "not an animation" alone does not tell which show was dropped.
     */
    fun describe(): String {
        if (error != null) {
            return "$platform — fetch failed: $error"
        }

        val lines = mutableListOf(
            "$platform — $totalCount item(s), $acceptedCount accepted, $rejectedCount rejected"
        )

        verdicts.filterIsInstance<IngestionVerdict.Rejected>().forEach { rejection ->
            lines += "  ${rejection.reason.name} — ${rejection.item.describe()} [${rejection.evidence}]"
        }

        return lines.joinToString("\n")
    }
}
