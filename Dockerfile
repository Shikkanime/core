ARG JAVA_VERSION=21
ARG PLAYWRIGHT_VERSION=1.53.0

FROM amazoncorretto:${JAVA_VERSION} AS java

FROM node:24-bookworm-slim
ARG PLAYWRIGHT_VERSION
RUN npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps firefox
ARG JAVA_VERSION
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto \
    TZ=Europe/Paris \
    DEBIAN_FRONTEND=noninteractive \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

# Install necessary packages and set timezone
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl fonts-dejavu tzdata webp && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* && \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    dpkg-reconfigure --frontend noninteractive tzdata

COPY build/install/core hibernate.cfg.xml /app/

WORKDIR /app
EXPOSE 37100
CMD ["./bin/core", "--enable-jobs"]