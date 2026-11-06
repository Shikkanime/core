# Cache Guide

The public API serves its catalog reads through `fr.shikkanime.framework:cache`: an in-process L1
LRU, a Valkey L2 shared by every instance, CBOR payloads and bucket versioning. A writer invalidates
a bucket by incrementing its version; the entries written under the previous version become
unreachable and expire with their TTL.

## Placement

- **Cache at the use case, never in the controller.** The controller stays a transport adapter; the
  use case owns the key, the TTL and the loader. A controller that calls the cache couples HTTP to
  the cache and cannot be reused by a job.
- **The loader returns the API DTO.** A hit then skips the SQL read *and* the aggregate mapping, and
  the cached value is exactly the response the endpoint serves.
- **One bucket per aggregate**, declared in `fr.shikkanime.database.cache.CacheBuckets`. The readers
  and the writers import the same constant; a bucket name written twice drifts silently.
- **Key only bounded dimensions.** A catalog page is keyed by `page` and `size`, a detail by its
  identifier. Never key a cache on a free-text search: the key space is unbounded and the hit rate
  collapses.
- **A TTL is a safety net, not the invalidation mechanism.** The catalog uses five minutes, so an
  out-of-band write (a SQL script, a deployment) cannot serve stale data forever.

## Invalidation

- **Invalidate after the transaction committed.** `@Transactional` service methods return after their
  commit, so a writer invalidates once the save call returned. Invalidating before the commit lets a
  reader reload the pre-commit state under the new version, and that stale entry is then served until
  the TTL.
- **Invalidate once per write workflow**, not per row: the harvest writes every anime and episode,
  then invalidates the two buckets at the end.
- **Bump the schema suffix** of `CacheBuckets` (`anime:v1` → `anime:v2`) when a cached DTO changes
  shape, so a rolling deployment never decodes an entry written by the previous build.
- **A failing invalidation is logged, not propagated.** The write is already committed; failing the
  job does not repair the cache, and the TTL bounds the staleness.

## What not to cache

- A personalized or authenticated response, unless the user or role is part of the key.
- An arbitrary search or pagination combination: cache the hot, bounded reads only.
- A value that changes on every request, or one whose loader has side effects.
