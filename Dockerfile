ARG JAVA_VERSION=21
ARG PLAYWRIGHT_VERSION=1.46.1
ARG BUILD_VERSION=full

FROM amazoncorretto:${JAVA_VERSION} AS java

# Base image for full version
FROM mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}-jammy AS full

# Base image for slim version
FROM node:22-bookworm-slim AS slim
ARG PLAYWRIGHT_VERSION
RUN npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps firefox

# Select the base image based on the build argument
FROM ${BUILD_VERSION}

ARG JAVA_VERSION
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto \
    TZ=Europe/Paris \
    DEBIAN_FRONTEND=noninteractive \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Install necessary packages and set timezone
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates tzdata libopencv-dev fonts-dejavu curl \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    && ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && echo "$TZ" > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata

COPY build/install/core hibernate.cfg.xml /app/

WORKDIR /app
EXPOSE 37100
CMD ["./bin/core"]