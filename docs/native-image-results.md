# Native image results

Host for the local runs: Linux x86-64, 2 vCPU, 3.7 GiB RAM, 4 GiB swap, Debian 13 (glibc 2.41).
Toolchain: GraalVM Community CE **25.4.4.1.1** (JDK 25), Native Build Tools 1.1.18. The CI native
matrix uses the same GraalVM build on `ubuntu-24.04`.

Every application is compared against its JVM distribution on the same prepared database. The probe
script is `native-compare.py` (profiles `api`, `admin`, `site`): it copies a provisioned H2
directory, starts the process on its own port, waits for `/health`, records status, `Content-Type`
and body, then stops it. UUIDs are normalised before the diff.

## `api` — 0 difference

JVM and native started on ports 18080 and 18090 against the same seeded database (five animes).

| Request | JVM | native | verdict |
| --- | --- | --- | --- |
| `GET /health` | `200`, `OK` | `200`, `OK` | identical |
| `GET /v1/animes` | `200`, 5 animes | `200`, 5 animes | identical |
| `GET /v1/animes/{id}` | `200` | `200` | identical |
| `GET /v1/animes/{unknown}` | `404`, error JSON | `404`, error JSON | identical |
| `GET /swagger` | `200`, 996 B HTML | `200`, 996 B HTML | identical |
| `GET /swagger/documentation.yaml` | `200`, OpenAPI | `200`, OpenAPI | identical |

`0 difference(s)` after UUID normalisation. Without the tracing-agent configuration the native
binary starts and then dies on the first request with `ClassNotFoundException:
org.bouncycastle.crypto.generators.Argon2BytesGenerator` in `UserTable.<clinit>`; the committed
`reachability-metadata.json` is what makes the comparison pass.

## `admin` — 0 difference

| Request | JVM | native | verdict |
| --- | --- | --- | --- |
| `GET /health` | `200`, `OK` | `200`, `OK` | identical |
| `GET /swagger` | `200` | `200` | identical |
| `GET /swagger/documentation.yaml` | `200`, OpenAPI | `200`, OpenAPI | identical |
| `POST /v1/users/login` (bad credentials) | `400`, `Invalid username or password` | `400`, same body | identical |

The login route exercises Argon2, the reflective path that needs the committed reachability
metadata.

## `site` — 0 difference

| Request | JVM | native | verdict |
| --- | --- | --- | --- |
| `GET /health` | `200`, `OK` | `200`, `OK` | identical |
| `GET /` | `404`, empty | `404`, empty | identical |
| `GET /v1/animes` | `404`, empty | `404`, empty | identical |
| `GET /swagger` | `404`, empty | `404`, empty | identical |

## `database` — identical schema

Both binaries ran on an empty directory and exited `0`. `org.h2.tools.Script` dumps of the two
databases are byte-identical (85 lines): same tables, columns, foreign keys, unique constraints and
indexes.

## `jobs` — same persistence

Both binaries ran one full harvest on a fresh schema and exited `0` with `Saved 4 animes`. The
resulting tables hold the same counts: 4 animes, 4 localizations, 4 platforms, 8 images. The harvest
itself is live data, so the comparison is on the exit code and the row counts, not on the titles.

## Artifact portability

`readelf -n` on the five CI artifacts reports `x86 ISA needed: x86-64-baseline` only. The native
matrix is pinned to `ubuntu-24.04` and to GraalVM 25.4.4.1.1: `ubuntu-latest` now resolves to Ubuntu
26.04, whose glibc/`crt1.o` is built for x86-64-v3, and a floating `java-version: 25` resolved to
GraalVM 25.0.2, whose runtime objects also require v3. Both produced images that refused to start on
a v2 host with `CPU ISA level is lower than required`.

## CI

The `native-images` job builds the five modules in parallel on `ubuntu-24.04` with
`NATIVE_IMAGE_HEAP=8g` and uploads one artifact per module. The run for `80a03f22` is green on all
five.
