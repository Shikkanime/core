FROM amazoncorretto:21 AS build
FROM mcr.microsoft.com/playwright:v1.45.1-jammy
COPY --from=build /usr/lib/jvm/java-21-amazon-corretto /usr/lib/jvm/java-21-amazon-corretto

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto \
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