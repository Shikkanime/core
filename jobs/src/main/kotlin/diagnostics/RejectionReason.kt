package fr.shikkanime.jobs.diagnostics

/**
 * Where an item was lost in the ingestion pipeline.
 *
 * The stage answers "where do I go fix this", not "what exactly happened": the wording of a
 * rule changes, the place that owns the fix does not.
 *
 * @property FETCH The platform call itself failed.
 * @property PLATFORM The platform's own content rules dropped the item.
 * @property TIME The item is not released yet.
 * @property DEDUP The item is already known to the database.
 * @property IDENTITY The item could not be resolved to a canonical anime.
 * @property PERSIST The resolved item could not be written.
 * @property DELIVERY The item was written but could not be announced.
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
 *
 * @property stage The pipeline stage the reason belongs to.
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
