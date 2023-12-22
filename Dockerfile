FROM gradle:8.5.0-jdk21-jammy AS build
COPY --chown=gradle:gradle . /app
WORKDIR /app
RUN gradle clean installDist

FROM mcr.microsoft.com/playwright:v1.40.1-jammy
ARG version=21.0.1.12-1
ENV LANG C.UTF-8
ENV JAVA_HOME /usr/lib/jvm/java-21-amazon-corretto
ENV TZ Europe/Paris

# Install Java
RUN set -eux; apt-get update && apt-get install -y --no-install-recommends \
        curl ca-certificates gnupg software-properties-common fontconfig java-common \
    && curl -fL https://apt.corretto.aws/corretto.key | apt-key add - \
    && add-apt-repository 'deb https://apt.corretto.aws stable main' \
    && mkdir -p /usr/share/man/man1 || true \
    && apt-get update; apt-get install -y java-21-amazon-corretto-jdk=1:"$version" \
    && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
        curl gnupg software-properties-common \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

# Copy app from build stage
COPY --from=build /app/build/install/core /app
COPY --from=build /app/hibernate.cfg.xml /app/hibernate.cfg.xml

# Configure timezone and install necessary packages
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y tzdata libopencv-dev fonts-dejavu && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* && \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && echo "$TZ" > /etc/timezone && \
    dpkg-reconfigure --frontend noninteractive tzdata

WORKDIR /app
EXPOSE 37100
CMD ["./bin/core"]