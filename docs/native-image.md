# Native images with GraalVM

Goal: ship a **JVM** distribution and a **native** executable for every application module
(`api`, `admin`, `site`, `jobs`, `database`), with the same observable behaviour.

The branch is `feat/graalvm-native`; the JVM distributions stay the default build, the native
executables are opt-in (`./gradlew :api:nativeCompile`).

## Toolchain

- GraalVM Community CE **25.4.4.1.1** (JDK 25), installed under `~/.local/graalvm`.
- Native Build Tools plugin **1.1.18** (`org.graalvm.buildtools.native`), declared in
  `gradle/libs.versions.toml`.
- The project already targets JDK 25 (`buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`).

```bash
export JAVA_HOME=~/.local/graalvm
export GRAALVM_HOME=~/.local/graalvm
./gradlew :api:nativeCompile
```

## Experiments

### What did not work

- **2 GiB builder heap on a 3.7 GiB / 2 vCPU host**: the analysis of the `api` image ends with
  `java.lang.OutOfMemoryError: Java heap space` after ~18 minutes. The `api` classpath carries
  Liquibase, the Swagger codegen (and Rhino), Jackson, Guava and Ktor; the analysis needs more
  than 2 GiB. Do not retry with `-Xmx2g`.
- **A native image without the reachability metadata fails at runtime**, not at build time:
  `UserTable.<clinit>` throws `ClassNotFoundException: org.bouncycastle.crypto.generators.
  Argon2BytesGenerator` because Exposed resolves the Argon2 generator with `Class.forName`. The
  tracing agent that produced `reachability-metadata.json` is mandatory, not optional.
- **A 300 s compilation alarm on the `admin` image**: with `-O1` and a 3 GiB heap, the compiler
  spent 344 s in GC for one method and bailed out with `Compilation exceeded 300.000 seconds`.
  The module now builds at `-O0`, which stays under the budget. Do not add
  `-H:+EnableFallbackCompilation`: the `-O0` builds do not need it, and an earlier CI setup that
  asked for "the latest JDK 25" resolved to GraalVM 25.0.2, which rejects the flag outright. The
  workflow now pins GraalVM 25.4.4.1.1, the same build as the development host.
- **`x86-64-v3` tuning**: the development host reports `The host machine does not support all
  features of 'x86-64-v3'. Falling back to '-march=compatibility'`. A runner that supports v3
  produces an image that refuses to start on an older host with
  `CPU ISA level is lower than required`. `-march=compatibility` alone is not enough (it only drives
  the Java code generation) and neither is `-H:CCompilerOption=-march=x86-64` when the runner's own
  `crt1.o`/glibc is built for v3. Ubuntu 26.04 (`ubuntu-latest` at the time of writing) does exactly
  that; the native matrix is pinned to `ubuntu-24.04`, and `readelf -n` on the artifact must report
  `x86 ISA needed: x86-64-baseline` only.
- **`--enable-url-protocols` / `--no-fallback`**: GraalVM 25 reports both as deprecated (the URL
  protocols come from the reachability metadata, the fallback threshold no longer exists). They
  are kept for older GraalVM compatibility, but they do not change the result on 25.
- **Shared library output**: without `sharedLibrary.set(false)` the main binary can be emitted as
  `shikkanime-api.so`; an application needs an executable, so every binary sets it explicitly.

### What works

- The Native Build Tools plugin resolves the reachability metadata repository (1.0.21) and applies
  the OkHttp feature (`okhttp3.internal.graal.OkHttpFeature`).
- `-Xmx3g` and the 4 GiB swap of the host let the `api` analysis complete; `-Xmx2g` does not.
  A CI runner with 16 GiB uses the same 3 GiB heap without swapping.
- The `api` image is built at `-O1`; on the 2 vCPU host the `admin`, `site`, `jobs` and `database`
  binaries use `-O0`, because their optimization phase exceeds the compiler's per-method budget.
  The CI matrix builds every module on `ubuntu-latest`, where the default optimization fits.

## Verification

Two applications are started on distinct ports and fed the same requests by
`native-compare.py`: status, `Content-Type` and body (JSON normalised) must be identical.
The `jobs` and `database` applications are compared on their exit code and on the database they
write (same schema, same row counts).

See `docs/native-image-results.md` for the exact runs.
