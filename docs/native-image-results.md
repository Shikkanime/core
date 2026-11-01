# Native image results

Host for the local runs: Linux x86-64, 2 vCPU, 3.7 GiB RAM, 4 GiB swap. GraalVM Community CE
25.4.4.1.1, Native Build Tools 1.1.18.

## `api` — built and verified

- `./gradlew :api:nativeCompile` with `-Xmx3g`, `-O1`: success in 10m39s, 74 MB executable,
  plus the AWT shared libraries next to it.
- JVM and native started on ports 18080 and 18081 against the same H2 database (schema provisioned
  by `:database`, five animes seeded by `:jobs`).

| Request | JVM | native | verdict |
| --- | --- | --- | --- |
| `GET /health` | `200`, 2 B | `200`, 2 B | identical |
| `GET /v1/animes` | `200`, 4831 B JSON | `200`, 4831 B JSON | identical |
| `GET /v1/animes/{id}` | `200`, 510 B JSON | `200`, 510 B JSON | identical |
| `GET /v1/animes/{unknown}` | `404`, 92 B JSON | `404`, 92 B JSON | identical |
| `GET /swagger` | `200`, 996 B HTML | `200`, 996 B HTML | identical |

`native-compare.py` reported `0 difference(s)`.

Without the tracing-agent configuration the native binary starts and then dies on the first request
with `ClassNotFoundException: org.bouncycastle.crypto.generators.Argon2BytesGenerator` in
`UserTable.<clinit>`. The committed `reachability-metadata.json` is what makes the comparison pass.

## `admin`, `site`, `jobs`, `database`

The local 2 vCPU / 3.7 GiB host cannot finish their optimization phase:

- `admin` at `-O1`: `Compilation exceeded 300.000 seconds` after 14m50s (344 s of GC for one method).
  The fallback compilation at `-O0` was killed after 18 minutes of swapping.
- `site` at `-O0`: analysis completed in 178 s, then the compile phase was killed after ten minutes
  with 3.0 GiB of swap in use.

The `native-images` CI job builds the five modules in parallel on `ubuntu-latest` (16 GiB) and
uploads one artifact per module; the artifacts are the reference binaries for these applications.
