ARG JAVA_VERSION=25

FROM amazoncorretto:${JAVA_VERSION}-alpine

ENV LANG=C.UTF-8 \
    TZ=Europe/Paris

# Install necessary packages and set timezone
RUN apk add --no-cache ca-certificates curl ttf-dejavu tzdata libwebp-tools gosu shadow && \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    groupadd -r -g 1001 appuser && useradd -r -u 1001 -g appuser -m -d /home/appuser appuser

COPY --chown=appuser:appuser build/install/core hibernate.cfg.xml /app/
COPY --chmod=755 entrypoint.sh /usr/local/bin/entrypoint.sh

WORKDIR /app
EXPOSE 37100
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["./bin/core", "--enable-jobs"]
