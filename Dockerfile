# syntax=docker/dockerfile:1.7

# One build for every module: the application images differ only by the distribution they copy, so
# a change to one module rebuilds only the cheap final stages of the others.
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /src

COPY gradlew gradle.properties settings.gradle.kts ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY models ./models
COPY database ./database
COPY api ./api
COPY site ./site
COPY admin ./admin
COPY jobs ./jobs
RUN chmod +x gradlew

# The build resolves the private framework snapshot from GitHub Packages.
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
RUN --mount=type=cache,target=/root/.gradle \
    GITHUB_ACTOR="${GITHUB_ACTOR}" GITHUB_TOKEN="${GITHUB_TOKEN}" \
    ./gradlew --no-daemon installDist

# Runtime image: a jlink-trimmed JRE carrying only the JDK modules the applications reach, instead
# of a full distribution.
FROM eclipse-temurin:25-jdk-alpine AS jre
RUN jlink \
        --add-modules java.base,java.desktop,java.instrument,java.logging,java.management,\
java.naming,java.net.http,java.security.jgss,java.sql,java.xml,jdk.crypto.ec,jdk.management,\
jdk.unsupported,jdk.zipfs \
        --strip-debug --no-header-files --no-man-pages --compress=zip-6 \
        --output /opt/jre

# Shared runtime of every module image: the trimmed JRE, the schema provisioner, and an entrypoint
# that provisions the schema before starting the module.
FROM alpine:3.22 AS runtime

ENV JAVA_HOME=/opt/java \
    PATH=/opt/java/bin:$PATH \
    LANG=C.UTF-8

COPY --from=jre /opt/jre /opt/java

WORKDIR /app

COPY --from=builder /src/database/build/install/database ./database
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN apk add --no-cache libwebp \
    && addgroup -S app \
    && adduser -S -G app app \
    && chmod +x /usr/local/bin/entrypoint.sh \
    && mkdir -p /app/data \
    && chown -R app:app /app
USER app

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

FROM runtime AS api
ENV MODULE_NAME=api
COPY --from=builder --chown=app:app /src/api/build/install/api ./api
RUN rm -f ./api/bin/*.bat

FROM runtime AS site
ENV MODULE_NAME=site
COPY --from=builder --chown=app:app /src/site/build/install/site ./site
RUN rm -f ./site/bin/*.bat

FROM runtime AS admin
ENV MODULE_NAME=admin
COPY --from=builder --chown=app:app /src/admin/build/install/admin ./admin
RUN rm -f ./admin/bin/*.bat

FROM runtime AS jobs
ENV MODULE_NAME=jobs
COPY --from=builder --chown=app:app /src/jobs/build/install/jobs ./jobs
RUN rm -f ./jobs/bin/*.bat

# The schema provisioner alone, for a deployment that migrates the database before rolling out an
# application image.
FROM runtime AS database
ENV MODULE_NAME=database
