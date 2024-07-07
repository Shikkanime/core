ARG JAVA_VERSION=21

FROM amazoncorretto:${JAVA_VERSION} AS build
FROM mcr.microsoft.com/playwright:v1.45.1-jammy
ARG JAVA_VERSION
COPY --from=build /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto \
    TZ=Europe/Paris

# Install necessary packages and set timezone
RUN set -eux \
    && apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl ca-certificates fontconfig tzdata libopencv-dev fonts-dejavu \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    && ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && echo "$TZ" > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata

# Copy app from build stage
COPY build/install/core /app
COPY hibernate.cfg.xml /app/hibernate.cfg.xml

WORKDIR /app
EXPOSE 37100
CMD ["./bin/core"]