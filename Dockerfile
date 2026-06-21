ARG JAVA_VERSION=25
ARG PLAYWRIGHT_VERSION=1.60.0

FROM amazoncorretto:${JAVA_VERSION} AS java
FROM node:25-bookworm-slim
ARG JAVA_VERSION
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto/lib/security/cacerts /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto/lib/security/cacerts

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto \
    TZ=Europe/Paris \
    DEBIAN_FRONTEND=noninteractive \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
    PLAYWRIGHT_BROWSERS_PATH=/opt/playwright

ARG PLAYWRIGHT_VERSION
# Install necessary packages and set timezone
RUN mkdir -p ${PLAYWRIGHT_BROWSERS_PATH} && \
    npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps chromium && \
    rm -rf ~/.npm && \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl fonts-dejavu fontconfig libfreetype6 tzdata webp gosu xvfb && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* && \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    groupadd -r -g 1001 appuser && useradd -r -u 1001 -g appuser -m -d /home/appuser appuser && \
    chown -R appuser:appuser ${PLAYWRIGHT_BROWSERS_PATH}

COPY --chown=appuser:appuser build/install/core hibernate.cfg.xml /app/
COPY --chmod=755 entrypoint.sh /usr/local/bin/entrypoint.sh

WORKDIR /app
EXPOSE 37100
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["./bin/core", "--enable-jobs"]
