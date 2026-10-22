FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY buildSrc buildSrc
COPY gradle.properties .
COPY settings.gradle.kts .
COPY models models
COPY database database
COPY jobs jobs
COPY api api
COPY admin admin
COPY site site

# The build resolves the private framework snapshot from GitHub Packages, which needs a token.
ARG MODULE=api
ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
RUN GITHUB_ACTOR="${GITHUB_ACTOR}" GITHUB_TOKEN="${GITHUB_TOKEN}" ./gradlew :${MODULE}:installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
ARG MODULE=api
ENV MODULE_NAME=${MODULE}
COPY --from=builder /app/${MODULE}/build/install/${MODULE} /app/

CMD ["sh", "-c", "bin/${MODULE_NAME}"]
