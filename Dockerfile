ARG JAVA_VERSION=21

FROM amazoncorretto:${JAVA_VERSION} AS java
FROM mcr.microsoft.com/playwright:v1.45.2-jammy

ARG JAVA_VERSION
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto \
    TZ=Europe/Paris \
    DEBIAN_FRONTEND=noninteractive

# Install necessary packages and set timezone
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates tzdata fonts-dejavu \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    && ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && echo "$TZ" > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata

COPY build/install/core hibernate.cfg.xml /app/

WORKDIR /app
EXPOSE 37100
CMD ["./bin/core"]