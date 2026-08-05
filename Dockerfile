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

ARG MODULE=api
RUN ./gradlew :${MODULE}:installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
ARG MODULE=api
ENV MODULE_NAME=${MODULE}
COPY --from=builder /app/${MODULE}/build/install/${MODULE} /app/

CMD ["sh", "-c", "bin/${MODULE_NAME}"]
