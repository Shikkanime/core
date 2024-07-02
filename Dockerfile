FROM mcr.microsoft.com/playwright:v1.45.0-jammy
ARG version=21.0.3.9-1
ENV LANG=C.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
ENV TZ=Europe/Paris

# Install Java and necessary packages
RUN set -eux \
    && apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl ca-certificates gnupg software-properties-common fontconfig java-common tzdata libopencv-dev fonts-dejavu \
    && curl -fL https://apt.corretto.aws/corretto.key | apt-key add - \
    && add-apt-repository 'deb https://apt.corretto.aws stable main' \
    && mkdir -p /usr/share/man/man1 || true \
    && apt-get update; apt-get install -y java-21-amazon-corretto-jdk=1:$version \
    && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false curl gnupg software-properties-common \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* \
    && ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && echo "$TZ" > /etc/timezone && dpkg-reconfigure --frontend noninteractive tzdata

# Copy app from build stage
COPY build/install/core /app
COPY hibernate.cfg.xml /app/hibernate.cfg.xml

WORKDIR /app
EXPOSE 37100
CMD ["./bin/core"]