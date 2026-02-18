ARG JAVA_VERSION=25

FROM amazoncorretto:${JAVA_VERSION} AS java

FROM ubuntu:noble

ARG JAVA_VERSION
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto/lib/security/cacerts /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto/lib/security/cacerts

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto \
    TZ=Europe/Paris \
    DEBIAN_FRONTEND=noninteractive

# Install necessary packages and set timezone
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl fonts-dejavu tzdata webp gosu && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* && \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    groupadd -r -g 1001 appuser && useradd -r -u 1001 -g appuser -m -d /home/appuser appuser

COPY --chown=appuser:appuser build/install/core hibernate.cfg.xml /app/
COPY --chmod=755 entrypoint.sh /usr/local/bin/entrypoint.sh

WORKDIR /app
EXPOSE 37100
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["./bin/core", "--enable-jobs"]
