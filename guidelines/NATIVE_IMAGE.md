# GraalVM Native Image Guide

Each application module (`:api`, `:site`, `:admin`, `:jobs`) and the `:database` provisioner can be
compiled ahead of time to a standalone executable with GraalVM Native Image. The JVM distribution
stays the default build; the native image is an additional artifact for deployments that want a small
runtime image and a fast start.

## Toolchain

- GraalVM **25** (the same major version as the JVM toolchain), installed outside the repository.
- `JAVA_HOME` and `GRAALVM_HOME` point at the GraalVM installation when the native task runs.
- The Gradle build resolves the private framework snapshot from GitHub Packages, so `GITHUB_ACTOR`
  and `GITHUB_TOKEN` must be exported exactly as for a JVM build.

## Build an application

```bash
export JAVA_HOME=~/.local/graalvm GRAALVM_HOME=~/.local/graalvm
export PATH=$JAVA_HOME/bin:$PATH
export GITHUB_ACTOR=... GITHUB_TOKEN=...
./gradlew --no-daemon :api:nativeCompile
```

The executable lands in `<module>/build/native/nativeCompile/<name>`, named after the application
(`shikkanime-api`, `shikkanime-admin`, …). The `:database` provisioner produces `shikkanime-database`.

The build args live in each application's `build.gradle.kts`:

- `--no-fallback` fails the build instead of producing an image that silently needs a JVM.
- `-O2` optimises for production; the constrained development host uses `-O0` to keep the compile
  under the compiler's per-method budget.
- `-H:+EnableFallbackCompilation` keeps an edge method the analyser cannot prove reachable from
  failing the whole build. It prints the method, and it stays off when `--no-fallback` already covers
  the case.
- `-H:+ReportExceptionStackTraces` turns a startup failure into a stack trace instead of a bare exit.
- `-Dorg.gradle.java.home` must not be used; the task already runs on the GraalVM JDK.

The native build is memory hungry: three gigabytes of heap (`-Xmx3g`) and two vCPU compile the `api`
image in roughly ten minutes. A 2 GiB heap fails in the inlining phase with an
`OutOfMemoryError`.

## Reflection, resources and services

GraalVM needs to know every reflective access and resource read at build time. The agent captures
them once per application:

```bash
java -agentlib:native-image-agent=config-output-dir=config/<app> -cp '<classpath>' <MainClass>
```

Drive the application through its real request paths (health, a controller read, a controller write,
the OpenAPI document) before stopping it. The agent writes `reflect-config.json`,
`resource-config.json`, `proxy-config.json`, `jni-config.json` and `serialization-config.json` into the
output directory.

Commit those files under `config/<app>/` and keep the merged result in the application's resources:

```
api/src/main/resources/META-INF/native-image/fr.shikkanime.api/
```

The resources are consumed by `nativeCompile` and by the tests. Regenerate them when a controller, a
DTO or a route changes; a stale configuration fails at startup with a `ClassNotFoundException`, not at
build time. The merge of several runs is incremental: use `config-merge-dir` for later captures so
the previous entries survive.

## Verify an image

A native image is verified by starting it and calling the same endpoints as the JVM build, not by its
exit code:

1. Start the JVM build and the native build on separate ports against the same database.
2. Call `/v1/health`, a read controller, a write controller and `/openapi.json` on both.
3. Compare the status codes and the bodies; a reflective miss shows as a 500 or a startup crash.
4. Start each application with an empty database to prove the schema and the Liquibase changelogs are
   registered in the image.

The OpenAPI document is the most reliable smoke test: it walks every controller, annotation and DTO,
so a missing reflection entry surfaces there before the endpoint is called.

## Docker

The `Dockerfile` still ships the JVM distribution through `installDist`; the native image is not copied
yet. A native image target needs a GraalVM base image, the generated configuration and a copy of the
`nativeCompile` output, and replaces the jlink runtime stage. Keep the JVM image as the default until
the native one is measured in the deployment environment.

## Limits

- Reflection, resources and proxies are closed-world: anything loaded by name must be in the committed
  configuration.
- `kotlin-reflect` is not native friendly; the framework's annotations are read at build time, so keep
  the reflective surface to the documented controller binding.
- Startup time and memory are the gain; peak throughput stays comparable to the JVM, and the image
  build is slower than the JVM build.
