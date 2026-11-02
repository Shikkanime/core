# Images and Artwork

How the artwork of an entity gets from a streaming platform to a browser, and why.

## What the old process cost

The previous design stored every artwork on disk, encoded by an external binary:

- `UpdateAttachmentJob` re-encoded a batch of attachments on a schedule.
- `Application.start` ran `encodeAllActiveWithUrlAndWithoutFile()` before serving, which downloaded
  every artwork whose file was missing: with 30 000 images, a fresh deployment was unusable for
  minutes and the volume filled up with pictures nobody would ever see.
- `FileManager.convertToWebP` shelled out to `cwebp`, so the runtime image had to carry the `webp`
  tools, and any image operation paid a process spawn.
- The media composition downloaded an artwork again even when its file was on disk, because ImageIO
  cannot read the WebP it had written.

## Decision

**The artwork is materialized on demand, and the encoded payload lives in the database.**

1. A row of the `image` table only carries the source URL of the platform, the type of the artwork,
   and the metadata of the last materialization (`version`, `fetched_at`, source validators,
   dimensions, failures).
2. The first request for an image fetches its source, decodes it, resizes it to the canonical
   dimensions of its `ImageType`, encodes it to WebP, and stores the bytes in `image_payload`, in its
   own table so a catalogue read never loads them.
3. The served URL embeds the payload version: `/v1/images/{entityId}/{type}/{version}.webp`, served
   by `ImageController`, which delegates the workflow to `FindImageUseCase`. The response is
   `Cache-Control: immutable` for a year, carries the version as its entity tag, and answers `304`
   when the client revalidates with that tag (`@HttpHeader`).
4. An artwork older than the refresh delay is revalidated **in the background**, after the response,
   with a conditional request. A platform answering `304` costs a round trip and no re-encode; a
   platform that changed the bytes publishes a new version, hence a new URL.

Nothing is downloaded ahead of a request, nothing runs at startup, and the only copy kept is the
encoded one. Databases travel with their artwork, which is what a deployment to another machine needs.

## Encoding without a command line

The JDK carries no WebP codec: ImageIO exposes BMP, GIF, JPEG, PNG, TIFF and WBMP only. The encoder
therefore calls `libwebp` directly through the Foreign Function & Memory API, in both directions:

- `WebPEncodeRGBA` / `WebPEncodeLosslessRGBA` to write, `WebPDecodeRGBA` to read.
- The library comes from the distribution (`libwebp` on alpine, `libwebp-dev` in the CI image), not
  from a jar with bundled native code: those are built against glibc and do not load on musl.
- Reading through the same library is what fixes the media composition: an artwork already stored is
  decoded from the database instead of being fetched again.

Decoding and resizing produce a `BufferedImage`, so the trimmed runtime of the images carries
`java.desktop`. It is the one module the pipeline adds, and removing it later means resizing on the
raw RGBA buffer instead of through `Graphics2D`.

## Quality and transparency

| type | encoding | why |
| --- | --- | --- |
| `THUMBNAIL`, `BANNER` | lossy, quality 80, 85 when the picture carries alpha | the historical 75-85 % range |
| `TITLE` (no height) | lossless | flat artwork: lossy frays the transparent edges of a logo |

The alpha plane of a lossy WebP is stored without loss, so transparency stays exact whatever the
quality; only the colour of the fully transparent pixels is approximated. Scaling happens in a
premultiplied buffer, because scaling straight alpha drags the colour of transparent pixels into the
edges and leaves dark fringes. Both properties are covered by `ImageCodecTest`.

## Freshness

Two independent triggers, none of them a job:

- **The harvest publishes a new URL** for an entity and type: the upsert drops the stored payload,
  and the next read materializes the new artwork.
- **The platform changes the bytes at the same URL**: the first request after the refresh delay
  serves the stored payload, then revalidates it in the background. `https://cdn.…/poster.jpg` keeps
  its address while its content changes, which is exactly the case the schedule covered before.

## Consequences

- The database grows with the artwork that is actually served, and by the size of a WebP payload at
  full quality; a deployment without traffic pays nothing.
- The first request for an artwork is slower (download, decode, encode) and every later one is a
  primary-key read. A burst of concurrent requests materializes once: the route serializes them per
  artwork.
- A platform outage makes new artwork unavailable (`503`) without affecting the stored ones.
- The payload bytes are only reachable through the image route; entity DTOs carry the versioned URL.

## Not covered yet

- An allowlist of source hosts, to close the SSRF surface of a URL coming from a platform payload.
- A memory cache in front of the payload read, once a profile shows the primary-key read matters.
- Prefetching the first page of the site, if the first-hit latency turns out to be visible.
