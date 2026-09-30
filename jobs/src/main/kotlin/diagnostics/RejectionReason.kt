package fr.shikkanime.jobs.diagnostics

/**
 * Where an item was lost in the ingestion pipeline.
 *
 * The stage answers "where do I go fix this", not "what exactly happened" — the wording of a
 * rule changes, the place that owns the fix does not.
 */
enum class RejectionStage {
    FETCH,
    PLATFORM,
    TIME,
    DEDUP,
    IDENTITY,
    PERSIST,
    DELIVERY
}

/**
 * Why an item was not ingested.
 *
 * Declared as an enum rather than a string so a typo cannot compile, and stable over time so a
 * stored reason keeps its meaning when the taxonomy grows.
 */
enum class RejectionReason(val stage: RejectionStage) {
    FETCH_FAILED(RejectionStage.FETCH),
    PROMOTIONAL_CONTENT(RejectionStage.PLATFORM),
    TRAILER_OR_OPENING(RejectionStage.PLATFORM),
    NOT_AN_ANIMATION(RejectionStage.PLATFORM),
    EMPTY_GENRES(RejectionStage.PLATFORM),
    NOT_RELEASED_YET(RejectionStage.TIME),
    ALREADY_KNOWN(RejectionStage.DEDUP),
    UNRESOLVED_IDENTITY(RejectionStage.IDENTITY),
    PERSIST_FAILED(RejectionStage.PERSIST),
    DELIVERY_FAILED(RejectionStage.DELIVERY)
}
